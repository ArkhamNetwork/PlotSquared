////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////
package com.intellectualcrafters.plot.commands;

import java.util.UUID;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.EventUtil;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.intellectualsites.commands.Argument;
import com.intellectualsites.commands.CommandDeclaration;
import com.intellectualsites.commands.CommandCaller;
import com.plotsquared.bukkit.util.bukkit.uuid.SQLUUIDHandler;

@CommandDeclaration(
        command = "add",
        aliases = {"a"},
        description = "Allow a user to build while you are online",
        usage = "/plot add <player>",
        category = CommandCategory.ACTIONS,
        permission = "plots.add",
        requiredType = PlotPlayer.class
)
public class Add extends SubCommand {

    public Add() {
        requiredArguments = new Argument[] {
                Argument.PlayerName
        };
    }

    @Override
    public boolean onCommand(CommandCaller caller, String[] args) {
        final PlotPlayer plr = (PlotPlayer) caller.getSuperCaller();
        final Location loc = plr.getLocation();
        final Plot plot = MainUtil.getPlot(loc);
        if (plot == null) {
            return !sendMessage(plr, C.NOT_IN_PLOT);
        }
        if (!plot.hasOwner()) {
            MainUtil.sendMessage(plr, C.PLOT_UNOWNED);
            return false;
        }
        if (!plot.isOwner(plr.getUUID()) && !Permissions.hasPermission(plr, "plots.admin.command.add")) {
            MainUtil.sendMessage(plr, C.NO_PLOT_PERMS);
            return true;
        }
        UUID uuid;
        if (args[0].equalsIgnoreCase("*")) {
            uuid = DBFunc.everyone;
        } else {
            uuid = UUIDHandler.getUUID(args[0], null);
        }
        if (uuid == null) {
            if (UUIDHandler.implementation instanceof SQLUUIDHandler) {
                MainUtil.sendMessage(plr, C.INVALID_PLAYER_WAIT, args[0]);
            } else {
                MainUtil.sendMessage(plr, C.INVALID_PLAYER, args[0]);
            }
            return false;
        }
        if (plot.isOwner(uuid)) {
            MainUtil.sendMessage(plr, C.ALREADY_OWNER);
            return false;
        }
        
        if (plot.getMembers().contains(uuid)) {
            MainUtil.sendMessage(plr, C.ALREADY_ADDED);
            return false;
        }
        if (plot.removeTrusted(uuid)) {
            plot.addMember(uuid);
        }
        else {
            if (plot.getMembers().size() + plot.getTrusted().size() >= PS.get().getPlotWorld(plot.world).MAX_PLOT_MEMBERS) {
                MainUtil.sendMessage(plr, C.PLOT_MAX_MEMBERS);
                return false;
            }
            if (plot.getDenied().contains(uuid)) {
                plot.removeDenied(uuid);
            }
            plot.addMember(uuid);
        }
        EventUtil.manager.callMember(plr, plot, uuid, true);
        MainUtil.sendMessage(plr, C.MEMBER_ADDED);
        return true;
    }
}
