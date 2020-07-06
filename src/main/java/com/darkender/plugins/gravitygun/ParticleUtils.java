package com.darkender.plugins.gravitygun;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public class ParticleUtils
{
    public static Location getHandScreenLocation(Location loc, boolean offhand)
    {
        Location spawnFrom = loc.clone();
        Vector normal2D = spawnFrom.getDirection().clone().setY(0).normalize()
                .rotateAroundY((offhand ? 1 : -1) * (Math.PI / 2))
                .multiply(0.40).setY(-0.35);
        spawnFrom.add(normal2D);
        spawnFrom.add(loc.getDirection().clone().multiply(-0.3));
        return spawnFrom;
    }
    
    public static Color fadeBetween(Color first, Color second, double progress)
    {
        return Color.fromRGB(
                (int) (first.getRed() + ((first.getRed() - second.getRed()) * progress)),
                (int) (first.getGreen() + ((first.getGreen() - second.getGreen()) * progress)),
                (int) (first.getBlue() + ((first.getBlue() - second.getBlue()) * progress)));
    }
    
    public static void displayCurve(Location from, Location to, Location center, Color color)
    {
        int points = 30;
        for(int i = 0; i < points; i++)
        {
            double t = (1.0 / points) * i;
            
            Location l = new Location(from.getWorld(),
                    (Math.pow(1 - t, 2) * from.getX()) + (2 * (1 - t) * t * center.getX()) + (Math.pow(t, 2) * to.getX()),
                    (Math.pow(1 - t, 2) * from.getY()) + (2 * (1 - t) * t * center.getY()) + (Math.pow(t, 2) * to.getY()),
                    (Math.pow(1 - t, 2) * from.getZ()) + (2 * (1 - t) * t * center.getZ()) + (Math.pow(t, 2) * to.getZ()));
            
            l.getWorld().spawnParticle(Particle.REDSTONE, l, 1, new Particle.DustOptions(color, 0.3f));
        }
    }
}
