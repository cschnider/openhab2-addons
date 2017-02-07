/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.stick;

/**
 * The {@link EasyAck} is the response (to EasySend and EasyInfo commands) read from an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class EasyAck extends EasyConfirm {
    byte status;

    public EasyAck(byte upperChannelBits, byte lowerChannelBits, byte state, byte checksum) {
        super(upperChannelBits, lowerChannelBits, checksum);
        status = state;
    }

    public ResponseStatus getStatus() {
        return ResponseStatus.getFor(status);
    }

    @Override
    public String toString() {
        return "AA054D" + Command.bytesToHex(channelBytes) + Command.bytesToHex(new byte[] { status, checksum });
    }
}
