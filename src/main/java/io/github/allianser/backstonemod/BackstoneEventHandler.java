package io.github.allianser.backstonemod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

public class BackstoneEventHandler
{
	// stops casting if the player takes damage
		@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
		public void onLivingHurtEvent(LivingHurtEvent event)
		{
			if(event.getEntity() instanceof Player)
			{
				Player player = (Player) event.getEntity();
				ItemStack currentItem = player.getInventory().getSelected();
				if(currentItem != null)
				{
					if(currentItem.getItem() instanceof ItemBackstone)
					{
						CompoundTag tagCompound = currentItem.getTag();
						tagCompound.putBoolean("stopCasting", true);
						currentItem.setTag(tagCompound);
					}
				}
			}
		}

/*		//uncomment when made sounds
		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
		public void onPlaySoundAtEntityEvent(PlaySoundAtEntityEvent event)
		{
			if(event.getSound() == BackstoneMod.channelSoundEvent)
			{
				event.setCanceled(true);
				Minecraft.getInstance().getSoundManager().play(new BackstoneChannelSound(event.getEntity()));
			}
		}
*/
}
