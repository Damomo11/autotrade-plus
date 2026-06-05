# Autotrade Plus

Autotrade Plus is a Minecraft Fabric client mod for automatic villager trading.

It scans nearby villagers, opens their trading screen, runs the favorited trades from Item Scroller, closes the screen, and moves to the next villager automatically. It also provides profession filters and post-trade item dropping to make repeated villager trading easier.

## Features

- Automatically trade with nearby villagers.
- Use Item Scroller favorite trades for villager trading.
- Filter target villagers by profession.
- Search and select villager professions from the config screen.
- Drop selected items after trading to prevent inventory overflow.
- Search and select drop items from the config screen.
- Add custom profession IDs and item IDs for server datapack content.
- Show action bar progress for completed trades and villagers with no favorite trades.
- Configurable toggle keybind and config screen keybind.

## Requirements

- Minecraft 26.1, 26.1.1, or 26.1.2
- Fabric Loader
- Fabric API
- Cloth Config
- Item Scroller
- Mod Menu is optional, but recommended for opening the config from the mod list.

## Usage

- Toggle automatic trading with `~` by default.
- Open the config screen with `O` by default.
- Configure profession filters and drop items from Mod Menu or the in-game config key.
- Favorite the villager trades you want to run with Item Scroller before using automatic trading.

## Build

Install JDK 25, then run:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew build
```

The built mod jar will be generated at:

```text
build/libs/autotrade-plus-26.1.0-26.1.2.jar
```
