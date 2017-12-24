# TMI4s

## About
TMI4s (Twitch Messaging Interface for Scala) is an abstraction layer that is designed to help manage rate limits and maximize up time for Twitch chat bots.

The premise is that TMI4s will sit between one or more chat bots and mimic Twitch. Instead of connecting to Twitch IRC servers, you connect to TMI4s and the rest is handled for you.

## Features
TMI4s is still in it's infancy but hosts a variety of features that make it's use worthwhile.

### Managing Rate Limits
You never have to worry about rate limits again. You can send messages to TMI4s as fast as you'd like. It will queue up the messages you send and throttle them accordingly.

It currently rate limits `PRIVMSG` and `JOIN` messages, but whispers will be added soon.

### Managing Disconnects and Minimizing Downtime
It can take time to join thousands of Twitch channels. Traditionally, all of these channels are joined through a single connection to Twitch. If this connection goes down, you will face a large amount of  down time while you rejoin the dropped channels.

TMI4s alleviates this problem by distributing your joins to multiple connections. This way if one connection drops, you are only losing a small number of channels that were living on that connection. Those channels will be queued to be rejoined on a new connection.

TMI4s can also be configured to join each channel twice and automatically filter duplicate messages. If one of the connections drop, you still have a back up connection. Since `JOIN` messages are rate limited, channels are joined a second time only when you can afford the extra `JOIN`s.

## Execution
### Dependencies
Java: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

SBT: http://www.scala-sbt.org/

### Running TMI4s
From the root directory, run
```
sbt run
```

## Configuration
The configuration for TMI4s can be found in `/src/main/resources/application.conf`

In here, you can override defaults that can be found in `/src/main/resources/tmi4s.conf`

The config is structured like this:
```text
tmi4s {
  twitch {
    irc {
      host = "irc.chat.twitch.tv"
      port = 6667
      limits {
        // <number> of messages to let through every <per> milliseconds,
        //     Uses a rolling window.
        // You can find valid numbers here:
        //     https://dev.twitch.tv/docs/irc#irc-command-and-message-limits
        messages { // PRIVMSG
          number = 19
          per = 30000 // ms
        }
        joins { // JOIN
          number = 19
          per = 30000 // ms
        }
      }
    }
  }
  // Will join each channel twice to eliminate down time if set to true.
  // Duplicate messages are removed automatically.
  redundancy = true
}
```
