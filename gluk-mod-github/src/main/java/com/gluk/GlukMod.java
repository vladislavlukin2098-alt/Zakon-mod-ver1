package com.gluk;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GlukMod.MOD_ID)
public class GlukMod {
    public static final String MOD_ID = "gluk";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Block> GLUK_PORTAL = BLOCKS.register("gluk_portal",
            () -> new GlukPortalBlock(BlockBehaviour.Properties.of()
                    .noCollission().noOcclusion().strength(-1.0f).sound(SoundType.GLASS)
                    .lightLevel(state -> 11).noLootTable().pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<EntityType<GlukEntity>> GLUK = ENTITY_TYPES.register("gluk",
            () -> EntityType.Builder.of(GlukEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("gluk"));

    public static final RegistryObject<Item> GLUK_SPAWN_EGG = ITEMS.register("gluk_spawn_egg",
            () -> new ForgeSpawnEggItem(GLUK, 0x111111, 0xFFFFFF, new Item.Properties()));

    public static final RegistryObject<Item> ERROR_OF_BEING = ITEMS.register("oshibka_bytiya",
            () -> new ErrorOfBeingItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> FIRE_ARTIFACT = ITEMS.register("fire_artifact",
            () -> new FireArtifactItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> LIGHTNING_ARTIFACT = ITEMS.register("lightning_artifact",
            () -> new LightningArtifactItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public GlukMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addToCreativeTab);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GLUK.get(), GlukEntity.createAttributes().build());
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(GLUK_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ERROR_OF_BEING.get());
            event.accept(FIRE_ARTIFACT.get());
            event.accept(LIGHTNING_ARTIFACT.get());
        }
    }
}
