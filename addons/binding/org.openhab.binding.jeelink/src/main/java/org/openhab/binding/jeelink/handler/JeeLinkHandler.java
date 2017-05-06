/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jeelink.handler;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jeelink.config.JeeLinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for a JeeLink USB Receiver thing.
 *
 * @author Volker Bier - Initial contribution
 */
public class JeeLinkHandler extends BaseBridgeHandler implements BridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(JeeLinkHandler.class);

    private JeeLinkConnection connection;
    private ArrayList<JeeLinkReadingConverter<?>> converters;

    private ScheduledFuture<?> connectJob;

    public JeeLinkHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        JeeLinkConfig cfg = getConfig().as(JeeLinkConfig.class);
        converters = SensorDefinition.createConverters(this, cfg.sketchName);

        try {
            connection = AbstractJeeLinkConnection.createFor(cfg, scheduler);
        } catch (java.net.ConnectException e) {
            updateStatus(ThingStatus.OFFLINE);
            logger.debug("JeeLinkHandler for port {} is OFFLINE.", connection.getPort());
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!connection.isOpen()) {
                    updateStatus(ThingStatus.OFFLINE);

                    try {
                        connection.openConnection();

                        String initCommands = cfg.initCommands;
                        if (initCommands != null && !initCommands.trim().isEmpty()) {
                            logger.debug("Setting init commands for port {}: {}", connection.getPort(), initCommands);
                            connection.setInitCommands(initCommands);
                        }

                        for (JeeLinkReadingConverter<?> cnv : converters) {
                            connection.addReadingConverter(cnv);
                        }
                        updateStatus(ThingStatus.ONLINE);
                        logger.debug("JeeLinkHandler for port {} is ONLINE.", connection.getPort());
                    } catch (ConnectException ex) {
                        logger.debug("JeeLinkHandler for port {} is OFFLINE.", connection.getPort());
                    }
                }
            }
        };
        connectJob = scheduler.scheduleWithFixedDelay(runnable, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUid, Command command) {
        logger.error("Thing {} channel {} does not support sending of commands!", getThing().getUID(),
                channelUid.getId());
    }

    @Override
    public void dispose() {
        if (connectJob != null) {
            connectJob.cancel(true);
            connectJob = null;
        }

        if (connection != null) {
            connection.removeReadingConverters();
            connection.closeConnection();
        }

        SensorDefinition.disposeConverters(this);

        super.dispose();
    }

    public <R extends Reading<R>> JeeLinkReadingConverter<R> getConverter(Class<R> clazz) {
        ArrayList<JeeLinkReadingConverter<R>> cs = new ArrayList<>();

        for (JeeLinkReadingConverter<?> c : converters) {
            if (c.convertsTo(clazz)) {
                return (JeeLinkReadingConverter<R>) c;
            }
        }

        return null;
    }
}
