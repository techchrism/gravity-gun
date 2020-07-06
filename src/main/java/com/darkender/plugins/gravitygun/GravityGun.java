package com.darkender.plugins.gravitygun;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class GravityGun extends JavaPlugin implements Listener
{
    public static NamespacedKey gravityGunKey;
    public static NamespacedKey heldBlockKey;
    private HashMap<UUID, HeldEntity> heldEntities;
    private HashSet<UUID> justClicked;
    
    @Override
    public void onEnable()
    {
        gravityGunKey = new NamespacedKey(this, "gravity-gun");
        heldBlockKey = new NamespacedKey(this, "held-block");
        heldEntities = new HashMap<>();
        justClicked = new HashSet<>();
        
        GravityGunCommand gravityGunCommand = new GravityGunCommand();
        getCommand("gravitygun").setExecutor(gravityGunCommand);
        getCommand("gravitygun").setTabCompleter(gravityGunCommand);
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        {
            @Override
            public void run()
            {
                justClicked.clear();
                
                // Tick all held entities and drop them if they're invalid
                heldEntities.entrySet().removeIf(heldEntityEntry ->
                {
                    if(!(heldEntityEntry.getValue().tick()))
                    {
                        Player p = heldEntityEntry.getValue().getHolder();
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
                        return true;
                    }
                    return false;
                });
            }
        }, 1L, 1L);
        
        // Check for old held blocks when loaded
        for(World world : getServer().getWorlds())
        {
            for(Chunk chunk : world.getLoadedChunks())
            {
                cleanChunk(chunk);
            }
        }
    }
    
    @Override
    public void onDisable()
    {
        for(UUID key : heldEntities.keySet())
        {
            drop(Bukkit.getPlayer(key));
        }
    }
    
    public static ItemStack getGravityGun()
    {
        ItemStack gravityGun = new ItemStack(Material.GOLDEN_HORSE_ARMOR, 1);
        ItemMeta meta = gravityGun.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Gravity Gun");
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(Arrays.asList(
                ChatColor.DARK_AQUA + "Right Click",
                ChatColor.BLUE + " \u2022 Pick up / drop block or mob",
                "",
                ChatColor.DARK_AQUA + "Left Click",
                ChatColor.BLUE + " \u2022 Repel surrounding mobs",
                ChatColor.BLUE + " \u2022 Repel held block or mob"));
        
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
                20.0, FluidCollisionMode.NEVER, true, 0.0, entity ->
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
        // Create an armor stand with a block on its head
        // Other possible solutions include falling sand and actually placing the block in the world
        // Falling sand has an update interval of 20 ticks instead of every tick - https://hub.spigotmc.org/jira/browse/SPIGOT-2749
        // Placing a block in the world can have unintended consequences
        ArmorStand stand = player.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5),
                ArmorStand.class, armorStand ->
                {
                    armorStand.setHelmet(new ItemStack(block.getType()));
                    armorStand.setGravity(false);
                    armorStand.setVisible(false);
                    armorStand.setInvulnerable(true);
                    armorStand.setSilent(true);
                    armorStand.getPersistentDataContainer().set(heldBlockKey, PersistentDataType.BYTE, (byte) 1);
                });
        heldEntities.put(player.getUniqueId(), new HeldEntity(player, stand, true));
        block.setType(Material.AIR);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.5f);
    }
    
    private void pickupEntity(Player player, Entity entity)
    {
        heldEntities.put(player.getUniqueId(), new HeldEntity(player, entity, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.5f);
    }

    private Entity drop(Player player)
    {
        HeldEntity heldEntity = heldEntities.get(player.getUniqueId());
        Entity newEntity;
        if(heldEntity.isBlockEntity())
        {
            // Spawns in falling sand for the picked up block
            ArmorStand stand = (ArmorStand) heldEntity.getHeld();
            FallingBlock fallingBlock = stand.getWorld().spawnFallingBlock(stand.getLocation().add(0, 1.7, 0),
                    stand.getHelmet().getType().createBlockData());
            fallingBlock.setVelocity(heldEntity.getVelocity());
            stand.remove();
            newEntity = fallingBlock;
        }
        else
        {
            Entity held = heldEntity.getHeld();
            held.setVelocity(heldEntity.getVelocity());
            held.setFallDistance(0.0F);
            newEntity = held;
        }
    
        heldEntities.remove(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
        return newEntity;
    }
    
    /**
     * Checks the chunk for any armor stands used to hold blocks and removes them
     * @param chunk the chunk to check
     */
    private void cleanChunk(Chunk chunk)
    {
        for(Entity e : chunk.getEntities())
        {
            if(e.getType() == EntityType.ARMOR_STAND && e.getPersistentDataContainer().has(heldBlockKey, PersistentDataType.BYTE))
            {
                e.remove();
            }
        }
    }
    
    @EventHandler
    private void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event)
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
                drop(p);
            }
            else
            {
                pickupEntity(p, event.getRightClicked());
            }
            // Ensure the player doesn't open any entity inventories
            justClicked.add(p.getUniqueId());
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onInventoryOpen(InventoryOpenEvent event)
    {
        if(justClicked.contains(event.getPlayer().getUniqueId()))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        // Prevent the player from damaging entities with the gravity gun
        if(event.getDamager().getType() == EntityType.PLAYER)
        {
            Player damager = (Player) event.getDamager();
            if(isGravityGun(damager.getInventory().getItemInMainHand()))
            {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if(isGravityGun(event.getItem()))
        {
            event.setCancelled(true);
    
            Player p = event.getPlayer();
            // Keep from accidentally clicking more than once per second
            if(p.hasMetadata("last-used") && p.getMetadata("last-used").get(0).asLong() > (System.nanoTime() - (1000 * 1000000)))
            {
                return;
            }
            p.setMetadata("last-used", new FixedMetadataValue(this, System.nanoTime()));
            
            // Right click - pick up or drop
            if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                if(heldEntities.containsKey(p.getUniqueId()))
                {
                    drop(p);
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
                        else if(ray.getHitEntity() != null)
                        {
                            pickupEntity(p, ray.getHitEntity());
                        }
                    }
                }
            }
            else
            {
                // Left click - repel
                if(heldEntities.containsKey(p.getUniqueId()))
                {
                    Entity newEntity = drop(p);
                    newEntity.setVelocity(newEntity.getVelocity().add(p.getEyeLocation().getDirection().multiply(2.0)));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0F, 1.2F);
                }
                else
                {
                    List<Entity> nearby = p.getNearbyEntities(5, 5, 5);
                    if(nearby.size() > 0)
                    {
                        for(Entity e : nearby)
                        {
                            Vector between = e.getLocation().toVector().subtract(p.getLocation().toVector());
                            e.setVelocity(e.getVelocity().add(between.normalize().multiply(7.5 / (between.length() + 1))));
                        }
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0F, 1.2F);
                    }
                }
            }
        }
    }
    
    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event)
    {
        if(heldEntities.containsKey(event.getPlayer().getUniqueId()))
        {
            drop(event.getPlayer());
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event)
    {
        // Prevent the player from using the gun as horse armor
        if(event.getInventory() instanceof HorseInventory && isGravityGun(event.getCurrentItem()))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkLoad(ChunkLoadEvent event)
    {
        cleanChunk(event.getChunk());
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkUnload(ChunkUnloadEvent event)
    {
        cleanChunk(event.getChunk());
    }
}
