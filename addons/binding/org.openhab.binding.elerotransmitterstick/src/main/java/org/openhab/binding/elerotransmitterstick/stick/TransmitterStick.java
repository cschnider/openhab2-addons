/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.elerotransmitterstick.stick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TooManyListenersException;

import org.openhab.binding.elerotransmitterstick.config.EleroTransmitterStickConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link TransmitterStick} is responsible for communicating with an elero transmitter stick.
 *
 * @author Volker Bier - Initial contribution
 */
public class TransmitterStick {
    private final Logger logger = LoggerFactory.getLogger(TransmitterStick.class);

    private final ArrayList<Byte> bytes = new ArrayList<>();

    private EleroTransmitterStickConfig config;
    private SerialPort serialPort;
    private boolean open;
    private Response response = null;

    public TransmitterStick(EleroTransmitterStickConfig stickConfig) {
        config = stickConfig;
    }

    public void openConnection() throws ConnectException {
        try {
            if (!open) {
                logger.info("Opening serial connection to port {}...", config.portName);

                CommPortIdentifier portIdentifier;

                try {
                    portIdentifier = CommPortIdentifier.getPortIdentifier(config.portName);
                    serialPort = portIdentifier.open("openhab", 3000);
                    open = true;

                    serialPort.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);

                    serialPort.addEventListener(new SerialPortEventListener() {
                        @Override
                        public void serialEvent(SerialPortEvent event) {
                            try {
                                if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                                    parseInput();
                                }
                            } catch (IOException ex) {
                                logger.error("elerotransmitterstick", "IOException reading from port {}!",
                                        config.portName);
                            }
                        }
                    });

                    serialPort.notifyOnDataAvailable(true);
                } catch (UnsupportedCommOperationException | TooManyListenersException ex) {
                    closeConnection();
                    throw ex;
                }
            } else {
                logger.debug("Serial connection to port {} is already open!", config.portName);
            }
        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException
                | TooManyListenersException ex) {
            throw new ConnectException(ex);
        }
    }

    public void closeConnection() {
        if (open) {
            logger.info("Closing serial connection to port {}...", config.portName);

            serialPort.close();
            open = false;
        } else {
            logger.debug("Serial connection to port {} is already closed or has active listeners!", config.portName);
        }
    }

    public EasyAck sendEasySend(CommandType cmd, Integer... channelIds) throws IOException {
        if (channelIds.length == 1) {
            return (EasyAck) sendPacket(Command.createEasySend(cmd, channelIds));
        }

        // this is a workaround for a bug in the stick firmware that does
        // not work when more than one channel is specified in a packet
        for (Integer id : channelIds) {
            sendPacket(Command.createEasySend(cmd, id));
        }

        return null;
    }

    public EasyConfirm sendEasyCheck() throws IOException {
        return (EasyConfirm) sendPacket(Command.createEasyCheck());
    }

    public EasyAck sendEasyInfo(Integer... channelIds) throws IOException {
        if (channelIds.length == 1) {
            return (EasyAck) sendPacket(Command.createEasyInfo(channelIds));
        }

        // this is a workaround for a bug in the stick firmware that does
        // not work when more than one channel is specified in a packet
        for (Integer id : channelIds) {
            sendPacket(Command.createEasyInfo(id));
        }

        return null;
    }

    // send a packet to the stick and wait for the response
    private synchronized Response sendPacket(Command p) throws IOException {
        Response r = response;

        synchronized (bytes) {
            response = null;
            logger.debug("Writing packet to stick: {}", p);
            serialPort.getOutputStream().write(p.tobytes());

            if (r != null) {
                return r;
            }

            try {
                logger.trace("Waiting {} ms for answer from stick...", p.getTimeout());
                bytes.wait(p.getTimeout());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            r = response;
            response = null;
        }

        logger.debug("Stick answered {} for packet {}.", r, p);
        return r;
    }

    private void parseInput() throws IOException {
        logger.trace("parsing input...");
        while (serialPort.getInputStream().available() > 0) {
            byte b = (byte) serialPort.getInputStream().read();
            bytes.add(b);
        }
        logger.trace("input parsed. buffer contains {} bytes.", bytes.size());
        analyzeBuffer();
    }

    private void analyzeBuffer() {
        // drop everything before the beginning of the packet header 0xAA
        while (!bytes.isEmpty() && bytes.get(0) != (byte) 0xAA) {
            logger.trace("dropping byte {} from buffer", bytes.get(0));
            bytes.remove(0);
        }

        logger.trace("buffer contains {} bytes: {}", bytes.size(), Command.bytesToHex(bytes));
        if (bytes.size() > 1) {
            // second byte should be length byte (has to be either 0x04 or 0x05)
            int len = bytes.get(1);
            logger.trace("packet length is {} bytes.", len);

            if (len != 4 && len != 5) {
                // invalid length, drop packet
                bytes.remove(0);
                analyzeBuffer();
            } else if (bytes.size() > len + 1) {
                // we have a complete packet in the buffer, analyze it
                // third byte should be response type byte (has to be either EASY_CONFIRM or EASY_ACK)
                byte respType = bytes.get(2);

                synchronized (bytes) {
                    if (respType == Response.EASY_CONFIRM) {
                        logger.trace("response type is EASY_CONFIRM.");

                        long val = bytes.get(0) + bytes.get(1) + bytes.get(2) + bytes.get(3) + bytes.get(4)
                                + bytes.get(5);
                        if (val % 256 == 0) {
                            response = new EasyConfirm(bytes.get(3), bytes.get(4), bytes.get(5));
                        } else {
                            logger.warn("invalid response checksum. Skipping response.");
                        }

                        bytes.notify();
                    } else if (respType == Response.EASY_ACK) {
                        logger.trace("response type is EASY_ACK.");

                        long val = bytes.get(0) + bytes.get(1) + bytes.get(2) + bytes.get(3) + bytes.get(4)
                                + bytes.get(5) + bytes.get(6);
                        if (val % 256 == 0) {
                            response = new EasyAck(bytes.get(3), bytes.get(4), bytes.get(5), bytes.get(6));
                        } else {
                            logger.warn("invalid response checksum. Skipping response.");
                        }

                        bytes.notify();
                    } else {
                        logger.warn("invalid response type {}. Skipping response.", respType);
                    }
                }

                bytes.remove(0);
                analyzeBuffer();
            }
        }
    }
}
