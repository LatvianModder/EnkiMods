package latmod.enkitools;

import cpw.mods.fml.common.eventhandler.*;
import latmod.enkitools.rank.*;
import latmod.ftbu.api.*;
import latmod.ftbu.api.readme.EventSaveReadme;
import latmod.ftbu.util.LatCoreMC;
import latmod.ftbu.world.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class EnkiToolsEventHandler // EnkiTools
{
	public static final EnkiToolsEventHandler instance = new EnkiToolsEventHandler();
	
	@SubscribeEvent
	public void saveReadme(EventSaveReadme e)
	{
		EnkiToolsConfig.saveReadme(e.file);
	}
	
	@SubscribeEvent
	public void onReloaded(EventFTBUReload e)
	{
		if(e.side.isServer())
		{
			EnkiToolsConfig.loadConfig();
			Rank.reload();
		}
	}
	
	@SubscribeEvent
	public void playerLoggedIn(EventLMPlayerServer.LoggedIn e)
	{
		Rank.getPlayerRank(e.player);
	}
	
	@SubscribeEvent
	public void onBlockClick(net.minecraftforge.event.entity.player.PlayerInteractEvent e)
	{
		if(!e.world.isRemote && e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
		{
			TileEntity te = e.world.getTileEntity(e.x, e.y, e.z);
			
			if(te != null && !te.isInvalid() && te instanceof TileEntitySign)
			{
				TileEntitySign t = (TileEntitySign)te;
				
				if(EnkiToolsConfig.general.enableHomeSigns && t.signText[1].equals("[home]"))
				{
					LatCoreMC.runCommand(e.entityPlayer, "home " + t.signText[2]);
					e.setCanceled(true);
					return;
				}
				else if(EnkiToolsConfig.general.enableWarpSigns && !t.signText[2].isEmpty() && t.signText[1].equals("[warp]"))
				{
					LatCoreMC.runCommand(e.entityPlayer, "warp " + t.signText[2]);
					e.setCanceled(true);
					return;
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChatEvent(ServerChatEvent e)
	{
		if(!EnkiToolsConfig.general.overrideChat) return;
		
		LMPlayerServer p = LMWorldServer.inst.getPlayer(e.player);
		if(p == null) return;
		
		Rank r = Rank.getPlayerRank(p);
		if(r == null) return;
		
		String name = r.getUsername(e.username);
		
		e.component = new ChatComponentTranslation("");
		IChatComponent nameC = new ChatComponentText(name);
		
		e.component.appendSibling(new ChatComponentText("<"));
		e.component.appendSibling(nameC);
		e.component.appendSibling(new ChatComponentText("> "));
		e.component.appendSibling(new ChatComponentText(e.message));
	}
	
	@SubscribeEvent
	public void onCommandEvent(CommandEvent e)
	{
		if(e.sender instanceof EntityPlayerMP)
		{
			LMPlayerServer p = LMWorldServer.inst.getPlayer(e.sender);
			Rank r = Rank.getPlayerRank(p);
			
			if(!r.allowCommand(new RankCommand(e.command.getCommandName(), e.parameters)))
			{
				ChatComponentTranslation c = new ChatComponentTranslation("commands.generic.permission", new Object[0]);
                c.getChatStyle().setColor(EnumChatFormatting.RED);
                LatCoreMC.printChat(e.sender, c);
				e.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void customInfo(EventLMPlayerServer.CustomInfo e)
	{
		Rank r = Rank.getPlayerRank(e.player);
		if(r != null) e.info.add(new ChatComponentText(r.getColor() + '[' + r.rankID + ']'));
	}
	
	@SubscribeEvent
	public void getMaxClaimPower(EventLMPlayerServer.GetMaxClaimPower e)
	{
		Rank r = Rank.getPlayerRank(e.player);
		if(r != null)
		{
			e.result = r.getConfig(RankConfig.MAX_CLAIM_POWER).getInt();
			e.setCanceled(true);
		}
	}
}