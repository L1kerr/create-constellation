package com.limer.createtree.data;

import java.util.function.Supplier;

import com.limer.createtree.CreateTreeMod;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {

	public static final DeferredRegister<AttachmentType<?>> REGISTER =
		DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateTreeMod.MODID);

	public static final Supplier<AttachmentType<PlayerProgress>> PROGRESS =
		REGISTER.register("progress", () -> AttachmentType.serializable(PlayerProgress::new)
			.copyOnDeath()
			.build());
}
