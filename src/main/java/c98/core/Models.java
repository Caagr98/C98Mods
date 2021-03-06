package c98.core;

import c98.core.impl.ModelImpl;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated public class Models {
	public static final ItemMeshDefinition DEFAULT_ITEM = new ItemMeshDefinition() {
		@Override public ModelResourceLocation getModelLocation(ItemStack is) {
			return new ModelResourceLocation(Item.REGISTRY.getNameForObject(is.getItem()), "inventory");
		}
	};
	public static final IStateMapper DEFAULT_BLOCK = new StateMap.Builder().build();

	public static void registerBlockModel(Block b, IStateMapper block) {
		ModelImpl.blockModels.put(b, block);
	}

	public static void registerItemModel(Item i, ItemMeshDefinition item) {
		ModelImpl.itemModels.put(i, item);
	}

	public static void registerItemModel(Item i) {
		registerItemModel(i, DEFAULT_ITEM);
	}

	public static void registerModel(Block b) {
		registerItemModel(Item.getItemFromBlock(b));
	}
}
