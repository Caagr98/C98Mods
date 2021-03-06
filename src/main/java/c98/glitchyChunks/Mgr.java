package c98.glitchyChunks;

import java.util.*;

import javax.annotation.Nullable;

import c98.GlitchyChunks;

import net.minecraft.client.gui.GuiFlatPresets;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.*;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.storage.WorldInfo;

public class Mgr extends BiomeProvider {
	private BiomeProvider normal;
	private BiomeProvider nether;
	private BiomeProvider end;
	private HashMap<String, BiomeProvider> flat;
	private WorldInfo world;
	private Random rand;
	private long seed;

	public Mgr(WorldInfo var1) {
		super(var1);
		world = var1;
		seed = world.getSeed();
		rand = new Random(seed);
		normal = new BiomeProvider(var1);
		nether = new BiomeProviderSingle(Biomes.HELL);
		end = new BiomeProviderSingle(Biomes.SKY);
		flat = new HashMap();
	}

	private BiomeProvider get(int chunkX, int chunkZ) {
		rand.setSeed(seed ^ chunkX * 71 ^ chunkZ * 37);

		BiomeProvider p = null;
		int type = rand.nextInt(GlitchyChunks.num);
		if(type == 0) p = nether;
		else if(type == 1) p = end;
		else if(type == 2) {
			List<GuiFlatPresets.LayerItem> presets = GuiFlatPresets.FLAT_WORLD_PRESETS;
			String s = presets.get(rand.nextInt(presets.size())).generatorInfo;
			FlatGeneratorInfo var2 = FlatGeneratorInfo.createFlatGeneratorFromString(s);
			if(!flat.containsKey(s)) flat.put(s, new BiomeProviderSingle(Biome.getBiome(var2.getBiome())));
			p = flat.get(s);
		} else p = normal;

		GenLayer[] layers = GenLayer.initializeAllBiomeGenerators(rand.nextLong(), world.getTerrainType(), null);
		p.genBiomes = layers[0];
		p.biomeIndexLayer = layers[1];
		return p;
	}

	@Override public Biome getBiome(BlockPos pos, Biome defaultBiome) {
		return get(pos.getZ() >> 4, pos.getZ() >> 4).getBiome(pos, defaultBiome);
	}

	@Override public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
		return get(x, z).getBiomesForGeneration(biomes, x, z, width, height);
	}

	@Override public Biome[] getBiomes(@Nullable Biome[] listToReuse, int x, int z, int width, int length, boolean cacheFlag) {
		return get(x, z).getBiomes(listToReuse, x, z, width, length, cacheFlag);
	}

	@Override public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
		return get(x, z).areBiomesViable(x, z, radius, allowed);
	}

	@Override @Nullable public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomes, Random random) {
		return get(x, z).findBiomePosition(x, z, range, biomes, random);
	}
}
