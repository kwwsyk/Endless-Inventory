
Endless Inventory
=========

📖 This README is also available in: [简体中文](README-zh_cn.md)


**Endless Inventory** is a Minecraft mod that provides an extensible inventory system with infinite capacity. 
It introduces a custom GUI to browse, sort, and filter items easily. 
Ideal for RPG-style gameplay or technical players who manage a vast number of items.

Versions
----

v 1.0.0 (Release) - Minecraft-1.21.1  
It's now under test, many features are expected to be implemented.

Features
-----
+ Open your endless inventory using a designated item (EndInv Accessor | endless_inventory:test_endinv) or press specific key I.
+ Paginated GUI with category filters and advanced sorting and searching.
+ config.toml to customize the behavior of EndInv to suit specific needs.
  + client config
    + Texture mode
    + Determine whether to attach endInv with menu screens.
    + EndInv menu/attachedScreen rows/columns
  + server config
    + Max size and max single item size player can insert
    + Auto pick mode (will soon move to other mods)

Menu Ops
------------
With attached endInv page screen:
+ `I` in gui: open `EndlessInventoryMenu`
+ `ctrl+Left Click` in menu: quick move item from menu to endInv.
+ `A` in page area: add item to bookmark
+ `A` in bookmark page area: remove item from bookmark

Commands
--------
`/endinv`
 - `backup` : backup `endless_inventories.dat` file
 - `new` : create a new Endless Inventory
 - - `public`/`restricted`/`private` : Accessibility of new EndInv default to `public`
 - `ofIndex` :
 - - `<no arg>` : get player's current endInv index in `levelEndInvs`
 - - `<index>`  : get endinv by index in `levelEndInvs`
 - - - `open` : open endinv, anyone can run this, but only the accessible players or ops can open.
 - - - `setDefault` : set player's default endInv in opening menu.
 - - - `setOwner`: set endinv's owner to executor player.
 - - - `addWhiteList` : add executor player to endinv's allowlist.
 - - - `removeWhiteList` : remove executor player from endinv's allowlist.
 - - - `setAccessibility <public/restricted/private>`: set endinv's accessibility.
 - - - `remove`: remove current endinv from level endinv data, before this a backup file will be created.
 - - - - `<forceRemove>: boolean` : this will force remove endinv though backup failed.

Installation
-------
+ Make sure NeoForge is installed and compatible with your Minecraft version.
+ Download the latest release of the Endless Inventory mod from the Releases page.
+ Place the `.jar` file in your `mods` folder.
+ Launch the game and enjoy your new inventory system!

Configuration
--------
This mod provides both client and server-side configurations:
+ config/endless_inventory-server.toml: Server behavior and inventory mode.
+ config/endless_inventory-client.toml: UI layout, sorting preferences, and GUI toggles.


Developer Guide
======
Prerequisites
----
Java 21 or later

Development Environment
--------
You can import the project into IntelliJ IDEA or Eclipse using the Gradle build script.
