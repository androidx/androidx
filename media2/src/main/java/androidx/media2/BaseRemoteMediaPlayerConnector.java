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

/**
 * Base interface for all remote media players that want media session and playback happens on the
 * remote device through MediaRouter.
 * <p>
 * If you use this to the {@link MediaSession2} followings would happen.
 * <ul>
 *     <li>Session wouldn't handle audio focus</li>
 *     <li>Session would dispatch volume change event to the player instead of changing device
 *         volume</li>
 * </ul>
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class BaseRemoteMediaPlayerConnector extends MediaPlayerConnector {
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
     * {@link #adjustPlayerVolume(int)}. Attempts to set the volume to a specific
     * value should be ignored.
     */
    public static final int VOLUME_CONTROL_RELATIVE = 1;

    /**
     * The volume control uses an absolute value. It may be adjusted using
     * {@link #adjustPlayerVolume(int)} or set directly using
     * {@link #setPlayerVolume(float)}.
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
    public abstract void adjustPlayerVolume(int direction);

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
     * Player event callback.
     */
    public static class RemotePlayerEventCallback extends PlayerEventCallback {
        /**
         * Called to indicate that the volume has changed.
         *
         * @param mpb the player that has completed volume changes.
         * @param volume the new volume
         * @see #setPlayerVolume(float)
         */
        public void onPlayerVolumeChanged(@NonNull MediaPlayerConnector mpb, float volume) { }
    }
}
