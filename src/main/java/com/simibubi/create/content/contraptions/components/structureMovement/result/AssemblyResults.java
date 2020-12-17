package com.simibubi.create.content.contraptions.components.structureMovement.result;

import java.util.Map;

import com.google.common.collect.Maps;
import com.simibubi.create.content.contraptions.components.structureMovement.result.AssemblyResult.Type;

import net.minecraft.nbt.CompoundNBT;

public final class AssemblyResults {

	private static final Map<String, Type> allTypes = Maps.newHashMap();

	private AssemblyResults() {
	}

	@Deprecated
	public static final Type<AssemblyResult> UNDEFINED = register(new Type<>("undefined"));
	public static final Type<AssemblyResult> SUCCESS = register(new Type<>("success"));
	public static final Type<UnmovableBlockResult> UNMOVABLE_BLOCK = register(
			new Type<>("unmovable_block", UnmovableBlockResult::read));

	public static Type register(Type type) {
		allTypes.put(type.getId(), type);
		return type;
	}

	public static AssemblyResult read(CompoundNBT data) {
		String id = data.getString("Id");
		return allTypes.getOrDefault(id, UNDEFINED).read(data);
	}
}
