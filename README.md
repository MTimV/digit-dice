# Digit Dice

A no-numbers tie-breaker game for kids. Everyone holds a finger, one gets picked.

https://github.com/user-attachments/assets/639ce00f-8c5a-42e1-9b88-c62c86a7fb7a

With two young daughters, who goes first is always a competition. Part of parenting is finding creative solutions. This is how I came up with a no-numbers tie-breaker game for kids. Everyone holds a finger, one gets picked.

The no-numbers part is the whole point. My youngest cannot read yet, so a countdown clock or a scoreboard means nothing to her. Everything the game shows has to work without a single number or word. The ring closing around each finger is the countdown. The finger left glowing is the winner. Nothing on screen asks you to read it, so the kid it was built for can play it on her own.

## What it does

- Three looks for the markers, which the app calls digits. Plain dots, blue dinosaurs, or pink unicorns. Everyone gets a different one.
- Winner mode or loser mode. One picks who goes first, the other picks who sits out.
- Adjustable hold time from 1.5 to 4 seconds.
- Sound and vibration, both optional.
- Settings are remembered next time you open it.

## How it works

The screen is the whole game. Everyone puts a finger down and holds. A ring hangs wide around each finger, then rushes in and tightens. When the rings close, one finger is picked at random.

A few of the decisions came from watching my kids actually use it.

The winner glows and stays lit until every finger lifts. Early on, the second a winner showed, someone would pull their finger off and jam it back down to try again. Holding the result until all fingers are up ended the instant rematch.

A fingertip covers most of a little unicorn. So the winner hops up out from under the finger and lingers for a few seconds after you lift, which is the only way the kid who won actually gets to see their own creature.

Loser mode had to read without words too. The picked finger drains to grey and stays put while everyone else stays bright, so the one who lost is obvious at a glance.

## Running it

You need Android Studio. Clone the repo and open the project folder.

```
git clone https://github.com/MTimV/digit-dice.git
```

Open the folder in Android Studio, let it sync, and run it on an Android phone. It is built for touch, so the emulator with its single mouse pointer will not show it properly. Runs on Android 8.0 and up.

## Built with

- Kotlin
- Jetpack Compose, with the whole game drawn on a single canvas
- SoundPool for the sounds and SharedPreferences for the saved settings

Built with AI assistance. The design and the decisions are mine. Claude wrote most of the Kotlin.

## How the code is organized

The rules live apart from the drawing. Game.kt holds the entire rule set with no Android code in it at all, so the logic stands on its own. The screen reads that state and draws it. The rest is small and each piece does one thing.

**`Game.kt`** The rules. Who is playing, when the pick happens, when everything resets. No Android.

**`Digits.kt`** The artwork. The dots and the ten creature variants, drawn as vector paths.

**`DigitDiceScreen.kt`** The screen. Touch handling, the closing ring, and every reveal animation.

**`MainActivity.kt`** Sets up the full-screen window and keeps the screen awake.

**`Prefs.kt`** Saves and loads the settings.

**`Haptics.kt`** The vibrations.

**`Sfx.kt`** The sounds.

## Where it stands

Honest about the limits. It has never been on the Play Store. It targets Android 15 for now, which the store rules will require moving up before any public release. The art and the sounds are hand-built rather than sourced. A team split for more than two players got cut on purpose, because with two kids it adds nothing a coin flip would not.
