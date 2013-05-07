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

import android.support.v4.internal.view.SupportMenuItem;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.HashMap;
import java.util.Iterator;

abstract class BaseMenuWrapper<T> extends BaseWrapper<T> {

    private HashMap<MenuItem, SupportMenuItem> mMenuItems;

    private HashMap<SubMenu, SubMenu> mSubMenus;

    BaseMenuWrapper(T object) {
        super(object);
    }

    final SupportMenuItem getMenuItemWrapper(android.view.MenuItem frameworkItem) {
        if (frameworkItem != null) {
            // Instantiate HashMap if null
            if (mMenuItems == null) {
                mMenuItems = new HashMap<MenuItem, SupportMenuItem>();
            }

            SupportMenuItem compatItem = mMenuItems.get(frameworkItem);

            if (null == compatItem) {
                compatItem = MenuWrapperFactory.createSupportMenuItemWrapper(frameworkItem);
                mMenuItems.put(frameworkItem, compatItem);
            }

            return compatItem;
        }
        return null;
    }

    final SubMenu getSubMenuWrapper(android.view.SubMenu frameworkSubMenu) {
        if (frameworkSubMenu != null) {
            // Instantiate HashMap if null
            if (mSubMenus == null) {
                mSubMenus = new HashMap<android.view.SubMenu, SubMenu>();
            }

            SubMenu compatSubMenu = mSubMenus.get(frameworkSubMenu);

            if (null == compatSubMenu) {
                compatSubMenu = MenuWrapperFactory.createSupportSubMenuWrapper(frameworkSubMenu);
                mSubMenus.put(frameworkSubMenu, compatSubMenu);
            }
            return compatSubMenu;
        }
        return null;
    }


    final void internalClear() {
        if (mMenuItems != null) {
            mMenuItems.clear();
        }
        if (mSubMenus != null) {
            mSubMenus.clear();
        }
    }

    final void internalRemoveGroup(final int groupId) {
        if (mMenuItems == null) {
            return;
        }

        Iterator<android.view.MenuItem> iterator = mMenuItems.keySet().iterator();
        android.view.MenuItem menuItem;

        while (iterator.hasNext()) {
            menuItem = iterator.next();
            if (groupId == menuItem.getGroupId()) {
                iterator.remove();
            }
        }
    }

    final void internalRemoveItem(final int id) {
        if (mMenuItems == null) {
            return;
        }

        Iterator<android.view.MenuItem> iterator = mMenuItems.keySet().iterator();
        android.view.MenuItem menuItem;

        while (iterator.hasNext()) {
            menuItem = iterator.next();
            if (id == menuItem.getItemId()) {
                iterator.remove();
                break;
            }
        }
    }
}
