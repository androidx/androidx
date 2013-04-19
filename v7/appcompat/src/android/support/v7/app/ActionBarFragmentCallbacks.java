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

package android.support.v7.app;

import android.support.v4.app.Fragment;
import android.support.v7.view.Menu;
import android.support.v7.view.MenuInflater;
import android.support.v7.view.MenuItem;

/**
 * This interface may be implemented by any {@link Fragment} used together with
 * {@link ActionBarActivity} to support interactions with the activity's {@link ActionBar}.
 * <p>
 * Instead of implementing this interface directly, you may also subclass
 * {@link ActionBarFragment} instead which will implement it for you.
 * </p>
 */
public interface ActionBarFragmentCallbacks {
    /**
     * Support library version of {@link Fragment#onCreateOptionsMenu}.
     * <p>
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link Fragment#setHasOptionsMenu}.
     * See {@link ActionBarActivity#onCreateSupportOptionsMenu} for more information.
     * </p>
     *
     * @param menu The options menu in which you place your items.
     * @param inflater The menu inflater to use when inflating the menu.
     *
     * @see Fragment#setHasOptionsMenu
     * @see #onPrepareSupportOptionsMenu
     * @see #onSupportOptionsItemSelected
     */
    public void onCreateSupportOptionsMenu(Menu menu, MenuInflater inflater);

    /**
     * Support library version of {@link Fragment#onPrepareOptionsMenu}.
     * <p>
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     * </p>
     *
     * @param menu The options menu as last shown or first initialized by
     * {@link #onCreateSupportOptionsMenu}.
     *
     * @see Fragment#setHasOptionsMenu
     * @see #onCreateSupportOptionsMenu
     */
    public void onPrepareSupportOptionsMenu(Menu menu);

    /**
     * Support library version of {@link Fragment#onCreateOptionsMenu}.
     * <p>
     * Called when this fragment's option menu items are no longer being
     * included in the overall options menu.  Receiving this call means that
     * the menu needed to be rebuilt, but this fragment's items were not
     * included in the newly built menu (its {@link #onCreateSupportOptionsMenu}
     * was not called).
     * </p>
     */
    public void onDestroySupportOptionsMenu();

    /**
     * Support library version of {@link Fragment#onOptionsItemSelected}.
     * <p>
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * </p><p>
     * Derived classes should call through to the base class for it to
     * perform the default menu handling.
     * </p>
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onPrepareSupportOptionsMenu
     * @see #onCreateSupportOptionsMenu
     */
    public boolean onSupportOptionsItemSelected(MenuItem item);
}
