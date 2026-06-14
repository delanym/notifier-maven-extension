# Notifier Maven Extension

A Maven core extension that sends desktop or remote notifications when your build finishes.

Supports Windows (BurntToast), macOS (terminal-notifier), Linux (notify-send), and cross-platform fallback (SystemTray) for desktop notifications, plus Slack, Discord, Microsoft Teams, Telegram, Mastodon, and MQTT for remote/chat notifications. Multiple channels can be used simultaneously.

## Installation

Add to `.mvn/extensions.xml` in your project (or in `~/.m2/extensions.xml` for all projects):

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0">
    <extension>
        <groupId>io.github.delanym</groupId>
        <artifactId>notifier-maven-extension</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </extension>
</extensions>
```

## What you get

Notifications include the project name, build outcome, duration, and git branch:

**Title:** `Maven Success [12s] My Project`
**Message:** `on feature/auth*`

The title includes:
- **Build outcome** (Success / Failure)
- **Duration**
- **Project name**

The message body includes:
- **Git branch**, with a `*` suffix when the working tree is dirty after the build

## Configuration

Create `~/.m2/notifier.properties` to configure the extension. All properties can also be passed as system properties (e.g. `-Dnotifier.implementation=slack`).

### Quick reference

| Property | Default | Description |
|----------|---------|-------------|
| `notifier.implementation` | `auto` | Notifier name, or comma-separated list for multi-channel |
| `notifier.threshold` | `-1` | Minimum build duration (seconds) to trigger a notification. `-1` means always notify |
| `notifier.timeout` | `-1` | Timeout in milliseconds for desktop notification display. `-1` uses the notifier's default |

### Overrides

| Flag | Effect |
|------|--------|
| `-DnotifyWith=<name>` | Override `notifier.implementation` for a single invocation |
| `-DskipNotification=true` | Disable notifications entirely for a single invocation |

### Notification threshold

`notifier.threshold` suppresses notifications for fast builds. Set it to `30` and you'll only be notified when a build takes longer than 30 seconds. This is useful to avoid noise from quick `mvn compile` cycles while still being notified for full `mvn clean install` runs.

A value of `-1` (the default) means every build triggers a notification.

### Notification timeout

`notifier.timeout` controls how long desktop notifications remain visible, in milliseconds. A value of `-1` means the notifier uses its own default (typically 2-5 seconds).

### Auto-discovery (default)

With no configuration file, the extension auto-detects the best available desktop notifier for your OS:

| OS      | Tried first        | Fallback   |
|---------|--------------------|------------|
| Windows | burnttoast         | systemtray |
| macOS   | terminal-notifier  | systemtray |
| Linux   | notify-send        | systemtray |

### Multi-channel

Comma-separate implementation names to send to multiple channels at once:

```properties
notifier.implementation=burnttoast,slack
notifier.slack.webhook=https://hooks.slack.com/services/T00/B00/xxxx
```

## Notifiers

### Desktop

#### BurntToast (Windows 10/11)

Requires the [BurntToast](https://github.com/Windos/BurntToast) PowerShell module: `Install-Module -Name BurntToast`.

```properties
notifier.implementation=burnttoast
# notifier.burnttoast.sound=Default
```

#### terminal-notifier (macOS)

Requires [terminal-notifier](https://github.com/julienXX/terminal-notifier): `brew install terminal-notifier`.

```properties
notifier.implementation=terminal-notifier
# notifier.terminal-notifier.bin=terminal-notifier
# notifier.terminal-notifier.activateApplication=com.apple.Terminal
# notifier.terminal-notifier.sound=default
```

#### notify-send (Linux)

Requires `libnotify` (pre-installed on most desktops): `sudo apt install libnotify-bin`.

```properties
notifier.implementation=notify-send
# notifier.notify-send.bin=notify-send
```

#### SystemTray (cross-platform)

Uses Java AWT. No external dependencies.

```properties
notifier.implementation=systemtray
```

### Chat / webhook

#### Slack

Uses [incoming webhooks](https://api.slack.com/messaging/webhooks).

```properties
notifier.implementation=slack
notifier.slack.webhook=https://hooks.slack.com/services/T00/B00/xxxx
```

#### Discord

Uses [Discord webhooks](https://support.discord.com/hc/en-us/articles/228383668).

```properties
notifier.implementation=discord
notifier.discord.webhook=https://discord.com/api/webhooks/1234/abcd
```

#### Microsoft Teams

Uses [incoming webhooks](https://learn.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook).

```properties
notifier.implementation=teams
notifier.teams.webhook=https://outlook.office.com/webhook/...
```

#### Telegram

Requires a bot token from [@BotFather](https://t.me/BotFather) and the target chat ID.

```properties
notifier.implementation=telegram
notifier.telegram.token=123456:ABC-DEF
notifier.telegram.chatId=-1001234567890
```

#### Mastodon

Requires an access token with `write:statuses` scope.

```properties
notifier.implementation=mastodon
notifier.mastodon.instanceUrl=https://mastodon.social
notifier.mastodon.accessToken=your-access-token
# notifier.mastodon.visibility=unlisted
```

### MQTT

Publishes build events as JSON to an MQTT v5 topic. Useful for home automation dashboards or custom integrations.

MQTT is an **optional dependency**. If you use it, add the Paho client alongside the extension:

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0">
    <extension>
        <groupId>io.github.delanym</groupId>
        <artifactId>notifier-maven-extension</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </extension>
    <extension>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
        <version>1.2.5</version>
    </extension>
</extensions>
```

```properties
notifier.implementation=mqtt
notifier.mqtt.brokerUrl=tcp://localhost:1883
# notifier.mqtt.topic=notifier/builds
# notifier.mqtt.clientId=notifier-maven
# notifier.mqtt.username=user
# notifier.mqtt.password=pass
```

Published messages are JSON:

```json
{
  "title": "Maven Success [12s] my-project",
  "message": "on main",
  "level": "INFO"
}
```

## Building from source

Requires JDK 21+.

```bash
mvn clean verify
```

Integration tests for MQTT require Docker (Testcontainers) and are skipped automatically when Docker is not available.

## License

MIT
