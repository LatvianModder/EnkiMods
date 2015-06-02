package latmod.enkitools.cmd;

import latmod.core.Teleporter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class CmdSpawn extends CmdEnki
{
	public CmdSpawn()
	{ super("spawn"); }
	
	public String onCommand(ICommandSender ics, String[] args)
	{
		EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
		WorldServer w = DimensionManager.getWorld(0);
		ChunkCoordinates spawn = w.getSpawnPoint();
		
		//while(ep.worldObj.getBlock(spawn.posX, spawn.posY, spawn.posZ).getCollisionBoundingBoxFromPool(w, spawn.posX, spawn.posY, spawn.posZ) != null)
		//	spawn.posY++;
		
		if(Teleporter.travelEntity(ep, spawn.posX + 0.5D, spawn.posY + 1D, spawn.posZ + 0.5D, 0))
			return FINE + "Teleported to spawn";
		return "Failed to teleport!";
	}
}