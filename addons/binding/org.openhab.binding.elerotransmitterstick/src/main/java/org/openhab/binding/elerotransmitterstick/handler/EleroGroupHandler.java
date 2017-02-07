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
import java.util.HashSet;
import java.util.List;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.openhab.binding.elerotransmitterstick.config.GroupConfig;
import org.openhab.binding.elerotransmitterstick.handler.EleroTransmitterStickHandler.Channel;
import org.openhab.binding.elerotransmitterstick.stick.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EleroGroupHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroGroupHandler extends EleroChannelHandler {
    private final Logger logger = LoggerFactory.getLogger(EleroGroupHandler.class);

    List<Channel> channels;

    public EleroGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void setChannelIds() {
        GroupConfig config = getConfig().as(GroupConfig.class);
        String[] idsArr = config.channelids.split(",");

        ArrayList<Integer> ids = new ArrayList<>();
        for (String idStr : idsArr) {
            try {
                int id = Integer.parseInt(idStr);

                if (id > 0 && id < 16) {
                    ids.add(id);
                } else {
                    throw new IllegalArgumentException(
                            "id " + idStr + " specified in thing configuration is out of range 1..15");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid id " + idStr + " specified in thing configuration");
            }
        }

        channelIds = ids.toArray(new Integer[ids.size()]);
        channels = bridge.getChannels(channelIds);
    }

    @Override
    public void statusChanged(ResponseStatus status) {
        logger.debug("Received updated state {} for thing {}", status, getThing().getUID().toString());

        HashSet<ResponseStatus> stati = new HashSet<>();
        for (Channel c : channels) {
            stati.add(c.getStatus());
        }

        // if all channels have the same status use this as the group status. otherwise return NO_INFORMATION
        if (stati.size() == 1) {
            super.statusChanged(stati.iterator().next());
        } else {
            super.statusChanged(ResponseStatus.NO_INFORMATION);
        }

        updateStatus(ThingStatus.ONLINE);
    }
}
