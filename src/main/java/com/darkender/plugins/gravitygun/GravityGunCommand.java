package com.darkender.plugins.gravitygun;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GravityGunCommand implements CommandExecutor, TabCompleter
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "You must be a player to run this command!");
            return true;
        }
        
        Player player = (Player) sender;
        player.getInventory().addItem(GravityGun.getGravityGun());
        sender.sendMessage(ChatColor.GREEN + "Got a gravity gun!");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        return Collections.emptyList();
    }
}
