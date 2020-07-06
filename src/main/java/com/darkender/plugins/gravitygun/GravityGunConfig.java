package com.darkender.plugins.gravitygun;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class GravityGunConfig
{
    private static GravityGun base;
    private static final Set<Material> bannedBlocks = new HashSet<>();
    private static final Set<EntityType> bannedEntities = new HashSet<>();
    private static ConfiguredSound pickupSound;
    private static ConfiguredSound dropSound;
    private static ConfiguredSound repelSound;
    
    public static void init(GravityGun base)
    {
        GravityGunConfig.base = base;
    }
    
    public static void reload()
    {
        base.saveDefaultConfig();
        base.reloadConfig();
        base.getConfig().options().copyDefaults(true);
        base.saveConfig();
    
        bannedBlocks.clear();
        bannedEntities.clear();
        
        for(String key : base.getConfig().getStringList("banned-blocks"))
        {
            try
            {
                bannedBlocks.add(Material.valueOf(key.toUpperCase()));
            }
            catch(Exception e)
            {
                Bukkit.getLogger().warning("Material \"" + key + "\" is invalid");
            }
        }
        for(String key : base.getConfig().getStringList("banned-entities"))
        {
            try
            {
                bannedEntities.add(EntityType.valueOf(key.toUpperCase()));
            }
            catch(Exception e)
            {
                Bukkit.getLogger().warning("Entity \"" + key + "\" is invalid");
            }
        }
        
        pickupSound = new ConfiguredSound(base.getConfig().getConfigurationSection("pickup-sound"));
        dropSound = new ConfiguredSound(base.getConfig().getConfigurationSection("drop-sound"));
        repelSound = new ConfiguredSound(base.getConfig().getConfigurationSection("repel-sound"));
    }
    
    public static boolean isBannedBlock(Material type)
    {
        return bannedBlocks.contains(type);
    }
    
    public static boolean isBannedEntity(EntityType type)
    {
        return bannedEntities.contains(type);
    }
    
    public static boolean areBlocksAllowed()
    {
        return base.getConfig().getBoolean("allow-blocks");
    }
    
    public static boolean areTilesAllowed()
    {
        return base.getConfig().getBoolean("allow-tile-blocks");
    }
    
    public static boolean areEntitiesAllowed()
    {
        return base.getConfig().getBoolean("allow-entities");
    }
    
    public static boolean isAreaRepelAllowed()
    {
        return base.getConfig().getBoolean("allow-area-repel");
    }
    
    public static boolean isHeldRepelAllowed()
    {
        return base.getConfig().getBoolean("allow-held-repel");
    }
    
    public static double getAreaRepelPower()
    {
        return base.getConfig().getDouble("area-repel-power");
    }
    
    public static double getHeldRepelPower()
    {
        return base.getConfig().getDouble("held-repel-power");
    }
    
    public static double getPickupRange()
    {
        return base.getConfig().getDouble("pickup-range");
    }
    
    public static void playPickupSound(Location location)
    {
        pickupSound.play(location);
    }
    
    public static void playDropSound(Location location)
    {
        dropSound.play(location);
    }
    
    public static void playRepelSound(Location location)
    {
        repelSound.play(location);
    }
}
