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

package androidx.recyclerview.selection;

import static androidx.core.util.Preconditions.checkArgument;

import androidx.annotation.IntDef;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides selection library access to stable selection keys identifying items
 * presented by a {@link RecyclerView RecyclerView} instance.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class ItemKeyProvider<K> {

    /**
     * Provides access to all data, regardless of whether it is bound to a view or not.
     * Key providers with this access type enjoy support for enhanced features like:
     * SHIFT+click range selection, and band selection.
     */
    public static final int SCOPE_MAPPED = 0;

    /**
     * Provides access to cached data based for items that were recently bound in the view.
     * Employing this provider will result in a reduced feature-set, as some
     * features like SHIFT+click range selection and band selection are dependent
     * on mapped access.
     */
    public static final int SCOPE_CACHED = 1;

    @IntDef({
            SCOPE_MAPPED,
            SCOPE_CACHED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Scope {}

    private final @Scope int mScope;

    /**
     * Creates a new provider with the given scope.
     *
     * @param scope Scope can't be changed at runtime.
     */
    protected ItemKeyProvider(@Scope int scope) {
        checkArgument(scope == SCOPE_MAPPED || scope == SCOPE_CACHED);

        mScope = scope;
    }

    final boolean hasAccess(@Scope int scope) {
        return scope == mScope;
    }

    /**
     * @return The selection key at the given adapter position, or null.
     */
    public abstract @Nullable K getKey(int position);

    /**
     * @return the position corresponding to the selection key, or RecyclerView.NO_POSITION
     * if the key is unrecognized.
     */
    public abstract int getPosition(@NonNull K key);
}
