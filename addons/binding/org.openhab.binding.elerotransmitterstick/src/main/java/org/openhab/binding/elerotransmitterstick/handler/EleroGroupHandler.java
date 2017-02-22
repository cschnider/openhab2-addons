/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.handler;

import java.util.Arrays;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.openhab.binding.elerotransmitterstick.config.EleroGroupConfig;
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

    ResponseStatus[] stati;

    public EleroGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void setChannelIds() {
        channelIds = parseChannelIds(getConfig().as(EleroGroupConfig.class).channelids);
        stati = new ResponseStatus[channelIds.length];
    }

    public static int[] parseChannelIds(String channelids) {
        String[] idsArr = channelids.split(",");
        int[] ids = new int[idsArr.length];
        int idx = 0;

        for (String idStr : idsArr) {
            try {
                int id = Integer.parseInt(idStr);

                if (id > 0 && id < 16) {
                    ids[idx++] = id;
                } else {
                    throw new IllegalArgumentException(
                            "id " + idStr + " specified in thing configuration is out of range 1..15");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid id " + idStr + " specified in thing configuration");
            }
        }

        return Arrays.copyOfRange(ids, 0, idx);
    }

    @Override
    public void statusChanged(int channelId, ResponseStatus status) {
        logger.debug("Received updated state {} for thing {}", status, getThing().getUID().toString());

        boolean same = true;
        for (int i = 0; i < channelIds.length; i++) {
            if (channelIds[i] == channelId) {
                stati[i] = status;
            } else {
                same = same && status == stati[i];
            }
        }

        // if all channels have the same status use this as the group status. otherwise return NO_INFORMATION
        if (same) {
            super.statusChanged(channelId, status);
        } else {
            super.statusChanged(channelId, ResponseStatus.NO_INFORMATION);
        }

        updateStatus(ThingStatus.ONLINE);
    }
}
