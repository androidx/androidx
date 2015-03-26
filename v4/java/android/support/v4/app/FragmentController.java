/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v4.util.SimpleArrayMap;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides integration points with a {@link FragmentManager}. For example, a fragment
 * host, such as {@link FragmentActivity}, uses the {@link FragmentController} to control
 * the {@link Fragment} lifecycle.
 */
public class FragmentController {
    private final FragmentHostCallbacks mHost;

    /**
     * Returns a {@link FragmentController}.
     */
    public static final FragmentController createController(FragmentHostCallbacks callbacks) {
        return new FragmentController(callbacks);
    }

    private FragmentController(FragmentHostCallbacks callbacks) {
        mHost = callbacks;
    }

    public FragmentManager getSupportFragmentManager() {
        return mHost.getFragmentManagerImpl();
    }

    public LoaderManager getSupportLoaderManager() {
        return mHost.getLoaderManagerImpl();
    }

    /** Returns the number of active fragments. */
    public int getActiveFragmentsCount() {
        final List<Fragment> actives = mHost.mFragmentManager.mActive;
        return actives == null ? 0 : actives.size();
    }

    /** Returns the list of active fragments. */
    public List<Fragment> getActiveFragments(List<Fragment> actives) {
        if (mHost.mFragmentManager.mActive == null) {
            return null;
        }
        if (actives == null) {
            actives = new ArrayList<Fragment>(getActiveFragmentsCount());
        }
        actives.addAll(mHost.mFragmentManager.mActive);
        return actives;
    }

    /** Attaches the host to the FragmentManager. */
    public void attachHost(Fragment parent) {
        mHost.mFragmentManager.attachController(
                mHost, mHost /*container*/, parent);
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return mHost.mFragmentManager.onCreateView(parent, name, context, attrs);
    }

    /**
     * Marks the fragment state as unsaved. This allows for "state loss" detection.
     */
    public void noteStateNotSaved() {
        mHost.mFragmentManager.noteStateNotSaved();
    }

    /**
     * Saves the state for all Fragments.
     */
    public Parcelable saveAllState() {
        return mHost.mFragmentManager.saveAllState();
    }

    /**
     * Restores the saved state for all Fragments. The given Fragment list are Fragment
     * instances retained across configuration changes.
     *
     * @see #retainNonConfig()
     */
    public void restoreAllState(Parcelable state, ArrayList<Fragment> nonConfigList) {
        mHost.mFragmentManager.restoreAllState(state, nonConfigList);
    }

    /**
     * Returns a list of Fragments that have opted to retain their instance across
     * configuration changes.
     */
    public ArrayList<Fragment> retainNonConfig() {
        return mHost.mFragmentManager.retainNonConfig();
    }

    public void dispatchCreate() {
        mHost.mFragmentManager.dispatchCreate();
    }

    public void dispatchActivityCreated() {
        mHost.mFragmentManager.dispatchActivityCreated();
    }

    public void dispatchStart() {
        mHost.mFragmentManager.dispatchStart();
    }

    public void dispatchResume() {
        mHost.mFragmentManager.dispatchResume();
    }

    public void dispatchPause() {
        mHost.mFragmentManager.dispatchPause();
    }

    public void dispatchStop() {
        mHost.mFragmentManager.dispatchStop();
    }

    public void dispatchReallyStop() {
        mHost.mFragmentManager.dispatchReallyStop();
    }

    public void dispatchDestroyView() {
        mHost.mFragmentManager.dispatchDestroyView();
    }

    public void dispatchDestroy() {
        mHost.mFragmentManager.dispatchDestroy();
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        mHost.mFragmentManager.dispatchConfigurationChanged(newConfig);
    }

    public void dispatchLowMemory() {
        mHost.mFragmentManager.dispatchLowMemory();
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        return mHost.mFragmentManager.dispatchCreateOptionsMenu(menu, inflater);
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        return mHost.mFragmentManager.dispatchPrepareOptionsMenu(menu);
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        return mHost.mFragmentManager.dispatchOptionsItemSelected(item);
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        return mHost.mFragmentManager.dispatchContextItemSelected(item);
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        mHost.mFragmentManager.dispatchOptionsMenuClosed(menu);
    }

    public boolean execPendingActions() {
        return mHost.mFragmentManager.execPendingActions();
    }

    void doLoaderStart() {
        mHost.doLoaderStart();
    }

    /**
     * Stops the loaders, optionally retaining their state. This is useful for keeping the
     * loader state across configuration changes.
     *
     * @param retain When {@code true}, the loaders aren't stopped, but, their instances
     * are retained in a started state
     */
    void doLoaderStop(boolean retain) {
        mHost.doLoaderStop(retain);
    }

    void doLoaderRetain() {
        mHost.doLoaderRetain();
    }

    void doLoaderDestroy() {
        mHost.doLoaderDestroy();
    }

    void reportLoaderStart() {
        mHost.reportLoaderStart();
    }

    /**
     * Returns a list of LoaderManagers that have opted to retain their instance across
     * configuration changes.
     */
    public SimpleArrayMap<String, LoaderManager> retainLoaderNonConfig() {
        return mHost.retainLoaderNonConfig();
    }

    /**
     * Restores the saved state for all LoaderManagers. The given LoaderManager list are
     * LoaderManager instances retained across configuration changes.
     *
     * @see #retainLoaderNonConfig()
     */
    public void restoreLoaderNonConfig(SimpleArrayMap<String, LoaderManager> loaderManagers) {
        mHost.restoreLoaderNonConfig(loaderManagers);
    }

    public void dumpLoaders(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        mHost.dumpLoaders(prefix, fd, writer, args);
    }
}
