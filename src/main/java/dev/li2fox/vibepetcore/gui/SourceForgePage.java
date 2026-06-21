package dev.li2fox.vibepetcore.gui;

import dev.li2fox.vibepetcore.core.GameText;
import dev.li2fox.vibepetcore.player.OwnedPetData;
import dev.li2fox.vibepetcore.pet.PetType;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

final class SourceForgePage implements PetGuiPage {
    private final PetGuiService gui;

    SourceForgePage(PetGuiService gui) {
        this.gui = gui;
    }

    @Override
    public GuiPageId id() {
        return GuiPageId.SOURCE_FORGE;
    }

    @Override
    public void open(Player player) {
        open(player, "master");
    }

    void open(Player player, String source) {
        Inventory inventory = Bukkit.createInventory(new PetGuiHolder("forge:" + gui.normalizeSource(source)), 54, gui.title(GameText.guiTitleForge()));
        gui.fillFrame(inventory);

        Optional<OwnedPetData> base = gui.heldPetData(player);
        inventory.setItem(13, base
            .map(pet -> gui.item(
                gui.eggMaterial(PetType.parse(pet.petType()).orElse(PetType.WOLF)),
                "&b" + pet.petName(),
                gui.rarityForgeCoreLore(player, pet)
            ))
            .orElseGet(() -> gui.item(Material.BARRIER, "&c" + GameText.petOverviewNoCore(), List.of(
                GameText.forgeNeedActiveCore(),
                GameText.guiUnavailable()
            ))));
        inventory.setItem(22, base
            .map(pet -> gui.item(Material.ANVIL, "&a" + GameText.forgeUpgradeTitle(), gui.rarityForgeAttemptLore(player, pet)))
            .orElseGet(() -> gui.item(Material.ANVIL, "&7" + GameText.forgeUpgradeTitle(), List.of(
                GameText.forgeNeedActiveCore(),
                GameText.guiUnavailable()
            ))));
        inventory.setItem(31, gui.item(Material.BOOK, "&e" + GameText.forgeInfoTitle(), List.of(
            GameText.forgeInfoLineOne(),
            GameText.forgeInfoLineTwo(),
            GameText.forgeInfoLineThree(),
            GameText.forgeInfoLineFour()
        )));
        inventory.setItem(40, gui.item(Material.CHEST, "&6" + GameText.forgeDonorChestTitle(), List.of(
            GameText.forgeDonorChestHint()
        )));
        inventory.setItem(49, gui.back());
        gui.playMenuOpen(player, Sound.BLOCK_ANVIL_USE, 0.7F, 1.1F);
        player.openInventory(inventory);
    }

    @Override
    public boolean handleClick(Player player, String menuId, int slot) {
        if (slot == 22 && gui.allowGuiAction(player)) {
            gui.attemptRarityUpgrade(player, gui.sourceFromMenu(menuId));
        }
        return true;
    }
}
