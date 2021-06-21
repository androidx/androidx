/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.appcompat.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatAutoCompleteTextView} class.
 */
@LargeTest
public class AppCompatAutoCompleteTextViewTest
        extends AppCompatBaseViewTest<AppCompatAutoCompleteTextViewActivity,
        AppCompatAutoCompleteTextView> {

    public AppCompatAutoCompleteTextViewTest() {
        super(AppCompatAutoCompleteTextViewActivity.class);
    }

    @UiThreadTest
    public void testSetCustomSelectionActionModeCallback() {
        final AppCompatAutoCompleteTextView view = new AppCompatAutoCompleteTextView(mActivity);
        final ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };

        // Default value is documented as null.
        assertNull(view.getCustomSelectionActionModeCallback());

        // Setter and getter should be symmetric.
        view.setCustomSelectionActionModeCallback(callback);
        assertEquals(callback, view.getCustomSelectionActionModeCallback());

        // Argument is nullable.
        view.setCustomSelectionActionModeCallback(null);
        assertNull(view.getCustomSelectionActionModeCallback());
    }
}
