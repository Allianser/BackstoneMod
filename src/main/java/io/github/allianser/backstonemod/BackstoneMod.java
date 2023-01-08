package io.github.allianser.backstonemod;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
//import net.minecraft.sounds.SoundEvent;

@Mod(value = BackstoneMod.MODID)
public class BackstoneMod {
	public static final String MODID = "backstonemod";
	public static final String NAME = "Backstone Mod";
	public static final String VERSION = "0.1";
	
	public static Item backstone;
	
/*
	public static SoundEvent channelSoundEvent;
	public static SoundEvent castSoundEvent;
	public static SoundEvent impactSoundEvent;
*/
	
	public BackstoneMod()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, io.github.allianser.backstonemod.BackstoneConfig.SPEC);
		MinecraftForge.EVENT_BUS.register(new BackstoneEventHandler());
	}
	
	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents
	{
		@SubscribeEvent
		public static void Items(final RegistryEvent.Register<Item> event)
		{
			BackstoneMod.backstone = new ItemBackstone();
			event.getRegistry().register(backstone);
		}

/*		
		@SubscribeEvent
		public static void registerSounds(final RegistryEvent.Register<SoundEvent> event)
		{
			HearthstoneMod.castSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstonecast")).setRegistryName("hearthstonecast");
			HearthstoneMod.impactSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstoneimpact")).setRegistryName("hearthstoneimpact");
			HearthstoneMod.channelSoundEvent = new SoundEvent(new ResourceLocation(MODID, "hearthstonechannel")).setRegistryName("hearthstonechannel");
			event.getRegistry().register(castSoundEvent);
			event.getRegistry().register(impactSoundEvent);
			event.getRegistry().register(channelSoundEvent);
		}
*/
	}

}
