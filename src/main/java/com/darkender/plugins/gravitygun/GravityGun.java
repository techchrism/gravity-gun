package com.darkender.plugins.gravitygun;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.UUID;

public class GravityGun extends JavaPlugin implements Listener
{
    public static NamespacedKey gravityGunKey;
    private HashMap<UUID, HeldEntity> heldEntities;
    
    @Override
    public void onEnable()
    {
        gravityGunKey = new NamespacedKey(this, "gravity-gun");
        heldEntities = new HashMap<>();
        
        GravityGunCommand gravityGunCommand = new GravityGunCommand();
        getCommand("gravitygun").setExecutor(gravityGunCommand);
        getCommand("gravitygun").setTabCompleter(gravityGunCommand);
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        {
            @Override
            public void run()
            {
                for(HeldEntity heldEntity : heldEntities.values())
                {
                    heldEntity.tick();
                }
            }
        }, 1L, 1L);
    }
    
    public static ItemStack getGravityGun()
    {
        ItemStack gravityGun = new ItemStack(Material.GOLDEN_HORSE_ARMOR, 1);
        ItemMeta meta = gravityGun.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Gravity Gun");
        meta.getPersistentDataContainer().set(gravityGunKey, PersistentDataType.BYTE, (byte) 1);
        gravityGun.setItemMeta(meta);
        return gravityGun;
    }
    
    public static boolean isGravityGun(ItemStack item)
    {
        if(item == null || !item.hasItemMeta())
        {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(gravityGunKey, PersistentDataType.BYTE);
    }
    
    private RayTraceResult raytraceFor(Player player)
    {
        Location rayStart = player.getEyeLocation();
        RayTraceResult rayTraceResult = rayStart.getWorld().rayTrace(rayStart, player.getEyeLocation().getDirection(),
                100.0, FluidCollisionMode.NEVER, true, 0.0, entity ->
                {
                    // Ensure the raytrace doesn't collide with the player
                    if(entity instanceof Player)
                    {
                        Player p = (Player) entity;
                        return (p.getEntityId() != player.getEntityId() && p.getGameMode() != GameMode.SPECTATOR && p.getGameMode() != GameMode.CREATIVE);
                    }
                    else
                    {
                        return (entity instanceof LivingEntity);
                    }
                });
        return rayTraceResult;
    }
    
    private void pickupBlock(Player player, Block block)
    {
        ArmorStand stand = player.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5),
                ArmorStand.class, armorStand ->
                {
                    armorStand.setHelmet(new ItemStack(block.getType()));
                    armorStand.setGravity(false);
                    armorStand.setVisible(false);
                    armorStand.setInvulnerable(true);
                    armorStand.setSilent(true);
                });
        heldEntities.put(player.getUniqueId(), new HeldEntity(player, stand));
        block.setType(Material.AIR);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.5f);
    }
    
    private void dropBlock(Player player)
    {
        HeldEntity heldEntity = heldEntities.get(player.getUniqueId());
        ArmorStand stand = (ArmorStand) heldEntity.getHeld();
        FallingBlock fallingBlock = stand.getWorld().spawnFallingBlock(stand.getLocation().add(0, 1.7, 0),
                stand.getHelmet().getType().createBlockData());
        fallingBlock.setVelocity(heldEntity.getVelocity());
        stand.remove();
        heldEntities.remove(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
    }
    
    @EventHandler
    public void onPlayerInteractArEntity(PlayerInteractAtEntityEvent event)
    {
        PlayerInventory i = event.getPlayer().getInventory();
        if(isGravityGun(event.getHand() == EquipmentSlot.HAND ? i.getItemInMainHand() : i.getItemInOffHand()))
        {
            event.setCancelled(true);
    
            Player p = event.getPlayer();
    
            // Keep from accidentally clicking more than once per second
            if(p.hasMetadata("last-used") && p.getMetadata("last-used").get(0).asLong() > (System.nanoTime() - (1000 * 1000000)))
            {
                return;
            }
            p.setMetadata("last-used", new FixedMetadataValue(this, System.nanoTime()));
    
            if(heldEntities.containsKey(p.getUniqueId()))
            {
                dropBlock(p);
            }
        }
    }
    
    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if(isGravityGun(event.getItem()))
        {
            event.setCancelled(true);
            
            // Right click - pick up or drop
            if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                Player p = event.getPlayer();
                
                // Keep from accidentally clicking more than once per second
                if(p.hasMetadata("last-used") && p.getMetadata("last-used").get(0).asLong() > (System.nanoTime() - (1000 * 1000000)))
                {
                    return;
                }
                p.setMetadata("last-used", new FixedMetadataValue(this, System.nanoTime()));
                
                if(heldEntities.containsKey(p.getUniqueId()))
                {
                    dropBlock(p);
                }
                else
                {
                    RayTraceResult ray = raytraceFor(p);
                    if(ray != null)
                    {
                        if(ray.getHitBlock() != null)
                        {
                            pickupBlock(p, ray.getHitBlock());
                        }
                    }
                }
            }
        }
    }
}
