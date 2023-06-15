/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Represent a key that references a dynamic value source, such as state pushed by app/tile or
 * real-time data from the platform.
 *
 * @param <T> The data type of the dynamic values that this key is bound to.
 */
public abstract class DynamicDataKey<T extends DynamicBuilders.DynamicType> {
    @NonNull private final String mKey;
    @NonNull private final String mNamespace;

    /**
     * Create a {@link DynamicDataKey} with the specified key in the given namespace.
     *
     * @param namespace The namespace of the key for the dynamic data source.
     * @param key The key that references the dynamic data source.
     */
    DynamicDataKey(@NonNull String namespace, @NonNull String key) {
        mKey = key;
        mNamespace = namespace;
    }

    /** Gets the key that references the dynamic data source */
    @NonNull
    public String getKey() {
        return mKey;
    }

    /** Gets the namespace of the key for the dynamic data source. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof DynamicDataKey)) {
            return false;
        }

        DynamicDataKey<?> comp = (DynamicDataKey<?>) other;

        return mKey.equals(comp.getKey()) && mNamespace.equals(comp.getNamespace());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mKey, mNamespace);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("DynamicDataKey{namespace=%s, key=%s}", mNamespace, mKey);
    }
}
