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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkArgument;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides support for sting based stable ids in the RecyclerView selection helper.
 * Client code can use this to look up stable ids when working with selection
 * in application code.
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class ItemKeyProvider<K> {

    /**
     * Provides access to all data, regardless of whether it is bound to a view or not.
     * Key providers with this access type enjoy support for enhanced features like:
     * SHIFT+click range selection, and band selection.
     */
    @VisibleForTesting  // otherwise protected would do nicely.
    public static final int SCOPE_MAPPED = 0;

    /**
     * Provides access cached data based on what was recently bound in the view.
     * Employing this provider will result in a reduced feature-set, as some
     * featuers like SHIFT+click range selection and band selection are dependent
     * on mapped access.
     */
    @VisibleForTesting  // otherwise protected would do nicely.
    public static final int SCOPE_CACHED = 1;

    @IntDef({
            SCOPE_MAPPED,
            SCOPE_CACHED
    })
    @Retention(RetentionPolicy.SOURCE)
    protected @interface Scope {}

    private final @Scope int mScope;

    /**
     * Creates a new provider with the given scope.
     * @param scope Scope can't change at runtime (at least code won't adapt)
     *         so it must be specified in the constructor.
     */
    protected ItemKeyProvider(@Scope int scope) {
        checkArgument(scope == SCOPE_MAPPED || scope == SCOPE_CACHED);

        mScope = scope;
    }

    final boolean hasAccess(@Scope int scope) {
        return scope == mScope;
    }

    /**
     * @return The selection key of the item at the given adapter position.
     */
    public abstract @Nullable K getKey(int position);

    /**
     * @return the position of a stable ID, or RecyclerView.NO_POSITION.
     */
    public abstract int getPosition(K key);
}
