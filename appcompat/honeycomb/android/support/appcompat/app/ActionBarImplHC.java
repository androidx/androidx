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

package android.support.appcompat.app;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.SpinnerAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class ActionBarImplHC extends ActionBar {

    Activity mActivity;
    Callback mCallback;
    android.app.ActionBar mActionBar;

    private ArrayList<WeakReference<OnMenuVisibilityListenerWrapper>> mAddedMenuVisWrappers =
            new ArrayList<WeakReference<OnMenuVisibilityListenerWrapper>>();

    public ActionBarImplHC(Activity activity, Callback callback) {
        mActivity = activity;
        mCallback = callback;
        mActionBar = activity.getActionBar();
    }

    private OnMenuVisibilityListenerWrapper findAndRemoveMenuVisWrapper(
            OnMenuVisibilityListener compatListener) {
        for (int i = 0; i < mAddedMenuVisWrappers.size(); i++) {
            OnMenuVisibilityListenerWrapper wrapper = mAddedMenuVisWrappers.get(i).get();
            if (wrapper == null) {
                mAddedMenuVisWrappers.remove(i--);
            } else if (wrapper.mWrappedListener == compatListener) {
                mAddedMenuVisWrappers.remove(i);
                return wrapper;
            }
        }
        return null;
    }

    @Override
    public void setCustomView(View view) {
        mActionBar.setCustomView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        android.app.ActionBar.LayoutParams lp =
                new android.app.ActionBar.LayoutParams(layoutParams);
        lp.gravity = layoutParams.gravity;

        mActionBar.setCustomView(view, lp);
    }

    @Override
    public void setCustomView(int resId) {
        // TODO: do layoutparams translation
        mActionBar.setCustomView(resId);
    }

    @Override
    public void setIcon(int resId) {
        // TODO compatibility impl?
        //mActionBar.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        // TODO compatibility impl?
        //mActionBar.setIcon(icon);
    }

    @Override
    public void setLogo(int resId) {
        // TODO compatibility impl?
        //mActionBar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        // TODO compatibility impl?
        //mActionBar.setLogo(logo);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
        mActionBar.setListNavigationCallbacks(adapter, new OnNavigationListenerWrapper(callback));
    }

    @Override
    public void setSelectedNavigationItem(int position) {
        mActionBar.setSelectedNavigationItem(position);
    }

    @Override
    public int getSelectedNavigationIndex() {
        return mActionBar.getSelectedNavigationIndex();
    }

    @Override
    public int getNavigationItemCount() {
        return mActionBar.getNavigationItemCount();
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionBar.setTitle(title);
    }

    @Override
    public void setTitle(int resId) {
        mActionBar.setTitle(resId);
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mActionBar.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        mActionBar.setSubtitle(resId);
    }

    @Override
    public void setDisplayOptions(int options) {
        mActionBar.setDisplayOptions(options);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        mActionBar.setDisplayOptions(options, mask);
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo) {
        mActionBar.setDisplayUseLogoEnabled(useLogo);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        mActionBar.setDisplayShowHomeEnabled(showHome);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        mActionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        mActionBar.setDisplayShowTitleEnabled(showTitle);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        mActionBar.setDisplayShowCustomEnabled(showCustom);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        mActionBar.setBackgroundDrawable(d);
    }

    @Override
    public View getCustomView() {
        return mActionBar.getCustomView();
    }

    @Override
    public CharSequence getTitle() {
        return mActionBar.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return mActionBar.getSubtitle();
    }

    @Override
    public int getNavigationMode() {
        return mActionBar.getNavigationMode();
    }

    @Override
    public void setNavigationMode(int mode) {
        mActionBar.setNavigationMode(mode);
    }

    @Override
    public int getDisplayOptions() {
        return mActionBar.getDisplayOptions();
    }

    @Override
    public Tab newTab() {
        final android.app.ActionBar.Tab realTab = mActionBar.newTab();
        final TabWrapper result = new TabWrapper(realTab);
        realTab.setTag(result);
        return result;
    }

    @Override
    public void addTab(Tab tab) {
        mActionBar.addTab(((TabWrapper) tab).mWrappedTab);
    }

    @Override
    public void addTab(Tab tab, boolean setSelected) {
        mActionBar.addTab(((TabWrapper) tab).mWrappedTab);
    }

    @Override
    public void addTab(Tab tab, int position) {
        mActionBar.addTab(((TabWrapper) tab).mWrappedTab, position);
    }

    @Override
    public void addTab(Tab tab, int position, boolean setSelected) {
        mActionBar.addTab(((TabWrapper) tab).mWrappedTab, position, setSelected);
    }

    @Override
    public void removeTab(Tab tab) {
        mActionBar.removeTab(((TabWrapper) tab).mWrappedTab);
    }

    @Override
    public void removeTabAt(int position) {
        mActionBar.removeTabAt(position);
    }

    @Override
    public void removeAllTabs() {
        mActionBar.removeAllTabs();
    }

    @Override
    public void selectTab(Tab tab) {
        mActionBar.selectTab(((TabWrapper) tab).mWrappedTab);
    }

    @Override
    public Tab getSelectedTab() {
        return (Tab) mActionBar.getSelectedTab().getTag();
    }

    @Override
    public Tab getTabAt(int index) {
        return (Tab) mActionBar.getTabAt(index).getTag();
    }

    @Override
    public int getTabCount() {
        return mActionBar.getTabCount();
    }

    @Override
    public int getHeight() {
        return mActionBar.getHeight();
    }

    @Override
    public void show() {
        mActionBar.show();
    }

    @Override
    public void hide() {
        mActionBar.hide();
    }

    @Override
    public boolean isShowing() {
        return mActionBar.isShowing();
    }

    @Override
    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        OnMenuVisibilityListenerWrapper w = new OnMenuVisibilityListenerWrapper(listener);
        mAddedMenuVisWrappers.add(new WeakReference<OnMenuVisibilityListenerWrapper>(w));
        mActionBar.addOnMenuVisibilityListener(w);
    }

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        OnMenuVisibilityListenerWrapper l = findAndRemoveMenuVisWrapper(listener);
        mActionBar.removeOnMenuVisibilityListener(l);
    }

    static class OnNavigationListenerWrapper implements android.app.ActionBar.OnNavigationListener {

        private final OnNavigationListener mWrappedListener;

        public OnNavigationListenerWrapper(OnNavigationListener l) {
            mWrappedListener = l;
        }

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            return mWrappedListener.onNavigationItemSelected(itemPosition, itemId);
        }

    }

    static class OnMenuVisibilityListenerWrapper implements
            android.app.ActionBar.OnMenuVisibilityListener {

        final OnMenuVisibilityListener mWrappedListener;

        public OnMenuVisibilityListenerWrapper(OnMenuVisibilityListener l) {
            mWrappedListener = l;
        }

        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mWrappedListener.onMenuVisibilityChanged(isVisible);
        }

    }

    class TabWrapper extends ActionBar.Tab {

        final android.app.ActionBar.Tab mWrappedTab;
        private Object mTag;
        private FragmentTransaction mActiveTransaction;
        private CharSequence mContentDescription;

        public TabWrapper(android.app.ActionBar.Tab tab) {
            mWrappedTab = tab;
        }

        @Override
        public int getPosition() {
            return mWrappedTab.getPosition();
        }

        @Override
        public Drawable getIcon() {
            return mWrappedTab.getIcon();
        }

        @Override
        public CharSequence getText() {
            return mWrappedTab.getText();
        }

        @Override
        public Tab setIcon(Drawable icon) {
            mWrappedTab.setIcon(icon);
            return this;
        }

        @Override
        public Tab setIcon(int resId) {
            mWrappedTab.setIcon(resId);
            return this;
        }

        @Override
        public Tab setText(CharSequence text) {
            mWrappedTab.setText(text);
            return this;
        }

        @Override
        public Tab setText(int resId) {
            mWrappedTab.setText(resId);
            return this;
        }

        @Override
        public Tab setCustomView(View view) {
            mWrappedTab.setCustomView(view);
            return this;
        }

        @Override
        public Tab setCustomView(int layoutResId) {
            mWrappedTab.setCustomView(layoutResId);
            return this;
        }

        @Override
        public View getCustomView() {
            return mWrappedTab.getCustomView();
        }

        @Override
        public Tab setTag(Object obj) {
            mTag = obj;
            return this;
        }

        @Override
        public Object getTag() {
            return mTag;
        }

        @Override
        public Tab setTabListener(TabListener listener) {
            mWrappedTab.setTabListener(new TabListenerWrapper(listener));
            return this;
        }

        @Override
        public void select() {
            mActiveTransaction = mCallback.getSupportFragmentManager().beginTransaction();
            mWrappedTab.select();
            mActiveTransaction.commit();
            mActiveTransaction = null;
        }

        @Override
        public Tab setContentDescription(int resId) {
            mContentDescription = mActivity.getText(resId);
            return this;
        }

        @Override
        public Tab setContentDescription(CharSequence contentDesc) {
            mContentDescription = contentDesc;
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return mContentDescription;
        }

    }

    class TabListenerWrapper implements android.app.ActionBar.TabListener {

        private final TabListener mWrappedListener;

        public TabListenerWrapper(TabListener l) {
            mWrappedListener = l;
        }

        @Override
        public void onTabSelected(android.app.ActionBar.Tab tab,
                android.app.FragmentTransaction ft) {
            final TabWrapper wrappedTab = (TabWrapper) tab.getTag();
            mWrappedListener.onTabSelected(wrappedTab, wrappedTab.mActiveTransaction);
        }

        @Override
        public void onTabUnselected(android.app.ActionBar.Tab tab,
                android.app.FragmentTransaction ft) {
            final TabWrapper wrappedTab = (TabWrapper) tab.getTag();
            mWrappedListener.onTabUnselected(wrappedTab, wrappedTab.mActiveTransaction);
        }

        @Override
        public void onTabReselected(android.app.ActionBar.Tab tab,
                android.app.FragmentTransaction ft) {
            final TabWrapper wrappedTab = (TabWrapper) tab.getTag();
            mWrappedListener.onTabReselected(wrappedTab, wrappedTab.mActiveTransaction);
        }

    }
}
