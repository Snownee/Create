package com.simibubi.create.content.contraptions.components.structureMovement.result;

import java.util.List;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.registries.ForgeRegistries;

public class UnmovableBlockResult extends AssemblyResult {

	private final BlockPos pos;
	private final Block block;

	public UnmovableBlockResult(BlockPos pos, Block block) {
		super(AssemblyResults.UNMOVABLE_BLOCK);
		this.pos = pos;
		this.block = block;
	}

	public static UnmovableBlockResult read(CompoundNBT data) {
		if (!data.contains("Pos", NBT.TAG_COMPOUND) || !data.contains("Block", NBT.TAG_STRING))
			return null;
		BlockPos pos = NBTUtil.readBlockPos(data.getCompound("Pos"));
		Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryCreate(data.getString("Block")));
		return new UnmovableBlockResult(pos, block);
	}

	@Override
	public CompoundNBT write(CompoundNBT data) {
		super.write(data);
		data.put("Pos", NBTUtil.writeBlockPos(pos));
		data.putString("Block", block.getRegistryName().toString());
		return data;
	}

	@Override
	public boolean addToGoggleTooltip(List<String> tooltip, boolean isPlayerSneaking) {
		String s = Lang.translate(getLangKey(), I18n.format(block.getTranslationKey()), pos.getX(), pos.getY(), pos.getZ());
		tooltip.add(IHaveGoggleInformation.spacing + s);
		return true;
	}

}
