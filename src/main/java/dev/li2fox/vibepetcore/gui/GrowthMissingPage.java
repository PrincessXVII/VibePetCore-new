package dev.li2fox.vibepetcore.gui;

import dev.li2fox.vibepetcore.core.GameText;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

final class GrowthMissingPage implements PetGuiPage {
    private final PetGuiService gui;

    GrowthMissingPage(PetGuiService gui) {
        this.gui = gui;
    }

    @Override
    public GuiPageId id() {
        return GuiPageId.GROWTH_MISSING;
    }

    @Override
    public void open(Player player) {
        open(player, "pet");
    }

    void open(Player player, String source) {
        String normalizedSource = gui.normalizeSource(source);
        Inventory inventory = Bukkit.createInventory(new PetGuiHolder("growth:" + normalizedSource), 27, gui.title(GameText.mainMenuGrowthTitle()));
        gui.fillFrame(inventory);
        inventory.setItem(13, gui.item(Material.AMETHYST_CLUSTER, GameText.mainMenuGrowthTitle(), List.of(
            gui.msg("gui.growth.missing.line.one", "&7Select an active pet core first."),
            gui.msg("gui.growth.missing.line.two", "&7Summon a pet or hold its core in your hand."),
            gui.msg("gui.growth.missing.line.three", "&8This page shows level, bond, quests, and materials.")
        )));
        inventory.setItem(22, gui.back());
        gui.playMenuOpen(player, Sound.UI_BUTTON_CLICK, 0.6F, 1.05F);
        player.openInventory(inventory);
    }
}
