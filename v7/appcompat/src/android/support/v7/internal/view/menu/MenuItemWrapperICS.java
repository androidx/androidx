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
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;

class MenuItemWrapperICS extends MenuItemWrapperHC {
    MenuItemWrapperICS(android.view.MenuItem object) {
        super(object);
    }

    @Override
    public MenuItem setActionProvider(android.view.ActionProvider provider) {
        mWrappedObject.setActionProvider(provider);
        return this;
    }

    @Override
    public android.view.ActionProvider getActionProvider() {
        return mWrappedObject.getActionProvider();
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
        mWrappedObject.setOnActionExpandListener(listener != null ?
                new OnActionExpandListenerWrapper(listener) : null);
        return this;
    }

    @Override
    public SupportMenuItem setSupportActionProvider(ActionProvider actionProvider) {
        mWrappedObject.setActionProvider(actionProvider != null ?
                createActionProviderWrapper(actionProvider) : null);
        return this;
    }

    @Override
    public ActionProvider getSupportActionProvider() {
        ActionProviderWrapper providerWrapper =
                (ActionProviderWrapper)mWrappedObject.getActionProvider();
        return providerWrapper.mInner;
    }

    ActionProviderWrapper createActionProviderWrapper(ActionProvider provider) {
        return new ActionProviderWrapper(provider);
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

    class ActionProviderWrapper extends android.view.ActionProvider {
        final ActionProvider mInner;

        public ActionProviderWrapper(ActionProvider inner) {
            super(inner.getContext());
            mInner = inner;
        }

        @Override
        public View onCreateActionView() {
            return mInner.onCreateActionView();
        }

        @Override
        public boolean onPerformDefaultAction() {
            return mInner.onPerformDefaultAction();
        }

        @Override
        public boolean hasSubMenu() {
            return mInner.hasSubMenu();
        }

        @Override
        public void onPrepareSubMenu(android.view.SubMenu subMenu) {
            mInner.onPrepareSubMenu(getSubMenuWrapper(subMenu));
        }
    }
}
