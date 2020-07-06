package com.darkender.plugins.gravitygun;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

public class ConfiguredSound
{
    private Sound sound = null;
    private float pitch = 0.0f;
    private float volume = 0.0f;
    private boolean enabled = false;
    
    public ConfiguredSound(ConfigurationSection section)
    {
        enabled = section.getBoolean("enabled");
        if(enabled)
        {
            pitch = (float) section.getDouble("pitch");
            volume = (float) section.getDouble("volume");
            try
            {
                sound = Sound.valueOf(section.getString("name").toUpperCase());
            }
            catch(Exception e)
            {
                enabled = false;
                Bukkit.getLogger().warning("Invalid sound \"" + section.getString("name") + "\"");
            }
        }
    }
    
    public void play(Location location)
    {
        if(enabled)
        {
            location.getWorld().playSound(location, sound, volume, pitch);
        }
    }
}
