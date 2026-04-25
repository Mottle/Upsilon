package com.github.wintersteve25.tau.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Client-side helper for playing UI sounds.
 */
public class ClientSoundHelper {
    private static final SoundManager SOUND_MANAGER = Minecraft.getInstance().getSoundManager();

    /** Plays the vanilla UI button click sound. */
    public static void playButtonClick() {
        ClientSoundHelper.playSound(SoundEvents.UI_BUTTON_CLICK.value());
    }

    /** Plays a UI sound using default volume. */
    public static void playSound(SoundEvent soundEvent) {
        playSound(soundEvent, 0.25f);
    }

    /** Plays a UI sound using custom volume and default pitch. */
    public static void playSound(SoundEvent soundEvent, float volume) {
        playSound(soundEvent, volume, 1);
    }

    /** Plays a UI sound using custom volume and pitch. */
    public static void playSound(SoundEvent sound, float volume, float pitch) {
        SOUND_MANAGER.play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
