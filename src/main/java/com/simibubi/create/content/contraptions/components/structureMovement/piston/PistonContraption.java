package com.simibubi.create.content.contraptions.components.structureMovement.piston;

import com.simibubi.create.content.contraptions.components.structureMovement.AllContraptionTypes;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementTraits;
import com.simibubi.create.content.contraptions.components.structureMovement.TranslatingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock.*;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.PistonType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.simibubi.create.AllBlocks.MECHANICAL_PISTON_HEAD;
import static com.simibubi.create.AllBlocks.PISTON_EXTENSION_POLE;
import static com.simibubi.create.content.contraptions.components.structureMovement.piston.MechanicalPistonBlock.*;
import static net.minecraft.state.properties.BlockStateProperties.FACING;

public class PistonContraption extends TranslatingContraption {

	protected int extensionLength;
	protected int initialExtensionProgress;
	protected Direction orientation;

	private AxisAlignedBB pistonExtensionCollisionBox;
	private boolean retract;

	@Override
	protected AllContraptionTypes getType() {
		return AllContraptionTypes.PISTON;
	}

	public PistonContraption() {}

	public PistonContraption(Direction direction, boolean retract) {
		orientation = direction;
		this.retract = retract;
	}

	@Override
	public boolean assemble(World world, BlockPos pos) {
		if (!collectExtensions(world, pos, orientation))
			return false;
		int count = blocks.size();
		if (!searchMovedStructure(world, anchor, retract ? orientation.getOpposite() : orientation))
			return false;
		if (blocks.size() == count) { // no new blocks added
			bounds = pistonExtensionCollisionBox;
		} else {
			bounds = bounds.union(pistonExtensionCollisionBox);
		}
		startMoving(world);
		return true;
	}

	private boolean collectExtensions(World world, BlockPos pos, Direction direction) {
		List<BlockInfo> poles = new ArrayList<>();
		BlockPos actualStart = pos;
		BlockState nextBlock = world.getBlockState(actualStart.offset(direction));
		int extensionsInFront = 0;
		BlockState blockState = world.getBlockState(pos);
		boolean sticky = isStickyPiston(blockState);

		if (!isPiston(blockState))
			return false;

		if (blockState.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
			while (PistonPolePlacementHelper.matchesAxis(nextBlock, direction.getAxis()) || isPistonHead(nextBlock) && nextBlock.get(FACING) == direction) {

				actualStart = actualStart.offset(direction);
				poles.add(new BlockInfo(actualStart, nextBlock.with(FACING, direction), null));
				extensionsInFront++;

				if (isPistonHead(nextBlock))
					break;

				nextBlock = world.getBlockState(actualStart.offset(direction));
				if (extensionsInFront > MechanicalPistonBlock.maxAllowedPistonPoles())
					return false;
			}
		}

		if (extensionsInFront == 0)
			poles.add(new BlockInfo(pos, MECHANICAL_PISTON_HEAD.getDefaultState()
				.with(FACING, direction)
				.with(BlockStateProperties.PISTON_TYPE, sticky ? PistonType.STICKY : PistonType.DEFAULT), null));
		else
			poles.add(new BlockInfo(pos, PISTON_EXTENSION_POLE.getDefaultState()
				.with(FACING, direction), null));

		BlockPos end = pos;
		nextBlock = world.getBlockState(end.offset(direction.getOpposite()));
		int extensionsInBack = 0;

		while (PistonPolePlacementHelper.matchesAxis(nextBlock, direction.getAxis())) {
			end = end.offset(direction.getOpposite());
			poles.add(new BlockInfo(end, nextBlock.with(FACING, direction), null));
			extensionsInBack++;
			nextBlock = world.getBlockState(end.offset(direction.getOpposite()));

			if (extensionsInFront + extensionsInBack > MechanicalPistonBlock.maxAllowedPistonPoles())
				return false;
		}

		anchor = pos.offset(direction, initialExtensionProgress + 1);
		extensionLength = extensionsInBack + extensionsInFront;
		initialExtensionProgress = extensionsInFront;
		pistonExtensionCollisionBox = new AxisAlignedBB(
				BlockPos.ZERO.offset(direction, -1),
				BlockPos.ZERO.offset(direction, -extensionLength - 1)).expand(1,
						1, 1);

		if (extensionLength == 0)
			return false;

		bounds = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

		for (BlockInfo pole : poles) {
			BlockPos relPos = pole.pos.offset(direction, -extensionsInFront);
			BlockPos localPos = relPos.subtract(anchor);
			getBlocks().put(localPos, new BlockInfo(localPos, pole.state, null));
			//pistonExtensionCollisionBox = pistonExtensionCollisionBox.union(new AxisAlignedBB(localPos));
		}

		return true;
	}

	@Override
	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pistonExtensionCollisionBox.contains(VecHelper.getCenterOf(pos.subtract(anchor)));
	}

	@Override
	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction direction, Queue<BlockPos> frontier) {
		frontier.clear();
		boolean sticky = isStickyPiston(world.getBlockState(pos.offset(orientation, -1)));
		boolean retracting = direction != orientation;
		if (retracting && !sticky)
			return true;
		for (int offset = 0; offset <= AllConfigs.SERVER.kinetics.maxChassisRange.get(); offset++) {
			if (offset == 1 && retracting)
				return true;
			BlockPos currentPos = pos.offset(orientation, offset + initialExtensionProgress);
			if (!world.isBlockPresent(currentPos))
				return false;
			BlockState state = world.getBlockState(currentPos);
			if (!BlockMovementTraits.movementNecessary(state, world, currentPos))
				return true;
			if (BlockMovementTraits.isBrittle(state) && !(state.getBlock() instanceof CarpetBlock))
				return true;
			if (isPistonHead(state) && state.get(FACING) == direction.getOpposite())
				return true;
			if (!BlockMovementTraits.movementAllowed(state, world, currentPos))
				return retracting;
			frontier.add(currentPos);
			if (BlockMovementTraits.notSupportive(state, orientation))
				return true;
		}
		return true;
	}

	@Override
	public void addBlock(BlockPos pos, Pair<BlockInfo, TileEntity> capture) {
		super.addBlock(pos.offset(orientation, -initialExtensionProgress), capture);
	}

	@Override
	public BlockPos toLocalPos(BlockPos globalPos) {
		return globalPos.subtract(anchor)
			.offset(orientation, -initialExtensionProgress);
	}

	@Override
	protected boolean customBlockPlacement(IWorld world, BlockPos pos, BlockState state) {
		BlockPos pistonPos = anchor.offset(orientation, -1);
		BlockState pistonState = world.getBlockState(pistonPos);
		TileEntity te = world.getTileEntity(pistonPos);
		if (pos.equals(pistonPos)) {
			if (te == null || te.isRemoved())
				return true;
			if (!isExtensionPole(state) && isPiston(pistonState))
				world.setBlockState(pistonPos, pistonState.with(MechanicalPistonBlock.STATE, PistonState.RETRACTED),
					3 | 16);
			return true;
		}
		return false;
	}

	@Override
	protected boolean customBlockRemoval(IWorld world, BlockPos pos, BlockState state) {
		BlockPos pistonPos = anchor.offset(orientation, -1);
		BlockState blockState = world.getBlockState(pos);
		if (pos.equals(pistonPos) && isPiston(blockState)) {
			world.setBlockState(pos, blockState.with(MechanicalPistonBlock.STATE, PistonState.MOVING), 66 | 16);
			return true;
		}
		return false;
	}

	@Override
	public void readNBT(World world, CompoundNBT nbt, boolean spawnData) {
		super.readNBT(world, nbt, spawnData);
		initialExtensionProgress = nbt.getInt("InitialLength");
		extensionLength = nbt.getInt("ExtensionLength");
		orientation = Direction.byIndex(nbt.getInt("Orientation"));
	}

	@Override
	public CompoundNBT writeNBT(boolean spawnPacket) {
		CompoundNBT tag = super.writeNBT(spawnPacket);
		tag.putInt("InitialLength", initialExtensionProgress);
		tag.putInt("ExtensionLength", extensionLength);
		tag.putInt("Orientation", orientation.getIndex());
		return tag;
	}

}
