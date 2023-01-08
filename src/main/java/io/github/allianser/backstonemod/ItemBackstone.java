package io.github.allianser.backstonemod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

import io.github.allianser.backstonemod.BackstoneConfig.BackstoneSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
//import net.minecraft.sounds.SoundSource;

public class ItemBackstone extends Item
{
public static final String NAME = "backstone";
	
	public final TranslatableComponent TEXT_ON_COOLDOWN = new TranslatableComponent("item.backstone.on_cooldown");
	public final TranslatableComponent TEXT_NO_LINK = new TranslatableComponent("item.backstone.no_link");
	public final TranslatableComponent TEXT_MISSING_LINK = new TranslatableComponent("item.backstone.missing_link");
	public final TranslatableComponent TEXT_LINKED = new TranslatableComponent("item.backstone.linked");
	public final TranslatableComponent TEXT_CANCELED = new TranslatableComponent("item.backstone.canceled");
	public final TranslatableComponent TEXT_COOLDOWN = new TranslatableComponent("item.backstone.cooldown");
	public final TranslatableComponent TEXT_MINUTES = new TranslatableComponent("item.backstone.minutes");
	public final TranslatableComponent TEXT_SECONDS = new TranslatableComponent("item.backstone.seconds");
	
	public final MutableComponent TEXT_HOME_SET = new TranslatableComponent("item.backstone.home_set");
	
	private Method createResourceKeyMethod = ObfuscationReflectionHelper.findMethod(ResourceKey.class, "m_135790_", ResourceLocation.class, ResourceLocation.class);
	
	public ItemBackstone()
	{
		super(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
		setRegistryName(NAME);
	}
	
	@Override
	public void inventoryTick(ItemStack itemStack, Level world, Entity entity, int itemSlot, boolean isSelected)
	{
		// server side
		if(!world.isClientSide)
		{
			CompoundTag tag = itemStack.getTag();
			
			// if item has tag, decrement cooldown
			if(tag != null)
			{
				int cooldown = tag.getInt("cooldown");
				if(cooldown > 0)
				{
					cooldown--;
					tag.putInt("cooldown", cooldown);
				}
			}
			// if no tag, add a tag
			else
			{
				tag = new CompoundTag();
				tag.putInt("cooldown", 0);
				tag.putInt("castTime", 0);
				tag.putInt("stoneX", 0);
				tag.putInt("stoneY", 0);
				tag.putInt("stoneZ", 0);
				tag.putString("dimensionResourceLocationParent", "");
				tag.putString("dimensionResourceLocation", "");
				tag.putDouble("prevX", -1);
				tag.putDouble("prevY", -1);
				tag.putDouble("prevZ", -1);
				tag.putBoolean("isCasting", false);
				tag.putBoolean("stopCasting", false);
			}
			
			// if player is casting
			if(tag.getBoolean("isCasting") && entity instanceof Player)
			{
				Player player = (Player) entity;
				
				// check if player is holding backstone
				ItemStack heldItem = player.getMainHandItem();
				if(heldItem != null)
				{
					if(heldItem != itemStack)
					{
						tag.putBoolean("stopCasting", true);
					}
				}
				else
					tag.putBoolean("stopCasting", true);
				
				// detect player movement
				double diffX = Math.abs(tag.getDouble("prevX") - player.getX());
				double diffY = Math.abs(tag.getDouble("prevY") - player.getY());
				double diffZ = Math.abs(tag.getDouble("prevZ") - player.getZ());
				
				//cancel cast on player move or item swap
				if(((diffX > 0.05 || diffY > 0.05 || diffZ > 0.05) && tag.getDouble("prevY") != -1) || tag.getBoolean("stopCasting"))
				{
					tag.putInt("castTime", 0);
					tag.putBoolean("isCasting", false);
					tag.putBoolean("stopCasting", false);
					player.displayClientMessage(TEXT_CANCELED, true);
				}
				else
				{
					// increment cast time
					tag.putInt("castTime", tag.getInt("castTime") + 1);
				}
				
				// initiate tp after casting
				if(tag.getInt("castTime") >= BackstoneSettings.channelTime.get())
				{
					// stop and reset cast time
					tag.putInt("castTime", 0);
					tag.putBoolean("isCasting", false);
					
//					world.playSound(null, player.getX(), player.getY(), player.getZ(), BackstoneMod.castSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
					
					// get waystone location
					int stoneX = tag.getInt("stoneX");
					int stoneY = tag.getInt("stoneY");
					int stoneZ = tag.getInt("stoneZ");
					
					// create dimension registry key from NBT
//					Moving same world check to initiation of channeling
					ResourceKey<Level> savedDimensionKey = getWorldResourceKey(tag.getString("dimensionResourceLocationParent"), tag.getString("dimensionResourceLocation"));
					ResourceKey<Level> playerDimensionKey = player.level.dimension();
					
					// if player is not in same dimension as waystone, travel to that dimension
					if(savedDimensionKey != null)
					{
						if(playerDimensionKey.compareTo(savedDimensionKey) != 0)
						{
							MinecraftServer server = player.level.getServer();
							ServerLevel destinationServerLevel = server.getLevel(savedDimensionKey);
							ServerPlayer serverPlayer = (ServerPlayer) player;
							player = (Player) serverPlayer.changeDimension(destinationServerLevel, new BackstoneTeleporter(destinationServerLevel));
							player.moveTo(stoneX, stoneY, stoneZ);
							world = player.level;
						}
					}
					
					// get block at waystone location
					BlockPos stonePos = new BlockPos(stoneX, stoneY, stoneZ);
					BlockState state = world.getBlockState(stonePos);
					Block block = state.getBlock();
					
					// checks if waystone is still there
					if(block.isBed(state, world, stonePos, player))
					{
						// find open spaces around waystone
						boolean north = player.level.getBlockState(stonePos.north()).getBlock().isPossibleToRespawnInThis();
						boolean northUp = player.level.getBlockState(stonePos.north().above()).getBlock().isPossibleToRespawnInThis();
						boolean northDown = player.level.getBlockState(stonePos.north().below()).getMaterial().isSolid();
						
						boolean east = player.level.getBlockState(stonePos.east()).getBlock().isPossibleToRespawnInThis();
						boolean eastUp = player.level.getBlockState(stonePos.east().above()).getBlock().isPossibleToRespawnInThis();
						boolean eastDown = player.level.getBlockState(stonePos.east().below()).getMaterial().isSolid();
						
						boolean south = player.level.getBlockState(stonePos.south()).getBlock().isPossibleToRespawnInThis();
						boolean southUp = player.level.getBlockState(stonePos.south().above()).getBlock().isPossibleToRespawnInThis();
						boolean southDown = player.level.getBlockState(stonePos.south().below()).getMaterial().isSolid();
						
						boolean west = player.level.getBlockState(stonePos.west()).getBlock().isPossibleToRespawnInThis();
						boolean westUp = player.level.getBlockState(stonePos.west().above()).getBlock().isPossibleToRespawnInThis();
						boolean westDown = player.level.getBlockState(stonePos.west().below()).getMaterial().isSolid();
						
						// tp player next to waystone
						if(north && northUp && northDown)
						{
							player.teleportTo(stonePos.north().getX() + 0.5, stonePos.north().getY(), stonePos.north().getZ() + 0.5);
						}
						else if(east && eastUp && eastDown)
						{
							player.teleportTo(stonePos.east().getX() + 0.5, stonePos.east().getY(), stonePos.east().getZ() + 0.5);
						}
						else if(south && southUp && southDown)
						{
							player.teleportTo(stonePos.south().getX() + 0.5, stonePos.south().getY(), stonePos.south().getZ() + 0.5);
						}
						else if(west && westUp && westDown)
						{
							player.teleportTo(stonePos.west().getX() + 0.5, stonePos.west().getY(), stonePos.west().getZ() + 0.5);
						}
						else // if no open space, tp player on top of bed
						{
							player.teleportTo(stoneX + 0.5, stoneY + 1, stoneZ + 0.5);
						}
						
//						world.playSound(null, player.getX(), player.getY(), player.getZ(), BackstoneMod.impactSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						
						// sets waystone on cooldown
						tag.putInt("cooldown", BackstoneSettings.cooldownTime.get());
					}
					
					// tp player to where waystone was, then breaks link
					else
					{
						player.teleportTo(stoneX + 0.5, stoneY + 1, stoneZ + 0.5);

//						world.playSound(null, player.getX(), player.getY(), player.getZ(), BackstoneMod.impactSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						
						// sets backstone on cooldown
						tag.putInt("cooldown", BackstoneSettings.cooldownTime.get());
						// clears the saved dimension
						tag.putString("dimensionResourceLocationParent", "");
						tag.putString("dimensionResourceLocation", "");
						// informs player of broken link
						player.displayClientMessage(TEXT_MISSING_LINK, true);
					}
				}
			}
			// record position of player for detecting movement
			tag.putDouble("prevX", entity.getX());
			tag.putDouble("prevY", entity.getY());
			tag.putDouble("prevZ", entity.getZ());
			
			// save tag
			itemStack.setTag(tag);
		}
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		if(!world.isClientSide && hand == InteractionHand.MAIN_HAND)
		{
			ItemStack itemStack = player.getItemInHand(hand);
			CompoundTag tagCompound = itemStack.getTag();
			
			// if not sneaking
			if(!player.isCrouching())
			{
				// if location is set
				if(tagCompound.getString("dimensionResourceLocation") != "")
				{
					int cooldown = tagCompound.getInt("cooldown");
					
					// if off cooldown
					if(cooldown == 0)
					{
						// if player is not casting, start casting
						if(!tagCompound.getBoolean("isCasting"))
						{
							tagCompound.putBoolean("isCasting", true);
//							world.playSound(player, player.getX(), player.getY(), player.getZ(), BackstoneMod.channelSoundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
						}
					}
					// if on cooldown
					else
					{
						player.displayClientMessage(TEXT_ON_COOLDOWN, true);
					}
				}
				// if location is not set
				else
				{
					player.displayClientMessage(TEXT_NO_LINK, true);
				}
			}
			// save tag
			itemStack.setTag(tagCompound);
		}
		return new InteractionResultHolder(InteractionResult.PASS, player.getItemInHand(hand));
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		if(!context.getLevel().isClientSide())
		{
			ItemStack itemStack = context.getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
			// if main hand is not a backstone, return
			if(itemStack.getItem() != BackstoneMod.backstone)
			{
				return InteractionResult.FAIL;
			}
			
			CompoundTag tagCompound = itemStack.getTag();
			
			// if sneaking
			if(context.getPlayer().isCrouching())
			{
				// checks if block right clicked is bed
				//replace check to check for signpost:waystone or instanceof(waystone)
				BlockState state = context.getLevel().getBlockState(context.getClickedPos());
				if(context.getLevel().getBlockState(context.getClickedPos()).getBlock().isBed(state, context.getLevel(), context.getClickedPos(), context.getPlayer()))
				{
					// links bed to hearthstone
					ResourceKey<Level> dimensionKey = context.getPlayer().level.dimension();
					
					tagCompound.putInt("stoneX", context.getClickedPos().getX());
					tagCompound.putInt("stoneY", context.getClickedPos().getY());
					tagCompound.putInt("stoneZ", context.getClickedPos().getZ());
					tagCompound.putString("dimensionResourceLocationParent", dimensionKey.getRegistryName().toString());
					tagCompound.putString("dimensionResourceLocation", dimensionKey.location().toString());
					context.getPlayer().displayClientMessage(TEXT_LINKED, true);
				}
				// save tag
				itemStack.setTag(tagCompound);
				return InteractionResult.SUCCESS;
			}
			else
			{
				use(context.getLevel(), context.getPlayer(), InteractionHand.MAIN_HAND);
			}
		}
		return InteractionResult.FAIL;
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
	{
		if(oldStack.getItem() == newStack.getItem())
		{
			CompoundTag oldTag = oldStack.getTag();
			CompoundTag newTag = newStack.getTag();
			if(oldTag != null && newTag != null)
			{
				if(oldTag.getInt("stoneX") == newTag.getInt("stoneX") && oldTag.getInt("stoneY") == newTag.getInt("stoneY") && oldTag.getInt("stoneZ") == newTag.getInt("stoneZ"))
				{
					if(oldTag.getInt("cooldown") < (newTag.getInt("cooldown") + 20) && oldTag.getInt("cooldown") > (newTag.getInt("cooldown") - 20))
						return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean showDurabilityBar(ItemStack itemStack)
	{
		CompoundTag tagCompound = itemStack.getTag();
		if(tagCompound != null)
		{
			return tagCompound.getInt("cooldown") > 0 || tagCompound.getInt("castTime") > 0;
		}
		
		return false;
	}
	
	@Override
	public double getDurabilityForDisplay(ItemStack itemStack)
	{
		CompoundTag tagCompound = itemStack.getTag();
		if(tagCompound.getInt("cooldown") > 0)
			return (double) tagCompound.getInt("cooldown") / (double) BackstoneSettings.cooldownTime.get();
		else
			return (double) 1 - (tagCompound.getInt("castTime") / (double) BackstoneSettings.channelTime.get());
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemStack, @Nullable Level world, List<Component> tooltip, TooltipFlag flagIn)
	{
		CompoundTag tag = itemStack.getTag();
		if(tag != null)
		{
			// display if backstone is linked to a waystone
			if(tag.getString("dimensionResourceLocation") != "")
				tooltip.add(TEXT_HOME_SET.withStyle(ChatFormatting.GRAY));
			
			// calculates and displays cooldown in minutes and seconds
			int cooldown = tag.getInt("cooldown");
			if(cooldown != 0)
			{
				cooldown += 19; // more intuitive cooldown timer
				float minutesExact, secondsExact;
				int minutes, seconds;
				minutesExact = cooldown / 1200;
				minutes = (int) minutesExact;
				secondsExact = cooldown / 20;
				seconds = (int) (secondsExact - (minutes * 60));
				
				MutableComponent MutableComponent = new TextComponent("").append(TEXT_COOLDOWN).append(Integer.toString(minutes)).append(TEXT_MINUTES).append(Integer.toString(seconds)).append(TEXT_SECONDS);
				tooltip.add(MutableComponent.withStyle(ChatFormatting.GRAY));
			}
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack stack)
	{
		CompoundTag tag = stack.getTag();
		if(tag != null)
		{
			return tag.getBoolean("isCasting");
		}
		return false;
	}
	
	
	private ResourceKey<Level> getWorldResourceKey(String locationParent, String location)
	{
		if(locationParent != "" && location != "")
		{
			ResourceLocation dimensionResourceLocationParent = new ResourceLocation(locationParent);
			ResourceLocation dimensionResourceLocation = new ResourceLocation(location);
			try
			{
				return (ResourceKey<Level>) createResourceKeyMethod.invoke(null, dimensionResourceLocationParent, dimensionResourceLocation);
			}
			catch(IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch(InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}
	
}
