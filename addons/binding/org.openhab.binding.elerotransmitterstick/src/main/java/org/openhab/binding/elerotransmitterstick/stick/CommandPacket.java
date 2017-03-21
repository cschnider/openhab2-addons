package org.openhab.binding.elerotransmitterstick.stick;

public class CommandPacket {
    public final static byte EASY_CHECK = (byte) 0x4A;
    public final static byte EASY_SEND = (byte) 0x4C;
    public final static byte EASY_INFO = (byte) 0x4E;

    byte[] data;

    public CommandPacket(byte[] bytes) {
        data = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, data, 0, bytes.length);

        data[bytes.length] = checksum(data);
    }

    public byte[] getBytes() {
        return data;
    }

    public long getResponseTimeout() {
        if (data[2] == EASY_CHECK) {
            return 1000;
        }

        return 4000;
    }

    private byte checksum(byte[] data) {
        long val = 0;

        for (byte b : data) {
            val += b;
        }

        val = val % 256;
        return (byte) (256 - val);
    }

    @Override
    public String toString() {
        return CommandUtil.bytesToHex(data);
    }

}
