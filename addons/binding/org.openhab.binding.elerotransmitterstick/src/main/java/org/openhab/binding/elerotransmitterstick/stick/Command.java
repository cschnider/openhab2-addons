/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.stick;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * The {@link Command} can be sent to an elero stick to invoke actions on channels of an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class Command {
    public final static byte EASY_CHECK = (byte) 0x4A;
    public final static byte EASY_SEND = (byte) 0x4C;
    public final static byte EASY_INFO = (byte) 0x4E;

    byte[] data;

    public Command(byte[] bytes) {
        data = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, data, 0, bytes.length);

        data[bytes.length] = checksum(data);
    }

    public long getTimeout() {
        if (data[2] == EASY_CHECK) {
            return 1000;
        }

        return 4000;
    }

    public byte[] tobytes() {
        return data;
    }

    public static Command createEasySend(CommandType command, Integer... channelIds) {
        byte[] channelBits = createChannelBits(channelIds);

        return new Command(
                new byte[] { (byte) 0xAA, 0x05, EASY_SEND, channelBits[0], channelBits[1], command.getCmdByte() });
    }

    public static Command createEasyInfo(Integer... channelIds) {
        byte[] channelBits = createChannelBits(channelIds);

        return new Command(new byte[] { (byte) 0xAA, 0x04, EASY_INFO, channelBits[0], channelBits[1] });
    }

    public static Command createEasyCheck() {
        return new Command(new byte[] { (byte) 0xaa, (byte) 0x02, EASY_CHECK });
    }

    @Override
    public String toString() {
        return bytesToHex(data);
    }

    /**
     * Create the two channel bytes for the given channel IDs
     *
     * @param channelIds channel ids (starting from 1)
     */
    private static byte[] createChannelBits(Integer... channelIds) {
        long channels = 0;

        for (int id : channelIds) {
            channels = channels + (1 << (id - 1));
        }

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort((short) (channels % 32768));

        return buffer.array();
    }

    private byte checksum(byte[] data) {
        long val = 0;

        for (byte b : data) {
            val += b;
        }

        val = val % 256;
        return (byte) (256 - val);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(ArrayList<Byte> bytes) {
        char[] hexChars = new char[bytes.size() * 2];
        for (int j = 0; j < bytes.size(); j++) {
            int v = bytes.get(j) & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 15; i++) {
            System.out.println("channel bytes for " + i + ": " + bytesToHex(createChannelBits(i)));
        }
    }
}