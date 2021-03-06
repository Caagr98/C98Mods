package c98.magic.furnace;

import java.util.Random;

import c98.magic.item.IItemConnection;
import c98.magic.item.IItemSource;
import c98.magic.xp.IXpConnection;
import c98.magic.xp.XpUtils;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BlockMagicFurnace extends BlockContainer {
	public static class TE extends TileEntityLockable implements ITickable, IXpConnection, IItemConnection, IItemSource {
		public static final int IN = 0, OUT = 1;
		public static final int FIELD_TIME = 0, FIELD_TOTAL_TIME = 1;
		private ItemStack[] slots = new ItemStack[2];
		private int cookTime;
		private int cookTimeTotal;
		private String customName;

		@Override public int getSizeInventory() {
			return slots.length;
		}

		@Override public boolean func_191420_l() {
			for (ItemStack itemstack : this.slots)
				if (!itemstack.func_190926_b())
					return false;

			return true;
		}


		@Override public ItemStack getStackInSlot(int slot) {
			return slots[slot];
		}

		@Override public ItemStack decrStackSize(int index, int count) {
			ItemStack is = null;
			if(slots[index] != null) {
				if(slots[index].stackSize <= count) {
					is = slots[index];
					slots[index] = null;
				} else {
					is = slots[index].splitStack(count);
					if(slots[index].stackSize == 0) slots[index] = null;
				}
			}
			return is;
		}

		@Override public ItemStack removeStackFromSlot(int index) {
			if(slots[index] != null) {
				ItemStack is = slots[index];
				slots[index] = null;
				return is;
			}
			return null;
		}

		@Override public void setInventorySlotContents(int index, ItemStack stack) {
			boolean same = stack != null && stack.isItemEqual(slots[index]) && ItemStack.areItemStackTagsEqual(stack, slots[index]);
			slots[index] = stack;

			if(stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();

			if(index == IN && !same) {
				cookTimeTotal = getCookTime(stack);
				cookTime = 0;
				markDirty();
			}
		}

		@Override public String getName() {
			return hasCustomName() ? customName : "container.magic_furnace";
		}

		@Override public boolean hasCustomName() {
			return customName != null && customName.length() > 0;
		}

		public void setCustomInventoryName(String name) {
			customName = name;
		}

		@Override public void readFromNBT(NBTTagCompound compound) {
			super.readFromNBT(compound);
			NBTTagList items = compound.getTagList("Items", 10);
			slots = new ItemStack[getSizeInventory()];

			for(int i = 0; i < items.tagCount(); ++i) {
				NBTTagCompound comp = items.getCompoundTagAt(i);
				byte slot = comp.getByte("Slot");

				if(slot >= 0 && slot < slots.length) slots[slot] = new ItemStack(comp);
			}

			cookTime = compound.getShort("CookTime");
			cookTimeTotal = compound.getShort("CookTimeTotal");

			if(compound.hasKey("CustomName", 8)) customName = compound.getString("CustomName");
		}

		@Override public NBTTagCompound writeToNBT(NBTTagCompound compound) {
			super.writeToNBT(compound);
			compound.setShort("CookTime", (short)cookTime);
			compound.setShort("CookTimeTotal", (short)cookTimeTotal);
			NBTTagList items = new NBTTagList();

			for(int i = 0; i < slots.length; ++i)
				if(slots[i] != null) {
					NBTTagCompound comp = new NBTTagCompound();
					comp.setByte("Slot", (byte)i);
					slots[i].writeToNBT(comp);
					items.appendTag(comp);
				}

			compound.setTag("Items", items);
			if(hasCustomName()) compound.setString("CustomName", customName);
			return compound;
		}

		@Override public int getInventoryStackLimit() {
			return 64;
		}

		@Override public void update() {
			boolean wasBurning = cookTime != -1;
			boolean changed = false;

			if(!world.isRemote) {
				if(cookTime == -1 && slots[IN] == null) {
					if(cookTime == -1 && cookTime > 0) cookTime = MathHelper.clamp(cookTime - 2, 0, cookTimeTotal);
				} else {
					if(cookTime == -1 && canSmelt()) {
						XpUtils.take(this);
						cookTime = 0;
					}
					if(cookTime != -1 && canSmelt()) {
						++cookTime;

						if(cookTime == cookTimeTotal) {
							cookTime = 0;
							cookTimeTotal = getCookTime(slots[IN]);
							smeltItem();
							changed = true;
						}
					} else cookTime = -1;
				}

				if(wasBurning != (cookTime != -1)) changed = true;
				//				BlockFurnace.func_176446_a(isBurning(), worldObj, pos); //TODO change blockstate
			}

			if(changed) markDirty();
		}

		public int getCookTime(ItemStack p_174904_1_) {
			return 200;
		}

		private boolean canSmelt() {
			if(slots[0] == null) return false;
			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(slots[IN]);
			ItemStack out = slots[OUT];
			if(result == null) return false;
			if(out == null) return true;
			if(!out.isItemEqual(result)) return false;
			if(out.stackSize < getInventoryStackLimit() && out.stackSize < out.getMaxStackSize()) return true;
			if(out.stackSize >= result.getMaxStackSize()) return false;
			return XpUtils.canTake(this);
		}

		public void smeltItem() {
			if(canSmelt()) {
				ItemStack is = FurnaceRecipes.instance().getSmeltingResult(slots[IN]);
				if(slots[OUT] == null) slots[OUT] = is.copy();
				else if(slots[OUT].getItem() == is.getItem()) ++slots[OUT].stackSize;
				--slots[IN].stackSize;
				if(slots[IN].stackSize <= 0) slots[IN] = null;
			}
		}

		@Override public boolean isUsableByPlayer(EntityPlayer playerIn) {
			return world.getTileEntity(pos) != this ? false : playerIn.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 8 * 8;
		}

		@Override public void openInventory(EntityPlayer playerIn) {}

		@Override public void closeInventory(EntityPlayer playerIn) {}

		@Override public boolean isItemValidForSlot(int index, ItemStack stack) {
			return index == IN;
		}

		@Override public String getGuiID() {
			return getBlockType().unlocalizedName;
		}

		@Override public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
			return new ContainerMagicFurnace(playerInventory, this);
		}

		@Override public int getField(int id) {
			switch(id) {
				case FIELD_TIME:
					return cookTime;
				case FIELD_TOTAL_TIME:
					return cookTimeTotal;
				default:
					return 0;
			}
		}

		@Override public void setField(int id, int value) {
			switch(id) {
				case FIELD_TIME:
					cookTime = value;
					break;
				case FIELD_TOTAL_TIME:
					cookTimeTotal = value;
			}
		}

		@Override public int getFieldCount() {
			return 2;
		}

		@Override public void clear() {
			for(int i = 0; i < slots.length; ++i)
				slots[i] = null;
		}

		@Override public IInventory getStacks(EnumFacing f) {
			return this;
		}

		@Override public boolean canTakeItem(IInventory inv, int slot, ItemStack is) {
			return slot == OUT;
		}

		@Override public boolean isXpInput(EnumFacing f) {
			return true;
		}

		@Override public boolean isXpOutput(EnumFacing f) {
			return false;
		}

		@Override public boolean isItemInput(EnumFacing f) {
			return true;
		}

		@Override public boolean isItemOutput(EnumFacing f) {
			return true;
		}
	}

	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockMagicFurnace() {
		super(Material.ROCK);
	}

	@Override public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		if(!worldIn.isRemote) {
			IBlockState n = worldIn.getBlockState(pos.north());
			IBlockState s = worldIn.getBlockState(pos.south());
			IBlockState w = worldIn.getBlockState(pos.west());
			IBlockState e = worldIn.getBlockState(pos.east());
			EnumFacing facing = state.getValue(FACING);

			if(facing == EnumFacing.NORTH && n.isFullBlock() && !s.isFullBlock()) facing = EnumFacing.SOUTH;
			else if(facing == EnumFacing.SOUTH && s.isFullBlock() && !n.isFullBlock()) facing = EnumFacing.NORTH;
			else if(facing == EnumFacing.WEST  && w.isFullBlock() && !e.isFullBlock()) facing = EnumFacing.EAST;
			else if(facing == EnumFacing.EAST  && e.isFullBlock() && !w.isFullBlock()) facing = EnumFacing.WEST;

			worldIn.setBlockState(pos, state.withProperty(FACING, facing), 2);
		}
	}

	@Override public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
		if(state.getValue(ACTIVE) == Boolean.TRUE) {
			double x = pos.getX() + 0.5D;
			double y = pos.getY() + rand.nextDouble() * 6 / 16;
			double z = pos.getZ() + 0.5D;
			double off = 0.52D;
			double side = rand.nextDouble() * 0.6D - 0.3D;

			switch(state.getValue(FACING)) {
				case WEST:
					worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x - off, y, z + side, 0, 0, 0);
					worldIn.spawnParticle(EnumParticleTypes.FLAME, x - off, y, z + side, 0, 0, 0);
					break;
				case EAST:
					worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + off, y, z + side, 0, 0, 0);
					worldIn.spawnParticle(EnumParticleTypes.FLAME, x + off, y, z + side, 0, 0, 0);
					break;
				case NORTH:
					worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + side, y, z - off, 0, 0, 0);
					worldIn.spawnParticle(EnumParticleTypes.FLAME, x + side, y, z - off, 0, 0, 0);
					break;
				case SOUTH:
					worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + side, y, z + off, 0, 0, 0);
					worldIn.spawnParticle(EnumParticleTypes.FLAME, x + side, y, z + off, 0, 0, 0);
					break;
				default:
					break;
			}
		}
	}

	@Override public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote) return true;
		playerIn.displayGUIChest((TE)worldIn.getTileEntity(pos));
		playerIn.addStat(StatList.FURNACE_INTERACTION);
		return true;
	}

	@Override public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TE();
	}

	@Override public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
		if(stack.hasDisplayName()) ((TE)worldIn.getTileEntity(pos)).setCustomInventoryName(stack.getDisplayName());
	}

	@Override public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		InventoryHelper.dropInventoryItems(worldIn, pos, (TE)worldIn.getTileEntity(pos));
		worldIn.updateComparatorOutputLevel(pos, this);
		super.breakBlock(worldIn, pos, state);
	}

	@Override public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
		return Container.calcRedstone(worldIn.getTileEntity(pos));
	}

	@Override public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		return new ItemStack(this);
	}

	@Override public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.getFront(meta);
		if(facing.getAxis() == EnumFacing.Axis.Y) facing = EnumFacing.NORTH;
		return getDefaultState().withProperty(FACING, facing);
	}

	@Override public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override public BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, ACTIVE);
	}
}
