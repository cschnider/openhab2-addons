/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.handler;

import static org.openhab.binding.elerotransmitterstick.EleroTransmitterStickBindingConstants.*;

import java.io.IOException;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.elerotransmitterstick.stick.CommandType;
import org.openhab.binding.elerotransmitterstick.stick.EasyAck;
import org.openhab.binding.elerotransmitterstick.stick.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EleroChannelHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroChannelHandler extends BaseThingHandler implements StatusListener {
    private final Logger logger = LoggerFactory.getLogger(EleroChannelHandler.class);

    protected Integer[] channelIds;
    protected EleroTransmitterStickHandler bridge;

    public EleroChannelHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        bridge = (EleroTransmitterStickHandler) getBridge().getHandler();

        setChannelIds();
        for (Integer channelId : channelIds) {
            bridge.addStatusListener(channelId, this);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    protected void setChannelIds() {
        String channelIdStr = getThing().getProperties().get(PROPERTY_CHANNEL_ID);
        channelIds = new Integer[] { Integer.parseInt(channelIdStr) };
    }

    @Override
    public void dispose() {
        for (Integer channelId : channelIds) {
            bridge.removeStatusListener(channelId, this);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        try {
            EasyAck ack = null;
            if (channelUID.getIdWithoutGroup().equals(CONTROL_CHANNEL)) {
                if (command == UpDownType.UP) {
                    ack = bridge.getStick().sendEasySend(CommandType.UP, channelIds);
                } else if (command == UpDownType.DOWN) {
                    ack = bridge.getStick().sendEasySend(CommandType.DOWN, channelIds);
                } else if (command == StopMoveType.STOP) {
                    ack = bridge.getStick().sendEasySend(CommandType.STOP, channelIds);
                } else if (command instanceof PercentType) {
                    CommandType cmd = CommandType.getForPercent(((PercentType) command).intValue());
                    if (cmd != null) {
                        ack = bridge.getStick().sendEasySend(cmd, channelIds);
                    } else {
                        logger.warn("Unhandled command {}.", command);
                    }
                } else if (command != RefreshType.REFRESH) {
                    logger.warn("Unhandled command {}", command);
                }
            }

            if (ack != null) {
                statusChanged(ack.getStatus());

                // if we have an ack, then we have successfully send a command. now
                // tell the bridge to poll the channel faster until movement has stopped
                for (Integer channelId : channelIds) {
                    bridge.triggerFastUpdate(channelId);
                }
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE);
            logger.error("Failed to send command " + command + " to channel " + channelUID, e);
        }
    }

    @Override
    public void statusChanged(ResponseStatus status) {
        logger.debug("Received updated state {} for thing {}", status, getThing().getUID().toString());

        Channel sChan = getThing().getChannel(STATUS_CHANNEL);
        if (sChan != null) {
            updateState(sChan.getUID(), new StringType(status.toString()));
        }

        Channel rChan = getThing().getChannel(CONTROL_CHANNEL);
        if (rChan != null && status.getPercentage() != -1) {
            updateState(rChan.getUID(), new PercentType(status.getPercentage()));
        }

        updateStatus(ThingStatus.ONLINE);
    }
}
