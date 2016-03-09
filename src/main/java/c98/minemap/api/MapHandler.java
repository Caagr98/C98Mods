package c98.minemap.api;

import net.minecraft.block.material.MapColor;
import net.minecraft.world.chunk.Chunk;

public abstract class MapHandler {
	public abstract int calc(Chunk chunk, int x, int z, int plY);

	public void line(int x) {}

	protected int getColor(byte colorID, int x) {
		if(colorID / 4 == 0) return x * 8 + 16 << 24; //Handle transparency
		int color = MapColor.mapColorArray[(colorID & 0xFF) / 4].colorValue;
		int brightness = colorID & 3;
		short multiplier = 220;

		if(brightness == 2) multiplier = 255;

		if(brightness == 0) multiplier = 180;

		int r = (color >> 020 & 255) * multiplier / 255;
		int g = (color >> 010 & 255) * multiplier / 255;
		int b = (color >> 000 & 255) * multiplier / 255;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}