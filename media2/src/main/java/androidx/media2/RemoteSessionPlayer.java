/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Future;

/**
 * Base interface for all remote media players that want media session and playback happens on the
 * remote device through MediaRouter.
 * <p>
 * If you use this to the {@link MediaSession}, session would dispatch incoming volume change event
 * to the player instead of changing device stream volume.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class RemoteSessionPlayer extends SessionPlayer {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({VOLUME_CONTROL_FIXED, VOLUME_CONTROL_RELATIVE, VOLUME_CONTROL_ABSOLUTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeControlType {}

    /**
     * The volume is fixed and can not be modified. Requests to change volume
     * should be ignored.
     */
    public static final int VOLUME_CONTROL_FIXED = 0;

    /**
     * The volume control uses relative adjustment via
     * {@link #adjustVolume(int)}. Attempts to set the volume to a specific
     * value should be ignored.
     */
    public static final int VOLUME_CONTROL_RELATIVE = 1;

    /**
     * The volume control uses an absolute value. It may be adjusted using
     * {@link #adjustVolume(int)} or set directly using
     * {@link #setVolume(int)}.
     */
    public static final int VOLUME_CONTROL_ABSOLUTE = 2;

    /**
     * Adjust player volume with the direction. Override this API to customize volume change in
     * remote device
     * <p>
     * Default implement adjust volume by 1
     * <p>
     * This would be ignored when volume control type is {@link #VOLUME_CONTROL_FIXED}.
     *
     * @param direction direction of the volume changes. Positive value for volume up, negative for
     *                  volume down.
     */
    public abstract Future<PlayerResult> adjustVolume(int direction);

    /**
     * Sets the volume of the audio of the media to play, expressed as a linear multiplier
     * on the audio samples.
     * <p>
     * Note that this volume is specific to the player, and is separate from stream volume
     * used across the platform.
     * <p>
     * A value of {@code 0} indicates muting. See {@link #getMaxVolume()} for the volume range
     * supported by this player.
     *
     * @param volume a value between 0.0f and {@link #getMaxVolume()}.
     */
    public abstract Future<PlayerResult> setVolume(int volume);

    /**
     * Gets the current volume of this player to this player.
     * <p>
     * Note that it does not take into account the associated stream volume because the playback is
     * happening outside of the phone device.
     *
     * @return the player volume.
     */
    public abstract int getVolume();

    /**
     * Gets the maximum volume that can be used in {@link #setVolume(int)}.
     *
     * @return the maximum volume. Shouldn't be negative.
     */
    public abstract int getMaxVolume();

    /**
     * Gets the volume type.
     * <p>
     * This shouldn't be changed after instantiation.
     *
     * @return one of the volume type
     * @see #VOLUME_CONTROL_FIXED
     * @see #VOLUME_CONTROL_RELATIVE
     * @see #VOLUME_CONTROL_ABSOLUTE
     */
    public abstract @VolumeControlType int getVolumeControlType();

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class Callback extends SessionPlayer.PlayerCallback {
        /**
         * Called to indicate that the volume has changed.
         *
         * @param player2 the player that has completed volume changes.
         * @param volume the new volume
         * @see #setVolume(int)
         */
        public void onVolumeChanged(@NonNull RemoteSessionPlayer player2, int volume) {
        }
    }
}
