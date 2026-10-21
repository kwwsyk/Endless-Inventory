# Search input regression checks (#49, #41)

## Build and scope

This fix targets branch `26.2`, starting at `c32785b`, mod version 1.1.4.1.
Issue #49 reports Minecraft 26.3, but the repository has no 26.3 branch and
`gradle.properties` permits Minecraft >=26.2 and <26.3. Do not change that range
to claim 26.3 compatibility. Verify the 26.2 client first; repeat the checks on
the actual 26.3 source/build when available. Issue #41 has insufficient detail
to establish that it has the same cause.

Use JDK 25 and the checked-in Gradle wrapper:

```sh
./gradlew test :common:compileJava :fabric:compileJava :neoforge:compileJava
./gradlew :fabric:runClient
```

On Windows use `gradlew.bat` instead of `./gradlew`. The compile/test command
does not exercise GUI input; the checks below require a running client.

## Setup

- Use English item names, default E inventory binding and I Endless Inventory
  binding, and enable attachment to the survival inventory.
- Store bread, stone and at least one other distinct item in Endless Inventory.
  Confirm they are visible on the All Items tab before each run.
- Start with Fabric and only required dependencies. Then repeat with JEI, and
  finally the issue reporter's mod environment on its matching game version.
- Repeat on NeoForge because `ScreenFramework` is shared. Repeat the filtering
  checks with both ALL and PART content transfer modes (server configuration),
  and with a dedicated server as well as singleplayer.

## Focused checks

Run each check through both E (attached screen) and I (standalone screen).

| Action | Expected result |
| --- | --- |
| Click search, type `bread` slowly, including `e` | Each character appears once. Only matching items remain after the normal server response. The inventory stays open. |
| Search for `zzzznomatch` | No items match. This distinguishes a working filter from unchanged results. |
| Select all and paste `stone` | The field and results change to stone, without reopening or clicking another control. |
| Backspace, Delete, cursor movement, selection, cut and paste | Editing behaves normally; every text change updates results. Navigation alone leaves text intact. |
| Right-click a nonempty search field | The field clears and all items return. No extra click is needed. |
| Focus search, move the pointer over a stored item, type letters matching the favorite/drop/hotbar bindings | Text remains editable; no item is favorited, dropped, moved or swapped. |
| Focus search, press E; then remap the inventory binding and type its printable key | The key becomes search text; it does not close the inventory. |
| Focus search, press Escape | The screen closes normally. |
| With search unfocused, press the inventory-close binding | The screen closes normally. |
| Click outside search, then use a normal favorite/hotbar shortcut over an item | Existing item shortcuts still work. |
| Switch to a page without a search field and use normal page shortcuts | Hidden search does not consume keys or characters. |
| Enter a query, close/reopen, resize the window, switch tabs | Restored search text and matching results agree; input still works after refocusing. |

## Where to inspect failures

1. **Input/state:** `EndlessInventoryScreen.charTyped` delegates exclusively to
   `ScreenFramework.charTyped`. The latter must call `searchBox.charTyped`.
   Attached screens can also reach the EditBox through the host widget pipeline.
2. **Filter recomputation:** the EditBox responder calls `refreshSearchResults`
   after mutations (typing, paste, deletion and clear). That method copies the
   field value into `ScreenFramework.searching` before calling
   `ItemPage.refreshItems`. `ItemDisplay` then rebuilds the cached view and sends
   the new search in `ItemPageContext` for remote contents. Inspect both ALL and
   PART modes if state changes but items do not.
3. **Accidental close:** while search is focused on a searchable page,
   `ScreenFramework.keyPressed` handles editing and consumes non-Escape keys
   before favorite/page shortcuts or `AbstractContainerScreen.keyPressed`.
   Fabric's `allowKeyPress` and NeoForge's key pre-event use that return value
   to cancel host processing. Escape remains available to the host screen.

When debugging, count EditBox mutations and `refreshSearchResults` calls for
one typed character, one paste, and a right-click clear. Each mutation should
produce one refresh, including when attached character handling is intercepted;
the same event must not insert text twice. Avoid committing diagnostic logs.
