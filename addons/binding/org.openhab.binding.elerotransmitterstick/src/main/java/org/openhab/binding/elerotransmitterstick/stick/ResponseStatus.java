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
 * The {@link ResponseStatus} represents the status of a channel learned to an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public enum ResponseStatus {
    NO_INFORMATION((byte) 0x00) {
        @Override
        public int getPercentage() {
            return -1;
        }
    },
    TOP((byte) 0x01) {
        @Override
        public int getPercentage() {
            return 0;
        }
    },
    BOTTOM((byte) 0x02) {
        @Override
        public int getPercentage() {
            return 100;
        }
    },
    INTERMEDIATE((byte) 0x03) {
        @Override
        public int getPercentage() {
            return 25;
        }
    },
    VENTILATION((byte) 0x04) {
        @Override
        public int getPercentage() {
            return 75;
        }
    },
    BLOCKING((byte) 0x05),
    OVERHEATED((byte) 0x06),
    TIMEOUT((byte) 0x07),
    START_MOVE_UP((byte) 0x08) {
        @Override
        public boolean isMoving() {
            return true;
        }
    },
    START_MOVE_DOWN((byte) 0x09) {
        @Override
        public boolean isMoving() {
            return true;
        }
    },
    MOVING_UP((byte) 0x0a) {
        @Override
        public boolean isMoving() {
            return true;
        }
    },
    MOVING_DOWN((byte) 0x0b) {
        @Override
        public boolean isMoving() {
            return true;
        }
    },
    STOPPED((byte) 0x0d),
    TOP_TILT((byte) 0x0e),
    BOTTOM_INTERMEDIATE((byte) 0x0f),
    SWITCHED_OFF((byte) 0x10),
    SWITCHED_ON((byte) 0x11);

    private byte statusByte;

    private ResponseStatus(byte statusByte) {
        this.statusByte = statusByte;
    }

    public static ResponseStatus getFor(byte statusByte) {
        if (statusByte <= MOVING_DOWN.statusByte) {
            return ResponseStatus.values()[statusByte];
        }
        return ResponseStatus.values()[statusByte - 1];
    }

    public boolean isMoving() {
        return false;
    }

    public int getPercentage() {
        return 50;
    }
}