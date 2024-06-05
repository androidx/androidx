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

package androidx.pdf.util;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils to deal with {@link Bundle}s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class BundleUtils {
    private BundleUtils() {
    }

    /**
     * Reads [String, String] key value pairs from a {@link Bundle}. Returns null values if the
     * value associated with a given key is not a String.
     */
    @Nullable
    public static Map<String, String> getMapFrom(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>(bundle.size());
        for (String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }
        return map;
    }

    /**
     * Checks equality of two {@link Bundle}s.
     *
     * <p>If both are null, return true. If one or the other is null, return false. If both are
     * non-null, verify lengths are the same, then compare values of each key. If any key's value
     * are not equal return false. Otherwise, return true;
     *
     * <p>NOTE: This implementation calls the {@link #equals(Object)} function for Object values,
     * and
     * thus does not account for comparing nested {@link Bundle}s which are values of keys in the
     * input {@link Bundle}s.
     */
    public static boolean bundleEquals(@NonNull Bundle b1, @NonNull Bundle b2) {
        if (b1 == null && b2 == null) {
            return true;
        }
        if (b1 == null || b2 == null) {
            return false;
        }
        if (b1.size() != b2.size()) {
            return false;
        }
        for (String key : b1.keySet()) {
            if (!b1.get(key).equals(b2.get(key))) {
                return false;
            }
        }
        return true;
    }
}
