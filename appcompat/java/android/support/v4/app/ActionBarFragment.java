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

package android.support.v4.app;

import android.support.appcompat.view.Menu;
import android.support.appcompat.view.MenuInflater;
import android.support.appcompat.view.MenuItem;

public class ActionBarFragment extends Fragment {

    @Override
    FragmentManagerImpl createChildFragmentManager() {
        return new SupportMenuFragmentManager();
    }

    boolean performCreateSupportOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onCreateSupportOptionsMenu(menu, inflater);
            }

            if (mChildFragmentManager != null) {
                show |= ((SupportMenuFragmentManager) mChildFragmentManager)
                        .dispatchCreateSupportOptionsMenu(menu, inflater);
            }
        }
        return show;
    }

    boolean performPrepareSupportOptionsMenu(Menu menu) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onPrepareSupportOptionsMenu(menu);
            }

            if (mChildFragmentManager != null) {
                show |= ((SupportMenuFragmentManager) mChildFragmentManager)
                        .dispatchPrepareSupportOptionsMenu(menu);
            }
        }
        return show;
    }

    boolean performSupportOptionsItemSelected(MenuItem item) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                if (onSupportOptionsItemSelected(item)) {
                    return true;
                }
            }

            if (mChildFragmentManager != null) {
                if (((SupportMenuFragmentManager) mChildFragmentManager)
                        .dispatchSupportOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link android.app.Activity#onCreateOptionsMenu(android.view.Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu The options menu in which you place your items.
     *
     * @see #setHasOptionsMenu
     * @see #onPrepareSupportOptionsMenu
     * @see #onSupportOptionsItemSelected
     */
    public void onCreateSupportOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.  See
     * {@link android.app.Activity#onPrepareOptionsMenu(android.view.Menu) Activity.onPrepareOptionsMenu}
     * for more information.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateSupportOptionsMenu().
     *
     * @see #setHasOptionsMenu
     * @see #onCreateSupportOptionsMenu
     */
    public void onPrepareSupportOptionsMenu(Menu menu) {
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onCreateSupportOptionsMenu
     */
    public boolean onSupportOptionsItemSelected(MenuItem item) {
        return false;
    }
}
