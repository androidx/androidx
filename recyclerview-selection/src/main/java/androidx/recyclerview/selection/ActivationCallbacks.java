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
import android.view.MotionEvent;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

/**
 * Override methods in this class to connect specialized behaviors of the selection
 * code to the application environment.
 *
 * @param <K> Selection key type. Usually String or Long.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class ActivationCallbacks<K> {

    static <K> ActivationCallbacks<K> dummy() {
        return new ActivationCallbacks<K>() {
            @Override
            public boolean onItemActivated(ItemDetails item, MotionEvent e) {
                return false;
            }
        };
    }

    /**
     * Called when an item is activated. An item is activitated, for example, when
     * there is no active selection and the user double clicks an item with a
     * pointing device like a Mouse.
     *
     * @param item details of the item.
     * @param e the event associated with item.
     * @return true if the event was handled.
     */
    public abstract boolean onItemActivated(ItemDetails<K> item, MotionEvent e);
}
