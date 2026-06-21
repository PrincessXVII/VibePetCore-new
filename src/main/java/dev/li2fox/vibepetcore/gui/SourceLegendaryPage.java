package dev.li2fox.vibepetcore.gui;

import dev.li2fox.vibepetcore.core.GameText;
import dev.li2fox.vibepetcore.pet.PetType;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

final class SourceLegendaryPage implements PetGuiPage {
    private final PetGuiService gui;

    SourceLegendaryPage(PetGuiService gui) {
        this.gui = gui;
    }

    @Override
    public GuiPageId id() {
        return GuiPageId.SOURCE_LEGENDARY;
    }

    @Override
    public void open(Player player) {
        open(player, "master");
    }

    void open(Player player, String source) {
        String normalizedSource = gui.normalizeSource(source);
        Inventory inventory = Bukkit.createInventory(new PetGuiHolder("legendary:" + normalizedSource), 54, gui.title(gui.msg("gui.legendary.title", "&dLegendary traits")));
        gui.fillFrame(inventory);
        inventory.setItem(4, gui.item(Material.CALIBRATED_SCULK_SENSOR, gui.msg("gui.legendary.header.title", "&dLegendary traits"), List.of(
            gui.msg("gui.legendary.header.line.one", "&7These effects work only for legendary pets."),
            gui.msg("gui.legendary.header.line.two", "&7Most traits have a 300 sec cooldown and trigger in combat."),
            gui.msg("gui.legendary.header.line.three", "&8Allay keeps its own Vex form logic.")
        )));

        int[] petSlots = gui.petHelpSlots();
        List<PetType> types = gui.playablePetTypes();
        for (int index = 0; index < petSlots.length && index < types.size(); index++) {
            PetType type = types.get(index);
            inventory.setItem(petSlots[index], gui.item(gui.eggMaterial(type), "&d" + GameText.petTypeName(type), gui.legendaryLore(type)));
        }

        inventory.setItem(49, gui.back());
        gui.playMenuOpen(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6F, 1.15F);
        player.openInventory(inventory);
    }
}
