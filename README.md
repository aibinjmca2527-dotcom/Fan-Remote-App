# Tom's Remote – native Android (Java + XML) IR fan remote

Plain Android framework only (no AndroidX, no Compose, no Flutter, no dependencies, no internet).
Open the folder in Android Studio (Koala or newer), let Gradle sync, run on an IR-capable phone.
minSdk 24 · targetSdk/compileSdk 34 · only permission: `TRANSMIT_IR`.

## Architecture
```
MainActivity (UI) ──┐
                    ├─> FanCommandManager ─> IrTransmitter ─> ConsumerIrManager ─> fan
AssistantActivity ──┘        ^
 (Assistant / shortcuts)     └── FanRemoteConfig (ALL IR signals live here)
        └ AssistantCommandHandler (phrase -> command mapping)
```
| File | Role |
|---|---|
| `FanDialView` | Round 1-5 dial + BOOST centre (Canvas) |
| `RemoteButton` | All commands (POWER, SPEED_1..5, BOOST, TIMER, LIGHT, SWING + optional POWER_ON/OFF, SPEED_UP/DOWN) |
| `IrCommand` | One signal (carrier + µs pattern, optional repeats); `pronto()` / `raw()` builders |
| `FanRemoteConfig` | The only place with IR data |
| `IrTransmitter` | Emitter check, frequency check, transmit, error mapping |
| `FanCommandManager` | Shared engine + assumed state (power/speed) |
| `AssistantCommandHandler`, `AssistantActivity` | Voice entry point, same engine |

## Adding the signals (next step)
Open `FanRemoteConfig.java` and fill the static block, one line per button:
```java
pronto(RemoteButton.POWER, "0000 006D ...");
raw(RemoteButton.SPEED_1, 38000, new int[]{9000, 4500, 560, 560, ...});
```
Until a signal is added, that button shows "signal has not been added yet" and sends nothing.
Optional voice-only signals (only if your remote has them): `POWER_ON`, `POWER_OFF`, `SPEED_UP`, `SPEED_DOWN`.
If your fan has only one POWER (toggle) signal, voice "turn on/off" sends it only when the app's
assumed state says it will do the right thing.

## Google Assistant – how it works and limits
* `res/xml/shortcuts.xml` declares App Actions (`actions.intent.OPEN_APP_FEATURE`) with static
  shortcuts: fan on/off, speed up/down, speed 1-5. Each launches `AssistantActivity`, which calls
  the same `FanCommandManager` methods as the buttons, then closes.
* Free-form phrases are mapped in `AssistantCommandHandler`.
* Testing: App Actions must be tested with the *Google Assistant plugin for Android Studio*
  (App Actions Test Tool) and the app must be uploaded to a Play Console testing track for
  general availability. Until then, say e.g. "Hey Google, fan on in Tom's Remote" or use the
  launcher long-press shortcuts.
* Assistant's built-in "turn on the fan" phrase is normally routed to Google Home smart-home
  devices, not to a local app. Requests like "Hey Google, open speed 3 in Tom's Remote" go to this app.
  A true "turn on the fan" smart-home integration would need a cloud service, which this
  offline app intentionally does not include.
* **Locked phone:** `AssistantActivity` is marked `showWhenLocked`, but Android/Assistant decide
  whether an app may be opened over the lock screen. Often you'll be asked to unlock first. The app does not
  bypass the keyguard, use accessibility services, or keep the screen awake. Unlocked phone: works
  as above. Screen-off behaviour is not guaranteed on all phones.

## Honest state
IR is one-way, so ON/speed on screen is what the app last sent. "Reset display" (gear icon) re-syncs it.
