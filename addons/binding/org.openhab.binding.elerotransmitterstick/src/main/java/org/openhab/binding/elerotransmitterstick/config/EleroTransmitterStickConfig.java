/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.config;

import org.openhab.binding.elerotransmitterstick.handler.EleroTransmitterStickHandler;

/**
 * The {@link EleroTransmitterStickConfig} holds configuration data of a
 * {@link EleroTransmitterStickHandler}
 *
 * @author Volker Bier - Initial contribution
 */
public class EleroTransmitterStickConfig {
    public String portName;
    public int updateInterval;
}
