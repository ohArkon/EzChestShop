package me.deadlight.ezchestshop.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class FloatingItem {

    private int entityID;
    private Player player;
    private Location location;

    static VersionUtils versionUtils;

    static {
        VersionUtils loaded = null;

        if (Utils.isFolia()) {
            loaded = instantiate("me.deadlight.ezchestshop.utils.v1_21_R1");
            if (loaded == null) {
                loaded = instantiate("me.deadlight.ezchestshop.utils.v1_20_R3");
            }
        } else {
            try {
                String packageName = Utils.class.getPackage().getName();
                String internalsName = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                loaded = (VersionUtils) Class.forName(packageName + "." + internalsName).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ignored) {
                // logged below if loading fails
            }
        }

        if (loaded == null) {
            Bukkit.getLogger().log(Level.SEVERE, "EzChestShop could not find a valid implementation for this server version.");
        } else {
            versionUtils = loaded;
        }
    }

    private static VersionUtils instantiate(String className) {
        try {
            return (VersionUtils) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ignored) {
            return null;
        }
    }

    public FloatingItem(Player player, ItemStack itemStack, Location location) {

        this.player = player;
        this.entityID = (int) (Math.random() * Integer.MAX_VALUE);
        this.location = location;
        versionUtils.spawnFloatingItem(player, location, itemStack, entityID);

    }

    public void destroy() {
        versionUtils.destroyEntity(player, entityID);
    }

    public void teleport(Location location) {
        this.location = location;
        versionUtils.teleportEntity(player, entityID, location);
    }

    public Location getLocation() {
        return location;
    }

}
