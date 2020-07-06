package com.darkender.plugins.gravitygun;

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
    private Location from;
    private Vector velocity;
    
    public HeldEntity(Player holder, Entity held)
    {
        this.holder = holder;
        this.held = held;
        this.from = this.held.getLocation();
        this.velocity = new Vector(0, 0, 0);
    }
    
    public void tick()
    {
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
        
        if(held.getType() == EntityType.ARMOR_STAND)
        {
            teleportSpot = teleportSpot.subtract(0, 1.7, 0);
        }
        
        if(teleportSpot.getWorld().equals(from.getWorld()))
        {
            velocity = teleportSpot.toVector().subtract(from.toVector());
        }
        from = teleportSpot;
        
        held.teleport(teleportSpot);
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
}
