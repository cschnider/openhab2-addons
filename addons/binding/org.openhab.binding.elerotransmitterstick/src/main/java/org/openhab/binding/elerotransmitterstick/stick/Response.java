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
 * The {@link Response} is a common interface for responses read from an elero stick.
 *
 * @author Volker Bier - Initial contribution
 */
interface Response {
    public final static byte EASY_CONFIRM = (byte) 0x4B;
    public final static byte EASY_ACK = (byte) 0x4D;
}