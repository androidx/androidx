/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import android.media.MediaPlayer;

import androidx.xr.runtime.internal.MediaPlayerExtensionsWrapper;
import androidx.xr.runtime.internal.PointSourceParams;
import androidx.xr.runtime.internal.SoundFieldAttributes;

import com.android.extensions.xr.media.MediaPlayerExtensions;

import org.jspecify.annotations.NonNull;

/** Implementation of the {@link MediaPlayerExtensionsWrapper}. */
final class MediaPlayerExtensionsWrapperImpl implements MediaPlayerExtensionsWrapper {

    private final MediaPlayerExtensions mExtensions;

    MediaPlayerExtensionsWrapperImpl(@NonNull MediaPlayerExtensions extensions) {
        mExtensions = extensions;
    }

    @Override
    public void setPointSourceParams(
            @NonNull MediaPlayer mediaPlayer, @NonNull PointSourceParams params) {
        com.android.extensions.xr.media.PointSourceParams extParams =
                MediaUtils.convertPointSourceParamsToExtensions(params);

        MediaPlayer unused = mExtensions.setPointSourceParams(mediaPlayer, extParams);
    }

    @Override
    public void setSoundFieldAttributes(
            @NonNull MediaPlayer mediaPlayer, @NonNull SoundFieldAttributes attributes) {
        com.android.extensions.xr.media.SoundFieldAttributes extAttributes =
                MediaUtils.convertSoundFieldAttributesToExtensions(attributes);

        MediaPlayer unused = mExtensions.setSoundFieldAttributes(mediaPlayer, extAttributes);
    }
}
