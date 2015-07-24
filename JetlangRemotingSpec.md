# Specification for the language agnostic protocol for jetlang remoting #

# Features #

  * Asynchronous
  * TCP Based
  * Big Endian


# Message Types #


Header - 1 unsigned byte

## Heartbeat 1 ##
  * Header only. No content.

Sent on a configurable schedule. Used by receiver to detect activity timeouts.

## Disconnect 2 ##
  * Header only. No content.

Sent as a graceful disconnect/logout message.

## Subscription 3 ##
  * ubyte topicSize, ascii(topicsize) topic

subscription request sent from client to acceptor.

## Data 4 ##
  * ubyte topicSize, ascii(topicsize) topic, uInt32 bodySize, bytes(bodySize) body

Message sent on a given topic.

## Unsubscribe 5 ##
  * ubyte topicSize, ascii(topicsize) topic

Unsubscribe sent from client to acceptor.

## Data Request 6 ##
  * int32 reqId, ubyte topicSize, ascii(topicSize), uInt32 bodySize, bytes(bodySize) body

## Data Reply 7 ##
  * int32 reqId, ubyte topicSize, ascii(topicSize), uInt32 bodySize, bytes(bodySize) body