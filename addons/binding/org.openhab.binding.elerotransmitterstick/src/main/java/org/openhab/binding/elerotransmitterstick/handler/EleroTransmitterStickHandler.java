/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.elerotransmitterstick.config.EleroTransmitterStickConfig;
import org.openhab.binding.elerotransmitterstick.stick.EasyAck;
import org.openhab.binding.elerotransmitterstick.stick.ResponseStatus;
import org.openhab.binding.elerotransmitterstick.stick.TransmitterStick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EleroTransmitterStickHandler} is responsible for managing the connection to an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroTransmitterStickHandler extends BaseBridgeHandler implements BridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(EleroTransmitterStickHandler.class);

    private TransmitterStick stick;
    private ScheduledFuture<?> statusUpdateJob;
    private int updateInterval;

    private final Channel[] channels = new Channel[15];

    public EleroTransmitterStickHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUid, Command command) {
        logger.error("Bridge {} does not support sending of commands!", getThing().getUID());
    }

    @Override
    public void dispose() {
        if (statusUpdateJob != null) {
            statusUpdateJob.cancel(true);
            statusUpdateJob = null;
        }

        if (stick != null) {
            stick.closeConnection();
        }

        super.dispose();
    }

    @Override
    public void initialize() {
        for (int i = 0; i < 15; i++) {
            channels[i] = new Channel(ResponseStatus.NO_INFORMATION, i * 2, i + 1);
        }

        EleroTransmitterStickConfig config = getConfig().as(EleroTransmitterStickConfig.class);
        try {
            stick = new TransmitterStick(config);
            stick.openConnection();

            updateStatus(ThingStatus.ONLINE);
            logger.info("EleroTransmitterStickHandler for port {} is ONLINE.", config.portName);

            updateInterval = getConfig().as(EleroTransmitterStickConfig.class).updateInterval;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (getThing().getStatus() == ThingStatus.ONLINE && stick != null) {
                        for (Channel c : channels) {
                            if (c.hasListeners()) {
                                try {
                                    synchronized (channels) {
                                        if (c.needsUpdate()) {
                                            EasyAck r = stick.sendEasyInfo(c.channelId);
                                            if (r != null) {
                                                c.setStatus(r.getStatus());
                                            }
                                        } else {
                                            c.countdown();
                                        }
                                    }
                                } catch (Throwable t) {
                                    logger.error("EleroTransmitterStickHandler", t);
                                }
                            }
                        }
                    }
                }
            };
            logger.debug("Elero channel status polling job for thing {} ({}) created", getThing().getLabel(),
                    getThing().getUID());
            statusUpdateJob = scheduler.scheduleAtFixedRate(runnable, 0, 2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            logger.info("EleroTransmitterStickHandler for port {} is OFFLINE.", config.portName);
        }
    }

    public TransmitterStick getStick() {
        return stick;
    }

    public void addStatusListener(int channelId, StatusListener listener) {
        channels[channelId - 1].addListener(listener);
    }

    public void removeStatusListener(int channelId, StatusListener listener) {
        channels[channelId - 1].removeListener(listener);
    }

    public void triggerFastUpdate(int channelId) {
        synchronized (channels) {
            Channel c = channels[channelId - 1];
            c.timeToPoll = 0;
        }
    }

    class Channel {
        private int timeToPoll;
        private ResponseStatus status;
        private int channelId;
        private List<StatusListener> listeners = Collections.synchronizedList(new ArrayList<StatusListener>());

        public Channel(ResponseStatus status, int timeToPoll, int id) {
            this.status = status;
            this.timeToPoll = timeToPoll;
            channelId = id;
        }

        public boolean hasListeners() {
            return !listeners.isEmpty();
        }

        public void removeListener(StatusListener listener) {
            listeners.remove(listener);
        }

        public void addListener(StatusListener listener) {
            listeners.add(listener);
            listener.statusChanged(status);
        }

        public void countdown() {
            timeToPoll -= 2;
        }

        public boolean needsUpdate() {
            return timeToPoll <= 0;
        }

        public ResponseStatus getStatus() {
            return status;
        }

        public void setStatus(ResponseStatus status) {
            if (this.status != status) {
                this.status = status;

                synchronized (listeners) {
                    for (StatusListener l : listeners) {
                        l.statusChanged(status);
                    }
                }
            }

            if (!status.isMoving()) {
                timeToPoll = updateInterval;
            }
        }
    }

    public List<Channel> getChannels(Integer[] channelIds) {
        ArrayList<Channel> channelList = new ArrayList<>();

        for (Integer id : channelIds) {
            channelList.add(channels[id - 1]);
        }

        return channelList;
    }
}
