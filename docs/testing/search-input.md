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
  ScreenFramework. Fabric uses ScreenKeyboardEvents.allowCharType at the keyboard
  dispatch boundary for attached and standalone EI, before default-phase
  listeners. The old GuiEventListener default-method injection did not cover
  screens that dispatch through overrides or ContainerEventHandler.
- Focus: Fabric consumes left/right clicks inside a visible, active attached
  search field, sets the host screen's focused child, and forwards cursor clicks
  and selection dragging explicitly. Outside clicks retain host processing.
- Filter: the EditBox responder copies its value into ScreenFramework.searching
  via refreshSearchResults, then refreshes the ItemPage. ItemDisplay rebuilds
  the local view and sends ItemPageContext for remote contents.
- Close: focused search handles editing and consumes non-Escape keys before
  favorite/page shortcuts and the host container. EditBox.keyPressed can return
  false for printable keys; their text arrives separately through charTyped.
  The attached-screen closing log records closure, not its underlying cause.

These are manual acceptance checks, not claims of a completed client test.

## Fabric / JEI focus regression

Run on Minecraft 26.3 with Fabric API 0.161.0+26.3 (repository dependency),
then with 0.164.0+26.3 (reporter environment), using matching JEI builds.
Repeat with JEI absent, then present. Test both E and I:

1. Focus JEI search and enter `jei-only`. Click EI search, type `bread` and
   paste `stone`. Only EI's text/results should change. No character may be
   inserted twice or appear in JEI. Repeat without moving the mouse, then with
   the mouse over JEI or a stored item while EI search remains focused.
2. Click the middle of EI text, use arrows, select text by dragging beyond the
   field, release, then type. Cursor placement and selection must work, without
   continuing to drag after release. Right-click clear must update the filter.
3. Type `e` and `i` with EI focused. It must remain open. Escape must still close.
4. Click JEI search again and type: only JEI should change. Click a vanilla slot
   and verify normal item/close shortcuts after EI has lost focus.
5. Repeat after resize, close/reopen, switching tabs and toggling attachment.

User feedback confirms NeoForge search works with the shared fix. This follow-up
changes Fabric only; repeat the basic NeoForge E/I checks as a regression check.
Runtime execution of this matrix remains required; source inspection alone does
not establish compatibility with every JEI event phase or mod combination.
