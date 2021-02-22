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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link MenuItemImpl}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class MenuItemImplTest {

    private MenuItemImpl mMenuItem;

    @Before
    public void setUp() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        final MenuBuilder menu = new MenuBuilder(context);
        mMenuItem = new MenuItemImpl(
                menu, Menu.NONE, Menu.NONE, 0, 0, "item", MenuItem.SHOW_AS_ACTION_IF_ROOM);
        assertThat(mMenuItem.requiresActionButton()).isFalse();
        assertThat(mMenuItem.requiresOverflow()).isFalse();
    }

    @Test
    public void setShowAsActionAlways() throws Exception {
        mMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        assertThat(mMenuItem.requiresActionButton()).isTrue();
        assertThat(mMenuItem.requiresOverflow()).isFalse();
    }

    @Test
    public void setShowAsActionNever() throws Exception {
        mMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        assertThat(mMenuItem.requiresActionButton()).isFalse();
        assertThat(mMenuItem.requiresOverflow()).isTrue();
    }
}
