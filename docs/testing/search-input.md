# Search input regression checks (#49)

## Build and scope

This branch targets Minecraft 26.3; use JDK 25.
The repository default is 1.21.1, while Issue #49 reports Minecraft 26.3.
Validate each artifact on its matching game version.

```sh
./gradlew test :common:compileJava :fabric:compileJava :neoforge:compileJava --no-daemon
./gradlew :fabric:runClient
```

On Windows use `gradlew.bat`. Compilation does not exercise GUI input.

## Setup and client checks

Use English item names, default E (vanilla inventory) and I (Endless Inventory)
bindings, and enable attachment to the survival inventory. Store bread, stone
and another distinct item in Endless Inventory. Use the All Items tab.
Start with Fabric and required dependencies, then repeat with JEI and the
reporter's remaining mods on the matching game version.

Run every row through both E (attached) and I (standalone):

| Action | Expected result |
| --- | --- |
| Click search and type `bread`, including `e` | Each character appears once; only matches remain; inventory stays open. |
| Replace the query with `zzzznomatch` | No matching items remain. |
| Select all and paste `stone` | Text and results update without reopening the screen. |
| Backspace, Delete, cut, paste, arrows and selection | Every text mutation updates results; navigation leaves text intact. |
| Right-click a nonempty search field | Text clears and all items return. |
| Focus search, hover an item and type favorite/drop/hotbar keys | No favorite, drop, move or swap action occurs. |
| Remap the inventory close key to another printable letter, then type it in search | Text appears; inventory stays open. Restore the binding afterwards. |
| Focus search and press Escape | Inventory closes normally. |
| Click outside search and press the inventory close key | Inventory closes normally. |
| Unfocus search and use favorite/hotbar shortcuts over an item | Existing shortcuts still work. |
| Switch to a page without search | Hidden search does not consume input. |
| Search, close/reopen, resize, switch tabs, then refocus | Text and results agree; editing still works. |

Repeat filtering with ALL and PART server content transfer modes, in singleplayer
and on a dedicated server. Wait for the normal server response in PART mode.
Repeat on the other supported loaders because ScreenFramework is shared.
Count text mutations and refreshSearchResults calls during debugging: a typed
character must appear once, not twice. Do not commit temporary diagnostic logs.

## Failure paths

- Input: EndlessInventoryScreen.charTyped delegates exclusively to
  ScreenFramework; attached Fabric screens intercept characters before the host
  widget pipeline and cancel events consumed by the framework.
- Filter: the EditBox responder copies its value into ScreenFramework.searching
  via refreshSearchResults, then refreshes the ItemPage. ItemDisplay rebuilds
  the local view and sends ItemPageContext for remote contents.
- Close: focused search handles editing and consumes non-Escape keys before
  favorite/page shortcuts and the host container. EditBox.keyPressed can return
  false for printable keys; their text arrives separately through charTyped.
  The attached-screen closing log records closure, not its underlying cause.

These are manual acceptance checks, not claims of a completed client test.
