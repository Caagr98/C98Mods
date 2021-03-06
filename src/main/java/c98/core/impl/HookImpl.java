package c98.core.impl;

import java.util.*;
import org.objectweb.asm.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import c98.core.*;
import c98.core.hooks.*;
import c98.core.impl.launch.C98Tweaker;

public class HookImpl {
	static {
		C98Core.client = C98Tweaker.client;
		C98Core.mc = C98Core.client ? Minecraft.getMinecraft() : null;
	}

	public static List<TickHook> tickHooks = new ArrayList();
	public static List<GuiHook> guiHooks = new ArrayList();
	public static List<HudRenderHook> hudRenderHooks = new ArrayList();
	public static List<WorldRenderHook> worldRenderHooks = new ArrayList();
	public static List<GuiRenderHook> guiRenderHooks = new ArrayList();
	public static List<GuiSetHook> guiSetHooks = new ArrayList();
	public static List<KeyHook> keyHooks = new ArrayList();
	public static List<ConnectHook> connectHooks = new ArrayList();
	public static List<PacketHook> packetHooks = new ArrayList();
	public static List<DisplayGuiHook> displayGuiHooks = new ArrayList();
	public static HashMap<KeyBinding, boolean[]> keyBindings = new LinkedHashMap(); //the boolean[] contains [continuous, wasPressed]

	public static void addHook(Object hook) {
		if(hook instanceof TickHook) tickHooks.add((TickHook)hook);
		if(!C98Core.client) return;
		if(hook instanceof GuiHook) guiHooks.add((GuiHook)hook);
		if(hook instanceof HudRenderHook) hudRenderHooks.add((HudRenderHook)hook);
		if(hook instanceof WorldRenderHook) worldRenderHooks.add((WorldRenderHook)hook);
		if(hook instanceof GuiRenderHook) guiRenderHooks.add((GuiRenderHook)hook);
		if(hook instanceof GuiSetHook) guiSetHooks.add((GuiSetHook)hook);
		if(hook instanceof KeyHook) keyHooks.add((KeyHook)hook);
		if(hook instanceof ConnectHook) connectHooks.add((ConnectHook)hook);
		if(hook instanceof PacketHook) packetHooks.add((PacketHook)hook);
		if(hook instanceof DisplayGuiHook) displayGuiHooks.add((DisplayGuiHook)hook);
		if(hook instanceof IResourceManagerReloadListener) ((SimpleReloadableResourceManager)C98Core.mc.getResourceManager()).reloadListeners.add((IResourceManagerReloadListener)hook);
	}

	public static void removeHook(Object hook) {
		if(hook instanceof TickHook) tickHooks.remove(hook);
		if(!C98Core.client) return;
		if(hook instanceof GuiHook) guiHooks.remove(hook);
		if(hook instanceof HudRenderHook) hudRenderHooks.remove(hook);
		if(hook instanceof WorldRenderHook) worldRenderHooks.remove(hook);
		if(hook instanceof GuiRenderHook) guiRenderHooks.remove(hook);
		if(hook instanceof GuiSetHook) guiSetHooks.remove(hook);
		if(hook instanceof KeyHook) keyHooks.remove(hook);
		if(hook instanceof ConnectHook) connectHooks.remove(hook);
		if(hook instanceof PacketHook) packetHooks.remove(hook);
		if(hook instanceof DisplayGuiHook) packetHooks.remove(hook);
		if(hook instanceof IResourceManagerReloadListener) ((SimpleReloadableResourceManager)C98Core.mc.getResourceManager()).reloadListeners.remove(hook);
	}

	public static void findMods() {
		try {
			C98Loader.loadMods((rdr, name) -> {
				rdr.accept(new ClassVisitor(Opcodes.ASM4) {
					@Override public void visit(int version, int access, String classname, String signature, String superName, String[] interfaces) {
						if(!superName.equals("c98/core/C98Mod")) return;
						try {
							Class modClass = Launch.classLoader.loadClass(name);
							C98Mod modInstance = (C98Mod)modClass.newInstance();
							C98Core.modList.add(modInstance);
							C98Log.log("C98Mod found: " + modInstance.toString());
						} catch(Exception e) {
							C98Log.error("Failed to load mod class " + name, e);
							return;
						}
					}
				}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			});
		} catch(Exception e) {
			C98Log.error(e);
		}
		Collections.sort(C98Core.modList);
		if(C98Core.modList.isEmpty()) C98Log.log("Didn't find any C98Mods :(");
		for(C98Mod mod : C98Core.modList) {
			addHook(mod);
			mod.preinit();
		}
	}

	public static void loadMods() {
		try {
			for(C98Mod mod : C98Core.modList)
				mod.load();
			C98Log.log("Loaded C98Mods");
			C98Log.debug("Mod list: " + C98Core.modList);
			if(C98Core.client) {
				addHook(new C98Core());
				//TODO this should be in EI
				// BiomeGenBase.getBiome(8).biomeName = "Nether";
				// BiomeGenBase.getBiome(9).biomeName = "End";
				C98Core.mc.gameSettings.keyBindings = getAllKeys(C98Core.mc.gameSettings.keyBindings);
				C98Core.mc.gameSettings.loadOptions();
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void tickGui() {
		C98Core.mc.mcProfiler.startSection("c98tickGui");
		GuiScreen g = C98Core.mc.currentScreen;
		if(g != null) for(GuiHook mod : guiHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.tickGui(g);
			C98Core.mc.mcProfiler.endSection();
		}
		C98Core.mc.mcProfiler.endSection();
	}

	public static void tick(World w) {
		if(!w.isRemote) return;
		C98Core.mc.mcProfiler.startSection("c98tick");
		C98Core.mc.mcProfiler.startSection("keys");
		doKeys();
		if(C98Core.mc.world != null && C98Core.mc.player != null && C98Core.mc.player.world != null)
			for(TickHook mod : tickHooks) {
				C98Core.mc.mcProfiler.endStartSection(mod.toString());
				mod.tickGame(w);
			}
		C98Core.mc.mcProfiler.endSection();
		C98Core.mc.mcProfiler.endSection();
	}

	public static void renderGui() {
		C98Core.mc.mcProfiler.startSection("c98renderGui");
		GuiScreen g = C98Core.mc.currentScreen;
		if(g != null) for(GuiRenderHook mod : guiRenderHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.renderGui(g);
			C98Core.mc.mcProfiler.endSection();
		}
		C98Core.mc.mcProfiler.endSection();
	}

	public static void renderWorld() {
		C98Core.mc.mcProfiler.startSection("c98renderWorld");

		float f = C98Core.getPartialTicks();
		Entity ent = C98Core.mc.renderViewEntity;
		double x = (ent.posX - ent.prevPosX) * f + ent.prevPosX;
		double y = (ent.posY - ent.prevPosY) * f + ent.prevPosY;
		double z = (ent.posZ - ent.prevPosZ) * f + ent.prevPosZ;
		GL.disableTexture();
		GL.pushMatrix();
		GL.translate(-x, -y, -z);

		for(WorldRenderHook mod : worldRenderHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.renderWorld(C98Core.mc.world, C98Core.getPartialTicks());
			C98Core.mc.mcProfiler.endSection();
		}
		GL.popMatrix();

		GL.enableTexture();

		C98Core.mc.mcProfiler.endSection();
	}

	public static void setGui(GuiScreen par1GuiScreen) {
		for(GuiSetHook mod : guiSetHooks)
			mod.setGui(par1GuiScreen);
	}

	private static void doKeys() {
		if(Keyboard.isCreated()) for(Map.Entry<KeyBinding, boolean[]> entry : keyBindings.entrySet()) {
			int key = entry.getKey().getKeyCode();
			boolean down;

			if(key < 0) down = Mouse.isButtonDown(key + 100);
			else down = Keyboard.isKeyDown(key);

			boolean[] flags = entry.getValue();
			boolean prevDown = flags[1];
			flags[1] = down;

			if(down && (!prevDown || flags[0])) for(KeyHook mod : keyHooks)
				mod.keyboardEvent(entry.getKey());
		}
	}

	private static KeyBinding[] getAllKeys(KeyBinding[] keyBindings2) {
		ArrayList l = new ArrayList(Arrays.asList(keyBindings2));
		l.addAll(keyBindings.keySet());
		return (KeyBinding[])l.toArray(new KeyBinding[0]);
	}

	public static void onConnect(NetHandlerPlayClient cli) {
		for(ConnectHook mod : connectHooks)
			mod.onConnect(cli);
	}

	public static void onDisconnect(NetHandlerPlayClient cli) {
		for(ConnectHook mod : connectHooks)
			mod.onDisconnect(cli);
	}
}
