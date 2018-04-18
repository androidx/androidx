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

import androidx.recyclerview.selection.FocusDelegate;
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.widget.RecyclerView;

public final class TestFocusDelegate<K> extends FocusDelegate<K> {

    private K mFocusItemId;
    private int mFocusPosition;

    @Override
    public void clearFocus() {
        mFocusPosition = RecyclerView.NO_POSITION;
        mFocusItemId = null;
    }

    @Override
    public void focusItem(ItemDetails<K> item) {
        mFocusItemId = item.getSelectionKey();
        mFocusPosition = item.getPosition();
    }

    @Override
    public int getFocusedPosition() {
        return mFocusPosition;
    }

    @Override
    public boolean hasFocusedItem() {
        return mFocusItemId != null;
    }

    public void assertHasFocus(boolean focused) {
        assertEquals(focused, hasFocusedItem());
    }

    public void assertFocused(String expectedId) {
        assertEquals(expectedId, mFocusItemId);
    }
}
