MemAkka is a distibuted memcached implementation using akka framework as underlying engine, where each key is holded by actor.

# Installation

```
git clone git://github.com/agnek/memakka
cd memakka
sbt run
```

# Overview

Actually, I was impressed by [curiodb](https://github.com/stephenmcd/curiodb) and decided to do something similar. Main goal of this project is to get better experience with akka and akka-cluster tools.
Almost all of commands were implemented, but some are missing.

As in curiodb I hold each key entry as separate actor, that are sharded using akka-cluster extension.

# Design

## Connection handling
Due to memcached protocol doesn't support multiplexing all of command shoud be handled with same order as they have been sent.
I had to use FSM extensions to hold connection state. There can be four states for connection:

1. WaitingForCommand - initial state of connection. We are waiting for command to parse it.
2. WaitingForData - some of commands such as set can be followed by data frame. So we should parse data before we start to handle this message.
3. WaitingForResponse - we successfully parsed command and sent it to handling by keys actors and waiting for response.
4. WaitingForGetResponse - I made special state for get commands because of in this state we can wait for reply from more than one actor if the were more than one key in get query.

## Keys handling
As I said before every key is holding by own actor. I also used FSM pattern to describe is logic. It can be only two states:

1. Uninitialized - when actor was just created and handling first message.
2. Initialized - actor was created and holds some data.

I should write couple of words about creating actors in akka-cluster. If akka-cluster handling message to given key in sharded region it can just forward message to existing actor or create new if there were no actor and send message to it. And this feature works greatly with FSM in key actors. For example, when someone sends get message for missing key system just creates actor for this key and actor in Uninitialized state answers NOT_EXISTS.

# Missing features

Only plain tcp protocol was implemented and also some commands are missing now. For example flush_all is not implemented.


