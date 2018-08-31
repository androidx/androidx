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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media2.DataSourceDesc2;
import androidx.media2.UriDataSourceDesc2;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.source.ExtractorMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.upstream.DataSource;

/**
 * Utility methods for translating between the MediaPlayer2 and ExoPlayer APIs.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class ExoPlayerUtils {

    /** Returns an ExoPlayer media source for the given data source description. */
    public static MediaSource createMediaSource(
            DataSource.Factory dataSourceFactory, DataSourceDesc2 dataSourceDescription) {
        // TODO(b/111150876): Add support for HLS streams and file descriptors.
        if (dataSourceDescription instanceof UriDataSourceDesc2) {
            Uri uri = ((UriDataSourceDesc2) dataSourceDescription).getUri();
            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setTag(dataSourceDescription).createMediaSource(uri);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** Returns ExoPlayer audio attributes for the given audio attributes. */
    public static AudioAttributes getAudioAttributes(AudioAttributesCompat audioAttributesCompat) {
        return new AudioAttributes.Builder()
                .setContentType(audioAttributesCompat.getContentType())
                .setFlags(audioAttributesCompat.getFlags())
                .setUsage(audioAttributesCompat.getUsage())
                .build();
    }

    /** Returns audio attributes for the given ExoPlayer audio attributes. */
    public static AudioAttributesCompat getAudioAttributesCompat(AudioAttributes audioAttributes) {
        return new AudioAttributesCompat.Builder()
                .setContentType(audioAttributes.contentType)
                .setFlags(audioAttributes.flags)
                .setUsage(audioAttributes.usage)
                .build();
    }

    private ExoPlayerUtils() {
        // Prevent instantiation.
    }

}
