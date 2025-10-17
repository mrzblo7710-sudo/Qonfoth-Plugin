package com.qonfoth.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class QonfothPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("✅ Qonfoth Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("❌ Qonfoth Plugin Disabled!");
    }
}

main: com.qonfoth.plugin.QonfothPlugin

version: 1.0.0

api-version: 1.21

description: Qonfoth — Rules GUI plugin with multi-page rules and Arabic/English support for Paper 1.21.8

commands:

rules:

description: Open the rules GUI (choose language then view pages)

usage: /rules

permissions:

qonfoth.admin:

description: Admin permission to force show rules or reload config

default: op


---

config.yml

----------------

# Increment this whenever you change rules to force re-show for all players

config-version: 1

languages:

arabic:

display-name: "العربية"

locale-code: "ar"

pages:

- - "1) احترم اللاعبين: لا تسب أو تهين الآخرين."

- "2) لا غش: ممنوع استخدام شيتات أو أكواد خارجية."

- "3) لا سرقة: لا تختلس ممتلكات اللاعبين."

- - "4) لا تضر العالم: تجنب التدمير أو التخريب العمدي."

- "5) لا استغلال الأخطاء: أبلغ عن أي بَغ تجد."

- "6) احترام الأدلة: اتبع قوانين المشرفين."

- - "7) استخدام أسماء لائقة: لا أسماء مسيئة."

- "8) لا نشر روابط خبيثة أو محتوى غير مناسب."

- "9) استمتع باللعب واحترم أوقات الصلاة واللاعبين الآخرين."

accept-button-name: "✅ أوافق على القواعد"

accept-button-lore:

- "بالضغط على هذا الزر توافق على قوانين السيرفر."

english:

display-name: "English"

locale-code: "en"

pages:

- - "1) Respect others: No insults or harassment."

- "2) No cheating: Don't use external cheats or hacks."

- "3) No stealing: Don't take other players' items."

- - "4) No griefing: Avoid intentional world damage."

- "5) Report bugs: Do not exploit glitches."

- "6) Follow staff guidance: Respect admins and moderators."

- - "7) Use appropriate names: No offensive nicknames."

- "8) No malicious links or inappropriate content."

- "9) Have fun and respect other players' time."

accept-button-name: "✅ I accept the rules"

accept-button-lore:

- "Clicking this button means you agree to follow server rules."

messages:

choose-lang-title: "اختر لغتك / Choose your language"

rules-inventory-title: "القواعد — Page %page% (%lang%)"

already-agreed: "أنت بالفعل قبلت القواعد — تم."

must-accept-to-play: "يجب قبول القواعد للعب على السيرفر."

kicked-for-refuse: "تم طردك لأنك رفضت قبول القواعد."


---

// Java source: src/main/java/com/qonfoth/plugin/QonfothPlugin.java package com.qonfoth.plugin;

import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.Material; import org.bukkit.NamespacedKey; import org.bukkit.command.Command; import org.bukkit.command.CommandSender; import org.bukkit.configuration.file.FileConfiguration; import org.bukkit.configuration.file.YamlConfiguration; import org.bukkit.entity.Player; import org.bukkit.event.EventHandler; import org.bukkit.event.Listener; import org.bukkit.event.inventory.InventoryClickEvent; import org.bukkit.event.player.PlayerJoinEvent; import org.bukkit.inventory.Inventory; import org.bukkit.inventory.ItemFlag; import org.bukkit.inventory.ItemStack; import org.bukkit.inventory.meta.ItemMeta; import org.bukkit.plugin.java.JavaPlugin;

import java.io.File; import java.io.IOException; import java.util.*;

public class QonfothPlugin extends JavaPlugin implements Listener {

private File dataFile;
private YamlConfiguration dataYaml;
private int configVersion;
private final Map<UUID, String> pendingLanguageSelection = new HashMap<>(); // temporary helper

@Override
public void onEnable() {
    saveDefaultConfig();
    getServer().getPluginManager().registerEvents(this, this);
    loadDataFile();
    configVersion = getConfig().getInt("config-version", 1);
    getCommand("rules").setExecutor(this::onRulesCommand);
    getLogger().info("Qonfoth Plugin enabled");
}

@Override
public void onDisable() {
    saveDataFile();
    getLogger().info("Qonfoth Plugin disabled");
}

private void loadDataFile() {
    dataFile = new File(getDataFolder(), "data.yml");
    if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
    if (!dataFile.exists()) {
        try {
            dataFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
    }
    dataYaml = YamlConfiguration.loadConfiguration(dataFile);
}

private void saveDataFile() {
    try {
        dataYaml.save(dataFile);
    } catch (IOException e) { e.printStackTrace(); }
}

private boolean hasPlayerAgreed(UUID uuid) {
    if (!dataYaml.contains("agreed." + uuid.toString())) return false;
    int playerVersion = dataYaml.getInt("agreed." + uuid.toString(), 0);
    return playerVersion >= configVersion;
}

private void setPlayerAgreed(UUID uuid) {
    dataYaml.set("agreed." + uuid.toString(), configVersion);
    saveDataFile();
}

@EventHandler
public void onJoin(PlayerJoinEvent event) {
    Player p = event.getPlayer();
    if (!hasPlayerAgreed(p.getUniqueId())) {
        // show language selector on next tick to allow player fully join
        Bukkit.getScheduler().runTaskLater(this, () -> openLanguageSelector(p), 5L);
    }
}

// Command handler for /rules
private boolean onRulesCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage("This command is for players only.");
        return true;
    }
    Player p = (Player) sender;
    openLanguageSelector(p);
    return true;
}

// Open language selection GUI
private void openLanguageSelector(Player p) {
    FileConfiguration cfg = getConfig();
    Set<String> langs = cfg.getConfigurationSection("languages").getKeys(false);
    int size = 9; // simple one-row selector
    Inventory inv = Bukkit.createInventory(null, size, ChatColor.GREEN + cfg.getString("messages.choose-lang-title","Choose your language"));
    int slot = 1;
    for (String key : langs) {
        String display = cfg.getString("languages." + key + ".display-name", key);
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + display);
        List<String> lore = new ArrayList<>();
        lore.add("\u00A77" + cfg.getString("languages." + key + ".locale-code", key));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
        slot += 2;
        if (slot >= size) slot = 0; // wrap just in case
    }
    p.openInventory(inv);
    pendingLanguageSelection.remove(p.getUniqueId()); // clear any previous
}

// Click handler for both language selection and rules pages
@EventHandler
public void onInventoryClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();
    if (e.getCurrentItem() == null) return;
    String title = e.getView().getTitle();
    FileConfiguration cfg = getConfig();
    if (title.equals(ChatColor.GREEN + cfg.getString("messages.choose-lang-title", "Choose your language"))) {
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (!clicked.hasItemMeta()) return;
        String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        // find matching language key
        String selectedKey = null;
        for (String key : cfg.getConfigurationSection("languages").getKeys(false)) {
            String d = cfg.getString("languages." + key + ".display-name", key);
            if (d.equalsIgnoreCase(display)) { selectedKey = key; break; }
        }
        if (selectedKey == null) return;
        // open first page of rules for that language
        openRulesGUI(p, selectedKey, 1);
        return;
    }

    // Rules inventory clicks
    if (title.startsWith(ChatColor.BLUE + cfg.getString("messages.rules-inventory-title", "Rules - Page %page% (%lang%)").split("%page%",2)[0])) {
        e.setCancelled(true);
        // title contains page and lang; parse
        String raw = ChatColor.stripColor(title);
        // expected form: Rules — Page X (lang)
        // we'll extract lang code between parentheses
        String lang = null;
        if (raw.contains("(")) {
            int a = raw.indexOf('(');
            int b = raw.indexOf(')');
            if (a!=-1 && b!=-1 && b>a) lang = raw.substring(a+1,b);
        }
        // if clicked accept button or navigation
        ItemStack item = e.getCurrentItem();
        if (!item.hasItemMeta()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase(ChatColor.stripColor(cfg.getString("languages." + lang + ".accept-button-name","Accept")))) {
            // player agreed
            setPlayerAgreed(p.getUniqueId());
            p.closeInventory();
            p.sendMessage(ChatColor.GREEN + cfg.getString("messages.already-agreed","You accepted the rules."));
            return;
        }
        // navigation: previous / next indicated by item names
        if (name.equalsIgnoreCase("<< Prev") || name.equalsIgnoreCase("Prev")) {
            // get current page from title
            int cur = extractPageFromTitle(raw);
            if (cur > 1) openRulesGUI(p, lang, cur - 1);
            return;
        }
        if (name.equalsIgnoreCase("Next >>") || name.equalsIgnoreCase("Next")) {
            int cur = extractPageFromTitle(raw);
            int max = getMaxPages(lang);
            if (cur < max) openRulesGUI(p, lang, cur + 1);
            return;
        }
    }
}

private int extractPageFromTitle(String rawTitle) {
    // tries to find the first integer in the title (page number)
    for (String tok : rawTitle.split(" ")) {
        try { return Integer.parseInt(tok); } catch (NumberFormatException ignored) {}
    }
    return 1;
}

private int getMaxPages(String lang) {
    List<?> pages = getConfig().getList("languages." + lang + ".pages");
    if (pages == null) return 1;
    return pages.size();
}

// Build and open the rules GUI for a language and page
@SuppressWarnings("unchecked")
private void openRulesGUI(Player p, String langKey, int pageNum) {
    FileConfiguration cfg = getConfig();
    List<?> pages = cfg.getList("languages." + langKey + ".pages");
    if (pages == null || pages.isEmpty()) {
        p.sendMessage(ChatColor.RED + "No rules found for language: " + langKey);
        return;
    }

    int max = pages.size();
    if (pageNum < 1) pageNum = 1;
    if (pageNum > max) pageNum = max;

    String titleTemplate = cfg.getString("messages.rules-inventory-title", "Rules — Page %page% (%lang%)");
    String displayLang = cfg.getString("languages." + langKey + ".display-name", langKey);
    String title = titleTemplate.replace("%page%", String.valueOf(pageNum)).replace("%lang%", displayLang);
    Inventory inv = Bukkit.createInventory(null, 9*3, ChatColor.BLUE + title);

    // center area for rules — take the page list
    List<String> lines = (List<String>) pages.get(pageNum - 1);
    // convert each rule line into an item (paper)
    int slot = 10; // center-ish
    for (String rule : lines) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta im = paper.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + rule);
        im.setLore(Collections.singletonList(ChatColor.GRAY + "Qonfoth Server Rule"));
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(im);
        if (slot < inv.getSize()) inv.setItem(slot, paper);
        slot++;
    }

    // navigation buttons
    if (pageNum > 1) {
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName(ChatColor.AQUA + "<< Prev");
        prev.setItemMeta(pm);
        inv.setItem(18, prev);
    }
    if (pageNum < max) {
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName(ChatColor.AQUA + "Next >>");
        next.setItemMeta(nm);
        inv.setItem(26, next);
    }

    // Accept button on last page
    if (pageNum == max) {
        ItemStack accept = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta am = accept.getItemMeta();
        String acceptName = cfg.getString("languages." + langKey + ".accept-button-name", "I accept");
        List<String> acceptLore = cfg.getStringList("languages." + langKey + ".accept-button-lore");
        am.setDisplayName(ChatColor.GREEN + acceptName);
        am.setLore(acceptLore);
        accept.setItemMeta(am);
        inv.setItem(13, accept);
    }

    p.openInventory(inv);
}

}

