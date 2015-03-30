package c98.resourcefulEntities;

import static org.lwjgl.opengl.GL11.*;
import java.nio.FloatBuffer;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import c98.core.C98Log;
import c98.core.GL;

public abstract class RenderJSON extends Render {
	private static final DynamicTexture white = new DynamicTexture(16, 16);
	private static final RenderParams DEFAULT = new RenderParams();
	private static FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
	
	protected final ModelJSON model;
	
	static {
		Arrays.fill(white.getTextureData(), -1);
		white.updateDynamicTexture();
	}
	
	public RenderJSON(RenderManager mgr, ModelJSON model) {
		super(mgr);
		this.model = model;
	}
	
	protected abstract void setAngles(float swing, float swingAmount, float age, float yaw, float pitch, Entity ent);
	
	protected void setupTransforms(EntityLivingBase e, float ptt) {}
	
	@Override public void doRender(Entity ent, double x, double y, double z, float p_76986_8_, float ptt) {
		EntityLivingBase e = (EntityLivingBase)ent;
		GL.pushMatrix();
		GL.disableCull();
//		mainModel.swingProgress = getSwingProgress(e, ptt);
//		mainModel.isRiding = e.isRiding();
//		mainModel.isChild = e.isChild();
		
		try {
			float yaw = interpolateRotation(e.prevRenderYawOffset, e.renderYawOffset, ptt);
			float yawhead = interpolateRotation(e.prevRotationYawHead, e.rotationYawHead, ptt);
			float yawdiff = yawhead - yaw;
			
			if(e.isRiding() && e.ridingEntity instanceof EntityLivingBase) {
				EntityLivingBase var13 = (EntityLivingBase)e.ridingEntity;
				yaw = interpolateRotation(var13.prevRenderYawOffset, var13.renderYawOffset, ptt);
				yawdiff = yawhead - yaw;
				float actualYawDiff = MathHelper.wrapAngleTo180_float(yawdiff);
				if(actualYawDiff < -85) actualYawDiff = -85;
				if(actualYawDiff >= 85) actualYawDiff = 85;
				yaw = yawhead - actualYawDiff;
				if(actualYawDiff * actualYawDiff > 2500) yaw += actualYawDiff * 0.2F;
			}
			
			float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * ptt;
			renderLivingAt(e, x, y, z);
			float age = getAge(e, ptt);
			rotateCorpse(e, age, yaw, ptt);
			GL.enableRescaleNormal();
			GL.scale(-1, -1, 1);
			setupTransforms(e, ptt);
			float scale = 0.0625F;
			float swingAmount = e.prevLimbSwingAmount + (e.limbSwingAmount - e.prevLimbSwingAmount) * ptt;
			float swing = e.limbSwing - e.limbSwingAmount * (1 - ptt);
			if(e.isChild()) swing *= 3;
			if(swingAmount > 1) swingAmount = 1;
			setAngles(swing, swingAmount, age, yawdiff, pitch, e);
			
			GL.enableAlpha();
			
			if(renderManager.field_178639_r) {
				boolean var18 = func_177088_c(e);
				renderModel(e, swing, swingAmount, age, yawdiff, pitch, scale, ptt);
				
				if(var18) func_180565_e();
			} else {
				boolean var18 = func_177090_c(e, ptt);
				renderModel(e, swing, swingAmount, age, yawdiff, pitch, scale, ptt);
				if(var18) func_177091_f();
				
				GL.depthMask(true);
				
				if(!(e instanceof EntityPlayer) || !((EntityPlayer)e).func_175149_v()) renderLayers(e, swing, swingAmount, ptt, age, yawdiff, pitch, scale);
			}
			
			GL.disableRescaleNormal();
		} catch(Exception ex) {
			C98Log.error("Couldn\'t render entity", ex);
		}
		
		GL.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL.enableTexture();
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL.enableCull();
		GL.popMatrix();
		
		if(!renderManager.field_178639_r) super.doRender(e, x, y, z, p_76986_8_, ptt);
	}
	
	protected void renderModel(EntityLivingBase e, float swingSpeed, float swingAmount, float age, float yaw, float pitch, float scale, float ptt) {
		boolean visible = !e.isInvisible();
		boolean visibleToPlayer = !visible && !e.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer);
		
		if(visible || visibleToPlayer) {
			if(visibleToPlayer) {
				GL.color(1, 1, 1, 0.15F);
				GL.depthMask(false);
				GL.enableBlend();
				GL.blendFunc(770, 771);
				GL.alphaFunc(516, 0.003921569F);
			}
			GL.pushMatrix();
			GL.scale(scale, scale, scale);
			if(renderManager.field_178639_r) model.render(DEFAULT);
			else renderModel(e, ptt);
			GL.popMatrix();
			
			if(visibleToPlayer) {
				GL.disableBlend();
				GL.alphaFunc(516, 0.1F);
				GL.depthMask(true);
			}
		}
	}
	
	protected void renderModel(EntityLivingBase e, float ptt) {
		model.render(DEFAULT);
	}
	
	protected void renderLayers(EntityLivingBase e, float swingSpeed, float swingAmount, float ptt, float age, float yawdiff, float pitch, float scale) {
//		for(LayerRenderer layer : field_177097_h) {
//			boolean var11 = func_177092_a(e, ptt, layer.shouldCombineTextures());
//			layer.doRenderLayer(e, swingSpeed, swingAmount, ptt, age, yawdiff, pitch, scale);
//
//			if(var11) func_177091_f();
//		}
	}
	
	@Override protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}
	
	protected int getColorMultiplier(EntityLivingBase e, float brightness, float ptt) {
		return 0;
	}
	
	protected boolean func_177092_a(EntityLivingBase e, float ptt, boolean combineTextures) {
		float brightness = e.getBrightness(ptt);
		int color = getColorMultiplier(e, brightness, ptt);
		boolean nontransparent = (color >> 24 & 255) > 0;
		boolean isRed = e.hurtTime > 0 || e.deathTime > 0;
		
		if(!nontransparent && !isRed) return false;
		else if(!nontransparent && !combineTextures) return false;
		else {
			GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
			GL.enableTexture();
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, GL_MODULATE);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, OpenGlHelper.defaultTexUnit);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.field_176093_u);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_REPLACE);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, OpenGlHelper.defaultTexUnit);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
			GL.setActiveTexture(OpenGlHelper.lightmapTexUnit);
			GL.enableTexture();
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, OpenGlHelper.field_176094_t);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, OpenGlHelper.field_176092_v);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.field_176091_w);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176080_A, OpenGlHelper.field_176092_v);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176076_D, GL.SRC_ALPHA);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_REPLACE);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, OpenGlHelper.field_176091_w);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
			buffer.position(0);
			
			if(isRed) {
				buffer.put(1);
				buffer.put(0);
				buffer.put(0);
				buffer.put(0.3F);
			} else {
				float a = (color >> 24 & 255) / 255F;
				float r = (color >> 16 & 255) / 255F;
				float g = (color >> 8 & 255) / 255F;
				float b = (color >> 0 & 255) / 255F;
				buffer.put(r);
				buffer.put(g);
				buffer.put(b);
				buffer.put(1 - a);
			}
			
			buffer.flip();
			glTexEnv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, buffer);
			GL.setActiveTexture(OpenGlHelper.field_176096_r);
			GL.enableTexture();
			GL.bindTexture(white.getGlTextureId());
			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, GL_MODULATE);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, OpenGlHelper.field_176091_w);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.lightmapTexUnit);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_REPLACE);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, OpenGlHelper.field_176091_w);
			glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
			GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
			return true;
		}
	}
	
	protected void func_177091_f() {
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL.enableTexture();
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, OpenGlHelper.defaultTexUnit);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.field_176093_u);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, OpenGlHelper.defaultTexUnit);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176079_G, OpenGlHelper.field_176093_u);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176086_J, GL.SRC_ALPHA);
		GL.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, GL.TEXTURE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.field_176091_w);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, GL.TEXTURE);
		GL.color(1, 1, 1, 1);
		GL.setActiveTexture(OpenGlHelper.field_176096_r);
		GL.disableTexture();
		GL.bindTexture(0);
		glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, OpenGlHelper.field_176095_s);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176099_x, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176081_B, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176082_C, GL.SRC_COLOR);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176098_y, GL.TEXTURE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176097_z, OpenGlHelper.field_176091_w);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176077_E, GL_MODULATE);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176085_I, GL.SRC_ALPHA);
		glTexEnvi(GL_TEXTURE_ENV, OpenGlHelper.field_176078_F, GL.TEXTURE);
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}
	
	protected boolean func_177090_c(EntityLivingBase e, float ptt) {
		return func_177092_a(e, ptt, true);
	}
	
	protected boolean func_177088_c(EntityLivingBase e) {
		int rgb = 0xFFFFFF;
		
		if(e instanceof EntityPlayer) {
			ScorePlayerTeam score = (ScorePlayerTeam)e.getTeam();
			
			if(score != null) {
				String format = FontRenderer.getFormatFromString(score.getColorPrefix());
				
				if(format.length() >= 2) rgb = getFontRendererFromRenderManager().func_175064_b(format.charAt(1));
			}
		}
		
		float r = (rgb >> 16 & 255) / 255F;
		float g = (rgb >> 8 & 255) / 255F;
		float b = (rgb & 255) / 255F;
		GL.disableLighting();
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL.color(r, g, b, 1);
		GL.disableTexture();
		GL.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL.disableTexture();
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
		return true;
	}
	
	protected void func_180565_e() {
		GL.enableLighting();
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL.enableTexture();
		GL.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL.enableTexture();
		GL.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}
	
	protected float getDeathMaxRotation(EntityLivingBase e) {
		return 90;
	}
	
	protected void rotateCorpse(EntityLivingBase e, float age, float yaw, float ptt) {
		GL.rotate(180 - yaw, 0, 1, 0);
		
		if(e.deathTime > 0) {
			float var5 = (e.deathTime + ptt - 1) / 20 * 1.6F;
			var5 = MathHelper.sqrt_float(var5);
			
			if(var5 > 1) var5 = 1;
			
			GL.rotate(var5 * getDeathMaxRotation(e), 0, 0, 1);
		} else {
			String var6 = EnumChatFormatting.getTextWithoutFormattingCodes(e.getName());
			
			if(var6 != null && (var6.equals("Dinnerbone") || var6.equals("Grumm")) && (!(e instanceof EntityPlayer) || ((EntityPlayer)e).func_175148_a(EnumPlayerModelParts.CAPE))) {
				GL.translate(0, e.height + 0.1F, 0);
				GL.rotate(180, 0, 0, 1);
			}
		}
	}
	
	protected float getAge(EntityLivingBase e, float ptt) {
		return e.ticksExisted + ptt;
	}
	
	protected float interpolateRotation(float prev, float next, float ptt) {
		float interp;
		for(interp = next - prev; interp < -180; interp += 360) {}
		while(interp >= 180)
			interp -= 360;
		return prev + ptt * interp;
	}
	
	protected void renderLivingAt(EntityLivingBase e, double x, double y, double z) {
		GL.translate(x, y, z);
	}
}
