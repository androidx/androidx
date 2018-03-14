/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget.drawer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/* package */ class WearableActionDrawerMenu implements Menu {

    private final Context mContext;
    private final List<WearableActionDrawerMenuItem> mItems = new ArrayList<>();
    private final WearableActionDrawerMenuListener mListener;
    private final WearableActionDrawerMenuItem.MenuItemChangedListener mItemChangedListener =
            new WearableActionDrawerMenuItem.MenuItemChangedListener() {
                @Override
                public void itemChanged(WearableActionDrawerMenuItem item) {
                    for (int i = 0; i < mItems.size(); i++) {
                        if (mItems.get(i) == item) {
                            mListener.menuItemChanged(i);
                        }
                    }
                }
            };

    WearableActionDrawerMenu(Context context, WearableActionDrawerMenuListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public MenuItem add(CharSequence title) {
        return add(0, 0, 0, title);
    }

    @Override
    public MenuItem add(int titleRes) {
        return add(0, 0, 0, titleRes);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return add(groupId, itemId, order, mContext.getResources().getString(titleRes));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        WearableActionDrawerMenuItem item =
                new WearableActionDrawerMenuItem(mContext, itemId, title, mItemChangedListener);
        mItems.add(item);
        mListener.menuItemAdded(mItems.size() - 1);
        return item;
    }

    @Override
    public void clear() {
        mItems.clear();
        mListener.menuChanged();
    }

    @Override
    public void removeItem(int id) {
        int index = findItemIndex(id);
        if ((index < 0) || (index >= mItems.size())) {
            return;
        }
        mItems.remove(index);
        mListener.menuItemRemoved(index);
    }

    @Override
    public MenuItem findItem(int id) {
        int index = findItemIndex(id);
        if ((index < 0) || (index >= mItems.size())) {
            return null;
        }
        return mItems.get(index);
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    @Nullable
    public MenuItem getItem(int index) {
        if ((index < 0) || (index >= mItems.size())) {
            return null;
        }
        return mItems.get(index);
    }

    private int findItemIndex(int id) {
        final List<WearableActionDrawerMenuItem> items = mItems;
        final int itemCount = items.size();
        for (int i = 0; i < itemCount; i++) {
            if (items.get(i).getItemId() == id) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("close is not implemented");
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        throw new UnsupportedOperationException("addSubMenu is not implemented");
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        throw new UnsupportedOperationException("addSubMenu is not implemented");
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        throw new UnsupportedOperationException("addSubMenu is not implemented");
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        throw new UnsupportedOperationException("addSubMenu is not implemented");
    }

    @Override
    public int addIntentOptions(
            int groupId,
            int itemId,
            int order,
            ComponentName caller,
            Intent[] specifics,
            Intent intent,
            int flags,
            MenuItem[] outSpecificItems) {
        throw new UnsupportedOperationException("addIntentOptions is not implemented");
    }

    @Override
    public void removeGroup(int groupId) {
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        throw new UnsupportedOperationException("setGroupCheckable is not implemented");
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        throw new UnsupportedOperationException("setGroupVisible is not implemented");
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        throw new UnsupportedOperationException("setGroupEnabled is not implemented");
    }

    @Override
    public boolean hasVisibleItems() {
        return false;
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        throw new UnsupportedOperationException("performShortcut is not implemented");
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        throw new UnsupportedOperationException("performIdentifierAction is not implemented");
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
    }

    /* package */ interface WearableActionDrawerMenuListener {

        void menuItemChanged(int position);

        void menuItemAdded(int position);

        void menuItemRemoved(int position);

        void menuChanged();
    }

    public static final class WearableActionDrawerMenuItem implements MenuItem {

        private final int mId;

        private final Context mContext;
        private final MenuItemChangedListener mItemChangedListener;
        private CharSequence mTitle;
        private Drawable mIconDrawable;
        private MenuItem.OnMenuItemClickListener mClickListener;

        WearableActionDrawerMenuItem(
                Context context, int id, CharSequence title, MenuItemChangedListener listener) {
            mContext = context;
            mId = id;
            mTitle = title;
            mItemChangedListener = listener;
        }

        @Override
        public int getItemId() {
            return mId;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            mTitle = title;
            if (mItemChangedListener != null) {
                mItemChangedListener.itemChanged(this);
            }
            return this;
        }

        @Override
        public MenuItem setTitle(int title) {
            return setTitle(mContext.getResources().getString(title));
        }

        @Override
        public CharSequence getTitle() {
            return mTitle;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            mIconDrawable = icon;
            if (mItemChangedListener != null) {
                mItemChangedListener.itemChanged(this);
            }
            return this;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            return setIcon(mContext.getResources().getDrawable(iconRes));
        }

        @Override
        public Drawable getIcon() {
            return mIconDrawable;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
            mClickListener = menuItemClickListener;
            return this;
        }

        @Override
        public int getGroupId() {
            return 0;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            return this;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return null;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            throw new UnsupportedOperationException("setIntent is not implemented");
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            throw new UnsupportedOperationException("setShortcut is not implemented");
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            return this;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            return this;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            return this;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            return this;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            return this;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            return this;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {
            throw new UnsupportedOperationException("setShowAsAction is not implemented");
        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            throw new UnsupportedOperationException("setShowAsActionFlags is not implemented");
        }

        @Override
        public MenuItem setActionView(View view) {
            throw new UnsupportedOperationException("setActionView is not implemented");
        }

        @Override
        public MenuItem setActionView(int resId) {
            throw new UnsupportedOperationException("setActionView is not implemented");
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            throw new UnsupportedOperationException("setActionProvider is not implemented");
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            throw new UnsupportedOperationException("expandActionView is not implemented");
        }

        @Override
        public boolean collapseActionView() {
            throw new UnsupportedOperationException("collapseActionView is not implemented");
        }

        @Override
        public boolean isActionViewExpanded() {
            throw new UnsupportedOperationException("isActionViewExpanded is not implemented");
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            throw new UnsupportedOperationException("setOnActionExpandListener is not implemented");
        }

        /**
         * Invokes the item by calling the listener if set.
         *
         * @return true if the invocation was handled, false otherwise
         */
    /* package */ boolean invoke() {
            return mClickListener != null && mClickListener.onMenuItemClick(this);

        }

        private interface MenuItemChangedListener {

            void itemChanged(WearableActionDrawerMenuItem item);
        }
    }
}
