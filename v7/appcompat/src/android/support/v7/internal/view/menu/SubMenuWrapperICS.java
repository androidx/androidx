/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v7.internal.view.menu;

import android.graphics.drawable.Drawable;
import android.support.v4.internal.view.SupportSubMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

class SubMenuWrapperICS extends MenuWrapperICS implements SupportSubMenu {
    SubMenuWrapperICS(android.view.SubMenu subMenu) {
        super(subMenu);
    }

    @Override
    public android.view.SubMenu getWrappedObject() {
        return (android.view.SubMenu) mWrappedObject;
    }

    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        ((android.view.SubMenu) mWrappedObject).setHeaderTitle(titleRes);
        return this;
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        ((android.view.SubMenu) mWrappedObject).setHeaderTitle(title);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        ((android.view.SubMenu) mWrappedObject).setHeaderIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        ((android.view.SubMenu) mWrappedObject).setHeaderIcon(icon);
        return this;
    }

    @Override
    public SubMenu setHeaderView(View view) {
        ((android.view.SubMenu) mWrappedObject).setHeaderView(view);
        return this;
    }

    @Override
    public void clearHeader() {
        ((android.view.SubMenu) mWrappedObject).clearHeader();
    }

    @Override
    public SubMenu setIcon(int iconRes) {
        ((android.view.SubMenu) mWrappedObject).setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        ((android.view.SubMenu) mWrappedObject).setIcon(icon);
        return this;
    }

    @Override
    public MenuItem getItem() {
        return getMenuItemWrapper(((android.view.SubMenu) mWrappedObject).getItem());
    }
}
