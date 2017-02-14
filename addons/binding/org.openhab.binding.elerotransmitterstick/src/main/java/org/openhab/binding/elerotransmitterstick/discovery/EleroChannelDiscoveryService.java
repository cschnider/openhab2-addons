/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.discovery;

import static org.openhab.binding.elerotransmitterstick.EleroTransmitterStickBindingConstants.*;

import java.util.Collections;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.elerotransmitterstick.handler.EleroTransmitterStickHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EleroChannelDiscoveryService} is responsible for discovery of elero channels from an Elero Transmitter
 * Stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroChannelDiscoveryService extends AbstractDiscoveryService {
    private static final int DISCOVER_TIMEOUT_SECONDS = 30;
    private final Logger logger = LoggerFactory.getLogger(EleroChannelDiscoveryService.class);

    EleroTransmitterStickHandler bridge;

    /**
     * Creates the discovery service for the given handler and converter.
     */
    public EleroChannelDiscoveryService(EleroTransmitterStickHandler stickHandler) {
        super(Collections.singleton(THING_TYPE_ELERO_CHANNEL), DISCOVER_TIMEOUT_SECONDS, true);

        bridge = stickHandler;
    }

    @Override
    protected void startScan() {
        discoverSensors();
    }

    @Override
    protected void startBackgroundDiscovery() {
        discoverSensors();
    }

    private void discoverSensors() {
        if (bridge.getStick() == null) {
            logger.debug("Stick not opened, scanning postponed.");
            return;
        }

        int[] channelIds = null;

        try {
            while (channelIds == null) {
                channelIds = bridge.getStick().getKnownIds();

                if (channelIds == null) {
                    Thread.sleep(2000);
                }
            }

            for (int id : channelIds) {
                ThingUID sensorThing = new ThingUID(THING_TYPE_ELERO_CHANNEL, String.valueOf(id));

                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(sensorThing).withLabel("Channel " + id)
                        .withBridge(bridge.getThing().getUID()).withProperty(PROPERTY_CHANNEL_ID, id).build();
                thingDiscovered(discoveryResult);
            }
        } catch (InterruptedException e) {
            logger.warn("got interrupt while waiting for answer from elero stick {}",
                    bridge.getThing().getUID().getId());
            Thread.currentThread().interrupt();
        }
    }
}
