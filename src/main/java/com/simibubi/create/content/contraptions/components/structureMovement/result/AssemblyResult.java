package com.simibubi.create.content.contraptions.components.structureMovement.result;

import java.util.List;
import java.util.function.Function;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.nbt.CompoundNBT;

public class AssemblyResult {

	private final Type type;

	public AssemblyResult(Type type) {
		this.type = type;
	}

	@OverridingMethodsMustInvokeSuper
	public CompoundNBT write(CompoundNBT data) {
		data.putString("Id", type.id);
		return data;
	}

	public final Type getType() {
		return type;
	}

	public final boolean isSuccess() {
		return type == AssemblyResults.SUCCESS;
	}

	@Override
	public String toString() {
		return type.id;
	}

	protected String getLangKey() {
		return "gui.goggles.contraptions." + type.id;
	}

	public boolean addToGoggleTooltip(List<String> tooltip, boolean isPlayerSneaking) {
		if (isSuccess())
			return false;
		tooltip.add(IHaveGoggleInformation.spacing + Lang.translate(getLangKey()));
		return true;
	}

	public static final class Type<T extends AssemblyResult> {

		private final String id;
		private final AssemblyResult simple;
		private final Function<CompoundNBT, ? extends AssemblyResult> factory;

		public Type(String id, Function<CompoundNBT, T> factory) {
			this.id = id;
			this.simple = null;
			this.factory = factory;
		}

		public Type(String id) {
			this.id = id;
			this.simple = new AssemblyResult(this);
			this.factory = $ -> simple;
		}

		public String getId() {
			return id;
		}

		public T read(CompoundNBT data) {
			return (T) factory.apply(data);
		}

		public AssemblyResult get() {
			if (simple != null)
				return simple;
			else
				throw new IllegalStateException("Unable to get simple object from a complex result");
		}

	}
}
