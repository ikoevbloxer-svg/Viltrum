package com.viltrum.registry;

import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Viltrum.MODID);

    public static final Supplier<AttachmentType<PowerData>> POWER = ATTACHMENTS.register("power",
            () -> AttachmentType.builder(() -> new PowerData())
                    .serialize(new IAttachmentSerializer<CompoundTag, PowerData>() {
                        @Override
                        public PowerData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            PowerData d = new PowerData();
                            d.read(tag);
                            return d;
                        }

                        @Override
                        public CompoundTag write(PowerData attachment, HolderLookup.Provider provider) {
                            CompoundTag tag = new CompoundTag();
                            attachment.write(tag);
                            return tag;
                        }
                    })
                    .copyOnDeath()
                    .build());
}
