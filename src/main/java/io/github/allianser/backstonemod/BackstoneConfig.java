package io.github.allianser.backstonemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class BackstoneConfig
{
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final BackstoneSettings SETTINGS = new BackstoneSettings(BUILDER);
	public static final ForgeConfigSpec SPEC = BUILDER.build();
	
	public static class BackstoneSettings
	{
		public static ConfigValue<Integer> channelTime;
		public static ConfigValue<Integer> cooldownTime;
		
		BackstoneSettings(ForgeConfigSpec.Builder builder)
		{
			builder.push("Settings");
			//default channel time is 10 seconds
			channelTime = builder.comment("How long to cast TP").translation("config.backstone.channel_time").define("ChannelTime", 200);
			//default cooldown time is 20 minutes (one daylight/half a day)
			cooldownTime = builder.comment("How long to wait between TP").translation("config.backstone.cooldown_time").define("CooldownTime", 24000);
			builder.pop();
		}
	}
}
