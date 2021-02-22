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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.core.internal.view.SupportSubMenu;

/**
 * Wraps a support {@link SupportSubMenu} as a framework {@link android.view.SubMenu}
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
class SubMenuWrapperICS extends MenuWrapperICS implements SubMenu {
    private final SupportSubMenu mSubMenu;

    SubMenuWrapperICS(Context context, SupportSubMenu subMenu) {
        super(context, subMenu);
        mSubMenu = subMenu;
    }

    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        mSubMenu.setHeaderTitle(titleRes);
        return this;
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        mSubMenu.setHeaderTitle(title);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        mSubMenu.setHeaderIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        mSubMenu.setHeaderIcon(icon);
        return this;
    }

    @Override
    public SubMenu setHeaderView(View view) {
        mSubMenu.setHeaderView(view);
        return this;
    }

    @Override
    public void clearHeader() {
        mSubMenu.clearHeader();
    }

    @Override
    public SubMenu setIcon(int iconRes) {
        mSubMenu.setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        mSubMenu.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem getItem() {
        return getMenuItemWrapper(mSubMenu.getItem());
    }
}
