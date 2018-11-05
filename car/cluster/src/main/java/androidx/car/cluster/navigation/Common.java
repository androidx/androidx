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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Helper methods
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
final class Common {
    /**
     * This is a utility class
     */
    private Common() {
    }

    /**
     * Returns the given string, or an empty string if the value is null.
     */
    @NonNull
    public static String nonNullOrEmpty(@Nullable String value) {
        return value != null ? value : "";
    }

    /**
     * Returns an immutable view of the given list, or an empty one if the list is null, or if any
     * of its elements is null.
     */
    @NonNull
    public static <T> List<T> immutableOrEmpty(@Nullable List<T> list) {
        if (list == null || list.stream().anyMatch(Objects::isNull)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }
}
