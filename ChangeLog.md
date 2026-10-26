# Change Log
__branch 1.21.1__

## Unreleased — search input (#49)

- Keep focused search key events out of inventory-close and item shortcuts; preserve Escape and unfocused shortcuts.

## 更新日志

**警告：升级 mod 版本前应备份游戏存档。**

### 1. 布局优化和组件渲染、交互体验优化
- 修复附随菜单与 Endless Inventory Menu 中 PageSwitchBar 的布局问题：Tab 数量会根据 Page 高度自动收缩，Tab 显示位置恢复到旧版表现，上下翻页按钮与 Page 左侧边缘对齐。
- 修复 PageSwitchBar 上下按钮、ConfigButton、SearchBox、Page 区域之间的默认排列关系。
- 修复 SortTypeSwitchBox 与 ReverseSortButton 在附随菜单中的显示和点击响应问题。
- 调整 SortTypeSwitchBox 渲染层级：关闭状态不再压过 Page、Tab 或 Tooltip，展开列表仍显示在 Page 内容上方。
- 修复展开 SortTypeSwitchBox 覆盖物品时数量、耐久等 item decoration 可能渲染到组件上方的问题；被覆盖的物品本体仍会渲染。
- 修复 Fabric 附随菜单中 SortTypeSwitchBox、ReverseSortButton 的显示次序问题。
- 修复 Page 背景、SortTypeSwitchBox、PageSwitchBar 等区域在手持物品点击时可能被宿主菜单视为丢弃区域的问题。
- 修复 Page 内鼠标滚轮、hover、点击区域的相对坐标处理问题。

### 2. Mod 兼容
- 修复 JEI 与 Page 物品的兼容表现：在 Page 物品上使用 JEI 的配方/用途查询按键时，现在会按原版菜单物品的交互表现返回正确物品与点击区域。
- 同步修复 NeoForge、Forge、Fabric 的 JEI Page clickable ingredient 处理。
- 初步增加对 Iron's Spellbooks 和 Silent Gear 的页面兼容支持。

### 3. Auto Pick 系统优化
- 优化了 Auto Pick 的拾取逻辑，并加入更复杂的设置，用于更细粒度地控制物品、经验、直接进入 EndInv、先进入背包、拾取后进入 EndInv 等行为。
- 这些设置旨在预防潜在的 mod 冲突，尤其是其它 mod 同时处理掉落物或经验拾取时的冲突。
- 可使用 `/endinv autoPick enable` 应用推荐的自动拾取设置。
- 可使用 `/endinv autoPick print` 查看当前 Auto Pick 相关设置状态。

### 4. Bug 修复与设置/指令系统优化
- 修复 dedicated server 上附随菜单 screen 相关代码导致的服务端崩溃问题，来自 GitHub PR by tyh1023。
- 更新物品显示逻辑，目标是解决因 mod 冲突导致部分物品无法从 EndInv 中取出的问题。
- 优化设置和指令系统，使配置项展示、查询与修改更清晰。

## 1.1.3 -> 1.1.4 Release ChangeLog (English)

**Warning: Back up your game saves before upgrading the mod version.**

### 1. Layout, Component Rendering, and Interaction Improvements
- Fixed PageSwitchBar layout in attached menus and the Endless Inventory Menu: the number of visible tabs now shrinks according to available Page height, tab visuals have been restored to their previous left-side placement, and tab scroll buttons align with the Page edge.
- Fixed the default arrangement between PageSwitchBar scroll buttons, ConfigButton, SearchBox, and the Page area.
- Fixed SortTypeSwitchBox and ReverseSortButton display and interaction in attached menus.
- Adjusted SortTypeSwitchBox render layering: the closed box no longer renders above Page, tabs, or tooltips, while the opened dropdown still appears above Page content.
- Fixed item decorations, such as stack counts and durability overlays, rendering above an opened SortTypeSwitchBox; covered item bodies still render normally.
- Fixed Fabric attached-menu render ordering for SortTypeSwitchBox and ReverseSortButton.
- Fixed carried-item clicks on Page background, SortTypeSwitchBox, PageSwitchBar, and related UI areas being treated by the host menu as item-drop clicks.
- Fixed relative coordinate handling for Page mouse scrolling, hover checks, and click regions.

### 2. Mod Compatibility
- Fixed JEI compatibility with Page items: using JEI recipe/usage lookup keys on Page items now returns the correct item and clickable region, matching vanilla menu behavior more closely.
- Applied the JEI Page clickable ingredient fix across NeoForge, Forge, and Fabric.
- Added initial page compatibility support for Iron's Spellbooks and Silent Gear.

### 3. Auto Pick System Improvements
- Improved Auto Pick pickup logic and added more detailed configuration options for item drops, experience drops, direct EndInv pickup, inventory-first pickup, and EndInv-after-inventory flows.
- These settings are intended to reduce potential mod conflicts, especially when other mods also handle dropped items or experience pickup.
- Use `/endinv autoPick enable` to apply the recommended Auto Pick settings.
- Use `/endinv autoPick print` to inspect the current Auto Pick-related settings.

### 4. Bug Fixes and Settings/Command Improvements
- Fixed a server-side crash on dedicated servers when attaching menu screens, from the GitHub PR by tyh1023.
- Updated item display logic to address cases where some items could not be extracted from EndInv because of mod conflicts.
- Improved the settings and command systems so config entries are easier to display, inspect, and modify.

## 2026-07-24

### Part 1: 1.1.4 UI and interaction fixes
- Fixed attached-menu and Endless Inventory Menu PageSwitchBar layout: visible tab count now follows the available Page height, tab visuals were restored to the previous left-side alignment, and tab scroll buttons align with the Page edge.
- Fixed SortTypeSwitchBox and ReverseSortButton rendering/interaction in attached screens, including Fabric-only render ordering so these controls draw above the attached Page content.
- Adjusted SortTypeSwitchBox layering so only the opened dropdown is elevated; item bodies remain visible below it while item decorations such as counts and durability overlays are suppressed when covered.
- Fixed Page click handling so Page background and framework widgets consume carried-item clicks without being treated as item-drop areas; widget clicks also consume the matching mouse release.
- Fixed Page scroll and hover coordinate handling for attached menus.
- Fixed JEI clickable ingredient detection inside EndInv Pages across NeoForge, Forge, and Fabric by returning absolute slot areas that include Page grid margins.
- Updated config command output so ComplexConfigEntryImpl nodes print their full section state instead of only a section label.

### Part 2: Unpushed 1.21.1 changes and PR merge
- Reworked the common configuration system with typed config entries, complex config sections, JSON-backed config loading/saving, localized comments, lazy save helpers, and a deprecated legacy config-value bridge for older adapter code.
- Added server/client config registries for common options and migrated Fabric, Forge, and NeoForge config adapters toward the new shared config model.
- Added `/endinv config` command-tree generation for typed config values and `/endinv autoPick` command support for status, print, enable, and disable operations.
- Reworked attached-menu/EIM layout configuration into reusable page, tab, texture, and screen-layout config objects, including transparent/dedicated texture modes and runtime cached layout sync.
- Refactored Page rendering and interaction around GridPage, item-entry pages, page view containers, slot views, and the new PageSwitchBar/SortTypeSwitchBox widgets.
- Updated item display, starred page, segmented/classified pages, search/sort state, and page metadata syncing to match the new Page view model.
- Reworked EndlessInventory/SourceInventory internals, item key handling, item-state snapshots, save strategies, affinity handling, and cached client inventory updates for the 1.21.1 data model.
- Added fluid integration base types for future mod integration work.
- Added or updated NeoForge compat pages for Iron's Spellbooks and Silent Gear, with shared compat item/entry page bases.
- Refreshed JEI integration and recipe transfer code across loaders, including attached-screen transfer handling, clickable ingredient handling, and current EndInv content/metadata sync APIs.
- Updated Cloth Config integration screens for Fabric and Forge to reflect the new config layout and hidden-page controls.
- Reworked auto-pick configuration into item/exp drop option sections, added NeoForge item/experience pickup mixins, and adjusted pickup/drop defaults for inventory-after-EndInv and pick-to-EndInv flows.
- Registered missing Fabric networking for `BulkQuickMoveFromPagePayload` and updated loader event/command registration paths to current shared command signatures.
- Fixed several 1.21.1 regression bugs from the unpushed branch, including searched-content refresh crashes, item rendering/transfer edge cases, player join/opening sync issues, and dedicated-server attaching classloading behavior from the merged PR.

## 2025-10-18
- Bumped Gradle properties to target Minecraft 1.21.1 and refreshed loader/tooling versions for Forge, NeoForge, and Fabric builds.
- Reworked common/loader build scripts to resolve 1.21.1 dependencies (NeoForm, JEI, Curios, Cloth Config) and add missing central Maven mirrors.
- Began API migration by replacing deprecated `ResourceLocation` constructors with the new 1.21 static factories across common and loader integrations.
- Updated JEI recipe-transfer hooks to the new identifier helpers while preparing to restore optional startup configuration gating.
- Fixed EndlessInventoryMenu row auto-sizing when rows is set to 0 and avoided partial row-layout refreshes on an already-open menu screen.
- Reopen EndlessInventoryMenu after leaving its settings screen so layout changes apply immediately, and close settings when clicking transparent space outside the panel.
- Note: Further work remains to finish data-component migrations and stabilize Fabric tooling once upstream repositories are reachable.

#### draft from another branch

## 1.21.1-alpha.1

### Toolchain updates
- Raised the project-wide Java toolchain to 21 and removed the hardcoded Gradle wrapper JDK override to unblock 1.21.1 builds.

### Dependencies bumped
- None.

### API changes addressed
- Replaced direct ItemStack tag access with Custom Data component copies across serialization helpers and source inventory, aligning with Minecraft 1.21.1’s data component system without temporary helpers.
- Migrated inventory keys, codec serialization, JEI recipe transfer comparisons, and inventory equality checks to the new `minecraft:custom_data` component instead of legacy CompoundTag tags.
- Updated GUI and networking resources to use `ResourceLocation.fromNamespaceAndPath`/`ResourceLocation.parse` ahead of constructor removals in 1.21.1.

### Known issues
- Loader-specific wiring is not yet migrated; Fabric, Forge, and NeoForge entry points remain on 1.20.1 logic.

### Links
- Pending bootstrap PR.

## 12.1 1.1.0.4
Neoforge and forge: Fixed player next joining the game crashes when opening EndInv.
- In fact, the problem is that only the first player joined the server can receive the init data packet in @PlayerEvent.

## 11.21 1.1.0.3

### 1.1.0.3 of 1.21.1 forge
Loading and launching
Resources

### common
Fix:
- Throw armors in armor page
- Starred Item Page will now show starred items.
- Add translations for key bindings.

__branch 1.20.1__

## 2025, before Sept: *A Brief*

First release was published in May 2025, supports Neoforge 1.21.1. 
The version support forge 1.20.1 was released in Aug 15.  

# Sept 2025

## 9.9
Switch Forge dev tool from 'legacyforge'(by neoforge group) to 'forgeGradle'(the original forge) as an issue existed in legacyforge.  

**SLICE FROM README**
#### forge 1.20.1 runServer problem:
'error loading client class `LanServerPinger`'  
It seems like forge (or legacyforge) internal problem that it *marks* class LanServerPinger
with `@OnlyIn(Dist.CLIENT)` but when running dedicated server, the class will be loaded to ping the server
to lan and a `RuntimeException` will be thrown.  
Normally, the class will not be marked with that annotation: neoforge notates it
"//neo: mark this class as server loadable...(the brief meaning)", and
forge(forgegradle instead of legacyforge) just does not mark this annotation.
###### Solution
Config forge's server config `forge-server.toml`, possibly located in
`run/world/serverconfig`.  
Set `advertiseDedicatedServerToLan` to `false`. 

## 9.17
Fixed a bug where certain items (`tacz:workbench_b`) could not be taken out.   
This bug was caused by the specific block item `tacz:workbench_b` in TACZ, whose NBT could under certain conditions be assigned an empty value (`CompoundTag tag = null -> tag = {}`).  
The issue was also related to JEI’s refresh behavior, which made it hard to detect.

### Technical:
Used remote debugging (by adding JVM parameters `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005` in the launcher and connecting with IntelliJ IDEA’s remote debug).  
Also used a Mixin to monitor item NBT operations.  

### Fix
Add a configuration: `Convert Empty Tag` and apply it in `SourceInventory #takeItem & #addItem`. Let item with `tag={}` server as `tag=null` 


## 9.19
- Refactored SyncedConfig to only carry attaching/auto-pick flags and moved page layout storage into local client config, with servers updating via ItemPageContext.
- Updated EndlessInventory menu and client UI to bootstrap from cached PageData so screens open without extra server round-trips.
- Expanded Fabric client config/network hooks to persist sort/search preferences and keep cached flags in sync when packets arrive.
## 9.18
- Texture: add `DEDICATED_LOCATION` mode and optimize related loading/performance
- Creative Mode: fix item taking logic to handle empty NBT tags and improve reliability

### DEDICATED_LOCATION
DEDICATED_LOCATION allows using custom texture in resource packs, to use refer such locations:  
In `assets/endless_inventory/textures/gui/*`:
- `item_grid.png` for common pages, derived from chest menu's generic54.png
- `tabs.png` for page switch tabs, derived from achievements gui sprites
- `item_entry.png` for item entry display (enchantment book classify page), derived from generic54 by removing vertical rules to form row strips  
- - the process is to fill each row of the grid with a long strip, in FromResource mode, `renderBg` method of `ItemEntryDisplay.class` does it so X).

## 9.25
- Fabric: ported screen attachment lifecycle, client input handlers, and debug overlays to Fabric API events, and refreshed `fabric.mod.json` metadata for the loader entry points.

## 9.26
- Fabric: finished port by wiring client events, networking handlers, and mixins to the updated Fabric API so the module now compiles cleanly.
- Fabric: added accessor/mixin shims for screen widgets and player persistent data so config toggles persist without Forge APIs.
- Fabric: implemented reflection fallback for loot event hooks and centralized NBT helpers shared with the common module.

## 9.21 1.1.0-SNAPSHOT-2
- Added a crafter menu into Endless Inventory Menu which is for Jei's recipe handler.

### after release
- Rearranged the JAR filename so the loader name (Forge/Fabric) is prefixed for easier version identification.

## 9.23 1.1.0-Pre1
- \[Forge] Implemented EndlessInventory's JEI recipe transfer.
- \[MAYBE] Updated readme

## 9.26 1.1.0-Pre2
Changed core logic of DisplayPage,ItemPage and their subclasses and some payloads to let pages item modify and some other logic clearer and well-organized.   
* Coincidentally, the bug of SegClassifyItemDisplay's items un-seg-geg bug is incidentally fixed.

## 9.27 1.1.0-Pre2
\[Forge] Added curios mod integration: Curio classify page.

## 10.8 1.1.0-Pre3
Fixed:
- Update is not instant when SortType and search is switched.
- Fabric: launch and attack

todo: check every feature and utilities to reduce bugs; jei's mixined recipe transfer
fabric: search; equipment page's arrangement
forge: quick move item in survival mode will not sync to server, fabric has no such problem

# 10.12 1.1.0-Pre3
Dev aspect
- SyncedConfig#attaching only presents player's client attaching config.
- AttachingManager -> AttachingMonitor and I marked it `reserved`.

Bug: quick-move creative item picker's items seems cannot sync to server.
    Seems not? But once after jei transfer, creative added items vanished...

# Develop note 10.12 1.1.0-Pre3
There are no attachability check for EndInv operations in packet handle. So if some bug I left exists or who cheats, 
player can interact with EndInv without depending on EIM even server's attaching config let the player's menu not attachable.

If I do not make mistakes, `OpenEndInvPayload (server)` and `ScreenAttachment (forge/fabric | client)`
are the only classes who check attachability.

# 10.13
Forge: Added icon to curios page

Forge: Added jei mouse click handler, so press R,U... can apply on Page's items.
todo: fabric

Ensure Endless Inventory updates always advance the last modified timestamp so quick successive edits keep their ordering.

# 10.17
Fabric: fix search

# 1.1.0 Pre-3 Changelog
Forge:
- Fixed several bugs
- Added "curios page" for Curios
- Added config screen, cloth config api is a selectable dependency now
- Experimental: Use mixin on Jei's recipe transfer handler to let Jei import ingredients from Attaching Screen in any menu.
- - To open it: config startup config in `endless_inventory-common.toml`
- - This feature depends on Jei's (`15.20.0.112`) non-api codes, this means it's not stable.

Fabric:
- Fixed several bugs
- Added config screen, cloth config api is a selectable dependency now
\
# 10.17 Fabric autopick & metadata
- Fabric: removed reflection-based loot registration; use mixins and Fabric API where applicable to restore autopick.
- Fabric: block break drops are absorbed via Block.popResource/popExperience mixins; no entities when fully absorbed; XP repairs Mending gear then grants remainder.
- Fabric: mob death drops are absorbed by intercepting LivingEntity.dropFromLootTable and Entity.spawnAtLocation; XP handled via ExperienceOrb.award.
- Fabric: added server/client networking init split to avoid unregistered payloads; ensured clientbound/serverbound encoders registered on respective sides.
- Metadata: mods metadata updated to suggest optional Cloth Config on Fabric and optional cloth_config on Forge; relaxed Fabric loader and MC version ranges.

# 10.18 Fabric autopick fix
- Fabric: fix BlockDropMixin null breaker by capturing and clearing the breaker around Block.playerDestroy; ThreadLocal now set precisely during drops/XP awarding to restore reliable auto-pick.
- Forge: Optimistic autopick mechanic: fixed bug in picking skulker_box.

# 10.18 1.1.0 Release

Endless Inventory 1.1.0 is a feature-packed update for 1.20.1 (Forge/Fabric), with stability fixes and quality-of-life improvements. Highlights below are curated for CurseForge/Modrinth.

What’s new
- Attached Item Page overhaul: faster open from cached data, improved sorting, searching, and category browsing; bookmark support and better quick-move behavior.
- Crafter inside Endless Inventory Menu: toggle an embedded crafting grid, supports JEI recipe transfer into the crafter.
- Curios integration (Forge): a dedicated Curios page for quick access and management.
- Config screens: optional Cloth Config UI on both Forge and Fabric to tweak layout, textures, and behavior.
- Texture “DedicatedLocation” mode: ship or resource-pack custom skins for the attached UI (item grid, tabs, entry rows) without editing code.

Fabric parity and autopick
- Fabric port completed for 1.20.1 with feature parity to Forge where applicable.
- Auto-pick utility restored and hardened on Fabric: block and mob drops go straight to EndInv; XP first repairs Mending, then awards the remainder.
- Fixed a critical null-breaker bug in Fabric autopick by scoping the breaker during Block.playerDestroy, ensuring reliable capture through popResource/popExperience.

Fixes and polish
- Consistent search box input on Fabric; character typing bugs resolved.
- Corrected quick-move behaviors and slot validation; fewer desyncs and edge-case fails.
- Empty NBT tag handling: added “Convert Empty Tag” option to treat {} as null, fixing items that couldn’t be taken from pages in specific mods.
- Stable “last modified” ordering: enforced monotonic timestamps so recent changes sort correctly.
- Numerous UI and attachment lifecycle refinements for smoother screen overlays and inputs.

Compatibility and metadata
- Loaders: Forge 1.20.1 and Fabric 1.20.1.
- Optional integrations: Cloth Config (both loaders), JEI (Forge). Integration paths are guarded when mods are absent.
- Pack authors: refreshed mod metadata and texture hooks for resource-pack friendly theming.

Notes
- Auto-pick is configurable on the server and is still experimental in heavily modded stacks. Disable if another loot-capture mod conflicts.
- JEI recipe transfer mixin on Forge uses non-API internals; treat as experimental and report incompatibilities.

Thanks for testing the pre-releases and reporting issues — your feedback directly shaped this update. Enjoy the inventory freedom!
