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

import android.support.v7.view.ActionProvider;
import android.support.v7.view.MenuItem;
import android.support.v7.view.SubMenu;
import android.view.View;

class MenuItemWrapperICS extends MenuItemWrapperHC {

    MenuItemWrapperICS(android.view.MenuItem object) {
        super(object);
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        mWrappedObject.setActionProvider(new ActionProviderWrapper(actionProvider));
        return this;
    }

    @Override
    public ActionProvider getActionProvider() {
        return ((ActionProviderWrapper) mWrappedObject.getActionProvider()).getWrappedObject();
    }

    @Override
    public boolean expandActionView() {
        return mWrappedObject.expandActionView();
    }

    @Override
    public boolean collapseActionView() {
        return mWrappedObject.collapseActionView();
    }

    @Override
    public boolean isActionViewExpanded() {
        return mWrappedObject.isActionViewExpanded();
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        mWrappedObject.setOnActionExpandListener(new OnActionExpandListenerWrapper(listener));
        return this;
    }

    @Override
    MenuItem createMenuItemWrapper(android.view.MenuItem menuItem) {
        return new MenuItemWrapperICS(menuItem);
    }

    @Override
    SubMenu createSubMenuWrapper(android.view.SubMenu subMenu) {
        return new SubMenuWrapperICS(subMenu);
    }

    private class OnActionExpandListenerWrapper extends BaseWrapper<MenuItem.OnActionExpandListener>
            implements android.view.MenuItem.OnActionExpandListener {

        OnActionExpandListenerWrapper(MenuItem.OnActionExpandListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemActionExpand(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemActionExpand(getMenuItemWrapper(item));
        }

        @Override
        public boolean onMenuItemActionCollapse(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemActionCollapse(getMenuItemWrapper(item));
        }
    }

    private class ActionProviderWrapper extends android.view.ActionProvider {

        private final ActionProvider mWrappedObject;

        public ActionProviderWrapper(ActionProvider object) {
            super(null);
            mWrappedObject = object;
        }

        @Override
        public View onCreateActionView() {
            return mWrappedObject.onCreateActionView();
        }

        @Override
        public boolean onPerformDefaultAction() {
            return mWrappedObject.onPerformDefaultAction();
        }

        @Override
        public boolean hasSubMenu() {
            return mWrappedObject.hasSubMenu();
        }

        @Override
        public void onPrepareSubMenu(android.view.SubMenu subMenu) {
            mWrappedObject.onPrepareSubMenu(getSubMenuWrapper(subMenu));
        }

        ActionProvider getWrappedObject() {
            return mWrappedObject;
        }
    }

}
