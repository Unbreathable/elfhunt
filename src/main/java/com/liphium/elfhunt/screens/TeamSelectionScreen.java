package com.liphium.elfhunt.screens;

import com.liphium.core.inventory.CClickEvent;
import com.liphium.core.inventory.CItem;
import com.liphium.core.inventory.CScreen;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.game.team.impl.HunterTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class TeamSelectionScreen extends CScreen {

    public TeamSelectionScreen() {
        super(1, Component.text("Teams", NamedTextColor.DARK_GREEN, TextDecoration.BOLD), 3, true);

        background();
        rebuild();
    }

    public void rebuild() {
        // 9 10 11 12 13 14 15 16 17
        for (Team team : Elfhunt.getInstance().getGameManager().getTeamManager().getTeams()) {
            int slot = team instanceof HunterTeam ? 10 : 16;

            setItem(slot, new CItem(new ItemStackBuilder(team.getMaterial()).withLore(team.playerLore())
                    .withName(Component.text(team.getCc() + team.getName())).buildStack())
                    .onClick(event -> click(team, event)));
        }
    }

    public void click(Team team, CClickEvent event) {

        if (team.getPlayers().contains(event.getPlayer())) {
            team.getPlayers().remove(event.getPlayer());
        } else {

            for (Team t : Elfhunt.getInstance().getGameManager().getTeamManager().getTeams()) {
                t.getPlayers().remove(event.getPlayer());
            }
            team.addPlayer(event.getPlayer());
        }

        rebuild();
    }

}
