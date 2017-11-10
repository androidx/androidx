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

package androidx.recyclerview.selection.testing;

import static org.junit.Assert.assertEquals;

import android.view.MotionEvent;

import androidx.recyclerview.selection.ActivationCallbacks;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;

public final class TestActivationCallbacks<K> extends ActivationCallbacks<K> {

    private ItemDetails<K> mActivated;

    @Override
    public boolean onItemActivated(ItemDetails<K> item, MotionEvent e) {
        mActivated = item;
        return true;
    }

    public void assertActivated(ItemDetails<K> expected) {
        assertEquals(expected, mActivated);
    }
}
