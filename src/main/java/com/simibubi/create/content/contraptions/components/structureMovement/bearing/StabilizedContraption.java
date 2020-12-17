package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import com.simibubi.create.content.contraptions.components.structureMovement.AllContraptionTypes;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.result.AssemblyResult;
import com.simibubi.create.content.contraptions.components.structureMovement.result.AssemblyResults;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StabilizedContraption extends Contraption {

	private Direction facing;

	public StabilizedContraption() {}

	public StabilizedContraption(Direction facing) {
		this.facing = facing;
	}

	@Override
	public AssemblyResult assemble(World world, BlockPos pos) {
		BlockPos offset = pos.offset(facing);
		AssemblyResult result = searchMovedStructure(world, offset, null);
		if (!result.isSuccess())
			return result;
		startMoving(world);
		expandBoundsAroundAxis(Axis.Y);
		if (blocks.isEmpty())
			return AssemblyResults.UNDEFINED.get();
		return result;
	}
	
	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return false;
	}

	@Override
	protected AllContraptionTypes getType() {
		return AllContraptionTypes.STABILIZED;
	}
	
	@Override
	public CompoundNBT writeNBT(boolean spawnPacket) {
		CompoundNBT tag = super.writeNBT(spawnPacket);
		tag.putInt("Facing", facing.getIndex());
		return tag;
	}

	@Override
	public void readNBT(World world, CompoundNBT tag, boolean spawnData) {
		facing = Direction.byIndex(tag.getInt("Facing"));
		super.readNBT(world, tag, spawnData);
	}
	
	@Override
	protected boolean canAxisBeStabilized(Axis axis) {
		return false;
	}
	
	public Direction getFacing() {
		return facing;
	}

}
