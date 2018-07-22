package com.wimbli.WorldBorder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.World;


public class BorderCheckTask implements Runnable
{
	@Override
	public void run()
	{
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		Collection<Player> players = ImmutableList.copyOf(Bukkit.getServer().getOnlinePlayers());

		for (Player player : players)
		{
			checkPlayer(player, null, false, true);
		}
	}

	// track players who are being handled (moved back inside the border) already; needed since Bukkit is sometimes sending teleport events with the old (now incorrect) location still indicated, which can lead to a loop when we then teleport them thinking they're outside the border, triggering event again, etc.
	private static Set<String> handlingPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());

	// set targetLoc only if not current player location; set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
	public static Location checkPlayer(Player player, Location targetLoc, boolean returnLocationOnly, boolean notify)
	{
		player.sendMessage(Config.Message());
		return null;
	}
	public static Location checkPlayer(Player player, Location targetLoc, boolean returnLocationOnly)
	{
		return checkPlayer(player, targetLoc, returnLocationOnly, true);
	}

	private static Location newLocation(Player player, Location loc, BorderData border, boolean notify)
	{
		if (Config.Debug())
		{
			Config.logWarn((notify ? "Border crossing" : "Check was run") + " in \"" + loc.getWorld().getName() + "\". Border " + border.toString());
			Config.logWarn("Player position X: " + Config.coord.format(loc.getX()) + " Y: " + Config.coord.format(loc.getY()) + " Z: " + Config.coord.format(loc.getZ()));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound(), player.isFlying());

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.logWarn("Target new location unviable, using spawn or killing player.");
			if (Config.getIfPlayerKill())
			{
				player.setHealth(0.0D);
				return null;
			}
			newLoc = player.getWorld().getSpawnLocation();
		}

		if (Config.Debug())
			Config.logWarn("New position in world \"" + newLoc.getWorld().getName() + "\" at X: " + Config.coord.format(newLoc.getX()) + " Y: " + Config.coord.format(newLoc.getY()) + " Z: " + Config.coord.format(newLoc.getZ()));

		if (notify)
			player.sendMessage(Config.Message());

		return newLoc;
	}

	private static void setPassengerDelayed(final Entity vehicle, final Player player, final String playerName, long delay)
	{
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				handlingPlayers.remove(playerName.toLowerCase());
				if (vehicle == null || player == null)
					return;

				vehicle.setPassenger(player);
			}
		}, delay);
	}
}
