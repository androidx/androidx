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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to generate {@link String} keys from {@link Collection}s of {@link Enum}s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EnumKeyGenerator {

    /** Private, static utility. */
    private EnumKeyGenerator() {
    }

    /**
     * Creates a {@link String} key to uniquely and consistently identify the combination of {@link
     * Enum}s in {@code enums}.
     *
     * <p>Generated key is unique to the dedup'd set. Duplicates and order will not change key.
     *
     * @return String representing the {@link Enum}s in {@code enums} or empty {@link String} when
     * {@code enums} is null or empty.
     */
    @NonNull
    public static <E extends Enum<E>> String createKey(@Nullable Collection<E> enums) {
        if (enums == null || enums.isEmpty()) {
            return "";
        }

        Set<E> enumsSet = enums instanceof Set ? (Set<E>) enums : new HashSet<>(enums);
        return setToString(enumsSet);
    }

    private static <E extends Enum<E>> String setToString(Set<E> enums) {
        List<E> enumsList = new ArrayList<>(enums);
        Collections.sort(enumsList);
        StringBuilder builder = new StringBuilder();
        for (E type : enumsList) {
            builder.append(type.name()).append("_");
        }
        return builder.toString();
    }
}
