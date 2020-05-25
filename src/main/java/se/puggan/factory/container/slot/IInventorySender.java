package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;

import java.util.ArrayList;
import java.util.List;

public interface IInventorySender {
    List<IIventoryListner> listners = new ArrayList<IIventoryListner>();
    public default void addListner(IIventoryListner il) {
        listners.add(il);
    }
    public default void tellListners(IInventory inv) {
        for(IIventoryListner l : listners) {
            l.inventoryUpdated(inv);
        }
    }
}
