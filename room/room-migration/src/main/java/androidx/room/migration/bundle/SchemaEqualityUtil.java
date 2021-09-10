/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.migration.bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.List;
import java.util.Map;

/**
 * utility class to run schema equality on collections.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SchemaEqualityUtil {
    static <T, K extends SchemaEquality<K>> boolean checkSchemaEquality(
            @Nullable Map<T, K> map1, @Nullable Map<T, K> map2) {
        if (map1 == null) {
            return map2 == null;
        }
        if (map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<T, K> pair : map1.entrySet()) {
            if (!checkSchemaEquality(pair.getValue(), map2.get(pair.getKey()))) {
                return false;
            }
        }
        return true;
    }

    static <K extends SchemaEquality<K>> boolean checkSchemaEquality(
            @Nullable List<K> list1, @Nullable List<K> list2) {
        if (list1 == null) {
            return list2 == null;
        }
        if (list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        // we don't care this is n^2, small list + only used for testing.
        for (K item1 : list1) {
            // find matching item
            boolean matched = false;
            for (K item2 : list2) {
                if (checkSchemaEquality(item1, item2)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    static <K extends SchemaEquality<K>> boolean checkSchemaEquality(
            @Nullable K item1, @Nullable K item2) {
        if (item1 == null) {
            return item2 == null;
        }
        if (item2 == null) {
            return false;
        }
        return item1.isSchemaEqual(item2);
    }

    private SchemaEqualityUtil() {
    }
}
