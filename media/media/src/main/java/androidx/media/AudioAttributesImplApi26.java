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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.media.AudioAttributesCompat.INVALID_STREAM_TYPE;

import android.media.AudioAttributes;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.VersionedParcelize;

/** @hide */
@VersionedParcelize(jetifyAs = "android.support.v4.media.AudioAttributesImplApi26")
@RestrictTo(LIBRARY)
@RequiresApi(26)
public class AudioAttributesImplApi26 extends AudioAttributesImplApi21 {

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /** @hide */
    // It should be public to allow Parcelizers which never be de/jetified can access the
    // constructor.
    @RestrictTo(LIBRARY)
    public AudioAttributesImplApi26() {
    }

    AudioAttributesImplApi26(AudioAttributes audioAttributes) {
        super(audioAttributes, INVALID_STREAM_TYPE);
    }

    @Override
    public int getVolumeControlStream() {
        return mAudioAttributes.getVolumeControlStream();
    }

    @RequiresApi(26)
    static class Builder extends AudioAttributesImplApi21.Builder {
        Builder() {
            super();
        }

        Builder(Object aa) {
            super(aa);
        }

        @Override
        @NonNull
        public AudioAttributesImpl build() {
            return new AudioAttributesImplApi26(mFwkBuilder.build());
        }

        @Override
        @NonNull
        public Builder setUsage(int usage) {
            mFwkBuilder.setUsage(usage);
            return this;
        }
    }
}
