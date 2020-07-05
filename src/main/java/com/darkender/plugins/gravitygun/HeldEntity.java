package com.darkender.plugins.gravitygun;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class HeldEntity
{
    private final Player holder;
    private final Entity held;
    
    public HeldEntity(Player holder, Entity held)
    {
        this.holder = holder;
        this.held = held;
    }
    
    public void tick()
    {
        Location rayStart = holder.getEyeLocation();
        RayTraceResult rayTraceResult = rayStart.getWorld().rayTraceBlocks(rayStart, holder.getEyeLocation().getDirection(),
                5.0, FluidCollisionMode.NEVER, true);
        if(rayTraceResult != null)
        {
            Vector between = rayTraceResult.getHitPosition().subtract(rayStart.toVector())
                    .add(rayTraceResult.getHitBlockFace().getDirection().multiply(0.4));
            held.teleport(rayStart.add(between).subtract(0, 1.7, 0));
        }
        else
        {
            Location idealLocation = holder.getEyeLocation().add(holder.getEyeLocation().getDirection().multiply(5.0));
            held.teleport(idealLocation.subtract(0, 1.7, 0));
        }
    }
    
    public Player getHolder()
    {
        return holder;
    }
    
    public Entity getHeld()
    {
        return held;
    }
}
