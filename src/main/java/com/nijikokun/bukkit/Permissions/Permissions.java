package com.nijikokun.bukkit.Permissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.MessageHelper;
import com.nijiko.configuration.NotNullConfiguration;
import com.nijiko.data.GroupWorld;
import com.nijiko.data.StorageFactory;
import com.nijiko.data.YamlCreator;
import com.nijiko.permissions.Entry;
import com.nijiko.permissions.Group;
import com.nijiko.permissions.ModularControl;
import com.nijiko.permissions.PermissionHandler;
import com.nijiko.permissions.User;

/**
 * Permissions 3.x Copyright (C) 2011 Matt 'The Yeti' Burnett
 * <admin@theyeticave.net> Original Credit & Copyright (C) 2010 Nijikokun
 * <nijikokun@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Permissions Public License as published by the Free
 * Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Permissions Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Permissions Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class Permissions extends JavaPlugin {
    protected static final Logger log = Logger.getLogger("Minecraft.Permissions");
    
    public static Plugin instance;
    // private Configuration storageConfig;
    
    @Deprecated
    public static final String name = "Permissions";
    
    protected String pluginVersion;
    protected String pluginName;
    protected String pluginInternalName = "Permissions";
    protected String codeName = "Yeti";

    public Listener listener = new Listener(this);

    /**
     * Controller for permissions and security. Use getHandler() instead.
     * @see getHandler() todo section
     */
    public static PermissionHandler Security;

    // /**
    // * Miscellaneous object for various functions that don't belong anywhere
    // * else
    // */
    // public static Misc Misc = new Misc();

    private String defaultWorld = "";
    private static final boolean autoComplete = true;
    private final YamlCreator yamlC;
    private int dist = 10;
    private final PrWorldListener wListener = new PrWorldListener();

    public Permissions() {
        yamlC = new YamlCreator();
        StorageFactory.registerDefaultCreator(yamlC);
        StorageFactory.registerCreator("YAML", yamlC);
    }

    @Override
    public void onLoad() {
        PluginDescriptionFile description = this.getDescription();
        this.pluginName = description.getName();
        this.pluginVersion = description.getVersion();
        
        instance = this;
        Properties prop = new Properties();
        FileInputStream in = null;

        this.getDataFolder().mkdirs();
        
        try {
            in = new FileInputStream(new File("server.properties"));
            prop.load(in);
            defaultWorld = prop.getProperty("level-name");
        } catch (IOException e) {
            System.err.println("[" + this.getInternalName() + "] Unable to read default world's name from server.properties.");
            e.printStackTrace();
            defaultWorld = "world";
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // PropertyHandler server = new PropertyHandler("server.properties");
        // defaultWorld = server.getString("level-name");

        File storageFile = new File("plugins" + File.separator + "Permissions" + File.separator, "storageconfig.yml"); // create pointer
        storageFile.getParentFile().mkdirs(); // create dirs
        
        // check if storageFile exists first
        if (!storageFile.exists()) {
            try {
                System.out.println("[" + this.getInternalName() + "] Creating storageconfig.yml.");
                if (!storageFile.createNewFile()) {
                    disable("[" + this.getInternalName() + "] Unable to create storageconfig.yml!");
                }
            } catch (IOException e) {
                disable("[" + this.getInternalName() + "] storageconfig.yml could not be created.");
                e.printStackTrace();
                return;
            }
        }
        
        if ( (!storageFile.isFile()) || (!storageFile.canRead()) ) {
            disable("[" + this.getInternalName() + "] storageconfig.yml is not a file or is not readable.");
        }
        
        Configuration storageConfig = new NotNullConfiguration(storageFile);
        storageConfig.load();
        // this.storageConfig = storageConfig;

        // Setup Permission
        setupPermissions(storageConfig);

        // Enabled
        log.info("[" + this.getInternalName() + "] (" + codeName + ") v" + this.getPluginVersion() + " initialized.");
    }

    @Override
    public void onDisable() {
        log.info("[" + this.getInternalName() + "] (" + codeName + ") saving data ...");
        
        this.getHandler().closeAll();
        // Security = null;
        
        log.info("[" + this.getInternalName() + "] (" + codeName + ") saved all data.");
        
        this.getServer().getScheduler().cancelTasks(this);
        
        log.info("[" + this.getInternalName() + "] (" + codeName + ") v" + this.getPluginVersion() + " disabled successfully.");
    }

    private void disable(String error) {
        if (error != null)
            log.severe(error);
        
        log.info("[" + this.getInternalName() + "] Shutting down " + this.getInternalName() + " v" + this.getPluginVersion() + " due to error(s).");
        this.getServer().getPluginManager().disablePlugin(this);
    }
    
    @Override
    public void onEnable() {
        StorageFactory.registerDefaultCreator(yamlC);
        StorageFactory.registerCreator("YAML", yamlC);
        
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, listener, Priority.High, this); //@TODO: listen on Priority.Low, refactor varName "listener" Listener
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, listener, Priority.High, this); //@TODO: listen on Priority.Low, refactor varName "listener" Listener
        this.getServer().getPluginManager().registerEvent(Event.Type.WORLD_LOAD, wListener, Priority.Monitor, this);
        
        log.info("[" + this.getPluginName() + "] v" + this.getPluginVersion() + " (" + codeName + ") enabled");
    }    

    /**
     * Alternative method of grabbing Permissions.Security <br />
     * <br />
     * <blockquote>
     * 
     * <pre>
     * Permissions.getHandler()
     * </pre>
     * 
     * </blockquote>
     * 
     * @return PermissionHandler
     * @TODO: don't return static! use getPluginManager().getPlugin() instead and return protected instance!
     */
    public PermissionHandler getHandler() {
        return Permissions.Security;
    }
    
    public String getPluginName() {
        return this.pluginName;
    }
    
    public String getInternalName() {
        return this.pluginInternalName;
    }
    
    public String getPluginVersion() {
        return this.pluginVersion;
    }
    
    public void setupPermissions(Configuration storageConfig) {
        try {
            Security = new ModularControl(storageConfig);
            Security.setDefaultWorld(defaultWorld);
            Security.load();
            
            //System.out.println(getServer().getWorlds());
            for(World w : getServer().getWorlds()) {
                Security.loadWorld(w.getName());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            disable("[" + this.getInternalName() + "] Unable to load permission data.");
            return;
        }
        
        // getServer().getServicesManager().register(PermissionHandler.class,
        // Security, this, ServicePriority.Normal);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        // String commandName = command.getName().toLowerCase();
        PluginDescriptionFile pdfFile = this.getDescription();
        MessageHelper msg = new MessageHelper(sender);
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0) {
            if (player != null) {
                msg.send("&7-------[ &fPermissions&7 ]-------");
                msg.send("&7Currently running version: &f[" + pdfFile.getVersion() + "] (" + codeName + ")");

                if (Security.has(player.getWorld().getName(), player.getName(), "permissions.reload")) {
                    msg.send("&7Reload with: &f/permissions &a-reload &e<world>");
                    msg.send("&fLeave &e<world> blank to reload default world.");
                }

                msg.send("&7-------[ &fPermissions&7 ]-------");
                return true;
            } else {
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codeName + ")  loaded");
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("-reload")) {
            String world;
            if (args.length > 1) {
                StringBuilder tempWorld = new StringBuilder();
                int val = extractQuoted(args, 1, tempWorld);
                switch (val) {
                case -1:
                    msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                    return true;
                case -2:
                    msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                    return true;
                }
                world = tempWorld.toString();
            } else
                world = "";
            return reload(sender, world);
        } else if (args[0].equalsIgnoreCase("-load")) {
            String world;
            if (args.length > 1) {
                StringBuilder tempWorld = new StringBuilder();
                int val = extractQuoted(args, 1, tempWorld);
                switch (val) {
                case -1:
                    msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                    return true;
                case -2:
                    msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                    return true;
                }
                world = tempWorld.toString();
            } else
                world = "";
            try {
                Security.forceLoadWorld(world);
            } catch (Exception e) {
                msg.send("&4[" + this.getInternalName() + "] Error occured while loading world.");
                e.printStackTrace();
                return true;
            }
            msg.send("&7[" + this.getInternalName() + "] World loaded.");
            return true;
        } else if (args[0].equalsIgnoreCase("-list")) {
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("worlds")) {
                    if (player != null && !Security.has(player, "permissions.list.worlds")) {
                        msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                        return true;
                    }
                    Set<String> worlds = Security.getWorlds();
                    StringBuilder text = new StringBuilder();
                    if (worlds.isEmpty()) {
                        text.append("&4[" + this.getInternalName() + "] No worlds loaded.");
                    } else {
                        text.append("&a[" + this.getInternalName() + "] Loaded worlds: &b");
                        for (String world : worlds) {
                            text.append(world).append(" ,");
                        }
                        text.delete(text.length() - 2, text.length());
                    }
                    msg.send(text.toString());
                    return true;
                }
                if (args.length > 2) {
                    StringBuilder tempWorld = new StringBuilder();
                    int val = extractQuoted(args, 2, tempWorld);
                    switch (val) {
                    case -1:
                        msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                        return true;
                    case -2:
                        msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                        return true;
                    }
                    String world = tempWorld.toString();
                    if (args[1].equalsIgnoreCase("users")) {
                        if (player != null && !Security.has(player, "permissions.list.users")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        Collection<User> users = Security.getUsers(world);
                        msg.send(listEntries(users, "Users"));
                        return true;
                    } else if (args[1].equalsIgnoreCase("groups")) {
                        if (player != null && !Security.has(player, "permissions.list.groups")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        Collection<Group> groups = Security.getGroups(world);
                        msg.send(listEntries(groups, "Groups"));
                        return true;
                    }
                }
            }
            msg.send("&7[" + this.getInternalName() + "] Syntax: ");
            msg.send("&b/permissions &a-list &eworlds.");
            msg.send("&b/permissions &a-list &e[users|groups] &d<world>.");
            return true;
        }

        // This part is for selecting the appropriate entry (/pr (g:)<entryname>
        // (w:<world>) ...)
        int currentArg = 0;
        boolean isGroup = args[0].startsWith("g:");
        String name = isGroup ? args[0].substring(2) : args[0];
        currentArg++;
        String world = sender instanceof Player ? ((Player) sender).getWorld().getName() : null;
        if (args.length > currentArg && args[currentArg].startsWith("w:")) {
            StringBuilder tempWorld = new StringBuilder();
            String[] tempArgs = new String[args.length];
            System.arraycopy(args, 0, tempArgs, 0, args.length); // XXX: Temp
                                                                 // solution for
                                                                 // w: prefix
            tempArgs[currentArg] = tempArgs[currentArg].substring(2);
            currentArg = extractQuoted(tempArgs, currentArg, tempWorld);
            switch (currentArg) {
            case -1:
                msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                return true;
            case -2:
                msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                return true;
            }
            world = tempWorld.toString();
        }
        if (world == null) {
            msg.send("&4[" + this.getInternalName() + "] No world specified. Defaulting to default world.");
            world = defaultWorld;
        }

        Entry entry = isGroup ? Security.getGroupObject(world, name) : Security.getUserObject(world, name);
        // Note that entry may be null if the user/group doesn't exist
        if (args.length > currentArg) {
            if (args[currentArg].equalsIgnoreCase("create")) {
                if (player != null && !Security.has(player, "permissions.create")) {
                    msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                    return true;
                }
                if (entry != null) {
                    msg.send("&4[" + this.getInternalName() + "] User/Group already exists.");
                    return true;
                }
                try {
                    entry = isGroup ? Security.safeGetGroup(world, name) : Security.safeGetUser(world, name);
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.send("&4[" + this.getInternalName() + "] Error creating user/group.");
                    return true;
                }
                msg.send("&7[" + this.getInternalName() + "] User/Group created.");
                return true;
            } else if (entry == null) {
                msg.send("&4[" + this.getInternalName() + "] User/Group does not exist.");

                if (autoComplete) {
                    Set<String> matches = getNames(isGroup ? Security.getGroups(world) : Security.getUsers(world));
                    Player[] online = getServer().getOnlinePlayers();
                    for (Player p : online) {
                        matches.add(p.getName());
                    }
                    String closest = getClosest(name, matches, dist);

                    if (closest != null) {
                        msg.send("&7[" + this.getInternalName() + "]&b Using closest match &4" + closest + "&b.");
                        name = closest;
                        entry = isGroup ? Security.getGroupObject(world, name) : Security.getUserObject(world, name);
                        if (entry == null) {
                            msg.send("&4[" + this.getInternalName() + "] Closest user/group does not exist.");
                            return true;
                        }
                    } else {
                        return true;
                    }
                }

            }
            if (args[currentArg].equalsIgnoreCase("delete")) {
                if (player != null && !Security.has(player, "permissions.delete")) {
                    msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                    return true;
                }
                String text = entry.delete() ? "&7[" + this.getInternalName() + "] User/Group deleted." : "&4[" + this.getInternalName() + "] Deletion failed.";
                msg.send(text);
                return true;
            } else if (args[currentArg].equalsIgnoreCase("has")) {
                currentArg++;
                if (player != null && !Security.has(player, "permissions.has")) {
                    msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                    return true;
                }
                if (args.length > currentArg) {
                    String permission = args[currentArg];
                    boolean has = entry.hasPermission(permission);
                    msg.send("&7[" + this.getInternalName() + "]&b User/Group " + (has ? "has" : "does not have") + " that permission.");
                    return true;
                }
                msg.send("&7[" + this.getInternalName() + "] Syntax: /pr (g:)<target> (w:<world>) has <permission>");
                return true;
            } else if (args[currentArg].equalsIgnoreCase("perms")) {
                currentArg++;
                if (args.length > currentArg) {
                    if (args[currentArg].equalsIgnoreCase("list")) {
                        if (player != null && !Security.has(player, "permissions.perms.list")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        Set<String> perms = entry.getPermissions();
                        String text = "";
                        if (perms == null || perms.isEmpty()) {
                            text = "&4[" + this.getInternalName() + "] User/Group has no non-inherited permissions.";
                        } else {
                            StringBuilder temp = new StringBuilder("&7[" + this.getInternalName() + "]&b Permissions: &c");
                            for (String perm : perms) {
                                temp.append(perm).append("&b,&c ");
                            }
                            text = temp.substring(0, temp.length() - 6);
                        }
                        msg.send(text);
                        return true;
                    } else if (args[currentArg].equalsIgnoreCase("listall")) {
                        if (player != null && !Security.has(player, "permissions.perms.listall")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        Set<String> perms = entry.getAllPermissions();
                        String text = "&7[" + this.getInternalName() + "]&b Permissions: &c";
                        if (perms == null || perms.isEmpty()) {
                            text = "&4[" + this.getInternalName() + "] User/Group has no permissions.";
                        } else {
                            StringBuilder temp = new StringBuilder("&7[" + this.getInternalName() + "]&b Permissions: &c");
                            for (String perm : perms) {
                                temp.append(perm).append("&b,&c ");
                            }
                            text = temp.substring(0, temp.length() - 6);
                        }
                        msg.send(text);
                        return true;
                    } else if (args[currentArg].equalsIgnoreCase("add") || args[currentArg].equalsIgnoreCase("remove")) {
                        boolean add = args[currentArg].equalsIgnoreCase("add");

                        String permNode = add ? "permissions.perms.add" : "permissions.perms.remove";
                        if (player != null && !Security.has(player, permNode)) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }

                        currentArg++;
                        String text = add ? "&7[" + this.getInternalName() + "]&b Permission added successfully." : "&7[" + this.getInternalName() + "]&b Permission removed successfully.";
                        if (args.length > currentArg) {
                            String permission = args[currentArg];
                            Set<String> perms = entry.getPermissions();
                            if (!(perms.contains(permission) ^ add))
                                text = "&4[" + this.getInternalName() + "] User/Group already has that permission.";
                            else
                                entry.setPermission(permission, add);
                        }
                        msg.send(text);
                        return true;
                    }
                }
                msg.send("&7[" + this.getInternalName() + "] Syntax: ");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) perms list");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) perms [add|remove] <node>");
                return true;
            } else if (args[currentArg].equalsIgnoreCase("parents")) {
                currentArg++;
                if (args.length > currentArg) {
                    if (args[currentArg].equalsIgnoreCase("list")) {
                        if (player != null && !Security.has(player, "permissions.parents.list")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        // LinkedHashSet<GroupWorld> parents =
                        // entry.getRawParents();
                        LinkedHashSet<Entry> parents = entry.getParents();
                        String text = "&7[" + this.getInternalName() + "]&b Parents: &c";
                        if (parents == null || parents.isEmpty()) {
                            text = "&4[" + this.getInternalName() + "] User/Group has no parents.";
                        } else {
                            StringBuilder temp = new StringBuilder("&7[" + this.getInternalName() + "]&b Parents: &c");
                            for (Entry parent : parents) {
                                temp.append(parent.toString()).append("&b,&c ");
                            }
                            text = temp.substring(0, temp.length() - 6);
                        }
                        msg.send(text);
                        return true;
                    } else if (args[currentArg].equalsIgnoreCase("listall")) {
                        if (player != null && !Security.has(player, "permissions.parents.listall")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        // LinkedHashSet<GroupWorld> parents =
                        // entry.getRawParents();
                        LinkedHashSet<Entry> parents = entry.getAncestors();
                        String text = "&7[" + this.getInternalName() + "]&b All parents: &c";
                        if (parents == null || parents.isEmpty()) {
                            text = "&4[" + this.getInternalName() + "] User/Group has no parents.";
                        } else {
                            StringBuilder temp = new StringBuilder("&7[" + this.getInternalName() + "]&b Parents: &c");
                            for (Entry parent : parents) {
                                temp.append(parent.toString()).append("&b,&c ");
                            }
                            text = temp.substring(0, temp.length() - 6);
                        }
                        msg.send(text);
                        return true;
                    } else if (args[currentArg].equalsIgnoreCase("add") || args[currentArg].equalsIgnoreCase("remove")) {
                        boolean add = args[currentArg].equalsIgnoreCase("add");
                        String permNode = add ? "permissions.parents.add" : "permissions.parents.remove";
                        if (player != null && !Security.has(player, permNode)) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        currentArg++;
                        String text = add ? "&7[" + this.getInternalName() + "]&b Parent added successfully." : "&7[" + this.getInternalName() + "]&b Parent removed successfully.";
                        if (args.length > currentArg) {
                            String parentName = args[currentArg];
                            String parentWorld = world;
                            if (args.length > (++currentArg)) {
                                StringBuilder tempWorld = new StringBuilder();
                                currentArg = extractQuoted(args, currentArg, tempWorld);
                                switch (currentArg) {
                                case -1:
                                    msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                                    return true;
                                case -2:
                                    msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                                    return true;
                                }
                                parentWorld = tempWorld.toString();
                            }
                            LinkedHashSet<GroupWorld> parents = entry.getRawParents();
                            if (add && parents.contains(new GroupWorld(parentWorld, parentName)))
                                text = "&4[" + this.getInternalName() + "] User/Group already has that parent.";
                            if (!add && !parents.contains(new GroupWorld(parentWorld, parentName)))
                                text = "&4[" + this.getInternalName() + "] User/Group does not have such a parent.";
                            else {
                                Group parent = Security.getGroupObject(parentWorld, parentName);
                                if (parent == null) {
                                    text = "&4[" + this.getInternalName() + "] No such group exists.";
                                } else {
                                    if (add)
                                        entry.addParent(parent);
                                    else
                                        entry.removeParent(parent);
                                }
                            }
                        }
                        msg.send(text);
                        return true;
                    }
                }
                msg.send("&7[" + this.getInternalName() + "] Syntax: ");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) parents list");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) parents [add|remove] <parentname> (parentworld)");
                return true;
            } else if (args[currentArg].equalsIgnoreCase("info")) {
                currentArg++;
                if (args.length > currentArg) {
                    String choice = args[currentArg];
                    if (choice.equalsIgnoreCase("get")) {
                        currentArg++;
                        if (player != null && !Security.has(player, "permissions.info.get")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        if (args.length > currentArg) {
                            String path = args[currentArg];
                            msg.send("&7[" + this.getInternalName() + "]&b " + entry.getString(path));
                            return true;
                        }
                    } else if (choice.equalsIgnoreCase("set")) {
                        currentArg++;
                        if (player != null && !Security.has(player, "permissions.info.set")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        if (args.length > currentArg) {
                            String path = args[currentArg];
                            currentArg++;
                            if (args.length > currentArg) {
                                String newValueString = args[currentArg];
                                Object newValue;
                                String type = "";
                                try {
                                    if (newValueString.startsWith("b:")) {
                                        newValue = Boolean.valueOf(newValueString.substring(2));
                                        type = "Boolean";
                                    } else if (newValueString.startsWith("d:")) {
                                        newValue = Double.valueOf(newValueString.substring(2));
                                        type = "Double";
                                    } else if (newValueString.startsWith("i:")) {
                                        newValue = Integer.valueOf(newValueString.substring(2));
                                        type = "Integer";
                                    } else {
                                        newValue = newValueString;
                                        type = "String";
                                    }
                                } catch (NumberFormatException e) {
                                    msg.send("&4[" + this.getInternalName() + "]&b Error encountered when parsing value.");
                                    return true;
                                }
                                entry.setData(path, newValue);
                                msg.send("&7[" + this.getInternalName() + "]&b &a" + path + "&b set to &a" + type + " &c" + newValue.toString());
                                return true;
                            }
                        }
                    } else if (choice.equalsIgnoreCase("remove")) {
                        currentArg++;
                        if (player != null && !Security.has(player, "permissions.info.remove")) {
                            msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                            return true;
                        }
                        if (args.length > currentArg) {
                            String path = args[currentArg];
                            entry.removeData(path);
                            msg.send("&7[" + this.getInternalName() + "]&b &a" + path + "&b cleared.");
                            return true;
                        }
                    }
                }
                msg.send("&7[" + this.getInternalName() + "] Syntax: ");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) info get <path>");
                msg.send("&b/permissions &a(g:)<target> (w:<world>) info set <path> (i:|d:|b:)<data>");
                return true;
            } else if (entry instanceof User) {
                User user = (User) entry;
                if (args[currentArg].equalsIgnoreCase("promote") || args[currentArg].equalsIgnoreCase("demote")) {
                    boolean isPromote = args[currentArg].equalsIgnoreCase("promote");
                    currentArg++;
                    if (args.length > currentArg) {
                        String parentName = args[currentArg];
                        String parentWorld = world;
                        currentArg++;
                        if (args.length > currentArg && args[currentArg].startsWith("w:")) {
                            StringBuilder tempWorld = new StringBuilder();
                            String[] tempArgs = new String[args.length];
                            System.arraycopy(args, 0, tempArgs, 0, args.length); // XXX:
                                                                                 // Temp
                                                                                 // solution
                                                                                 // for
                                                                                 // w:
                                                                                 // prefix
                            tempArgs[currentArg] = tempArgs[currentArg].substring(2);
                            currentArg = extractQuoted(tempArgs, currentArg, tempWorld);
                            switch (currentArg) {
                            case -1:
                                msg.send("&4[" + this.getInternalName() + "] Argument index error.");
                                return true;
                            case -2:
                                msg.send("&4[" + this.getInternalName() + "] No ending quote found.");
                                return true;
                            }
                            parentWorld = tempWorld.toString();
                            currentArg++;
                        }
                        GroupWorld group = new GroupWorld(parentWorld, parentName);
                        
                        if (!user.inGroup(parentWorld, parentName)) {
                            msg.send("&4[" + this.getInternalName() + "] User not in specified group.");
                            return true;
                        }
                        if (args.length > currentArg) {
                            String track = args[currentArg];
                            Set<String> tracks = Security.getTracks(world);
                            if (tracks == null || tracks.isEmpty()) {
                                msg.send("&4[" + this.getInternalName() + "] No tracks in specified world.");
                                return true;
                            }
                            if (!tracks.contains(track)) {
                                msg.send("&4[" + this.getInternalName() + "] Specified track does not exist.");
                                return true;
                            }
                            String permNode = isPromote ? "permissions.promote." + track : "permission.demote." + track;
                            if (player != null && !Security.has(player, permNode)) {
                                msg.send("&4[" + this.getInternalName() + "] You do not have permissions to use this command.");
                                return true;
                            }
                            if (isPromote)
                                user.promote(group, track);
                            else
                                user.demote(group, track);
                            String text = isPromote ? "&7[" + this.getInternalName() + "]&b User promoted along track " + track + "." : "&7[" + this.getInternalName() + "]&7 User demoted along track " + track + ".";
                            msg.send(text);
                            return true;
                        }
                        msg.send("&4[" + this.getInternalName() + "] Syntax: /permissions <target> (w:<world>) [promote|demote] <parent> (w:<parentworld>) <track>");
                        return true;
                    }
                }

                msg.send("&7[" + this.getInternalName() + "] Syntax: ");
                msg.send("&b/permissions &a<target> (w:<world>) [promote|demote] ...");
            }

            msg.send("&b/permissions &a(g:)<target> (w:<world>) [perms|parents] [list|add|remove] ...");
            msg.send("&b/permissions &a(g:)<target> (w:<world>) info [get|set|remove] ...");
            msg.send("&b/permissions &a(g:)<target> (w:<world>) [prefix|suffix|build] [get|set] ...");
        }

        return false;
    }

    private boolean reload(CommandSender sender, String arg) {
        Player p = null;
        if (sender instanceof Player) {
            p = (Player) sender;
        }

        if (arg == null || arg.equals("")) {

            if (p != null && !Security.has(p.getWorld().getName(), p.getName(), "permissions.reload.default")) {
                p.sendMessage(ChatColor.RED + "[" + this.getInternalName() + "] You lack the necessary permissions to perform this action.");
                return true;
            }

            Security.reload(defaultWorld);
            sender.sendMessage(ChatColor.GRAY + "[" + this.getInternalName() + "] Default world reloaded.");
            return true;
        }

        if (arg.equalsIgnoreCase("all")) {

            if (p != null && !Security.has(p.getWorld().getName(), p.getName(), "permissions.reload.all")) {
                p.sendMessage(ChatColor.RED + "[" + this.getInternalName() + "] You lack the necessary permissions to perform this action.");
                return true;
            }

            Security.reload();
            sender.sendMessage(ChatColor.GRAY + "[" + this.getInternalName() + "] All worlds reloaded.");
            return true;
        }

        if (p != null && !Security.has(p.getWorld().getName(), p.getName(), "permissions.reload." + arg)) {
            p.sendMessage(ChatColor.RED + "[" + this.getInternalName() + "] You lack the necessary permissions to perform this action.");
            return true;
        }

        if (Security.reload(arg))
            sender.sendMessage(ChatColor.GRAY + "[" + this.getInternalName() + "] Reload of World " + arg + " completed.");
        else
            sender.sendMessage(ChatColor.GRAY + "[" + this.getInternalName() + "] World " + arg + " does not exist.");
        return true;

    }

    @Override
    public String toString() {
        PluginDescriptionFile pdf = this.getDescription();
        return pdf.getName() + " version " + pdf.getVersion() + " (" + codeName + ")";
    }

    private String listEntries(Collection<? extends Entry> entries, String type) {
        StringBuilder text = new StringBuilder();
        if (entries == null) {
            text.append("&4[" + this.getInternalName() + "] World does not exist.");
        } else if (entries.isEmpty()) {
            text.append("&4[" + this.getInternalName() + "] No ").append(type.toLowerCase()).append(" in that world.");
        } else {
            text.append("&a[" + this.getInternalName() + "] " + type + ": &b");
            for (Entry entry : entries) {
                text.append(entry.getName()).append(", ");
            }
            text.delete(text.length() - 2, text.length());
        }
        return text.toString();
    }

    private int extractQuoted(String[] args, int currentArg, StringBuilder target) {
        if (args.length <= currentArg)
            return -1; // Args array too small
        target.append(args[currentArg]);
        currentArg++;
        if (target.charAt(0) != '"')
            return currentArg;

        target.deleteCharAt(0); // Delete the starting quote

        while (args.length > currentArg) {
            target.append(" ").append(args[currentArg]);
            currentArg++;
            if (target.charAt(target.length() - 1) == '"') {
                target.deleteCharAt(target.length() - 1);
                return currentArg;
            }
        }

        return -2; // No ending quote
    }

    private Set<String> getNames(Collection<? extends Entry> entries) {
        Set<String> names = new HashSet<String>();
        for (Entry e : entries) {
            if (e != null)
                names.add(e.getName());
        }
        return names;
    }

    public static String getClosest(String word, Set<String> dict, final int threshold) {
        if (word == null || word.isEmpty() || dict == null || dict.isEmpty()) {
            return null;
        }
        if (dict.contains(word)) {
            return word;
        }
        String result = null;
        int currentDist = threshold;
        String lw = word.toLowerCase();
        for (String s : dict) {
            if (s == null)
                continue;
            String ls = s.toLowerCase();
            int dist;
            if (ls.startsWith(lw)) {
                dist = s.length() - word.length();
                if (currentDist > dist) {
                    result = s;
                    currentDist = dist;
                }
            }
        }
        return result;
    }
}
