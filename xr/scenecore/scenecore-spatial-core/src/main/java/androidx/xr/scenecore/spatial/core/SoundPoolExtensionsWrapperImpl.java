/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.spatial.core;

import android.media.SoundPool;

import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.PointSourceParams;
import androidx.xr.scenecore.runtime.SoundFieldAttributes;
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper;

import com.android.extensions.xr.media.SoundPoolExtensions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Implementation of {@link SoundPoolExtensionsWrapper}. */
final class SoundPoolExtensionsWrapperImpl implements SoundPoolExtensionsWrapper {

    private final SoundPoolExtensions mExtensions;

    SoundPoolExtensionsWrapperImpl(SoundPoolExtensions extensions) {
        mExtensions = extensions;
    }

    @Override
    public int play(
            @NonNull SoundPool soundPool,
            int soundId,
            @NonNull PointSourceParams params,
            @Nullable Entity entity,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.PointSourceParams extParams =
                MediaUtils.convertPointSourceParamsToExtensions(params, entity);
        return mExtensions.playAsPointSource(
                soundPool, soundId, extParams, volume, priority, loop, rate);
    }

    @Override
    public int play(
            @NonNull SoundPool soundPool,
            int soundId,
            @NonNull SoundFieldAttributes params,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.SoundFieldAttributes extAttributes =
                MediaUtils.convertSoundFieldAttributesToExtensions(params);

        return mExtensions.playAsSoundField(
                soundPool, soundId, extAttributes, volume, priority, loop, rate);
    }

    @Override
    public int getSpatialSourceType(@NonNull SoundPool soundPool, int streamId) {
        return MediaUtils.convertExtensionsToSourceType(
                mExtensions.getSpatialSourceType(soundPool, streamId));
    }
}
