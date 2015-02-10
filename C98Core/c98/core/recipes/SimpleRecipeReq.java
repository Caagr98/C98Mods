package c98.core.recipes;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class SimpleRecipeReq extends RecipeReq {
	private ItemStack is;
	
	public SimpleRecipeReq(ItemStack itemStack) {
		is = itemStack;
	}
	
	@Override public ItemStack getResult(InventoryCrafting inv, BlockPos coords, int gridX, int gridY, boolean mirror) {
		return is;
	}
}
