package org.openhab.binding.elerotransmitterstick.stick;

import java.util.Arrays;

public class Response {
    private ResponseStatus status;
    private int[] channels;

    public Response(ResponseStatus status, int[] channels) {
        this.status = status;
        this.channels = channels;
    }

    public Response(int[] channels) {
        this.channels = channels;
    }

    public boolean isMoving() {
        return status == ResponseStatus.MOVING_DOWN || status == ResponseStatus.MOVING_UP;
    }

    public int[] getChannelIds() {
        return channels;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status + " for channels " + Arrays.toString(channels);
    }

}
