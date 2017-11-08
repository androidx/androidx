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

import android.support.annotation.RestrictTo;

import androidx.recyclerview.selection.SelectionHelper.SelectionPredicate;

/**
 * Utility class for creating SelectionPredicate instances.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class SelectionPredicates {

    private SelectionPredicates() {}

    /**
     * Returns a selection predicate that allows multiples items to be selected, without
     * any restrictions on which items can be selected.
     * @param <K>
     * @return
     */
    public static <K> SelectionPredicate<K> selectAnything() {
        return new SelectionPredicate<K>() {
            @Override
            public boolean canSetStateForKey(K key, boolean nextState) {
                return true;
            }

            @Override
            public boolean canSetStateAtPosition(int position, boolean nextState) {
                return true;
            }

            @Override
            public boolean canSelectMultiple() {
                return true;
            }
        };
    }

    /**
     * Returns a selection predicate that allows a single item to be selected, without
     * any restrictions on which item can be selected.
     * @param <K>
     * @return
     */
    public static <K> SelectionPredicate<K> selectSingleAnything() {
        return new SelectionPredicate<K>() {
            @Override
            public boolean canSetStateForKey(K key, boolean nextState) {
                return true;
            }

            @Override
            public boolean canSetStateAtPosition(int position, boolean nextState) {
                return true;
            }

            @Override
            public boolean canSelectMultiple() {
                return false;
            }
        };
    }
}
