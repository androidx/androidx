/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.internal.widget.drawer;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.widget.drawer.WearableNavigationDrawerView.OnItemSelectedListener;
import androidx.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * Controls the behavior of this view where the behavior may differ between single and multi-page.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public abstract class WearableNavigationDrawerPresenter {

    private final Set<OnItemSelectedListener> mOnItemSelectedListeners = new HashSet<>();

    /**
     * Indicates to the presenter that the underlying data has changed.
     */
    @MainThread
    public abstract void onDataSetChanged();

    /**
     * Indicates to the presenter that the drawer has a new adapter.
     */
    @MainThread
    public abstract void onNewAdapter(WearableNavigationDrawerAdapter adapter);

    /**
     * Indicates to the presenter that the user has selected an item.
     */
    @MainThread
    public abstract void onSelected(int index);

    /**
     * Indicates to the presenter that the developer wishes to change which item is selected.
     */
    @MainThread
    public abstract void onSetCurrentItemRequested(int index, boolean smoothScrollTo);

    /**
     * Indicates to the presenter that the user has tapped on the drawer.
     *
     * @return {@code true} if the touch event has been handled and should not propagate further.
     */
    @MainThread
    public abstract boolean onDrawerTapped();

    /**
     * Indicates to the presenter that a new {@link OnItemSelectedListener} has been added.
     */
    @MainThread
    public void onItemSelectedListenerAdded(OnItemSelectedListener listener) {
        mOnItemSelectedListeners.add(listener);
    }

    /**
     * Indicates to the presenter that an {@link OnItemSelectedListener} has been removed.
     */
    @MainThread
    public void onItemSelectedListenerRemoved(OnItemSelectedListener listener) {
        mOnItemSelectedListeners.remove(listener);
    }

    /**
     * Notifies all listeners that the item at {@code selectedPos} has been selected.
     */
    @MainThread
    void notifyItemSelectedListeners(int selectedPos) {
        for (OnItemSelectedListener listener : mOnItemSelectedListeners) {
            listener.onItemSelected(selectedPos);
        }
    }
}
