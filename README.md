
Endless Inventory
=========

Endless Inventory is a Minecraft mod providing an RPG like, infinite space inventory.

Versions
----

v 0.0.1 (Alpha) - Minecraft-1.21.1  
It's now under test, many features are expected to be implemented.

Features
-----
+ Player can access an Endless Inventory using specific item 'endless_inventory:test_endinv'
+ config.toml to customize the behavior of EndInv to suit specific needs.
  + client config
  + server config
    + Max size and max single item size player can insert

Features coming soon
----
+ config.toml to customize the behavior of EndInv to suit specific needs.
  + client config:
    + Way of sorting items
    + Texture of EndInv menu
    + EndInv menu size/rows
    + Classify, like component items will be in one entry
  + server config
    + How player can access his/her or other's EndInv
    + How player can use working space (coming soon feature)
    + How EndInv interacts with other
+ Player will access EndInv by press specific key.
+ When player loot, loots will enter EndInv automatically.
+ Compatibility with other mods
  + Capability with fluids, chemical and unlocking entries.
  + Classifies, like magic.
+ Technical
  + Api
  + Changing ItemDisplay logic, as now ItemDisplay acts as a container across client and server.\
With useless operations reserved, causing unnecessary performance and memory overhead.