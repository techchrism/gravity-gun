package com.darkender.plugins.gravitygun;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class HeldEntity
{
    private final Player holder;
    private final Entity held;
    private final boolean blockEntity;
    private Location from;
    private Vector velocity;
    
    /**
     * Constructs a HeldEntity object
     * @param holder the player holding the entity
     * @param held the entity being held
     * @param blockEntity true if the held entity is an armor stand with a block on its head
     */
    public HeldEntity(Player holder, Entity held, boolean blockEntity)
    {
        this.holder = holder;
        this.held = held;
        this.blockEntity = blockEntity;
        this.from = this.held.getLocation();
        this.velocity = new Vector(0, 0, 0);
    }
    
    public boolean isValid()
    {
        return this.held.isValid() && !this.held.isDead();
    }
    
    /**
     * Called every tick - does teleportation and velocity checks
     * @return true if the entity is valid, false if the held entity is no longer valid
     */
    public boolean tick()
    {
        if(!isValid())
        {
            return false;
        }
        Location rayStart = holder.getEyeLocation();
        RayTraceResult rayTraceResult = rayStart.getWorld().rayTraceBlocks(rayStart, holder.getEyeLocation().getDirection(),
                5.0, FluidCollisionMode.NEVER, true);
        Location teleportSpot;
        if(rayTraceResult != null)
        {
            Vector between = rayTraceResult.getHitPosition().subtract(rayStart.toVector())
                    .add(rayTraceResult.getHitBlockFace().getDirection().multiply(0.4));
            teleportSpot = rayStart.add(between);
        }
        else
        {
            teleportSpot = holder.getEyeLocation().add(holder.getEyeLocation().getDirection().multiply(5.0));
        }
        
        ParticleUtils.displayCurve(ParticleUtils.getHandScreenLocation(holder.getEyeLocation(), false),
                teleportSpot,
                holder.getEyeLocation().add(velocity),
                Color.GRAY);
        if(teleportSpot.getWorld().equals(from.getWorld()))
        {
            velocity = teleportSpot.toVector().subtract(from.toVector());
        }
        from = teleportSpot.clone();
        
        if(blockEntity)
        {
            teleportSpot = teleportSpot.subtract(0, 1.7, 0);
        }
        else
        {
            held.setFallDistance(0.0F);
        }
        
        if(held.getType() == EntityType.PLAYER)
        {
            TeleportUtils.teleport((Player) held, teleportSpot);
        }
        else
        {
            held.teleport(teleportSpot);
        }
        return true;
    }
    
    public Player getHolder()
    {
        return holder;
    }
    
    public Entity getHeld()
    {
        return held;
    }
    
    public Vector getVelocity()
    {
        return velocity;
    }
    
    public boolean isBlockEntity()
    {
        return blockEntity;
    }
}
