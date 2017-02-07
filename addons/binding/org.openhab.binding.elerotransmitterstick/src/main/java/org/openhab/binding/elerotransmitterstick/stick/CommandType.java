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
 * The {@link CommandType} is the type of the {@link Command}.
 *
 * @author Volker Bier - Initial contribution
 */
public enum CommandType {
    UP((byte) 0x20) {
        @Override
        public ResponseStatus getResultStatus() {
            return ResponseStatus.TOP;
        }
    },
    INTERMEDIATE((byte) 0x44) {
        @Override
        public ResponseStatus getResultStatus() {
            return ResponseStatus.INTERMEDIATE;
        }
    },
    VENTILATION((byte) 0x24) {
        @Override
        public ResponseStatus getResultStatus() {
            return ResponseStatus.VENTILATION;
        }
    },
    DOWN((byte) 0x40) {
        @Override
        public ResponseStatus getResultStatus() {
            return ResponseStatus.BOTTOM;
        }
    },
    STOP((byte) 0x10) {
        @Override
        public ResponseStatus getResultStatus() {
            return null;
        }
    };

    private byte cmdByte;

    private CommandType(byte cmdByte) {
        this.cmdByte = cmdByte;
    }

    public byte getCmdByte() {
        return cmdByte;
    }

    public abstract ResponseStatus getResultStatus();

    public static CommandType getForPercent(int percentage) {
        for (CommandType c : values()) {
            if (c.getResultStatus() != null && c.getResultStatus().getPercentage() == percentage) {
                return c;
            }
        }

        return null;
    }
}