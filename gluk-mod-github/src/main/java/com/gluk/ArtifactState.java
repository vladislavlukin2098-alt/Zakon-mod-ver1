package com.gluk;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Кто сейчас включил какой артефакт (на сервере). */
public final class ArtifactState {
    public static final Set<UUID> FIRE = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> LIGHTNING = ConcurrentHashMap.newKeySet();

    private ArtifactState() {}

    public static boolean hasItem(Player player, Item item) {
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) if (s.is(item)) return true;
        for (ItemStack s : inv.offhand) if (s.is(item)) return true;
        return false;
    }
}
