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

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

/**
 * Override methods in this class to provide application specific behaviors
 * related to focusing item.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class FocusDelegate<K> {

    static <K> FocusDelegate<K> stub() {
        return new FocusDelegate<K>() {
            @Override
            public void focusItem(@NonNull ItemDetails<K> item) {
            }

            @Override
            public boolean hasFocusedItem() {
                return false;
            }

            @Override
            public int getFocusedPosition() {
                return RecyclerView.NO_POSITION;
            }

            @Override
            public void clearFocus() {
            }
        };
    }

    /**
     * If environment supports focus, focus {@code item}.
     */
    public abstract void focusItem(@NonNull ItemDetails<K> item);

    /**
     * @return true if there is a focused item.
     */
    public abstract boolean hasFocusedItem();

    /**
     * Returns the position of the currently focused item, or
     * {@link RecyclerView#NO_POSITION} if nothing is focused.
     *
     * <p>You must implement this feature if you intend your app
     * to work well with mouse and keyboard. Selection
     * ranges are inferred from focused item when there is
     * no explicit last-selected item.
     *
     * <p>You can manage and advance focus using keyboard arrows,
     * reflecting this state visibly in the view item.
     * Use can then press shift, then click another item with
     * their mouse to select all items between the focused
     * item and the clicked item.
     *
     * @return the position of the currently focused item,
     *     or {@code RecyclerView#NO_POSITION} if none.
     */
    public abstract int getFocusedPosition();

    /**
     * If the environment supports focus and something is focused, unfocus it.
     */
    public abstract void clearFocus();
}
