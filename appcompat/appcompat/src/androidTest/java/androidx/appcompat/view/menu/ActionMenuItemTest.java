/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.appcompat.view.menu;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ActionMenuItemTest}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ActionMenuItemTest {

    @Test
    public void setShowAsActionNever() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        final ActionMenuItem item = new ActionMenuItem(
                context, Menu.NONE, Menu.NONE, 0, 0, "item");

        // ActionMenuItem always require action button and never require overflow irrespective of
        // the set showAsAction flag.

        assertThat(item.requiresActionButton()).isTrue();
        assertThat(item.requiresOverflow()).isFalse();

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        assertThat(item.requiresActionButton()).isTrue();
        assertThat(item.requiresOverflow()).isFalse();
    }
}
