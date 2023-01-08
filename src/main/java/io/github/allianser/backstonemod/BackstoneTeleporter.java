package io.github.allianser.backstonemod;

import java.util.function.Function;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

public class BackstoneTeleporter implements ITeleporter
{
	protected final ServerLevel world;
	
	public BackstoneTeleporter(ServerLevel ServerLevel)
	{
		this.world = ServerLevel;
	}
	
	@Override
	public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destinationWorld, float yaw, Function<Boolean, Entity> repositionEntity)
	{
		if(entity instanceof ServerPlayer)
		{
			ServerPlayer player = (ServerPlayer) entity;
			currentWorld.getProfiler().push("placing");
			player.setLevel(destinationWorld);
			destinationWorld.addDuringPortalTeleport(player);
			currentWorld.getProfiler().pop();
		}
		return entity;
	}
	
	@Override
	public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo)
	{
		return new PortalInfo(entity.position(), Vec3.ZERO, entity.getYRot(), entity.getXRot());
	}
	
	@Override
	public boolean isVanilla()
	{
		return false;
	}

/*	
	@Override
	public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld)
	{
		return false;
	}
*/
}
