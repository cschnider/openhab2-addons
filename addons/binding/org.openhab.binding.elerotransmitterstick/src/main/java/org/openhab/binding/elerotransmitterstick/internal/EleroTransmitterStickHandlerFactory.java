/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.internal;

import static org.openhab.binding.elerotransmitterstick.EleroTransmitterStickBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.elerotransmitterstick.discovery.EleroChannelDiscoveryService;
import org.openhab.binding.elerotransmitterstick.handler.EleroChannelHandler;
import org.openhab.binding.elerotransmitterstick.handler.EleroGroupHandler;
import org.openhab.binding.elerotransmitterstick.handler.EleroTransmitterStickHandler;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link EleroTransmitterStickHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroTransmitterStickHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(2);
    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_STICK);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ELERO_CHANNEL);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ELERO_GROUP);
    }

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_STICK)) {
            EleroTransmitterStickHandler bridgeHandler = new EleroTransmitterStickHandler((Bridge) thing);

            EleroChannelDiscoveryService discoveryService = new EleroChannelDiscoveryService(bridgeHandler);
            discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext.registerService(
                    DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));

            return bridgeHandler;
        } else if (thingTypeUID.equals(THING_TYPE_ELERO_CHANNEL)) {
            EleroChannelHandler h = new EleroChannelHandler(thing);

            return h;
        } else if (thingTypeUID.equals(THING_TYPE_ELERO_GROUP)) {
            EleroGroupHandler h = new EleroGroupHandler(thing);

            return h;
        }

        return null;
    }
}
