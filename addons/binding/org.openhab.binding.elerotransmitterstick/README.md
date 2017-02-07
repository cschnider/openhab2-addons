# Elero Transmitter Stick Binding

Allows to control Elero rollershutters with openHAB through a connected Elero Transmitter Stick. 

## Supported Things

### Elero Transmitter Stick (Bridge)

Represents the physical Elero Transmitter Stick connected to a USB port.

### Elero Channel (Thing)

Represents one of the channels of an Elero Transmitter Stick.

### Elero Group (Thing)

Represents a group of channels allowing to have a common status channel as well as sending a command to all channels of this group simultaneously. 

## Discovery

Background discovery is supported only for Elero Channels. Just press the button in order to put it into your inbox after you have successfully created an Elero Transmitter Stick.

## Thing Configuration

### Elero Transmitter Stick

* Port Name: The name of the port to which the stick is connected, e.g. /dev/ttyUSB0.
* Update Interval: The number of seconds to wait before polling a single channel again.

### Elero Group

* Channel IDs: A comma separated list of channel ids. These have to match the channelid property of the respective Elero Channel.

## Channels

* Control: Rollershutter channel allowing to control the Elero Channel(s). Supports UP, DOWN, STOP and the following distinct percentages:

Percentage | Rollershutter Command | Result
---|---|---
0 | UP | rollershutter drives completely up
25 | - | rollershutter drives to the INTERMEDIATE position
75 | - | rollershutter drives to the VENTILATION position
100 | DOWN | rollershutter drives completely down

* Status: Readonly channel providing a string with status information from the Elero Channel. Possible values are: 

Status | Rollershutter Percentage
---|---
NO_INFORMATION | - 
TOP | 0
BOTTOM | 100
INTERMEDIATE | 25
VENTILATION | 75
BLOCKING | 50
OVERHEATED | 50
TIMEOUT | 50
START_MOVE_UP | 50
START_MOVE_DOWN | 50
MOVING_UP | 50
MOVING_DOWN | 50
STOPPED | 50
TOP_TILT | 50
BOTTOM_INTERMEDIATE | 50
SWITCHED_OFF | 50
SWITCHED_ON | 50

In case of Elero Groups, the status is NO_INFORMATION as long as not all connectd channels have the same status, the status of the channels otherwise. 


## Example usage

```
Rollershutter Rollershutter1 (gRollershutters) {channel="elerotransmitterstick:elerochannel:1:control",autoupdate="false" }
String Rollershutter1State  {channel="elerotransmitterstick:elerochannel:1:status" } 

```