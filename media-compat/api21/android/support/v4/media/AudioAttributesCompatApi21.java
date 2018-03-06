/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.media;

import android.media.AudioAttributes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiresApi(21)
class AudioAttributesCompatApi21 {
    private static final String TAG = "AudioAttributesCompat";

    // used to introspect AudioAttributes @hidden API
    // I'm sorry, CheckStyle, but this is much more readable
    private static Method sAudioAttributesToLegacyStreamType;

    public static int toLegacyStreamType(Wrapper aaWrap) {
        final AudioAttributes aaObject = aaWrap.unwrap();
        try {
            if (sAudioAttributesToLegacyStreamType == null) {
                sAudioAttributesToLegacyStreamType = AudioAttributes.class.getMethod(
                        "toLegacyStreamType", AudioAttributes.class);
            }
            Object result = sAudioAttributesToLegacyStreamType.invoke(
                    null, aaObject);
            return (Integer) result;
        } catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | ClassCastException e) {
            Log.w(TAG, "getLegacyStreamType() failed on API21+", e);
            return -1; // AudioSystem.STREAM_DEFAULT
        }
    }

    static final class Wrapper {
        private AudioAttributes mWrapped;
        private Wrapper(AudioAttributes obj) {
            mWrapped = obj;
        }
        public static Wrapper wrap(@NonNull AudioAttributes obj) {
            if (obj == null) {
                throw new IllegalArgumentException("AudioAttributesApi21.Wrapper cannot wrap null");
            }
            return new Wrapper(obj);
        }
        public AudioAttributes unwrap() {
            return mWrapped;
        }
    }
}
