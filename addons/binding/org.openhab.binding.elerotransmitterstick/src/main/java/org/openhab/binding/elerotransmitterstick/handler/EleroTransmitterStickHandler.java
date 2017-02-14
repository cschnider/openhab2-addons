/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.elerotransmitterstick.config.EleroTransmitterStickConfig;
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

    public EleroTransmitterStickHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUid, Command command) {
        logger.error("Bridge {} does not support sending of commands!", getThing().getUID());
    }

    @Override
    public void dispose() {
        if (stick != null) {
            stick.dispose();
            stick = null;
        }

        super.dispose();
    }

    @Override
    public void initialize() {
        EleroTransmitterStickConfig config = getConfig().as(EleroTransmitterStickConfig.class);
        try {
            stick = new TransmitterStick(config);
            stick.initialize();

            updateStatus(ThingStatus.ONLINE);
            logger.info("EleroTransmitterStickHandler for port {} is ONLINE.", config.portName);
        } catch (Exception ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            logger.info("EleroTransmitterStickHandler for port {} is OFFLINE.", config.portName);
        }
    }

    public TransmitterStick getStick() {
        return stick;
    }

    public void addStatusListener(int channelId, StatusListener listener) {
        stick.addStatusListener(channelId, listener);
    }

    public void removeStatusListener(int channelId, StatusListener listener) {
        stick.removeStatusListener(channelId, listener);
    }
}