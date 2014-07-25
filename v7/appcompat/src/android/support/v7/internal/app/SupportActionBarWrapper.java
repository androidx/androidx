/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.internal.app;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.SpinnerAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * An wrapper class which shims all {@link android.app.ActionBar} to our
 * {@link android.support.v7.app.ActionBar} class.
 *
 * @hide
 */
public class SupportActionBarWrapper extends ActionBar {

    private final android.support.v7.app.ActionBar mActionBar;

    private ArrayList<WeakReference<OnMenuVisibilityListenerWrapper>> mAddedMenuVisWrappers =
            new ArrayList<WeakReference<OnMenuVisibilityListenerWrapper>>();

    public SupportActionBarWrapper(android.support.v7.app.ActionBar actionBar) {
        mActionBar = actionBar;
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
        android.support.v7.app.ActionBar.LayoutParams lp =
                new android.support.v7.app.ActionBar.LayoutParams(layoutParams);
        lp.gravity = layoutParams.gravity;

        mActionBar.setCustomView(view, lp);
    }

    @Override
    public void setCustomView(int resId) {
        mActionBar.setCustomView(resId);
    }

    @Override
    public void setIcon(int resId) {
        mActionBar.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        mActionBar.setIcon(icon);
    }

    @Override
    public void setLogo(int resId) {
        mActionBar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        mActionBar.setLogo(logo);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
        mActionBar.setListNavigationCallbacks(adapter,
                callback != null ? new OnNavigationListenerWrapper(callback) : null);
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
    public void setStackedBackgroundDrawable(Drawable d) {
        mActionBar.setStackedBackgroundDrawable(d);
    }

    @Override
    public void setSplitBackgroundDrawable(Drawable d) {
        mActionBar.setSplitBackgroundDrawable(d);
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
    public ActionBar.Tab newTab() {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, boolean setSelected) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, int position) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void addTab(ActionBar.Tab tab, int position, boolean setSelected) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeTabAt(int position) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void removeAllTabs() {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public void selectTab(ActionBar.Tab tab) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public ActionBar.Tab getSelectedTab() {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public ActionBar.Tab getTabAt(int index) {
        throw new UnsupportedOperationException(
                "Tabs are not supported in toolbar action bars");
    }

    @Override
    public int getTabCount() {
        return mActionBar.getTabCount();
    }

    @Override
    public Context getThemedContext() {
        return mActionBar.getThemedContext();
    }

    @Override
    public void setHomeAsUpIndicator(Drawable indicator) {
        mActionBar.setHomeAsUpIndicator(indicator);
    }

    @Override
    public void setHomeAsUpIndicator(int resId) {
        mActionBar.setHomeAsUpIndicator(resId);
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
        if (listener != null) {
            OnMenuVisibilityListenerWrapper w = new OnMenuVisibilityListenerWrapper(listener);
            mAddedMenuVisWrappers.add(new WeakReference<OnMenuVisibilityListenerWrapper>(w));
            mActionBar.addOnMenuVisibilityListener(w);
        }
    }

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        OnMenuVisibilityListenerWrapper l = findAndRemoveMenuVisWrapper(listener);
        mActionBar.removeOnMenuVisibilityListener(l);
    }

    @Override
    public void setHomeButtonEnabled(boolean enabled) {
        mActionBar.setHomeButtonEnabled(enabled);
    }

    static class OnNavigationListenerWrapper implements
            android.support.v7.app.ActionBar.OnNavigationListener {
        private final OnNavigationListener mWrappedListener;

        public OnNavigationListenerWrapper(OnNavigationListener l) {
            mWrappedListener = l;
        }

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            return mWrappedListener.onNavigationItemSelected(itemPosition, itemId);
        }
    }

    static class OnMenuVisibilityListenerWrapper
            implements android.support.v7.app.ActionBar.OnMenuVisibilityListener {
        private final OnMenuVisibilityListener mWrappedListener;

        public OnMenuVisibilityListenerWrapper(OnMenuVisibilityListener l) {
            mWrappedListener = l;
        }

        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mWrappedListener.onMenuVisibilityChanged(isVisible);
        }

    }
}