package me.deadlight.ezchestshop.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.logging.Level;

public abstract class ImprovedOfflinePlayer {

    protected OfflinePlayer player;
    protected boolean isOnline;
    protected boolean exists;

    public static ImprovedOfflinePlayer improvedOfflinePlayer;

    static {
        ImprovedOfflinePlayer loaded = null;

        if (Utils.isFolia()) {
            loaded = instantiate("me.deadlight.ezchestshop.utils.ImprovedOfflinePlayer_v1_21_R1");
            if (loaded == null) {
                loaded = instantiate("me.deadlight.ezchestshop.utils.ImprovedOfflinePlayer_v1_20_R3");
            }
        } else {
            try {
                String packageName = Utils.class.getPackage().getName();
                String internalsName = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                loaded = (ImprovedOfflinePlayer) Class
                        .forName(packageName + ".ImprovedOfflinePlayer_" + internalsName)
                        .newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ignored) {
                // logged below if loading fails
            }
        }

        if (loaded == null) {
            Bukkit.getLogger().log(Level.SEVERE, "EzChestShop could not find a valid implementation for this server version.");
        } else {
            improvedOfflinePlayer = loaded;
        }
    }

    private static ImprovedOfflinePlayer instantiate(String className) {
        try {
            return (ImprovedOfflinePlayer) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ignored) {
            return null;
        }
    }

    public ImprovedOfflinePlayer() {
    }

    public ImprovedOfflinePlayer(OfflinePlayer player) {
        this.player = player;
        this.isOnline = player.isOnline();
        if (!isOnline) {
            exists = loadPlayerData();
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean hasPlayedBefore() {
        return player.hasPlayedBefore();
    }

    public abstract ImprovedOfflinePlayer fromOfflinePlayer(OfflinePlayer player);

    public abstract int getLevel();

    public abstract void setLevel(int level);

    public abstract float getExp();

    public abstract void setExp(float exp);

    public abstract int getExpToLevel();

    public abstract boolean loadPlayerData();

    public abstract boolean savePlayerData();

}
