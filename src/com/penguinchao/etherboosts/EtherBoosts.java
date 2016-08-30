package com.penguinchao.etherboosts;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EtherBoosts extends JavaPlugin {
	private DoubleExp doubleExp;
	private Kits kits;
	@Override
	public void onEnable(){
		saveDefaultConfig();
		doubleExp = new DoubleExp(this);
		kits = new Kits(this);
	}
	public void onDisable(){
		//saveConfig();
	}
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) { 
		if (cmd.getName().equalsIgnoreCase("doublexp")){
			if(sender instanceof Player){
				sender.sendMessage(ChatColor.RED+"You do not have permission");
			}else if(args.length == 1){
				if(args[0].equals("true") || args[0].equals("false")){
					Boolean enabled = Boolean.parseBoolean(args[0]);
					doubleExp.setDoubleExp(enabled);
					sender.sendMessage(ChatColor.GREEN+"Double EXP has been set to "+args[0]);
				}else{
					DoubleExp.showHelp(sender);
				}
			}else{
				DoubleExp.showHelp(sender);
			}
		}else if(cmd.getName().equalsIgnoreCase("kit")){
			if(sender instanceof Player){
				Player player = (Player) sender;
				if(args.length == 1){
					//Give kit
					if(kits.isValidKit(args[0])){
						if(player.hasPermission("etherboosts.kit."+args[0])){
							if(kits.isKitReady(player, args[0])){
								kits.giveKit(player, args[0]);
							}else{
								sender.sendMessage(ChatColor.RED+"That kit is not ready yet. Use /kit to list your available kits");
							}
						}else{
							sender.sendMessage(ChatColor.RED+"You do not have permission to use that kit. Use /kit to list your available kits");
						}
					}else{
						sender.sendMessage(ChatColor.RED+"That kit does not exist. Use /kit to list your available kits");
					}
				}else if(args.length == 0){
					//List Available Kits
					kits.listKits(player);
				}else{
					//Show help
					Kits.showHelp(player);
				}
			}else{
				sender.sendMessage(ChatColor.RED+"This command can only be sent by a player");
			}
		}
		return false;
	}
}
