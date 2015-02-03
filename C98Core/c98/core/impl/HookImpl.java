package c98.core.impl;

import static org.lwjgl.opengl.GL11.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.Timer;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.objectweb.asm.*;
import c98.core.*;
import c98.core.hooks.*;
import c98.core.impl.launch.C98Tweaker;

public class HookImpl {
//	private static final Field srrm_listeners;
	static {
//		Field srrm = null;
//		try {
//			srrm = ReflectHelper.getFields(SimpleReloadableResourceManager.class, List.class).get(0);
//		} catch(Throwable e) {}
//		srrm_listeners = srrm;
		C98Core.client = C98Tweaker.client;
		C98Core.mc = C98Core.client ? Minecraft.getMinecraft() : null;
		timer = C98Core.mc.timer;
		C98Formatter.Target.OUT = C98Core.client ? C98Formatter.Target.LAUNCHER : C98Formatter.Target.CONSOLE;
	}
	
	public static Timer timer;
	public static List<TickHook> tickHooks = new ArrayList();
	public static List<GuiHook> guiHooks = new ArrayList();
	public static List<HudRenderHook> hudRenderHooks = new ArrayList();
	public static List<HudTopRenderHook> hudTopRenderHooks = new ArrayList();
	public static List<WorldRenderHook> worldRenderHooks = new ArrayList();
	public static List<GuiRenderHook> guiRenderHooks = new ArrayList();
	public static List<GuiSetHook> guiSetHooks = new ArrayList();
	public static List<KeyHook> keyHooks = new ArrayList();
	public static List<ConnectHook> connectHooks = new ArrayList();
	public static List<RenderBlockHook> renderBlockHooks = new ArrayList();
	public static List<EntitySpawnHook> entitySpawnHooks = new ArrayList();
	public static HashMap<KeyBinding, boolean[]> keyBindings = new LinkedHashMap(); //the boolean[] contains [continuous, wasPressed]
	
	public static void addHook(Object hook) {
		if(hook instanceof TickHook) tickHooks.add((TickHook)hook);
		if(!C98Core.client) return;
		if(hook instanceof GuiHook) guiHooks.add((GuiHook)hook);
		if(hook instanceof HudRenderHook) hudRenderHooks.add((HudRenderHook)hook);
		if(hook instanceof HudTopRenderHook) hudTopRenderHooks.add((HudTopRenderHook)hook);
		if(hook instanceof WorldRenderHook) worldRenderHooks.add((WorldRenderHook)hook);
		if(hook instanceof GuiRenderHook) guiRenderHooks.add((GuiRenderHook)hook);
		if(hook instanceof GuiSetHook) guiSetHooks.add((GuiSetHook)hook);
		if(hook instanceof KeyHook) keyHooks.add((KeyHook)hook);
		if(hook instanceof ConnectHook) connectHooks.add((ConnectHook)hook);
		if(hook instanceof RenderBlockHook) renderBlockHooks.add((RenderBlockHook)hook);
		if(hook instanceof EntitySpawnHook) entitySpawnHooks.add((EntitySpawnHook)hook);
		if(hook instanceof IResourceManagerReloadListener) ((SimpleReloadableResourceManager)C98Core.mc.getResourceManager()).reloadListeners.add(hook);
	}
	
	public static void removeHook(Object hook) {
		if(hook instanceof TickHook) tickHooks.remove(hook);
		if(!C98Core.client) return;
		if(hook instanceof GuiHook) guiHooks.remove(hook);
		if(hook instanceof HudRenderHook) hudRenderHooks.remove(hook);
		if(hook instanceof HudTopRenderHook) hudTopRenderHooks.remove(hook);
		if(hook instanceof WorldRenderHook) worldRenderHooks.remove(hook);
		if(hook instanceof GuiRenderHook) guiRenderHooks.remove(hook);
		if(hook instanceof GuiSetHook) guiSetHooks.remove(hook);
		if(hook instanceof KeyHook) keyHooks.remove(hook);
		if(hook instanceof ConnectHook) connectHooks.remove(hook);
		if(hook instanceof RenderBlockHook) renderBlockHooks.remove(hook);
		if(hook instanceof EntitySpawnHook) entitySpawnHooks.remove(hook);
		if(hook instanceof IResourceManagerReloadListener) ((SimpleReloadableResourceManager)C98Core.mc.getResourceManager()).reloadListeners.remove(hook);
	}
	
	public static void init() {
		try {
			for(C98Mod mod:C98Core.modList)
				try {
					mod.load();
				} catch(Exception e) {
					Console.error("Failed to load mod " + mod, e);
				}
			
			Console.log.finer("Mod list: " + C98Core.modList);
			if(C98Core.client) {
				addHook(new C98Core());
				if(C98Core.forge) new ForgeMenuHack(C98Core.mc);
				BiomeGenBase.hell.biomeName = "Nether";
				BiomeGenBase.sky.biomeName = "End";
				C98Core.mc.gameSettings.keyBindings = getAllKeys(C98Core.mc.gameSettings.keyBindings);
				C98Core.mc.gameSettings.loadOptions();
				Console.log.finer("Tick: " + tickHooks);
				Console.log.finer("Gui: " + guiHooks);
				Console.log.finer("GuiRender: " + guiRenderHooks);
				Console.log.finer("GuiSet: " + guiSetHooks);
				Console.log.finer("Hud: " + hudRenderHooks);
				Console.log.finer("HudTop: " + hudTopRenderHooks);
				Console.log.finer("World: " + worldRenderHooks);
				Console.log.finer("Key: " + keyHooks);
				Console.log.finer("Connect: " + connectHooks);
				Console.log.finer("RenderBlock: " + renderBlockHooks);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void postInit() {
		for(C98Mod mod:C98Core.modList)
			mod.postInit();
	}
	
	public static void preInit() {
		try {
			C98Loader.loadMods(new C98Loader.ModHandler() {
				@Override public void load(String name) {
					try {
						ClassReader rdr = new ClassReader(C98Tweaker.class.getClassLoader().getResourceAsStream(name));
						final String clName = name.replace(".class", "").replace("/", ".");
						rdr.accept(new ClassVisitor(Opcodes.ASM4) {
							@Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
								if(!superName.equals("c98/core/C98Mod")) return;
								Class modClass = null;
								try {
									modClass = Launch.classLoader.loadClass(clName);
								} catch(Throwable e) {
									Console.error("Failed to load class " + clName, e);
									return;
								}
								
								C98Mod modInstance;
								try {
									modInstance = (C98Mod)modClass.newInstance();
								} catch(InstantiationException | IllegalAccessException e) {
									Console.error("Failed to create instance of " + clName, e);
									return;
								}
								
								if(modInstance != null) {
									C98Core.modList.add(modInstance);
									Console.log("[C98Core] C98Mod found: " + modInstance.toString());
								}
								
							}
						}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					} catch(Throwable e) {
						Console.error(e);
					}
				}
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
		Collections.sort(C98Core.modList);
		if(C98Core.modList.isEmpty()) Console.log("[C98Core] Didn't find any C98Mods :(");
		for(C98Mod mod:C98Core.modList) {
			addHook(mod);
			mod.preInit();
		}
	}
	
	public static void tickGui() {
		C98Core.mc.mcProfiler.startSection("c98tickGui");
		GuiScreen g = C98Core.mc.currentScreen;
		if(g != null) for(GuiHook mod:guiHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.tickGui(g);
			C98Core.mc.mcProfiler.endSection();
		}
		C98Core.mc.mcProfiler.endSection();
	}
	
	public static void tick(World w) {
		if(!w.isClient) return;
		C98Core.mc.mcProfiler.startSection("c98tick");
		C98Core.mc.mcProfiler.startSection("keys");
		doKeys();
		C98Core.mc.mcProfiler.endStartSection("lang");
		LangImpl.tick();
		if(C98Core.mc.theWorld != null && C98Core.mc.thePlayer != null && C98Core.mc.thePlayer.worldObj != null) for(TickHook mod:tickHooks) {
			C98Core.mc.mcProfiler.endStartSection(mod.toString());
			mod.tickGame(w);
		}
		C98Core.mc.mcProfiler.endSection();
		C98Core.mc.mcProfiler.endSection();
	}
	
	public static void renderGui() {
		C98Core.mc.mcProfiler.startSection("c98renderGui");
		GuiScreen g = C98Core.mc.currentScreen;
		if(g != null) for(GuiRenderHook mod:guiRenderHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.renderGui(g);
			C98Core.mc.mcProfiler.endSection();
		}
		C98Core.mc.mcProfiler.endSection();
	}
	
	public static void renderWorld() {
		C98Core.mc.mcProfiler.startSection("c98renderWorld");
		glDepthMask(true);
		glDisable(GL_TEXTURE_2D);
		
		float f = C98Core.getPartialTicks();
		Entity ent = C98Core.mc.renderViewEntity;
		double x = (ent.posX - ent.prevPosX) * f + ent.prevPosX;
		double y = (ent.posY - ent.prevPosY) * f + ent.prevPosY;
		double z = (ent.posZ - ent.prevPosZ) * f + ent.prevPosZ;
		glPushMatrix();
		glTranslated(-x, -y, -z);
		
		for(WorldRenderHook mod:worldRenderHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.renderWorld();
			C98Core.mc.mcProfiler.endSection();
		}
		glPopMatrix();
		
		glEnable(GL_TEXTURE_2D);
		
		C98Core.mc.mcProfiler.endSection();
	}
	
	public static void renderHud() {
		if(C98Core.mc.playerController.enableEverythingIsScrewedUpMode()) return;
		C98Core.mc.mcProfiler.startSection("c98renderHud");
		
		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		glLoadIdentity();
		ScaledResolution var1 = new ScaledResolution(C98Core.mc, C98Core.mc.displayWidth, C98Core.mc.displayHeight);
		glOrtho(0.0D, var1.getScaledWidth_double(), var1.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadIdentity();
		glTranslatef(0.0F, 0.0F, -2000.0F);
		glEnable(GL_ALPHA_TEST);
		
		for(HudRenderHook mod:hudRenderHooks) {
			C98Core.mc.mcProfiler.startSection(mod.toString());
			mod.renderHud(C98Core.mc.playerController.shouldDrawHUD());
			C98Core.mc.mcProfiler.endSection();
		}
		C98Core.mc.getTextureManager().bindTexture(Gui.icons);
		glColor3f(1, 1, 1);
		
		glDisable(GL_ALPHA_TEST);
		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
		
		C98Core.mc.mcProfiler.endSection();
	}
	
	public static void renderHudTop() {
		if(C98Core.mc.playerController.enableEverythingIsScrewedUpMode()) return;
		
		for(HudTopRenderHook mod:hudTopRenderHooks)
			mod.renderHudTop();
		glColor3f(1, 1, 1);
	}
	
	public static void setGui(GuiScreen par1GuiScreen) {
		for(GuiSetHook mod:guiSetHooks)
			mod.setGui(par1GuiScreen);
	}
	
	private static void doKeys() {
		if(Keyboard.isCreated()) for(Map.Entry<KeyBinding, boolean[]> entry:keyBindings.entrySet()) {
			int key = entry.getKey().getKeyCode();
			boolean down;
			
			if(key < 0) down = Mouse.isButtonDown(key + 100);
			else down = Keyboard.isKeyDown(key);
			
			boolean[] flags = entry.getValue();
			boolean prevDown = flags[1];
			flags[1] = down;
			
			if(down && (!prevDown || flags[0])) for(KeyHook mod:keyHooks)
				mod.keyboardEvent(entry.getKey());
		}
	}
	
	private static KeyBinding[] getAllKeys(KeyBinding[] keyBindings2) {
		ArrayList l = new ArrayList(Arrays.asList(keyBindings2));
		l.addAll(keyBindings.keySet());
		return (KeyBinding[])l.toArray(new KeyBinding[0]);
	}
	
	public static void onConnect() {
		for(ConnectHook mod:connectHooks)
			mod.onConnect(C98Core.mc.getNetHandler());
	}
	
	public static void onDisconnect() {
		for(ConnectHook mod:connectHooks)
			mod.onDisconnect(C98Core.mc.getNetHandler());
	}
	
}
