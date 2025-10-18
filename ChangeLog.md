# Change Log
__branch 1.21.1__

## 2025-10-18
- Bumped Gradle properties to target Minecraft 1.21.1 and refreshed loader/tooling versions for Forge, NeoForge, and Fabric builds.
- Reworked common/loader build scripts to resolve 1.21.1 dependencies (NeoForm, JEI, Curios, Cloth Config) and add missing central Maven mirrors.
- Began API migration by replacing deprecated `ResourceLocation` constructors with the new 1.21 static factories across common and loader integrations.
- Updated JEI recipe-transfer hooks to the new identifier helpers while preparing to restore optional startup configuration gating.
- Note: Further work remains to finish data-component migrations and stabilize Fabric tooling once upstream repositories are reachable.

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
