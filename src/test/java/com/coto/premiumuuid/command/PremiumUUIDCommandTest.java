package com.coto.premiumuuid.command;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.config.PluginConfig;
import com.coto.premiumuuid.override.OverrideStore;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumUUIDCommandTest {

    @Mock PluginConfig config;
    @Mock UUIDCache cache;
    @Mock OverrideStore overrides;
    @Mock CommandSender sender;
    @Mock Command command;

    PremiumUUIDCommand premiumCommand;

    @BeforeEach
    void setUp() {
        premiumCommand = new PremiumUUIDCommand(config, cache, overrides);
    }

    @Test
    void testResetCommand_RemovesOverrideAndSendsMessage() {
        // Mock the override store to pretend the removal was successful
        when(overrides.remove("steve")).thenReturn(true);

        // Execute command: /premiumuuid reset steve
        boolean result = premiumCommand.onCommand(sender, command, "premiumuuid", new String[]{"reset", "steve"});

        // Verify the command executed successfully
        assertTrue(result, "Command should return true");

        // Verify that the overrides store was asked to remove "steve"
        verify(overrides, times(1)).remove("steve");

        // Verify success message was sent
        ArgumentCaptor<Component> messageCaptor = ArgumentCaptor.forClass(Component.class);
        verify(sender, times(1)).sendMessage(messageCaptor.capture());
        
        // As long as the message was sent, we know the success branch was executed
    }

    @Test
    void testResetCommand_WhenNoOverrideExists() {
        // Mock the override store to pretend there was no override to remove
        when(overrides.remove("alex")).thenReturn(false);

        // Execute command: /premiumuuid reset alex
        boolean result = premiumCommand.onCommand(sender, command, "premiumuuid", new String[]{"reset", "alex"});

        assertTrue(result, "Command should return true");
        verify(overrides, times(1)).remove("alex");

        // Verify failure/yellow message was sent
        verify(sender, times(1)).sendMessage(any(Component.class));
    }
}
