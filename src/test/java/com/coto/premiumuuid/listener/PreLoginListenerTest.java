package com.coto.premiumuuid.listener;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.config.PluginConfig;
import com.coto.premiumuuid.mojang.MojangApiClient;
import com.coto.premiumuuid.override.OverrideStore;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreLoginListenerTest {

    @Mock PluginConfig config;
    @Mock UUIDCache cache;
    @Mock MojangApiClient mojangApi;
    @Mock OverrideStore overrides;
    @Mock Logger logger;
    
    @Mock PlayerLoginEvent event;
    @Mock Player player;
    @Mock ProfileBanList banList;
    @Mock PlayerProfile playerProfile;
    @Mock OfflinePlayer offlinePlayer;

    MockedStatic<Bukkit> bukkitMock;
    PreLoginListener listener;
    
    UUID currentUuid = UUID.randomUUID();
    String playerName = "Notch";

    @Mock org.bukkit.plugin.Plugin plugin;

    @BeforeEach
    void setUp() {
        listener = new PreLoginListener(config, cache, mojangApi, overrides, logger, plugin);
        bukkitMock = mockStatic(Bukkit.class);

        // Setup base event & player mocks
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(player.getName()).thenReturn(playerName);
        lenient().when(player.getUniqueId()).thenReturn(currentUuid);

        // Setup Bukkit mocks
        lenient().when(Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE)).thenReturn(banList);
        lenient().when(Bukkit.createProfile(any(UUID.class), any())).thenReturn(playerProfile);
        lenient().when(Bukkit.createProfile(null, playerName)).thenReturn(playerProfile);
        lenient().when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    @Test
    void testBan_CurrentUuidBanned_KeepsBan() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.KICK_BANNED);
        
        // Simulate that any checked profile is banned
        when(banList.isBanned(any(PlayerProfile.class))).thenReturn(true);

        listener.onPlayerLogin(event);

        // Should not allow login
        verify(event, never()).allow();
    }

    @Test
    void testBan_NoBansFound_AllowsLogin() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.KICK_BANNED);
        
        // No identity is banned
        when(banList.isBanned(any(PlayerProfile.class))).thenReturn(false);

        listener.onPlayerLogin(event);

        // Should cancel the kick (Bukkit false positive) and allow login
        verify(event, times(1)).allow();
    }

    @Test
    void testWhitelist_NameWhitelisted_AllowsLogin() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.KICK_WHITELIST);
        lenient().when(Bukkit.hasWhitelist()).thenReturn(true);
        
        // The whitelisted OfflinePlayer mocks the same name
        when(offlinePlayer.getName()).thenReturn(playerName);
        when(Bukkit.getWhitelistedPlayers()).thenReturn(Collections.singleton(offlinePlayer));

        listener.onPlayerLogin(event);

        verify(event, times(1)).allow();
    }
    
    @Test
    void testWhitelist_NotWhitelisted_KeepsKick() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.KICK_WHITELIST);
        lenient().when(Bukkit.hasWhitelist()).thenReturn(true);
        
        // Nobody is whitelisted
        when(Bukkit.getWhitelistedPlayers()).thenReturn(Collections.emptySet());
        lenient().when(offlinePlayer.isWhitelisted()).thenReturn(false);

        listener.onPlayerLogin(event);

        // Keeps the whitelist kick (implemented via disallow)
        verify(event, never()).allow();
    }

    @Test
    void testBan_BypassIntercepted_BlocksLogin() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.ALLOWED);

        // Player is not banned by current UUID, but IS banned by offline UUID
        when(banList.isBanned(any(PlayerProfile.class))).thenReturn(true);
        
        // Mock getEntries
        org.bukkit.BanEntry<?> banEntry = mock(org.bukkit.BanEntry.class);
        lenient().when(banEntry.getTarget()).thenReturn("Notch");
        lenient().when(banEntry.getReason()).thenReturn("Bypassing is bad");
        
        java.util.Set<org.bukkit.BanEntry<?>> entries = new java.util.HashSet<>();
        entries.add(banEntry);
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.Set rawEntries = entries;
        lenient().when(banList.getEntries()).thenReturn(rawEntries);

        listener.onPlayerLogin(event);

        verify(event, times(1)).disallow(eq(PlayerLoginEvent.Result.KICK_BANNED), org.mockito.ArgumentMatchers.contains("Bypassing is bad"));
    }

    @Test
    void testPardon_Intercepted() {
        org.bukkit.event.player.PlayerCommandPreprocessEvent cmdEvent = mock(org.bukkit.event.player.PlayerCommandPreprocessEvent.class);
        when(cmdEvent.getMessage()).thenReturn("/pardon Notch");
        
        org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        lenient().when(Bukkit.getScheduler()).thenReturn(scheduler);
        
        org.mockito.ArgumentCaptor<Runnable> runnableCaptor = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        
        listener.onPlayerCommand(cmdEvent);
        
        verify(scheduler).runTask(eq(plugin), runnableCaptor.capture());
        
        runnableCaptor.getValue().run();
        
        verify(banList, atLeastOnce()).pardon(any(PlayerProfile.class));
    }

    @Test
    void testBan_CorruptedEntry_CatchesException() {
        when(event.getResult()).thenReturn(PlayerLoginEvent.Result.ALLOWED);
        when(banList.isBanned(any(PlayerProfile.class))).thenReturn(true);

        org.bukkit.BanEntry<?> banEntry = mock(org.bukkit.BanEntry.class);
        lenient().when(banEntry.getBanTarget()).thenThrow(new IllegalArgumentException("Name cannot be longer than 16 characters"));
        lenient().when(banEntry.getTarget()).thenReturn(currentUuid.toString());
        lenient().when(banEntry.getReason()).thenReturn("Corrupted profile ban");
        
        java.util.Set<org.bukkit.BanEntry<?>> entries = new java.util.HashSet<>();
        entries.add(banEntry);
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.Set rawEntries = entries;
        lenient().when(banList.getEntries()).thenReturn(rawEntries);

        listener.onPlayerLogin(event);

        verify(event, times(1)).disallow(eq(PlayerLoginEvent.Result.KICK_BANNED), org.mockito.ArgumentMatchers.contains("Corrupted profile ban"));
    }
}
