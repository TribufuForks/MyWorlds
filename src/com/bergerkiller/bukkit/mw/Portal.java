package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.util.config.Configuration;

public class Portal {
	private String name;
	private String destination;
	private Location location;
	
	public String getName() {
		return this.name;
	}
	public Location getLocation() {
		return this.location;
	}
	public String getDestinationName() {		
		return this.destination;
	}
	public Location getDestination() {
		Location loc = getPortalLocation(destination, true);
		if (loc == null) {
			String portalname = WorldManager.matchWorld(destination);
			World w = WorldManager.getWorld(portalname);
			if (w != null) {
				loc = w.getSpawnLocation();
			}
		}
		return loc;
	}
	public boolean hasDestination() {
		if (this.destination == null) return false;
		if (this.destination.trim().equals("")) return false;
		return getDestination() != null;
	}
	public boolean teleport(Player p) {
		return teleport(p);
	}
	public boolean teleport(Entity e) {
		Location dest = getDestination();
		if (dest != null && e != null) {
			e.teleport(dest);
			return true;
		}
		return false;
	}
			
	public void add() {
		portallocations.put(name, new Position(location));
	}
	public void remove() {
		remove(this.name);
	}		
	public boolean isAdded() {
		return portallocations.containsKey(name);
	}

	/*
	 * Getters and setters
	 */
	public static void remove(String name) {
		portallocations.remove(name);
	}
	public static Portal get(String name) {
		return get(getPortalLocation(name));
	}
	public static Portal get(Location signloc) {
		if (signloc == null) return null;
		return get(signloc.getBlock(), false);
	}
	public static Portal get(Location signloc, double radius) {
		Portal p = null;
		for (String portalname : portallocations.keySet()) {
			Location ploc = getPortalLocation(portalname);
			if (ploc != null && ploc.getWorld() == signloc.getWorld()) {
				double distance = ploc.distance(signloc);
				if (distance <= radius) {
					Portal newp = Portal.get(ploc);
					if (newp != null) {
						p = newp;
						radius = distance;
					} else if (ploc.getWorld().isChunkLoaded(ploc.getBlockX() >> 4, ploc.getBlockZ() >> 4)) {
						//In loaded chunk and NOT found!
						//Remove it
						portallocations.remove(portalname);
						MyWorlds.log(Level.WARNING, "Removed portal '" + portalname + "' because it is no longer there!");
						//End the loop and call the function again
						return get(signloc, radius);
					}
				}
			}
		}
		return p;
	}
	
	public static Portal get(Block signblock, boolean loadchunk) {
		int cx = signblock.getLocation().getBlockX() >> 4;
		int cz = signblock.getLocation().getBlockZ() >> 4;
		if (!signblock.getWorld().isChunkLoaded(cx, cz)) {
			if (loadchunk) {
				signblock.getWorld().loadChunk(cx, cz);
			} else {
				return null;
			}
		}
		if (signblock.getState() instanceof Sign) {
			return get(signblock, ((Sign) signblock.getState()).getLines());
		}
		return null;
	}
	public static Portal get(Block signblock, String[] lines) {
		if (signblock.getState() instanceof Sign) {
			if (lines[0].equalsIgnoreCase("[portal]")) {
				String name = lines[1];
				if (name != null && name.trim().equals("") == false) {
					Portal p = new Portal();
					p.name = name.replace("\"", "").replace("'", "");
					p.destination = lines[2].replace("\"", "").replace("'", "");
					p.location = signblock.getLocation();
			    	MaterialData data = signblock.getState().getData();
			    	float yaw = 0;
			    	if (data instanceof Directional) {
			    		switch (((Directional) data).getFacing()) {
			    		case NORTH: yaw = 90; break;		
			    		case NORTH_EAST: yaw = 135; break;		
			    		case EAST: yaw = 180; break;	 
			    		case SOUTH_EAST: yaw = 225; break;	
			    		case SOUTH: yaw = 270; break;	
			    		case SOUTH_WEST: yaw = 315; break;	
			    		case WEST: yaw = 0; break;
			    		case NORTH_WEST: yaw = 45; break;	
			    		}
			    	}
			    	p.location.setYaw(yaw);
					return p;
				}
			}
		}
		return null;
	}

	public static String[] getPortals() {
		return portallocations.keySet().toArray(new String[0]);
	}
	public static String[] getPortals(World w) {
		ArrayList<String> rval = new ArrayList<String>();
		for (Map.Entry<String, Position> entry : portallocations.entrySet()) {
			if (entry.getValue().getWorldName().equals(w.getName())) {
				rval.add(entry.getKey());
			}
		}
		return rval.toArray(new String[0]);
	}
	public static String[] getPortals(Chunk c) {
		ArrayList<String> rval = new ArrayList<String>();
		for (String name : getPortals()) {
			Location loc = getPortalLocation(name);
			if (loc != null && loc.getWorld() == c.getWorld()) {
				if (c.getX() == (loc.getBlockX() >> 4)) {
					if (c.getZ() == (loc.getBlockZ() >> 4)) {
						rval.add(name);
					}
				}
			}
		}
		return rval.toArray(new String[0]);
	}
	
	private static Location getPortalLocation(Position portalpos) {
		if (portalpos != null) {
			Location loc = portalpos.toLocation();
	    	if (loc.getWorld() != null) {
	    		return loc;
	    	}
		}
		return null;
	}
	public static Location getPortalLocation(String portalname) {
		if (portalname == null) return null;
		Position pos = portallocations.get(portalname);
		if (pos == null) return null;
		Location loc = getPortalLocation(pos);
	    if (loc == null) {
			for (Map.Entry<String, Position> entry : portallocations.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(portalname)) {
					return getPortalLocation(entry.getValue());
				}
			}
			return null;
		} else {
			return loc;
		}
	}
	public static Location getPortalLocation(String portalname, boolean spawnlocation) {
		Location loc = getPortalLocation(portalname);
		if (loc != null && spawnlocation) return loc.add(0.5, 2, 0.5);
		return loc;
	}
		     
    /*
     * Teleportation and teleport defaults
     */
    private static HashMap<Entity, Long> portaltimes = new HashMap<Entity, Long>();
    private static ArrayList<TeleportCommand> teleportations = new ArrayList<TeleportCommand>();  
    public static void setDefault(String worldname, String destination) {
    	if (destination == null) {
        	defaultlocations.remove(worldname.toLowerCase());
    	} else {
        	defaultlocations.put(worldname.toLowerCase(), destination);
    	}
    }
    public static void handlePortalEnter(Entity e) {
        long currtime = System.currentTimeMillis();
        if (!portaltimes.containsKey(e) || currtime - portaltimes.get(e) >= MyWorlds.teleportInterval) {
        	Portal portal = get(e.getLocation(), 5);  	
        	if (portal == null) {
        		//Default portals
        		String def = defaultlocations.get(e.getWorld().getName().toLowerCase());
        		if (def != null) portal = Portal.get(def);
        		if (portal == null) {
        			//world spawn?
        			World w = WorldManager.getWorld(def);
        			if (w != null) {
        				if (Permission.handleTeleport(e, w.getSpawnLocation())) {
        					MyWorlds.message(e, Localization.getWorldEnter(w));
        				}
        			} else {
        				//Additional destinations??
        			}
        		}
        	}
        	if (portal != null) {
        		delayedTeleport(portal, e);
        	}
    	}
        portaltimes.put(e, currtime);
    }
    private static class TeleportCommand {
    	public Entity e;
    	public Portal portal;
    	public TeleportCommand(Entity e, Portal portal) {
    		this.e = e;
    		this.portal = portal;
    	}
    }
    public static void delayedTeleport(Portal portal, Entity e) {
    	teleportations.add(new TeleportCommand(e, portal));
    	MyWorlds.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MyWorlds.plugin, new Runnable() {
    	    public void run() {
    	    	TeleportCommand telec = teleportations.remove(0);
	    		if (telec.portal.hasDestination()) {
	    			if (Permission.handleTeleport(telec.e, telec.portal)) {
	    				//Success
	    			}
    			} else {
    				MyWorlds.message(telec.e, Localization.get("portal.nodestination"));
    			}
    	    }
    	}, 0L);
    }
	
    /*
     * Loading and saving
     */
	public static void loadDefaultPortals(Configuration config, String worldname) {
		String def = config.getString(worldname + ".defaultPortal", null);
		if (def != null) {
			defaultlocations.put(worldname.toLowerCase(), def);
		}
	}
	public static void saveDefaultPortals(Configuration config) {
		for (String worldname : config.getKeys()) {
			if (!defaultlocations.containsKey(worldname.toLowerCase())) {
				config.removeProperty(worldname + ".defaultPortal");
			}
		}
		for (Map.Entry<String, String> entry : defaultlocations.entrySet()) {
			config.setProperty(entry.getKey() + ".defaultPortal", entry.getValue());
		}
	}
	public static void loadPortals(String filename) {
		for (String textline : SafeReader.readAll(filename, true)) {
			String[] args = MyWorlds.convertArgs(textline.split(" "));
			if (args.length == 7) {
				String name = args[0];
				try {
					Position pos = new Position(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));
					portallocations.put(name, pos);
				} catch (Exception ex) {
					MyWorlds.log(Level.SEVERE, "Failed to load portal: " + name);
				}
			}
		}
	}
	public static void savePortals(String filename) {
		SafeWriter w = new SafeWriter(filename);
		for (String portal : getPortals()) {
			Position pos = portallocations.get(portal);
			w.writeLine("\"" + portal + "\" \"" + pos.getWorldName() + "\" " + pos.getBlockX() + " " + pos.getBlockY() + " " + pos.getBlockZ() + " " + pos.getYaw() + " " + pos.getPitch());
		}
		w.close();
	}
	
	private static HashMap<String, String> defaultlocations = new HashMap<String, String>();
	private static HashMap<String, Position> portallocations = new HashMap<String, Position>();
}
