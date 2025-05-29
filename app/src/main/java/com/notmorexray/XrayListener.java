package com.notmorexray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.UUID;

public class XrayListener implements Listener {
    private final Plugin plugin;
    private final HashMap<UUID, Integer> warningCount = new HashMap<>();
    private final HashMap<UUID, Long> lastOreBreakTime = new HashMap<>(); // Novo HashMap para o tempo

    // Tempo em milissegundos para resetar os avisos (ex: 5 minutos)
    private static final long WARNING_RESET_TIME = 5 * 60 * 1000L;

    public XrayListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Material block = event.getBlock().getType();

        if (isOre(block)) {
            long currentTime = System.currentTimeMillis();
            long lastBreakTime = lastOreBreakTime.getOrDefault(uuid, 0L);

            // Se o tempo desde a última mineração de minério for maior que o tempo de reset,
            // ou se for a primeira vez que o jogador mina um minério, resetar os avisos.
            if (currentTime - lastBreakTime > WARNING_RESET_TIME) {
                warningCount.put(uuid, 0); // Reseta a contagem
            }

            warningCount.put(uuid, warningCount.getOrDefault(uuid, 0) + 1);
            lastOreBreakTime.put(uuid, currentTime); // Atualiza o tempo da última mineração

            int warnings = warningCount.get(uuid);
            String playerName = player.getName();
            String motivo = "Uso de Xray";

            if (warnings == 1) {
                player.sendMessage(ChatColor.YELLOW + "⚠️ Xray detectado! Desligue para evitar punição. (Aviso 1/3)");
            } else if (warnings == 2) {
                player.sendMessage(ChatColor.RED + "⚠️ Xray detectado! Último aviso antes do ban. (Aviso 2/3)");
            } else if (warnings >= 3) {
                String playerIP = player.getAddress().getAddress().getHostAddress();

                player.sendMessage(ChatColor.RED + "🚨 Você foi BANIDO por " + motivo + "!");
                Bukkit.getServer().broadcast(Component.text("🚨 " + playerName + " foi banido do servidor! Motivo: " + motivo));

                plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "ban-ip " + playerIP + " " + motivo);
                });

                warningCount.remove(uuid); // Remove do registro
                lastOreBreakTime.remove(uuid); // Remove também o tempo
            }
        }
    }

    private boolean isOre(Material block) {
        return block == Material.DIAMOND_ORE ||
                block == Material.EMERALD_ORE ||
                block == Material.GOLD_ORE ||
                block == Material.IRON_ORE ||
                block == Material.LAPIS_ORE ||
                block == Material.REDSTONE_ORE;
    }
}