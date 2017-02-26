package org.openhab.binding.elerotransmitterstick.stick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openhab.binding.elerotransmitterstick.config.EleroTransmitterStickConfig;
import org.openhab.binding.elerotransmitterstick.handler.StatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmitterStick {
    private final Logger logger = LoggerFactory.getLogger(TransmitterStick.class);

    private final CommandWorker worker;

    private final HashMap<Integer, ArrayList<StatusListener>> allListeners = new HashMap<>();

    private final StickListener listener;

    private EleroTransmitterStickConfig config;

    public TransmitterStick(EleroTransmitterStickConfig stickConfig, StickListener l) {
        config = stickConfig;
        worker = new CommandWorker();

        listener = l;
    }

    public synchronized void initialize() {
        logger.debug("Initializing Transmitter Stick...");
        worker.start();
        logger.debug("Transmitter Stick initialized, worker running.");
    }

    public synchronized void dispose() {
        logger.debug("Disposing Transmitter Stick...");
        allListeners.clear();
        worker.terminateUpdates();
        logger.debug("Transmitter Stick disposed.");
    }

    public int[] getKnownIds() {
        return worker.knownIds;
    }

    public void sendCommand(CommandType cmd, int... channelIds) {
        worker.executeCommand(cmd, channelIds);
    }

    public void requestUpdate(int... channelIds) {
        worker.requestUpdate(channelIds);
    }

    public void addStatusListener(int channelId, StatusListener listener) {
        synchronized (allListeners) {
            ArrayList<StatusListener> listeners = allListeners.get(channelId);
            if (listeners == null) {
                listeners = new ArrayList<>();
                allListeners.put(channelId, listeners);
            }
            listeners.add(listener);
        }
    }

    public void removeStatusListener(int channelId, StatusListener listener) {
        synchronized (allListeners) {
            ArrayList<StatusListener> listeners = allListeners.get(channelId);
            if (listeners != null) {
                listeners.remove(listener);

                if (listeners.isEmpty()) {
                    allListeners.remove(channelId);
                }
            }
        }
    }

    private void notifyListeners(int channelId, ResponseStatus status) {
        synchronized (allListeners) {
            ArrayList<StatusListener> listeners = allListeners.get(channelId);
            if (listeners != null) {
                for (StatusListener l : listeners) {
                    l.statusChanged(channelId, status);
                }
            }
        }
    }

    /**
     * Make sure we have
     * - only one INFO for the same channel ids
     * - only one other command for the same channel ids
     */
    private static boolean prepareAddition(Command newCmd, Collection<Command> coll) {
        Iterator<Command> queuedCommands = coll.iterator();
        while (queuedCommands.hasNext()) {
            Command existingCmd = queuedCommands.next();

            if (Arrays.equals(newCmd.getChannelIds(), existingCmd.getChannelIds())) {
                // remove pending INFOs for same channel ids
                if (newCmd.getCommandType() == CommandType.INFO && existingCmd.getCommandType() == CommandType.INFO) {
                    if (existingCmd.getPriority() < newCmd.priority) {
                        // we have an older INFO command with same or lower priority, remove
                        queuedCommands.remove();
                    } else {
                        // existing has higher priority, skip addition
                        return false;
                    }
                }

                if (newCmd.getCommandType() != CommandType.INFO && existingCmd.getCommandType() != CommandType.INFO) {
                    // we have an older command for the same channels, remove
                    queuedCommands.remove();
                }
            }
        }

        return true;
    }

    static class DueCommandSet extends TreeSet<Command> {
        private static final long serialVersionUID = -3216360253151368826L;

        public DueCommandSet() {
            super(new Comparator<Command>() {
                /**
                 * Due commands are sorted by priority first and then by delay.
                 */
                @Override
                public int compare(Command o1, Command o2) {
                    if (o1.equals(o2)) {
                        return 0;
                    }

                    int d = o2.getPriority() - o1.getPriority();
                    if (d < 0) {
                        return -1;
                    }

                    if (d == 0 && o1.getDelay(TimeUnit.MILLISECONDS) < o2.getDelay(TimeUnit.MILLISECONDS)) {
                        return -1;
                    }
                    return 1;
                }
            });
        }

        @Override
        public boolean add(Command e) {
            if (TransmitterStick.prepareAddition(e, this)) {
                return super.add(e);
            }

            return false;
        }
    }

    class CommandWorker extends Thread {
        int[] knownIds;
        private HashSet<Integer> validIds = new HashSet<>();
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final int updateInterval;
        private final SerialConnection connection;

        private final BlockingQueue<Command> cmdQueue = new DelayQueue<Command>() {
            @Override
            public boolean add(Command e) {
                if (TransmitterStick.prepareAddition(e, this)) {
                    return super.add(e);
                }

                return false;
            }
        };

        CommandWorker() {
            connection = new SerialConnection(config.portName);
            updateInterval = config.updateInterval;
            setDaemon(true);
        }

        void terminateUpdates() {
            terminated.set(true);
            connection.close();

            // add a NONE command to make the thread exit from the call to take()
            cmdQueue.add(new Command(CommandType.NONE));
        }

        void requestUpdate(int... channelIds) {
            // this is a workaround for a bug in the stick firmware that does not
            // handle commands that are sent to multiple channels correctly
            if (channelIds.length > 1) {
                for (int channelId : channelIds) {
                    requestUpdate(channelId);
                }
            } else if (channelIds.length == 1) {
                logger.debug("adding INFO command for channel id {} to queue...", Arrays.toString(channelIds));
                cmdQueue.add(new DelayedCommand(CommandType.INFO, 0, Command.FAST_INFO_PRIORITY, channelIds));
            }
        }

        void executeCommand(CommandType command, int... channelIds) {
            // this is a workaround for a bug in the stick firmware that does not
            // handle commands that are sent to multiple channels correctly
            if (channelIds.length > 1) {
                for (int channelId : channelIds) {
                    executeCommand(command, channelId);
                }
            } else if (channelIds.length == 1) {
                logger.debug("adding command {} for channel ids {} to queue...", command, Arrays.toString(channelIds));
                cmdQueue.add(new Command(command, channelIds));
            }
        }

        @Override
        public void run() {
            // list of due commands sorted by priority
            final DueCommandSet dueCommands = new DueCommandSet();

            logger.debug("querying available channels...");
            while (!terminated.get() && knownIds == null) {
                waitConnected();

                try {
                    Response r = null;
                    while (r == null && !terminated.get() && connection.isOpen()) {
                        logger.debug("sending CHECK packet...");
                        r = connection.sendPacket(CommandUtil.createPacket(CommandType.CHECK));

                        if (r == null) {
                            Thread.sleep(2000);
                        }
                    }

                    if (r != null) {
                        knownIds = r.getChannelIds();
                        logger.debug("Worker found channels: {} ", Arrays.toString(knownIds));

                        for (int id : knownIds) {
                            validIds.add(id);
                        }

                        requestUpdate(knownIds);
                    }
                } catch (IOException e) {
                    logger.error("Got IOException communicating with the stick", e);
                    listener.connectionDropped(e);
                    connection.close();
                } catch (InterruptedException e) {
                    logger.error("Got interrupt while waiting for next command time", e);
                    Thread.currentThread().interrupt();
                }
            }

            logger.debug("worker started.");
            while (!terminated.get()) {
                waitConnected();

                try {
                    // in case we have no commands that are currently due, wait for a new one
                    if (dueCommands.size() == 0) {
                        logger.debug("No due commands, invoking take on queue...");
                        dueCommands.add(cmdQueue.take());
                        logger.trace("take returned {}", dueCommands.first());
                    }

                    if (!terminated.get()) {
                        // take all commands that are due from the queue
                        logger.trace("Draining all available commands...");
                        Command cmd;
                        int drainCount = 0;
                        while ((cmd = cmdQueue.poll()) != null) {
                            drainCount++;
                            dueCommands.remove(cmd);
                            dueCommands.add(cmd);
                        }
                        logger.trace("Drained {} commands, active queue size is {}, queue size is {}", drainCount,
                                dueCommands.size(), cmdQueue.size());

                        // process the command with the highest priority
                        cmd = dueCommands.first();
                        logger.debug("active command is {}", cmd);

                        if (cmd.getCommandType() != CommandType.NONE) {
                            Response response = connection.sendPacket(CommandUtil.createPacket(cmd));
                            // remove the command now we know it has been correctly processed
                            dueCommands.pollFirst();

                            if (response != null && response.hasStatus()) {
                                for (int id : response.getChannelIds()) {
                                    notifyListeners(id, response.getStatus());
                                }
                            }

                            if (cmd instanceof TimedCommand) {
                                long delay = 1000 * ((TimedCommand) cmd).getDuration();
                                logger.debug("adding timed command STOP for channel ids {} to queue with delay {}...",
                                        cmd.getChannelIds(), delay);

                                cmdQueue.add(new DelayedCommand(CommandType.STOP, delay, Command.TIMED_PRIORITY,
                                        cmd.getChannelIds()));
                            } else if (response != null && response.isMoving()) {
                                logger.debug("adding timed command INFO for channel ids {} to queue with delay 2000...",
                                        cmd.getChannelIds());

                                cmdQueue.add(new DelayedCommand(CommandType.INFO, 2000, Command.FAST_INFO_PRIORITY,
                                        cmd.getChannelIds()));
                            } else if (cmd.getCommandType() == CommandType.INFO) {
                                logger.debug("adding timed command INFO for channel ids {} to queue with delay {}...",
                                        cmd.getChannelIds(), updateInterval * 1000);

                                cmdQueue.add(new DelayedCommand(CommandType.INFO, updateInterval * 1000,
                                        Command.INFO_PRIORITY, cmd.getChannelIds()));
                            }
                        } else {
                            logger.trace("ignoring NONE command.");
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("Got interrupt while waiting for next command time", e);
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    logger.error("Got IOException communicating with the stick", e);
                    listener.connectionDropped(e);
                    connection.close();
                } catch (Throwable t) {
                    logger.error("Unhandled throwable", t);
                }
            }

            logger.debug("worker finished.");
        }

        private void waitConnected() {
            if (!connection.isOpen()) {
                while (!connection.isOpen() && !terminated.get()) {
                    try {
                        connection.open();
                        listener.connectionEstablished();
                    } catch (ConnectException e1) {
                        listener.connectionDropped(e1);
                    }

                    if (!connection.isOpen() && !terminated.get()) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            logger.error("Got interrupt while waiting for next command time", e);
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            logger.trace("finished waiting. connection open={}, terminated={}", connection.isOpen(), terminated.get());
        }
    }

    public interface StickListener {
        void connectionEstablished();

        void connectionDropped(Exception e);
    }
}