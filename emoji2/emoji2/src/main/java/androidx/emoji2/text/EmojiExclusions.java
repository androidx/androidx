/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.text;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

class EmojiExclusions {
    private EmojiExclusions() { /* cannot instantiate */ }

    static @NonNull Set<int[]> getEmojiExclusions() {
        if (Build.VERSION.SDK_INT >= 34) {
            return EmojiExclusions_Api34.getExclusions();
        } else {
            return EmojiExclusions_Reflections.getExclusions();
        }
    }

    @RequiresApi(34)
    private static class EmojiExclusions_Api34 {
        private EmojiExclusions_Api34() { /* cannot instantiate */ }

        static @NonNull Set<int[]> getExclusions() {
            // TODO: Call directly when API34 is published
            return EmojiExclusions_Reflections.getExclusions();
        }
    }

    private static class EmojiExclusions_Reflections {
        private EmojiExclusions_Reflections() { /* cannot instantiate */ }

        /**
         * Attempt to reflectively call EmojiExclusion
         *
         * If anything goes wrong, return Collections.emptySet.
         */
        @SuppressWarnings("unchecked")
        // will be checked after platform API for 34 published
        @SuppressLint({ "BanUncheckedReflection" })
        static @NonNull Set<int[]> getExclusions() {
            try {
                Class<?> clazz = Class.forName("android.text.EmojiConsistency");
                Method method = clazz.getMethod("getEmojiConsistencySet");
                Object result = method.invoke(null);
                if (result == null) {
                    return Collections.emptySet();
                }
                // validate the result type before exposing it to caller
                Set<?> resultList = (Set<?>) result;
                for (Object item : resultList) {
                    if (!(item instanceof int[])) {
                        return Collections.emptySet();
                    }
                }
                return (Set<int[]>) resultList;
            } catch (Throwable ignore) {
                return Collections.emptySet();

            }
        }
    }

}
