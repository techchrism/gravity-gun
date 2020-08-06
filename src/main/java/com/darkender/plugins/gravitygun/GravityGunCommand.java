package com.darkender.plugins.gravitygun;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GravityGunCommand implements CommandExecutor, TabCompleter
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length == 0 || args[0].equals("get"))
        {
            if(!sender.hasPermission("gravitygun.cmd.get"))
            {
                sender.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
                return true;
            }
            
            if(!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You must be a player to run this command!");
                return true;
            }
    
            Player player = (Player) sender;
            player.getInventory().addItem(GravityGun.getGravityGun());
            sender.sendMessage(ChatColor.GREEN + "Got a gravity gun!");
        }
        else if(args[0].equals("reload"))
        {
            if(!sender.hasPermission("gravitygun.cmd.reload"))
            {
                sender.sendMessage(ChatColor.RED + "You don't have permission to run this command!");
                return true;
            }
            
            GravityGunConfig.reload();
            sender.sendMessage(ChatColor.GREEN + "Reloaded!");
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length != 1)
        {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        if(sender.hasPermission("gravitygun.cmd.get"))
        {
            options.add("get");
        }
        if(sender.hasPermission("gravitygun.cmd.reload"))
        {
            options.add("reload");
        }
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[0], options, completions);
        Collections.sort(completions);
        return completions;
    }
}
