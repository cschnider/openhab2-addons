/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.stick;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link EasyConfirm} is the response (to EasyCheck commands) read from an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class EasyConfirm implements Response {
    byte[] channelBytes;
    byte checksum;

    public EasyConfirm(byte upperChannelBits, byte lowerChannelBits, byte checksum) {
        channelBytes = new byte[] { upperChannelBits, lowerChannelBits };
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return "AA044B" + Command.bytesToHex(channelBytes) + Command.bytesToHex(new byte[] { checksum });
    }

    /**
     * returns the list of channels (starting with 1)
     */
    public List<Integer> getChannelIds() {
        List<Integer> result = new ArrayList<>();

        byte b = channelBytes[1];
        for (int i = 0; i < 8; i++) {
            if ((b & 1) > 0) {
                result.add(i + 1);
            }
            b = (byte) (b >> 1);
        }
        b = channelBytes[0];
        for (int i = 0; i < 8; i++) {
            if ((b & 1) > 0) {
                result.add(i + 9);
            }
            b = (byte) (b >> 1);
        }

        return result;
    }
}
