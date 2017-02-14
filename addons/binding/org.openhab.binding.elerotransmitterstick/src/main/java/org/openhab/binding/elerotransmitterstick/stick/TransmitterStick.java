package org.openhab.binding.elerotransmitterstick.stick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

    private final SerialConnection connection;
    private final CommandWorker worker;

    private final HashMap<Integer, ArrayList<StatusListener>> allListeners = new HashMap<>();

    private EleroTransmitterStickConfig config;

    public TransmitterStick(EleroTransmitterStickConfig stickConfig) {
        config = stickConfig;
        connection = new SerialConnection(config.portName);
        worker = new CommandWorker(config.updateInterval);
    }

    public void initialize() throws ConnectException {
        logger.debug("Initializing Transmitter Stick...");
        connection.open();
        try {
            worker.startUpdates();
        } catch (Exception e) {
            connection.close();
            throw new ConnectException("Failed to query channels from stick", e);
        }
        logger.debug("Transmitter Stick initialized, worker running.");
    }

    public void dispose() {
        logger.debug("Disposing Transmitter Stick...");
        allListeners.clear();
        worker.terminateUpdates();
        connection.close();
        logger.debug("Transmitter Stick disposed.");
    }

    public int[] getKnownIds() {
        return worker.knownIds;
    }

    public void sendCommand(CommandType cmd, int... channelIds) throws IOException {
        worker.executeCommand(cmd, channelIds);
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

    static class DueCommandSet extends TreeSet<Command> {
        private static final long serialVersionUID = -3216360253151368826L;

        public DueCommandSet() {
            super(new Comparator<Command>() {
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
            super.remove(e);
            return super.add(e);
        }
    }

    class CommandWorker extends Thread {
        public int[] knownIds;
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final int updateInterval;

        private final BlockingQueue<Command> cmdQueue = new DelayQueue<Command>();

        CommandWorker(int updateInterval) {
            this.updateInterval = updateInterval;
            setDaemon(true);
        }

        void startUpdates() throws IOException, InterruptedException {
            Response r = null;
            while (r == null) {
                logger.debug("sending CHECK packet...");
                r = connection.sendPacket(CommandUtil.createPacket(CommandType.CHECK));

                if (r == null) {
                    Thread.sleep(2000);
                }
            }

            knownIds = r.getChannelIds();
            logger.debug("Worker found channels: {} ", Arrays.toString(knownIds));

            for (int id : knownIds) {
                logger.debug("adding INFO command for channel id {} to queue with a delay of 1000...", id);
                cmdQueue.add(new DelayedCommand(CommandType.INFO, 1000, Command.FAST_INFO_PRIORITY, id));
            }
            start();
        }

        void terminateUpdates() {
            terminated.set(true);

            // add a NONE command to make the thread exit from the call to take()
            cmdQueue.add(new Command(CommandType.NONE));
        }

        void executeCommand(CommandType command, int... channelIds) {
            logger.debug("adding command {} for channel ids {} to queue...", command, Arrays.toString(channelIds));
            cmdQueue.add(new Command(command, channelIds));
        }

        @Override
        public void run() {
            // list of due commands sorted by priority
            final DueCommandSet dueCommands = new DueCommandSet();

            logger.debug("worker started.");
            while (!terminated.get()) {
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
                        cmd = dueCommands.pollFirst();
                        logger.debug("active command is {}", cmd);

                        if (cmd.getCommandType() != CommandType.NONE) {
                            Response response = null;
                            if (cmd.getChannelIds().length > 1) {
                                // this is a workaround for a bug in the stick firmware that does not
                                // handle commands that are sent to multiple channels correctly
                                for (int id : cmd.getChannelIds()) {
                                    connection.sendPacket(CommandUtil.createPacket(cmd, id));
                                }
                            } else {
                                response = connection.sendPacket(CommandUtil.createPacket(cmd));
                            }

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
                    // TODO Auto-generated catch block
                } catch (Throwable t) {
                    logger.error("Unhandled throwable", t);
                }
            }

            logger.debug("worker finished.");
        }
    }
}