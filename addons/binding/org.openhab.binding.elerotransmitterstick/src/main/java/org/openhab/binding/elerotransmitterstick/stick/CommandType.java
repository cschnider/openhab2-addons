package org.openhab.binding.elerotransmitterstick.stick;

/**
 * The {@link CommandType} is the type of the {@link Command}.
 *
 * @author Volker Bier - Initial contribution
 */
public enum CommandType {
    UP,
    INTERMEDIATE,
    VENTILATION,
    DOWN,
    STOP,
    INFO,
    CHECK,
    NONE;

    public static CommandType getForPercent(int percentage) {
        if (percentage == 0) {
            return UP;
        }

        if (percentage == 25) {
            return CommandType.INTERMEDIATE;
        }

        if (percentage == 75) {
            return CommandType.VENTILATION;
        }

        if (percentage == 100) {
            return CommandType.DOWN;
        }

        return null;
    }
}