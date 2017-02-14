package org.openhab.binding.elerotransmitterstick.stick;

public class TimedCommand extends Command {
    private int duration;

    public TimedCommand(CommandType cmd, int cmdDuration, int[] channels) {
        super(cmd, channels);

        duration = cmdDuration;
    }

    public int getDuration() {
        return duration;
    }
}
