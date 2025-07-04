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

import android.media.AudioTrack;

import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.PointSourceParams;
import androidx.xr.runtime.internal.SoundFieldAttributes;

import com.android.extensions.xr.media.AudioTrackExtensions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Implementation of the {@link AudioTrackExtensionsWrapper} */
final class AudioTrackExtensionsWrapperImpl implements AudioTrackExtensionsWrapper {
    private final AudioTrackExtensions mExtensions;

    private final EntityManager mEntityManager;

    AudioTrackExtensionsWrapperImpl(AudioTrackExtensions extensions, EntityManager entityManager) {
        mExtensions = extensions;
        mEntityManager = entityManager;
    }

    @Override
    public @Nullable PointSourceParams getPointSourceParams(@NonNull AudioTrack audioTrack) {
        com.android.extensions.xr.media.PointSourceParams extParams =
                mExtensions.getPointSourceParams(audioTrack);

        if (extParams == null) {
            return null;
        }

        Entity entity = mEntityManager.getEntityForNode(extParams.getNode());

        if (entity == null) {
            return null;
        }

        return new PointSourceParams(entity);
    }

    @Override
    public @Nullable SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack audioTrack) {
        com.android.extensions.xr.media.SoundFieldAttributes extAttributes =
                mExtensions.getSoundFieldAttributes(audioTrack);

        if (extAttributes == null) {
            return null;
        }

        return new SoundFieldAttributes(extAttributes.getAmbisonicsOrder());
    }

    @Override
    public int getSpatialSourceType(@NonNull AudioTrack audioTrack) {
        return MediaUtils.convertExtensionsToSourceType(
                mExtensions.getSpatialSourceType(audioTrack));
    }

    @Override
    public void setPointSourceParams(@NonNull AudioTrack track, @NonNull PointSourceParams params) {
        com.android.extensions.xr.media.PointSourceParams extParams =
                MediaUtils.convertPointSourceParamsToExtensions(params);

        mExtensions.setPointSourceParams(track, extParams);
    }

    @Override
    public AudioTrack.@NonNull Builder setPointSourceParams(
            AudioTrack.@NonNull Builder builder, @NonNull PointSourceParams params) {
        com.android.extensions.xr.media.PointSourceParams extParams =
                MediaUtils.convertPointSourceParamsToExtensions(params);

        return mExtensions.setPointSourceParams(builder, extParams);
    }

    @Override
    public AudioTrack.@NonNull Builder setSoundFieldAttributes(
            AudioTrack.@NonNull Builder builder, @NonNull SoundFieldAttributes attributes) {
        com.android.extensions.xr.media.SoundFieldAttributes extAttributes =
                MediaUtils.convertSoundFieldAttributesToExtensions(attributes);

        return mExtensions.setSoundFieldAttributes(builder, extAttributes);
    }
}
