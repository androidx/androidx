/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.appcompat.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.appcompat.view.menu.MenuWrapperFactory;
import androidx.collection.SimpleArrayMap;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;

import java.util.ArrayList;

/**
 * Wraps a support {@link androidx.appcompat.view.ActionMode} as a framework
 * {@link android.view.ActionMode}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SupportActionModeWrapper extends ActionMode {

    final Context mContext;
    final androidx.appcompat.view.ActionMode mWrappedObject;

    public SupportActionModeWrapper(Context context,
            androidx.appcompat.view.ActionMode supportActionMode) {
        mContext = context;
        mWrappedObject = supportActionMode;
    }

    @Override
    public Object getTag() {
        return mWrappedObject.getTag();
    }

    @Override
    public void setTag(Object tag) {
        mWrappedObject.setTag(tag);
    }

    @Override
    public void setTitle(CharSequence title) {
        mWrappedObject.setTitle(title);
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mWrappedObject.setSubtitle(subtitle);
    }

    @Override
    public void invalidate() {
        mWrappedObject.invalidate();
    }

    @Override
    public void finish() {
        mWrappedObject.finish();
    }

    @Override
    public Menu getMenu() {
        return MenuWrapperFactory.wrapSupportMenu(mContext, (SupportMenu) mWrappedObject.getMenu());
    }

    @Override
    public CharSequence getTitle() {
        return mWrappedObject.getTitle();
    }

    @Override
    public void setTitle(int resId) {
        mWrappedObject.setTitle(resId);
    }

    @Override
    public CharSequence getSubtitle() {
        return mWrappedObject.getSubtitle();
    }

    @Override
    public void setSubtitle(int resId) {
        mWrappedObject.setSubtitle(resId);
    }

    @Override
    public View getCustomView() {
        return mWrappedObject.getCustomView();
    }

    @Override
    public void setCustomView(View view) {
        mWrappedObject.setCustomView(view);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return mWrappedObject.getMenuInflater();
    }

    @Override
    public boolean getTitleOptionalHint() {
        return mWrappedObject.getTitleOptionalHint();
    }

    @Override
    public void setTitleOptionalHint(boolean titleOptional) {
        mWrappedObject.setTitleOptionalHint(titleOptional);
    }

    @Override
    public boolean isTitleOptional() {
        return mWrappedObject.isTitleOptional();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class CallbackWrapper implements androidx.appcompat.view.ActionMode.Callback {
        final Callback mWrappedCallback;
        final Context mContext;

        final ArrayList<SupportActionModeWrapper> mActionModes;
        final SimpleArrayMap<Menu, Menu> mMenus;

        public CallbackWrapper(Context context, Callback supportCallback) {
            mContext = context;
            mWrappedCallback = supportCallback;
            mActionModes = new ArrayList<>();
            mMenus = new SimpleArrayMap<>();
        }

        @Override
        public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            return mWrappedCallback.onCreateActionMode(getActionModeWrapper(mode),
                    getMenuWrapper(menu));
        }

        @Override
        public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            return mWrappedCallback.onPrepareActionMode(getActionModeWrapper(mode),
                    getMenuWrapper(menu));
        }

        @Override
        public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode,
                android.view.MenuItem item) {
            return mWrappedCallback.onActionItemClicked(getActionModeWrapper(mode),
                    MenuWrapperFactory.wrapSupportMenuItem(mContext, (SupportMenuItem) item));
        }

        @Override
        public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
            mWrappedCallback.onDestroyActionMode(getActionModeWrapper(mode));
        }

        private Menu getMenuWrapper(Menu menu) {
            Menu wrapper = mMenus.get(menu);
            if (wrapper == null) {
                wrapper = MenuWrapperFactory.wrapSupportMenu(mContext, (SupportMenu) menu);
                mMenus.put(menu, wrapper);
            }
            return wrapper;
        }

        public ActionMode getActionModeWrapper(androidx.appcompat.view.ActionMode mode) {
            // First see if we already have a wrapper for this mode
            for (int i = 0, count = mActionModes.size(); i < count; i++) {
                SupportActionModeWrapper wrapper = mActionModes.get(i);
                if (wrapper != null && wrapper.mWrappedObject == mode) {
                    // We've found a wrapper, return it
                    return wrapper;
                }
            }

            // If we reach here then we haven't seen this mode before. Create a new wrapper and
            // add it to our collection
            SupportActionModeWrapper wrapper = new SupportActionModeWrapper(mContext, mode);
            mActionModes.add(wrapper);
            return wrapper;
        }
    }
}