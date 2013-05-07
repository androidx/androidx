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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ActionProvider;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

class MenuItemWrapperHC extends BaseMenuWrapper<android.view.MenuItem> implements SupportMenuItem {
    MenuItemWrapperHC(android.view.MenuItem object) {
        super(object);
    }

    @Override
    public int getItemId() {
        return mWrappedObject.getItemId();
    }

    @Override
    public int getGroupId() {
        return mWrappedObject.getGroupId();
    }

    @Override
    public int getOrder() {
        return mWrappedObject.getOrder();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        mWrappedObject.setTitle(title);
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        mWrappedObject.setTitle(title);
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return mWrappedObject.getTitle();
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mWrappedObject.setTitleCondensed(title);
        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return mWrappedObject.getTitleCondensed();
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        mWrappedObject.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        mWrappedObject.setIcon(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon() {
        return mWrappedObject.getIcon();
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        mWrappedObject.setIntent(intent);
        return this;
    }

    @Override
    public Intent getIntent() {
        return mWrappedObject.getIntent();
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        mWrappedObject.setShortcut(numericChar, alphaChar);
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        mWrappedObject.setNumericShortcut(numericChar);
        return this;
    }

    @Override
    public char getNumericShortcut() {
        return mWrappedObject.getNumericShortcut();
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        mWrappedObject.setAlphabeticShortcut(alphaChar);
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return mWrappedObject.getAlphabeticShortcut();
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        mWrappedObject.setCheckable(checkable);
        return this;
    }

    @Override
    public boolean isCheckable() {
        return mWrappedObject.isCheckable();
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        mWrappedObject.setChecked(checked);
        return this;
    }

    @Override
    public boolean isChecked() {
        return mWrappedObject.isChecked();
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        mWrappedObject.setVisible(visible);
        return this;
    }

    @Override
    public boolean isVisible() {
        return mWrappedObject.isVisible();
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        mWrappedObject.setEnabled(enabled);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return mWrappedObject.isEnabled();
    }

    @Override
    public boolean hasSubMenu() {
        return mWrappedObject.hasSubMenu();
    }

    @Override
    public SubMenu getSubMenu() {
        return getSubMenuWrapper(mWrappedObject.getSubMenu());
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        mWrappedObject.setOnMenuItemClickListener(menuItemClickListener != null ?
                new OnMenuItemClickListenerWrapper(menuItemClickListener) : null);
        return this;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return mWrappedObject.getMenuInfo();
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        mWrappedObject.setShowAsAction(actionEnum);
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        mWrappedObject.setShowAsActionFlags(actionEnum);
        return this;
    }

    @Override
    public MenuItem setActionView(View view) {
        mWrappedObject.setActionView(view);
        return this;
    }

    @Override
    public MenuItem setActionView(int resId) {
        mWrappedObject.setActionView(resId);
        return this;
    }

    @Override
    public View getActionView() {
        return mWrappedObject.getActionView();
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

    @Override
    public MenuItem setActionProvider(android.view.ActionProvider actionProvider) {
        // APIv14+ API
        throw new UnsupportedOperationException();
    }

    @Override
    public android.view.ActionProvider getActionProvider() {
        // APIv14+ API
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean expandActionView() {
        // APIv14+ API
        return false;
    }

    @Override
    public boolean collapseActionView() {
        // APIv14+ API
        return false;
    }

    @Override
    public boolean isActionViewExpanded() {
        // APIv14+ API
        return false;
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        // APIv14+ API
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportMenuItem setSupportOnActionExpandListener(
            MenuItemCompat.OnActionExpandListener listener) {
        // APIv14+ API
        return null;
    }

    ActionProviderWrapper createActionProviderWrapper(ActionProvider provider) {
        return new ActionProviderWrapper(provider);
    }

    private class OnMenuItemClickListenerWrapper extends BaseWrapper<OnMenuItemClickListener>
            implements android.view.MenuItem.OnMenuItemClickListener {

        OnMenuItemClickListenerWrapper(OnMenuItemClickListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemClick(getMenuItemWrapper(item));
        }
    }

    class ActionProviderWrapper extends android.view.ActionProvider {
        final ActionProvider mInner;

        public ActionProviderWrapper(ActionProvider inner) {
            super(inner.getContext());
            mInner = inner;
        }

        @Override
        @SuppressWarnings("deprecation")
        public View onCreateActionView() {
            return mInner.onCreateActionView(mWrappedObject);
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
