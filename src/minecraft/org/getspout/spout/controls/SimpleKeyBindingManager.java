package org.getspout.spout.controls;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.src.EntityClientPlayerMP;
import net.minecraft.src.Packet3Chat;

import org.getspout.spout.client.SpoutClient;
import org.getspout.spout.io.FileUtil;
import org.getspout.spout.packet.PacketKeyBinding;
import org.spoutcraft.spoutcraftapi.keyboard.KeyBinding;
import org.spoutcraft.spoutcraftapi.keyboard.KeyBindingManager;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class SimpleKeyBindingManager implements KeyBindingManager {
	private HashMap<Integer, KeyBinding> bindingsForKey = new HashMap<Integer, KeyBinding>();
	private HashMap<Integer, Shortcut> shortcutsForKey = new HashMap<Integer, Shortcut>();
	private ArrayList<KeyBinding> bindings;
	private ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
	
	public SimpleKeyBindingManager(){
		load();
	}
	
	public void registerControl(KeyBinding binding){
		KeyBinding result = null;
		for(KeyBinding check:bindings){
			if(check.getId().equals(binding.getId()) && check.getPlugin().equals(binding.getPlugin())){
				result = check;
			}
		}
		if(result != null){
			result.takeChanges(binding);
		} else {
			bindings.add(binding);
		}
		updateBindings();
		save();
	}
	
	public void registerShortcut(Shortcut shortcut){
		shortcuts.add(shortcut);
		updateShortcuts();
		save();
	}
	
	public void unregisterShortcut(Shortcut shortcut) {
		shortcuts.remove(shortcut);
		shortcutsForKey.remove(shortcut.getKey());
		save();
	}
	
	public void unregisterControl(KeyBinding binding){
		bindings.remove(binding);
		bindingsForKey.remove(binding.getKey());
		save();
	}
	
	public void updateBindings() {
		if(bindings == null){
			bindings = new ArrayList<KeyBinding>();
			return;
		}
		
		bindingsForKey.clear();
		for(KeyBinding binding:bindings){
			bindingsForKey.put(binding.getKey(), binding);
		}
	}
	
	private void updateShortcuts() {
		if(shortcuts == null) {
			shortcuts = new ArrayList<Shortcut>();
			return;
		}
		
		shortcutsForKey.clear();
		for(Shortcut shortcut:shortcuts) {
			shortcutsForKey.put(shortcut.getKey(), shortcut);
		}
	}
	
	public void save(){
		Yaml yaml = new Yaml();
		yaml.setBeanAccess(BeanAccess.FIELD); // to ignore transient fields!!
		try {
			FileWriter writer = new FileWriter(getBindingsFile());
			yaml.dump(bindings, writer);
			writer = new FileWriter(getShortcutsFile());
			ArrayList<Object> shsave = new ArrayList<Object>();
			for(Shortcut sh:shortcuts){
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("title", sh.getTitle());
				item.put("key", sh.getKey());
				item.put("commands", sh.getCommands());
				shsave.add(item);
			}
			yaml.dump(shsave, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private File getBindingsFile() {
		return new File(FileUtil.getSpoutcraftDirectory(), "bindings.yml");
	}
	
	private File getShortcutsFile() {
		return new File(FileUtil.getSpoutcraftDirectory(), "shortcuts.yml");
	}

	@SuppressWarnings("unchecked")
	public void load(){
		Yaml yaml = new Yaml();
		try {
			bindings = yaml.loadAs(new FileReader(getBindingsFile()), ArrayList.class);
		} catch (Exception e) {
			bindings = new ArrayList<KeyBinding>();
		}
		updateBindings();
		try {
			shortcuts.clear();
			ArrayList<Object> shsave = yaml.loadAs(new FileReader(getShortcutsFile()), ArrayList.class);
			for(Object obj:shsave) {
				HashMap<String, Object> item = (HashMap<String, Object>) obj;
				Shortcut sh = new Shortcut();
				sh.setTitle((String)item.get("title"));
				sh.setKey((Integer)item.get("key"));
				sh.setCommands((ArrayList<String>)item.get("commands"));
				shortcuts.add(sh);
			}
		} catch (Exception e) {
			shortcuts = new ArrayList<Shortcut>();
		}
		updateShortcuts();
	}

	public void pressKey(int key, boolean keyReleased, int screen) {
		KeyBinding binding = bindingsForKey.get(key);
		if(!(binding==null || binding.getUniqueId() == null)){
			SpoutClient.getInstance().getPacketManager().sendSpoutPacket(new PacketKeyBinding(binding, key, keyReleased, screen));
		}
		if(screen == 0) {
			Shortcut shortcut = shortcutsForKey.get(key);
			if(shortcut != null && !keyReleased) {
				//TODO: send to addons!
				for(String cmd:shortcut.getCommands()) {
					if(SpoutClient.getHandle().isMultiplayerWorld()) {
						EntityClientPlayerMP player = (EntityClientPlayerMP)SpoutClient.getHandle().thePlayer;
						player.sendQueue.addToSendQueue(new Packet3Chat(cmd));
					}
				}
			}
		}
	}
	
	public List<KeyBinding> getAllBindings() {
		return bindings;
	}

	public List<Shortcut> getAllShortcuts() {
		return Collections.unmodifiableList(shortcuts);
	}
}
