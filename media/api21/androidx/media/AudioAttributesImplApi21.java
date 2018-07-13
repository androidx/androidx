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

import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_FRAMEWORKS;
import static androidx.media.AudioAttributesCompat.AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE;
import static androidx.media.AudioAttributesCompat.INVALID_STREAM_TYPE;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@VersionedParcelize
@TargetApi(21)
class AudioAttributesImplApi21 implements AudioAttributesImpl {
    private static final String TAG = "AudioAttributesCompat21";

    @ParcelField(1)
    AudioAttributes mAudioAttributes;
    @ParcelField(2)
    int mLegacyStreamType = INVALID_STREAM_TYPE;

    /**
     * Used for VersionedParcelable
     */
    AudioAttributesImplApi21() {
    }

    AudioAttributesImplApi21(AudioAttributes audioAttributes) {
        this(audioAttributes, INVALID_STREAM_TYPE);
    }
    AudioAttributesImplApi21(AudioAttributes audioAttributes, int explicitLegacyStream) {
        mAudioAttributes = audioAttributes;
        mLegacyStreamType = explicitLegacyStream;
    }

    static Method sAudioAttributesToLegacyStreamType;

    static Method getAudioAttributesToLegacyStreamTypeMethod() {
        try {
            if (sAudioAttributesToLegacyStreamType == null) {
                sAudioAttributesToLegacyStreamType = AudioAttributes.class.getMethod(
                        "toLegacyStreamType", AudioAttributes.class);
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
        return sAudioAttributesToLegacyStreamType;
    }

    //////////////////////////////////////////////////////////////////////
    // Implements AudioAttributesImpl interface
    @Override
    public Object getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    public int getVolumeControlStream() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mAudioAttributes.getVolumeControlStream();
        }
        return AudioAttributesCompat.toVolumeStreamType(true, getFlags(), getUsage());
    }

    @Override
    public int getLegacyStreamType() {
        if (mLegacyStreamType != INVALID_STREAM_TYPE) {
            return mLegacyStreamType;
        }
        Method frameworkMethod = getAudioAttributesToLegacyStreamTypeMethod();
        if (frameworkMethod == null) {
            Log.w(TAG, "No AudioAttributes#toLegacyStreamType() on API: "
                    + android.os.Build.VERSION.SDK_INT);
            return INVALID_STREAM_TYPE;
        }
        try {
            Object result = frameworkMethod.invoke(null, mAudioAttributes);
            return (Integer) result;
        } catch (InvocationTargetException | IllegalAccessException e) {
            Log.w(TAG, "getLegacyStreamType() failed on API: "
                    + android.os.Build.VERSION.SDK_INT, e);
            return -1; // AudioSystem.STREAM_DEFAULT
        }
    }

    @Override
    public int getRawLegacyStreamType() {
        return mLegacyStreamType;
    }

    @Override
    public int getContentType() {
        return mAudioAttributes.getContentType();
    }

    @Override
    public @AudioAttributesCompat.AttributeUsage int getUsage() {
        return mAudioAttributes.getUsage();
    }

    @Override
    public int getFlags() {
        return mAudioAttributes.getFlags();
    }

    @Override
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(AUDIO_ATTRIBUTES_FRAMEWORKS, mAudioAttributes);
        if (mLegacyStreamType != INVALID_STREAM_TYPE) {
            bundle.putInt(AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE, mLegacyStreamType);
        }
        return bundle;
    }

    //////////////////////////////////////////////////////////////////////
    // Override Object methods

    @Override
    public int hashCode() {
        return mAudioAttributes.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioAttributesImplApi21)) {
            return false;
        }
        final AudioAttributesImplApi21 that = (AudioAttributesImplApi21) o;
        return mAudioAttributes.equals(that.mAudioAttributes);
    }

    @Override
    public String toString() {
        return "AudioAttributesCompat: audioattributes=" + mAudioAttributes;
    }

    //////////////////////////////////////////////////////////////////////
    // Other public methods

    public static AudioAttributesImpl fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        AudioAttributes frameworkAttrs = (AudioAttributes)
                bundle.getParcelable(AUDIO_ATTRIBUTES_FRAMEWORKS);
        if (frameworkAttrs == null) {
            return null;
        }
        int legacyStream = bundle.getInt(AUDIO_ATTRIBUTES_LEGACY_STREAM_TYPE,
                INVALID_STREAM_TYPE);
        return new AudioAttributesImplApi21(frameworkAttrs, legacyStream);
    }
}
