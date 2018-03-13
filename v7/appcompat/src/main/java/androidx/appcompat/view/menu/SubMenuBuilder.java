/*
 * Copyright (C) 2006 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.RestrictTo;

/**
 * The model for a sub menu, which is an extension of the menu.  Most methods are proxied to the
 * parent menu.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SubMenuBuilder extends MenuBuilder implements SubMenu {
    private MenuBuilder mParentMenu;
    private MenuItemImpl mItem;

    public SubMenuBuilder(Context context, MenuBuilder parentMenu, MenuItemImpl item) {
        super(context);

        mParentMenu = parentMenu;
        mItem = item;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mParentMenu.setQwertyMode(isQwerty);
    }

    @Override
    public boolean isQwertyMode() {
        return mParentMenu.isQwertyMode();
    }

    @Override
    public void setShortcutsVisible(boolean shortcutsVisible) {
        mParentMenu.setShortcutsVisible(shortcutsVisible);
    }

    @Override
    public boolean isShortcutsVisible() {
        return mParentMenu.isShortcutsVisible();
    }

    public Menu getParentMenu() {
        return mParentMenu;
    }

    @Override
    public MenuItem getItem() {
        return mItem;
    }

    @Override
    public void setCallback(Callback callback) {
        mParentMenu.setCallback(callback);
    }

    @Override
    public MenuBuilder getRootMenu() {
        return mParentMenu.getRootMenu();
    }

    @Override
    boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return super.dispatchMenuItemSelected(menu, item) ||
                mParentMenu.dispatchMenuItemSelected(menu, item);
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        mItem.setIcon(icon);
        return this;
    }

    @Override
    public SubMenu setIcon(int iconRes) {
        mItem.setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        return (SubMenu) super.setHeaderIconInt(icon);
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        return (SubMenu) super.setHeaderIconInt(iconRes);
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        return (SubMenu) super.setHeaderTitleInt(title);
    }

    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        return (SubMenu) super.setHeaderTitleInt(titleRes);
    }

    @Override
    public SubMenu setHeaderView(View view) {
        return (SubMenu) super.setHeaderViewInt(view);
    }

    @Override
    public boolean expandItemActionView(MenuItemImpl item) {
        return mParentMenu.expandItemActionView(item);
    }

    @Override
    public boolean collapseItemActionView(MenuItemImpl item) {
        return mParentMenu.collapseItemActionView(item);
    }

    @Override
    public String getActionViewStatesKey() {
        final int itemId = mItem != null ? mItem.getItemId() : 0;
        if (itemId == 0) {
            return null;
        }
        return super.getActionViewStatesKey() + ":" + itemId;
    }

    @Override
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        mParentMenu.setGroupDividerEnabled(groupDividerEnabled);
    }

    @Override
    public boolean isGroupDividerEnabled() {
        return mParentMenu.isGroupDividerEnabled();
    }
}
