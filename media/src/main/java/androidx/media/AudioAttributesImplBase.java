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

import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_CONTENT_TYPE;
import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_FLAGS;
import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE;
import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_USAGE;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_UNKNOWN;
import static androidx.media.AudioAttributesCompat.INVALID_STREAM_TYPE;
import static androidx.media.AudioAttributesCompat.USAGE_UNKNOWN;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat.AudioManagerHidden;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Arrays;

@VersionedParcelize
class AudioAttributesImplBase implements AudioAttributesImpl {
    @ParcelField(1)
    int mUsage = USAGE_UNKNOWN;
    @ParcelField(2)
    int mContentType = CONTENT_TYPE_UNKNOWN;
    @ParcelField(3)
    int mFlags = 0x0;
    @ParcelField(4)
    int mLegacyStream = INVALID_STREAM_TYPE;

    /**
     * Used for VersionedParcelable
     */
    AudioAttributesImplBase() { }

    AudioAttributesImplBase(int contentType, int flags, int usage, int legacyStream) {
        mContentType = contentType;
        mFlags = flags;
        mUsage = usage;
        mLegacyStream = legacyStream;
    }

    //////////////////////////////////////////////////////////////////////
    // Implements AudioAttributesImpl interface
    public Object getAudioAttributes() {
        return null;
    }

    @Override
    public int getVolumeControlStream() {
        return AudioAttributesCompat.toVolumeStreamType(true, mFlags, mUsage);
    }

    @Override
    public int getLegacyStreamType() {
        if (mLegacyStream != INVALID_STREAM_TYPE) {
            return mLegacyStream;
        }
        return AudioAttributesCompat.toVolumeStreamType(false, mFlags, mUsage);
    }

    @Override
    public int getRawLegacyStreamType() {
        return mLegacyStream;
    }

    @Override
    public int getContentType() {
        return mContentType;
    }

    @Override
    public @AudioAttributesCompat.AttributeUsage int getUsage() {
        return mUsage;
    }

    @Override
    public int getFlags() {
        int flags = mFlags;
        int legacyStream = getLegacyStreamType();
        if (legacyStream == AudioManagerHidden.STREAM_BLUETOOTH_SCO) {
            flags |= AudioAttributesCompat.FLAG_SCO;
        } else if (legacyStream == AudioManagerHidden.STREAM_SYSTEM_ENFORCED) {
            flags |= AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED;
        }
        return flags & AudioAttributesCompat.FLAG_ALL_PUBLIC;
    }

    @Override
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(AUDIO_ATTRIBUTES_USAGE, mUsage);
        bundle.putInt(AUDIO_ATTRIBUTES_CONTENT_TYPE, mContentType);
        bundle.putInt(AUDIO_ATTRIBUTES_FLAGS, mFlags);
        if (mLegacyStream != INVALID_STREAM_TYPE) {
            bundle.putInt(AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE, mLegacyStream);
        }
        return bundle;
    }

    //////////////////////////////////////////////////////////////////////
    // Override Object methods

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {mContentType, mFlags, mUsage, mLegacyStream});
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioAttributesImplBase)) {
            return false;
        }
        final AudioAttributesImplBase that = (AudioAttributesImplBase) o;
        return ((mContentType == that.getContentType())
                && (mFlags == that.getFlags())
                && (mUsage == that.getUsage())
                && (mLegacyStream  == that.mLegacyStream)); // query the slot directly, don't guess
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AudioAttributesCompat:");
        if (mLegacyStream != INVALID_STREAM_TYPE) {
            sb.append(" stream=").append(mLegacyStream);
            sb.append(" derived");
        }
        sb.append(" usage=")
                .append(AudioAttributesCompat.usageToString(mUsage))
                .append(" content=")
                .append(mContentType)
                .append(" flags=0x")
                .append(Integer.toHexString(mFlags).toUpperCase());
        return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////
    // Other public methods

    public static AudioAttributesImpl fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int usage = bundle.getInt(AUDIO_ATTRIBUTES_USAGE, USAGE_UNKNOWN);
        int contentType = bundle.getInt(AUDIO_ATTRIBUTES_CONTENT_TYPE, CONTENT_TYPE_UNKNOWN);
        int flags = bundle.getInt(AUDIO_ATTRIBUTES_FLAGS, 0);
        int legacyStream = bundle.getInt(AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE,
                INVALID_STREAM_TYPE);
        return new AudioAttributesImplBase(contentType, flags, usage, legacyStream);
    }
}
