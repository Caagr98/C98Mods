package c98.glitchyChunks;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.gui.GuiFlatPresets;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.gen.*;

import c98.GlitchyChunks;
import c98.core.C98Log;

public class Gen extends ChunkProviderOverworld {
	private ChunkProviderOverworld normal;
	private ChunkProviderHell nether;
	private ChunkProviderEnd end;
	private HashMap<String, ChunkProviderFlat> flat;
	private World world;
	private long seed;
	private boolean struct;
	private Random rand;

	public Gen(World wld, long seed, boolean structs, String settings) {
		super(wld, seed, structs, settings);
		world = wld;
		this.seed = seed;
		struct = structs;
		rand = new Random(seed);
		normal = new ChunkProviderOverworld(wld, seed, struct, settings);
		nether = new ChunkProviderHell(wld, structs, seed);
		end = new ChunkProviderEnd(wld, structs, seed);
		flat = new HashMap();
	}

	@Override public Chunk provideChunk(int par1, int par2) {
		return get(par1 << 4, par2 << 4).provideChunk(par1, par2);
	}

	private IChunkGenerator get(int chunkX, int chunkZ) {
		rand.setSeed(seed ^ chunkX * 71 ^ chunkZ * 37);

		IChunkGenerator p = null;
		int type = rand.nextInt(GlitchyChunks.num);
		if(type == 0) p = nether;
		else if(type == 1) p = end;
		else if(type == 2) {
			List<GuiFlatPresets.LayerItem> presets = GuiFlatPresets.FLAT_WORLD_PRESETS;
			String s = presets.get(rand.nextInt(presets.size())).generatorInfo;
			if(!flat.containsKey(s)) flat.put(s, new ChunkProviderFlat(world, seed, struct, s));
			p = flat.get(s);
		} else p = normal;


		try {
			long chunkSeed = rand.nextLong();

			for(Field field : p.getClass().getDeclaredFields())
				if(field.getType() == NoiseGeneratorOctaves.class) {
					field.setAccessible(true);
					NoiseGeneratorOctaves g = (NoiseGeneratorOctaves)field.get(p);
					field.set(p, new NoiseGeneratorOctaves(new Random(chunkSeed), g.octaves));
				}
			return p;
		} catch(IllegalArgumentException | IllegalAccessException e) {
			C98Log.error(e);
		}
		return null;
	}
}
