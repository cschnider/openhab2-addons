package org.openhab.binding.elerotransmitterstick.stick;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CommandUtil {
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

    /**
     * Create the two channel bytes for the given channel IDs
     *
     * @param channelIds channel ids (starting from 1)
     */
    private static byte[] createChannelBits(int... channelIds) {
        long channels = 0;

        for (int id : channelIds) {
            channels = channels + (1 << (id - 1));
        }

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort((short) (channels % 32768));

        return buffer.array();
    }

    public static CommandPacket createPacket(Command cmd, int channelId) {
        return createPacket(cmd.getCommandType(), new int[] { channelId });
    }

    public static CommandPacket createPacket(Command cmd) {
        return createPacket(cmd.getCommandType(), cmd.getChannelIds());
    }

    public static CommandPacket createPacket(CommandType cmd, int... channelIds) {
        if (cmd == CommandType.INFO) {
            byte[] channelBits = createChannelBits(channelIds);

            return new CommandPacket(
                    new byte[] { (byte) 0xAA, 0x04, CommandPacket.EASY_INFO, channelBits[0], channelBits[1] });
        }

        if (cmd == CommandType.CHECK) {
            return new CommandPacket(new byte[] { (byte) 0xaa, (byte) 0x02, CommandPacket.EASY_CHECK });
        }

        byte[] channelBits = createChannelBits(channelIds);
        byte cmdByte = getCommandByte(cmd);

        return new CommandPacket(
                new byte[] { (byte) 0xAA, 0x05, CommandPacket.EASY_SEND, channelBits[0], channelBits[1], cmdByte });
    }

    private static byte getCommandByte(CommandType command) {
        switch (command) {
            case DOWN:
                return (byte) 0x40;
            case INTERMEDIATE:
                return (byte) 0x44;
            case STOP:
                return (byte) 0x10;
            case UP:
                return (byte) 0x20;
            case VENTILATION:
                return (byte) 0x24;
            default:
                throw new IllegalArgumentException("Unhandled command type " + command);

        }
    }
}
