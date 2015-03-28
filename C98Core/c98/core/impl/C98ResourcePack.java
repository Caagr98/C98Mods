package c98.core.impl;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import c98.core.C98Core;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

public class C98ResourcePack implements IResourcePack { //TODO sounds.json

	@Override public InputStream getInputStream(ResourceLocation l) {
		String ns = l.getResourceDomain();
		String path = l.getResourcePath();
		if(ns.equals("c98") && path.startsWith("lang/")) return C98Core.modList.stream().map(mod -> mod.getName()).map(name -> name.toLowerCase()).map(name -> new ResourceLocation(ns + "/" + name, path)).filter(loc -> resourceExists(loc)).map(loc -> getInputStream(loc)).reduce((a, b) -> new SequenceInputStream(a, b)).get();
		return C98Core.class.getResourceAsStream("/assets/" + l.getResourceDomain() + "/" + l.getResourcePath());
	}
	
	@Override public boolean resourceExists(ResourceLocation l) {
		if(l.getResourceDomain().equals("c98") && l.getResourcePath().startsWith("lang/")) return true;
		return getInputStream(l) != null;
	}
	
	@Override public Set getResourceDomains() {
		return ImmutableSet.of("c98");
	}
	
	@Override public IMetadataSection getPackMetadata(IMetadataSerializer var1, String var2) throws IOException {
		return var1.parseMetadataSection(var2, new JsonObject());
	}
	
	@Override public BufferedImage getPackImage() throws IOException {
		return ImageIO.read(DefaultResourcePack.class.getResourceAsStream("/" + new ResourceLocation("pack.png").getResourcePath()));
	}
	
	@Override public String getPackName() {
		return "C98Mods";
	}
	
}
