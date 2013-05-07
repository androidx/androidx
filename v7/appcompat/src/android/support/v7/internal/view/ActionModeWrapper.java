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

package android.support.v7.internal.view;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuWrapperFactory;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

/**
 * @hide
 */
public class ActionModeWrapper extends ActionMode {

    final MenuInflater mInflater;
    final android.view.ActionMode mWrappedObject;

    public ActionModeWrapper(Context context, android.view.ActionMode frameworkActionMode) {
        mWrappedObject = frameworkActionMode;
        mInflater = new SupportMenuInflater(context);
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
        return MenuWrapperFactory.createMenuWrapper(mWrappedObject.getMenu());
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
        return mInflater;
    }

    /**
     * @hide
     */
    public static class CallbackWrapper implements android.view.ActionMode.Callback {
        final Callback mWrappedCallback;
        final Context mContext;

        private ActionModeWrapper mLastStartedActionMode;

        public CallbackWrapper(Context context, Callback supportCallback) {
            mContext = context;
            mWrappedCallback = supportCallback;
        }

        @Override
        public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
            return mWrappedCallback.onCreateActionMode(getActionModeWrapper(mode),
                    MenuWrapperFactory.createMenuWrapper(menu));
        }

        @Override
        public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
            return mWrappedCallback.onPrepareActionMode(getActionModeWrapper(mode),
                    MenuWrapperFactory.createMenuWrapper(menu));
        }

        @Override
        public boolean onActionItemClicked(android.view.ActionMode mode,
                android.view.MenuItem item) {
            return mWrappedCallback.onActionItemClicked(getActionModeWrapper(mode),
                    MenuWrapperFactory.createMenuItemWrapper(item));
        }

        @Override
        public void onDestroyActionMode(android.view.ActionMode mode) {
            mWrappedCallback.onDestroyActionMode(getActionModeWrapper(mode));
        }

        public void setLastStartedActionMode(ActionModeWrapper modeWrapper) {
            mLastStartedActionMode = modeWrapper;
        }

        private ActionMode getActionModeWrapper(android.view.ActionMode mode) {
            if (mLastStartedActionMode != null && mLastStartedActionMode.mWrappedObject == mode) {
                // If the given mode equals our wrapped mode, just return it
                return mLastStartedActionMode;
            } else {
                return new ActionModeWrapper(mContext, mode);
            }
        }
    }
}
