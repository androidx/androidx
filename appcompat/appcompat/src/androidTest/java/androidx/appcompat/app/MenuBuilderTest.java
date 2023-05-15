/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Parcelable;
import android.view.ViewGroup;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SubMenuBuilder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MenuBuilderTest {

    @Test
    public void setOptionalIconsVisibleMethodShouldRemainPublic() throws Exception {
        // This test is to verify workaround for bug in the ROM of Explay Fresh devices with 4.2.2 ROM.
        // Manufacturer has modified ROM and added a public method setOptionalIconsVisible
        // to android.view.Menu interface. Because of that the runtime can't load MenuBuilder class
        // because it had no such public method (it was package local)
        Method method = MenuBuilder.class
                .getMethod("setOptionalIconsVisible", boolean.class);
        assertNotNull(method);
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }

    @Test
    public void addMultipleMenuItems_withoutSuppression_updateMenuView_calledOncePerItem() {
        MenuBuilder menuBuilder =
                new MenuBuilder(InstrumentationRegistry.getInstrumentation().getContext());
        CountingMenuPresenter presenter = new CountingMenuPresenter();
        menuBuilder.addMenuPresenter(presenter);
        menuBuilder.add("One");
        menuBuilder.add("Two");
        menuBuilder.add("Three");
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(3);
    }

    @Test
    public void addMultipleMenuItems_withSuppression_updateMenuView_calledOnceAtEnd() {
        MenuBuilder menuBuilder =
                new MenuBuilder(InstrumentationRegistry.getInstrumentation().getContext());
        CountingMenuPresenter presenter = new CountingMenuPresenter();
        menuBuilder.addMenuPresenter(presenter);
        menuBuilder.stopDispatchingItemsChanged();
        menuBuilder.add("One");
        menuBuilder.add("Two");
        menuBuilder.add("Three");
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(0);
        menuBuilder.startDispatchingItemsChanged();
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(1);
    }

    @Test
    public void addMultipleMenuItems_withMultipleSuppressionCalls() {
        MenuBuilder menuBuilder =
                new MenuBuilder(InstrumentationRegistry.getInstrumentation().getContext());
        CountingMenuPresenter presenter = new CountingMenuPresenter();
        menuBuilder.addMenuPresenter(presenter);

        assertThat(menuBuilder.isDispatchingItemsChanged()).isTrue();

        menuBuilder.stopDispatchingItemsChanged();
        menuBuilder.add("One");
        assertThat(menuBuilder.isDispatchingItemsChanged()).isFalse();

        // Should be a no-op
        menuBuilder.stopDispatchingItemsChanged();
        assertThat(menuBuilder.isDispatchingItemsChanged()).isFalse();

        menuBuilder.add("Two");
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(0);

        menuBuilder.startDispatchingItemsChanged();
        assertThat(menuBuilder.isDispatchingItemsChanged()).isTrue();
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(1);

        menuBuilder.add("Three");
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(2);

        // Should be a no-op
        menuBuilder.startDispatchingItemsChanged();
        assertThat(presenter.mUpdateMenuViewCalls).isEqualTo(2);
    }

    private static class CountingMenuPresenter implements MenuPresenter {
        int mUpdateMenuViewCalls = 0;

        @Override
        public void initForMenu(Context context, MenuBuilder menu) {

        }

        @Override
        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        @Override
        public void updateMenuView(boolean cleared) {
            mUpdateMenuViewCalls++;
        }

        @Override
        public void setCallback(Callback cb) {

        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {

        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            return false;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            return false;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {

        }
    }
}

