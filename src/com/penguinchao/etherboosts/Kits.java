package com.penguinchao.etherboosts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Kits {
	private EtherBoosts main;
	private Connection connection;
        private boolean couldConnect;
	public Kits(EtherBoosts passedMain){
		main = passedMain;
		establishConnection();
		checkTables();
	}
	private void establishConnection(){
		String mysqlHostName= main.getConfig().getString("mysqlHostName");
		String mysqlPort	= main.getConfig().getString("mysqlPort");
		String mysqlUsername= main.getConfig().getString("mysqlUsername");
		String mysqlPassword= main.getConfig().getString("mysqlPassword");
		String mysqlDatabase= main.getConfig().getString("mysqlDatabase");
		String dburl = "jdbc:mysql://" + mysqlHostName + ":" + mysqlPort + "/" + mysqlDatabase;
		try{
			connection = DriverManager.getConnection(dburl, mysqlUsername, mysqlPassword);
                        couldConnect = true;
		}catch(Exception exception){
			main.getLogger().info("[ERROR] Could not connect to the database -- disabling EtherBoosts");
			Bukkit.getPluginManager().disablePlugin(main);
                        couldConnect = false;
		}
	}
	private void checkTables(){
                if(!couldConnect){
                    return;
                }
		String createQuery = "CREATE TABLE IF NOT EXISTS `etherboosts_kits` ( `player` VARCHAR(36) NOT NULL , `kitname` VARCHAR(200) NOT NULL , `readytime` VARCHAR(1000) NOT NULL ) ENGINE = InnoDB; ";
		try{
			java.sql.PreparedStatement sql = connection.prepareStatement(createQuery);
			sql.executeUpdate();
		}catch(SQLException e){
			main.getLogger().info("[ERROR] Could not check tables");
			e.printStackTrace();
		}
	}
        private void checkConnection(){
            if(!couldConnect){
                return;
            }
            try {
                if(connection.isClosed()){
                    main.getLogger().info("Connection was closed. Reopening it.");
                    establishConnection();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Kits.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
	protected static void showHelp(Player player){
		player.sendMessage(ChatColor.YELLOW+"-- /kit --");
		player.sendMessage(ChatColor.YELLOW+"/kit "+ChatColor.GREEN+"List the kits that you have available");
		player.sendMessage(ChatColor.YELLOW+"/kit [name] "+ChatColor.GREEN+"Redeem the specified kit");
	}
	private Set<String> getAllKits(){
		//TODO Alphabetize?
		return main.getConfig().getConfigurationSection("kits").getKeys(false);
	}
	protected void listKits(Player player){
		//List the kits that the player has access to; green kits are ready, red kits are not ready
		Set<String> kits = getAllKits();
		boolean hasKits = false;
		boolean hasShownHeader = false;
		for(String kit : kits){
			if(player.hasPermission("etherboosts.kit."+kit)){
				hasKits = true;
				if(!hasShownHeader){
					hasShownHeader = true;
					player.sendMessage(ChatColor.YELLOW + "-- Your Kits --");
				}
				 if(isKitReady(player, kit)){
					 player.sendMessage(ChatColor.YELLOW+"- "+ChatColor.GREEN+kit);
				 }else{
					 player.sendMessage(ChatColor.YELLOW+"- "+ChatColor.RED+kit);
				 }
			}
		}
		if(hasKits){
			player.sendMessage(ChatColor.YELLOW+"/kit [name] "+ChatColor.GREEN+" to redeem");
			player.sendMessage(ChatColor.YELLOW+"Key: "+ChatColor.GREEN+"Available"+ChatColor.YELLOW+" | "+ChatColor.RED+" Cooling Down");
		}else{
			player.sendMessage(ChatColor.RED+"You do not have access to any kits right now.");
		}
		
	}
	@SuppressWarnings("deprecation")
	protected void giveKit(Player player, String kitName){
		//Gives the player a kit; use this after the kit has been confirmed to be ready and permissions are checked
		Set<String> items = main.getConfig().getConfigurationSection("kits."+kitName+".items").getKeys(false);
		for(String currentItem : items){
			//Get Item
			int id = main.getConfig().getInt("kits."+kitName+".items."+currentItem+".id");
			short data = (short) main.getConfig().getInt("kits."+kitName+".items."+currentItem+".data");
			int count = main.getConfig().getInt("kits."+kitName+".items."+currentItem+".count");
			ItemStack giveMe = new ItemStack(id, count, data);
			//Give Item
			HashMap<Integer,ItemStack> excess = player.getInventory().addItem(giveMe);
			if(excess != null){
				for( Map.Entry<Integer, ItemStack> me : excess.entrySet() ){
					player.getWorld().dropItem(player.getLocation(), me.getValue() );
				}
			}
		}
		resetKitTime(player, kitName);
	}
	protected boolean isKitReady(Player player, String kitName){
		//Returns true if the time is past the minimim time for the kit to be ready
                checkConnection();
		UUID uuid = player.getUniqueId();
		Timestamp timeStamp = new Timestamp(new Date().getTime());
		long currentTime = timeStamp.getTime();
		long readyTime;
		String query = "SELECT * FROM `etherboosts_kits` WHERE `player` = '"+uuid.toString()+"' AND `kitname` = '"+kitName+"'; ";
		try {
			java.sql.PreparedStatement sql = connection.prepareStatement(query);
			ResultSet result = sql.executeQuery();
			if(result.next()){
				readyTime = result.getLong("readytime");
			}else{
				return true;
			}
		} catch (SQLException e) {
			main.getLogger().info("[ERROR] Could not check if kit ready");
			e.printStackTrace();
			return false;
		}
		if(readyTime > currentTime){
			return false;
		}else{
			return true;
		}
	}
	protected boolean isValidKit(String kitName){
		//Returns true if kitName is a kit in the configuration
		Set<String> kits = getAllKits();
		for(String kit : kits){
			if(kit.equals(kitName)){
				return true;
			}
		}
		return false;
	}
	private void resetKitTime(Player player, String kitName){
		//Goes in database and sets the kit time to the max wait time
                checkConnection();
		UUID uuid = player.getUniqueId();
		Timestamp timeStamp = new Timestamp( new Date().getTime() );
		long currentTime = timeStamp.getTime();
		long waitTime = main.getConfig().getLong("kits."+kitName+".cooldown");
		long readyTime = currentTime + (waitTime * 1000);
		String query1 = "DELETE FROM `etherboosts_kits` WHERE `player` = '"+uuid.toString()+"' AND `kitname` = '"+kitName+"';";
		String query2 = "INSERT INTO `etherboosts_kits` (`player`, `kitname`, `readytime`) VALUES ('"+uuid.toString()+"', '"+kitName+"', '"+readyTime+"');";
		try{
			java.sql.PreparedStatement sql = connection.prepareStatement(query1);
			sql.executeUpdate();
			java.sql.PreparedStatement sql2 = connection.prepareStatement(query2);
			sql2.executeUpdate();
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
}