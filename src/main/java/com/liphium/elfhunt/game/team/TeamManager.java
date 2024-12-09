package com.liphium.elfhunt.game.team;

import com.liphium.elfhunt.game.team.impl.HunterTeam;
import com.liphium.elfhunt.game.team.impl.ElfTeam;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class TeamManager {

    private final ArrayList<Team> teams = new ArrayList<>();

    public TeamManager() {

        // Register teams
        teams.add(new ElfTeam());
        teams.add(new HunterTeam());
    }

    public Team getTeam(String name) {
        for (Team team : teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }

        return null;
    }

    public Team getTeam(Player player) {
        for (Team team : teams) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }

        return null;
    }

    public void tick() {
        for (Team team : teams) {
            team.tick();
        }
    }

    public ArrayList<Team> getTeams() {
        return teams;
    }
}
