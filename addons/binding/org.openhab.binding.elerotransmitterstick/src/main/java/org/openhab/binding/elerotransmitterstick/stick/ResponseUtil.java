package org.openhab.binding.elerotransmitterstick.stick;

import java.util.Arrays;

public class ResponseUtil {

    public static Response createResponse(byte upperChannelByte, byte lowerChannelByte) {
        return new Response(getChannelIds(upperChannelByte, lowerChannelByte));
    }

    public static Response createResponse(byte upperChannelByte, byte lowerChannelByte, byte responseType) {
        return new Response(ResponseStatus.getFor(responseType), getChannelIds(upperChannelByte, lowerChannelByte));
    }

    /**
     * returns the list of channels (starting with 1)
     */
    private static int[] getChannelIds(byte upperChannelByte, byte lowerChannelByte) {
        int[] result = new int[16];
        int idx = 0;

        byte b = lowerChannelByte;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) > 0) {
                result[idx++] = i + 1;
            }
            b = (byte) (b >> 1);
        }

        b = upperChannelByte;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) > 0) {
                result[idx++] = i + 9;
            }
            b = (byte) (b >> 1);
        }

        return Arrays.copyOfRange(result, 0, idx);
    }

}
