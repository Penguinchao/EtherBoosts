package com.penguinchao.etherboosts;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class DoubleExp implements Listener {
	private EtherBoosts main;
	private boolean doubleEnabled;
	private int expModifier;
	public DoubleExp(EtherBoosts passedMain){
		main = passedMain;
		doubleEnabled = main.getConfig().getBoolean("double-exp-enabled");
		expModifier = main.getConfig().getInt("double-exp-modifier");
		main.getServer().getPluginManager().registerEvents(this, main);
	}
	protected void setDoubleExp(boolean newValue){
		doubleEnabled = newValue;
		String newString;
		if(newValue){
			newString = "true";
		}else{
			newString = "false";
		}
		main.getConfig().set("double-exp-enabled", newString);
		main.saveConfig();
		main.getLogger().info("Double Exp has been set to "+newValue);
	}
	public static void showHelp(CommandSender sender){
		sender.sendMessage(ChatColor.YELLOW+"/doublexp [true/false]");
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onExpCollect(PlayerExpChangeEvent event){
		if(doubleEnabled){
			event.setAmount(event.getAmount() * expModifier);
		}
	}
}
