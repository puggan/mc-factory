# Changelog 
## v1.3.1
* Bugfix, when trying to insert more items then the stack allowed, the split mixed up the instered and the leftover counts. (Forge only)

## v1.3.0
* Auto Rebalance, if there are enough items to make another item, but they are not spread out in the correct slots, the Factory will split one of the stacks, and move it to an empty slot. At a speed of 1 stack-split per second.
* Bugfix, hoppers can now load materials in any slot, the sorting of input slots counted all empty slot as equal, and removed the duplicates.
* Bugfix, shift-clicking to fill the recipe, now correctly updates the current recipe.
* Bugfix, shift-clicking to fill the recipe, now put 1 item in each slot, and not more then one item in each slot. (Fabric only bug)

## v1.2.1
* Localization for Brazilian Portuguese (pr_br) by Mikeliro
* Replace SidedInvWrapper, to allow mods to insert and extract from any direction. (Forge only)

## v1.2.0
* use SidedInvWrapper to make it work together with other mods. (Forge only)
 Versions 1.1.1 only had support for ISidedInventory-handling,
 but some mods only have support the ITEM_HANDLER_CAPABILITY way.

## v1.1.1
* Add recipe: crafting table + iron block + redstone

## v1.1.0
* Ghost items in inputs-slots, so you know what goes where.
* Graphical update of slot status, see what slots are open/enabled.
* A lot of client/server sync issues solved

## v1.0.1
* Bugfix: server-side crash

## v1.0.0
* Offical release

## v0.0.2
* The hopper input now priorities empty slots

## v0.0.1
* First release
* No longer any known bug that crashes the game.
