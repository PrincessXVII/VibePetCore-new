package dev.li2fox.vibepetcore.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.li2fox.vibepetcore.config.BalanceConfig;
import dev.li2fox.vibepetcore.economy.EconomyManager;
import dev.li2fox.vibepetcore.player.PlayerData;
import dev.li2fox.vibepetcore.player.PlayerDataManager;
import dev.li2fox.vibepetcore.player.QuestProgressData;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

final class QuestManagerTurnInRollbackTest {
    @Test
    void saveFailureRestoresItemsProgressPointsAndStatistics() throws Exception {
        Fixture fixture = fixture(true);
        UUID playerId = UUID.randomUUID();
        PlayerData playerData = fixture.playerDataManager().getOrLoad(playerId);
        QuestProgressData progress = fixture.questManager().progress(playerId, "gather_wheat");
        progress.setAccepted(true);
        Player player = player(playerId, GameMode.SURVIVAL, new FakeItemStack(Material.WHEAT, 3));

        QuestManager.TurnInResult result = fixture.questManager().turnInResult(player, "gather_wheat");

        assertFalse(result.turnedIn());
        assertTrue(result.saveFailed());
        assertEquals(3, count(player, Material.WHEAT));
        assertTrue(progress.accepted());
        assertFalse(progress.completed());
        assertEquals(0, progress.progress());
        assertEquals(0L, playerData.points());
        assertEquals(0L, playerData.statistics().questsCompleted());
    }

    @Test
    void successfulTurnInConsumesItemsAndAwardsQuestRewards() throws Exception {
        Fixture fixture = fixture(false);
        UUID playerId = UUID.randomUUID();
        PlayerData playerData = fixture.playerDataManager().getOrLoad(playerId);
        QuestProgressData progress = fixture.questManager().progress(playerId, "gather_wheat");
        progress.setAccepted(true);
        Player player = player(playerId, GameMode.SURVIVAL, new FakeItemStack(Material.WHEAT, 3));

        QuestManager.TurnInResult result = fixture.questManager().turnInResult(player, "gather_wheat");

        assertTrue(result.turnedIn());
        assertFalse(result.saveFailed());
        assertEquals(0, count(player, Material.WHEAT));
        assertTrue(progress.completed());
        assertEquals(3, progress.progress());
        assertEquals(10L, playerData.points());
        assertEquals(1L, playerData.statistics().questsCompleted());
    }

    private Fixture fixture(boolean failSave) throws Exception {
        BalanceConfig balanceConfig = new BalanceConfig(null);
        setField(balanceConfig, "config", questConfig());
        PlayerDataManager playerDataManager = new PlayerDataManager(dummyPlugin());
        setField(playerDataManager, "storage", storageProxy(failSave));
        EconomyManager economyManager = new EconomyManager(playerDataManager, balanceConfig);
        QuestManager questManager = new QuestManager(balanceConfig, playerDataManager, economyManager);
        questManager.reload();
        return new Fixture(questManager, playerDataManager);
    }

    private YamlConfiguration questConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("language", "en");
        yaml.set("economy.quests.gather_wheat.title", "Gather Wheat");
        yaml.set("economy.quests.gather_wheat.category", "gather");
        yaml.set("economy.quests.gather_wheat.description", "Bring wheat.");
        yaml.set("economy.quests.gather_wheat.type", "PICKUP_ITEM");
        yaml.set("economy.quests.gather_wheat.target", "WHEAT");
        yaml.set("economy.quests.gather_wheat.amount", 3);
        yaml.set("economy.quests.gather_wheat.reward-points", 10L);
        yaml.set("economy.quests.gather_wheat.repeat-cooldown-minutes", 0L);
        return yaml;
    }

    private Object storageProxy(boolean failSave) throws Exception {
        Class<?> storageType = Class.forName("dev.li2fox.vibepetcore.player.PlayerStorage");
        return Proxy.newProxyInstance(
            storageType.getClassLoader(),
            new Class<?>[] {storageType},
            (proxy, method, args) -> switch (method.getName()) {
                case "enable", "close" -> null;
                case "load" -> Optional.empty();
                case "save" -> {
                    if (failSave) {
                        throw new IOException("simulated save failure");
                    }
                    yield null;
                }
                case "topByPoints", "playerIds" -> List.of();
                case "name" -> "test-storage";
                case "toString" -> "test-storage";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private Player player(UUID playerId, GameMode gameMode, ItemStack... contents) {
        AtomicReference<ItemStack[]> storage = new AtomicReference<>(cloneContents(contents));
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
            PlayerInventory.class.getClassLoader(),
            new Class<?>[] {PlayerInventory.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getStorageContents" -> storage.get();
                case "setStorageContents" -> {
                    storage.set(cloneContents((ItemStack[]) args[0]));
                    yield null;
                }
                case "toString" -> "test-player-inventory";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> playerId;
                case "getGameMode" -> gameMode;
                case "getInventory" -> inventory;
                case "updateInventory", "sendMessage" -> null;
                case "toString" -> "test-player";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private int count(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            clone[index] = source[index] == null ? null : source[index].clone();
        }
        return clone;
    }

    private JavaPlugin dummyPlugin() throws Exception {
        return (JavaPlugin) unsafe().allocateInstance(DummyPlugin.class);
    }

    private Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Fixture(QuestManager questManager, PlayerDataManager playerDataManager) {
    }

    public static final class DummyPlugin extends JavaPlugin {
        private static final Logger LOGGER = Logger.getLogger(DummyPlugin.class.getName());

        @Override
        public Logger getLogger() {
            return LOGGER;
        }
    }

    private static final class FakeItemStack extends ItemStack {
        private Material type;
        private int amount;

        private FakeItemStack(Material type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        @Override
        public Material getType() {
            return type;
        }

        @Override
        public void setType(Material type) {
            this.type = type;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            return new FakeItemStack(type, amount);
        }
    }
}
