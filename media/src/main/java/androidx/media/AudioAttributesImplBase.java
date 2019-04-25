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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MOVIE;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_SONIFICATION;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_SPEECH;
import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_UNKNOWN;
import static androidx.media.AudioAttributesCompat.INVALID_STREAM_TYPE;
import static androidx.media.AudioAttributesCompat.USAGE_ALARM;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_ACCESSIBILITY;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANCE_SONIFICATION;
import static androidx.media.AudioAttributesCompat.USAGE_ASSISTANT;
import static androidx.media.AudioAttributesCompat.USAGE_GAME;
import static androidx.media.AudioAttributesCompat.USAGE_MEDIA;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_DELAYED;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_INSTANT;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_REQUEST;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_EVENT;
import static androidx.media.AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE;
import static androidx.media.AudioAttributesCompat.USAGE_UNKNOWN;
import static androidx.media.AudioAttributesCompat.USAGE_VIRTUAL_SOURCE;
import static androidx.media.AudioAttributesCompat.USAGE_VOICE_COMMUNICATION;
import static androidx.media.AudioAttributesCompat.USAGE_VOICE_COMMUNICATION_SIGNALLING;

import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat.AudioManagerHidden;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Arrays;

/** @hide */
@VersionedParcelize(jetifyAs = "android.support.v4.media.AudioAttributesImplBase")
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class AudioAttributesImplBase implements AudioAttributesImpl {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(1)
    public int mUsage = USAGE_UNKNOWN;
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(2)
    public int mContentType = CONTENT_TYPE_UNKNOWN;
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(3)
    public int mFlags = 0x0;
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @ParcelField(4)
    public int mLegacyStream = INVALID_STREAM_TYPE;

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

    @Override
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

    static class Builder implements AudioAttributesImpl.Builder {
        private int mUsage = USAGE_UNKNOWN;
        private int mContentType = CONTENT_TYPE_UNKNOWN;
        private int mFlags = 0x0;
        private int mLegacyStream = INVALID_STREAM_TYPE;

        Builder() {
        }

        Builder(AudioAttributesCompat aa) {
            mUsage = aa.getUsage();
            mContentType = aa.getContentType();
            mFlags = aa.getFlags();
            mLegacyStream = aa.getRawLegacyStreamType();
        }

        @Override
        public AudioAttributesImpl build() {
            return new AudioAttributesImplBase(mContentType, mFlags, mUsage, mLegacyStream);
        }

        @Override
        public Builder setUsage(@AudioAttributesCompat.AttributeUsage int usage) {
            switch (usage) {
                case USAGE_UNKNOWN:
                case USAGE_MEDIA:
                case USAGE_VOICE_COMMUNICATION:
                case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                case USAGE_ALARM:
                case USAGE_NOTIFICATION:
                case USAGE_NOTIFICATION_RINGTONE:
                case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                case USAGE_NOTIFICATION_EVENT:
                case USAGE_ASSISTANCE_ACCESSIBILITY:
                case USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                case USAGE_ASSISTANCE_SONIFICATION:
                case USAGE_GAME:
                case USAGE_VIRTUAL_SOURCE:
                    mUsage = usage;
                    break;
                // TODO: shouldn't it be USAGE_ASSISTANT?
                case USAGE_ASSISTANT:
                    mUsage = USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
                    break;
                default:
                    mUsage = USAGE_UNKNOWN;
            }
            return this;
        }

        @Override
        public Builder setContentType(@AudioAttributesCompat.AttributeContentType int contentType) {
            switch (contentType) {
                case CONTENT_TYPE_UNKNOWN:
                case CONTENT_TYPE_MOVIE:
                case CONTENT_TYPE_MUSIC:
                case CONTENT_TYPE_SONIFICATION:
                case CONTENT_TYPE_SPEECH:
                    mContentType = contentType;
                    break;
                default:
                    mUsage = CONTENT_TYPE_UNKNOWN;
            }
            return this;
        }

        @Override
        public Builder setFlags(int flags) {
            flags &= AudioAttributesCompat.FLAG_ALL;
            mFlags |= flags;
            return this;
        }

        @Override
        public Builder setLegacyStreamType(int streamType) {
            if (streamType == AudioManagerHidden.STREAM_ACCESSIBILITY) {
                throw new IllegalArgumentException(
                        "STREAM_ACCESSIBILITY is not a legacy stream "
                                + "type that was used for audio playback");
            }
            mLegacyStream = streamType;
            return this;
        }
    }
}
