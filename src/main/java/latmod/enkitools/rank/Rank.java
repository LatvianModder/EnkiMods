package latmod.enkitools.rank;

import java.util.*;

import latmod.enkitools.*;
import latmod.ftbu.core.LatCoreMC;
import latmod.ftbu.core.net.LMNetHelper;
import latmod.ftbu.core.util.*;
import latmod.ftbu.core.world.*;
import net.minecraft.util.EnumChatFormatting;

import com.google.gson.annotations.Expose;

public class Rank
{
	public String rankID;
	@Expose public String color;
	@Expose public String prefix;
	@Expose public String parentRank;
	@Expose public List<String> commands;
	@Expose private Map<String, String> config;
	public final FastMap<RankConfig, RankConfig.Inst> configMap = new FastMap<RankConfig, RankConfig.Inst>();
	
	public void setDefaults()
	{
		if(color == null) color = EnumChatFormatting.YELLOW.getFormattingCode() + "";
		if(prefix == null) prefix = "";
	}
	
	public String getColor()
	{
		if(color == null || color.isEmpty()) return "";
		String s = "";
		for(int i = 0; i < color.length(); i++)
			s += LatCoreMC.FORMATTING + color.charAt(i);
		return s;
	}
	
	public String getUsername(String s)
	{ return getColor() + prefix + s + EnumChatFormatting.RESET; }
	
	public String toString()
	{ return rankID; }
	
	public boolean allowCommand(String cmd, String[] args)
	{
		for(int i = 0; i < commands.size(); i++)
		{
			String orig = commands.get(i);
			if(orig.equals("*")) return true;
			
			String s = orig.substring(1);
			boolean add = orig.startsWith("+");
			
			if(cmdsEquals(s, cmd))
				return add;
		}
		
		if(parentRank == null || parentRank.length() == 0)
			return false;
		Rank r = Rank.getRank(parentRank);
		if(r == null) return false;
		return r.allowCommand(cmd, args);
	}
	
	public RankConfig.Inst getConfig(RankConfig c)
	{
		RankConfig.Inst o = configMap.get(c);
		
		if(o == null)
		{
			if(parentRank == null || parentRank.isEmpty())
			{
				o = new RankConfig.Inst(c, c.defaultValue);
				configMap.put(c, o);
			}
			else
			{
				Rank r = Rank.getRank(parentRank);
				if(r != null) return r.getConfig(c);
			}
		}
		
		return o;
	}
	
	private boolean cmdsEquals(String cmd, String perm)
	{
		if(cmd.equals(perm))
			return true;
		
		return false;
	}
	
	// Static //
	
	private static class RanksFile
	{
		@Expose public String defaultRank;
		@Expose public Map<String, Rank> ranks;
	}
	
	private static Rank defRank = null;
	private static final FastMap<String, Rank> ranks = new FastMap<String, Rank>();
	private static final FastMap<UUID, Rank> playerRanks = new FastMap<UUID, Rank>();
	
	public static void reload()
	{
		RanksFile ranksFile;
		
		if(!EnkiData.ranks.exists())
		{
			EnkiData.ranks = LMFileUtils.newFile(EnkiData.ranks);
			
			ranksFile = new RanksFile();
			ranksFile.ranks = new HashMap<String, Rank>();
			ranksFile.defaultRank = DefaultRanks.loadDefaultRanks(ranksFile.ranks);
			
			LatCore.toJsonFile(EnkiData.ranks, ranksFile);
		}
		else ranksFile = LatCore.fromJsonFile(EnkiData.ranks, RanksFile.class);
		
		ranks.clear();
		
		for(String k : ranksFile.ranks.keySet())
		{
			Rank v = ranksFile.ranks.get(k);
			v.rankID = k;
			v.configMap.clear();
			v.setDefaults();
			
			if(v.config != null && v.config.size() > 0)
			{
				for(String cfgK : v.config.keySet())
				{
					String cfgV = v.config.get(cfgK);
					
					RankConfig c = RankConfig.registry.get(cfgK);
					if(c != null) v.configMap.put(c, new RankConfig.Inst(c, cfgV));
				}
			}
			
			ranks.put(k, v);
		}
		
		defRank = ranks.get(ranksFile.defaultRank);
		
		EnkiTools.mod.logger.info("Loaded ranks [Def: " + defRank + "]: " + ranks.values);
		
		playerRanks.clear();
		
		try
		{
			FastList<String> al = LMFileUtils.load(EnkiData.players);
			
			if(al != null && al.size() > 0)
			{
				for(int i = 0; i < al.size(); i++)
				{
					String[] s = al.get(i).split(": ");
					
					if(s != null && s.length >= 2)
					{
						String k = s[0]; if(k.indexOf(',') != -1) k = k.split(",")[0];
						setRawRank(LMWorldServer.inst.getPlayer(k), Rank.getRank(s[1]));
					}
				}
			}
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		saveRanks();
	}
	
	private static void setRawRank(LMPlayerServer p, Rank r)
	{
		if(p == null) return;
		playerRanks.put(p.getUUID(), (r == null) ? defRank : r);
	}
	
	public static void setPlayerRank(LMPlayerServer p, Rank r)
	{
		setRawRank(p, r);
		saveRanks();
		if(p != null)
		{
			p.updateMaxClaimPower();
			p.sendUpdate(null, true);
			LMNetHelper.sendTo(null, p.getInfo());
		}
	}
	
	public static void saveRanks()
	{
		FastList<String> al = new FastList<String>();
		
		for(int i = 0; i < playerRanks.size(); i++)
		{
			UUID id = playerRanks.keys.get(i);
			LMPlayerServer p = LMWorldServer.inst.getPlayer(id);
			if(p != null) al.add(p.uuidString + "," + p.getName() + ": " + playerRanks.values.get(i));
			else al.add(LatCoreMC.toShortUUID(id) + ": " + playerRanks.values.get(i));
		}
		
		al.sort(null);
		
		try { LMFileUtils.save(EnkiData.players, al); }
		catch(Exception e) { e.printStackTrace(); }
	}
	
	private static boolean hasLoaded = false;
	public static Rank getPlayerRank(LMPlayerServer p)
	{
		if(!hasLoaded) { reload(); hasLoaded = true; }
		
		if(p == null) return defRank;
		Rank r = playerRanks.get(p.getUUID());
		
		if(r == null)
		{
			r = defRank;
			playerRanks.put(p.getUUID(), r);
			saveRanks();
			return r;
		}
		
		return r;
	}
	
	public static Rank getDefaultRank()
	{ return defRank; }
	
	public static Rank getRank(String s)
	{
		Rank r = ranks.get(s);
		if(r != null) return r;
		return defRank;
	}
	
	public static RankConfig.Inst getConfig(Object o, RankConfig c)
	{ return getPlayerRank(LMWorldServer.inst.getPlayer(o)).getConfig(c); }
	
	public void setConfig(RankConfig c, String val)
	{
		config.put(c.key, val);
		configMap.put(c, new RankConfig.Inst(c, val));
	}
	
	public static final String[] getAllRanks()
	{ return ranks.keys.toArray(new String[0]); }
}