package org.openhab.binding.elerotransmitterstick.stick;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedCommand extends Command {
    private final long origin;
    private final long delay;

    public DelayedCommand(CommandType cmd, long delayInMillis, int priority, int... channels) {
        super(cmd, channels);

        delay = delayInMillis;
        this.origin = System.currentTimeMillis();
        this.priority = priority;
    }

    @Override
    public int compareTo(Delayed delayed) {
        if (delayed == this) {
            return 0;
        }

        long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
        return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return super.toString() + " and delay " + getDelay(TimeUnit.MILLISECONDS);
    }

}
