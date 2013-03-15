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

import java.util.ArrayList;

class SupportMenuFragmentManager extends FragmentManagerImpl {

    boolean dispatchCreateSupportOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f instanceof ActionBarFragment) {
                    if (((ActionBarFragment) f).performCreateSupportOptionsMenu(menu, inflater)) {
                        show = true;
                        if (newMenus == null) {
                            newMenus = new ArrayList<Fragment>();
                        }
                    }
                    newMenus.add(f);
                }
            }
        }

        if (mCreatedMenus != null) {
            for (int i = 0; i < mCreatedMenus.size(); i++) {
                Fragment f = mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(f)) {
                    f.onDestroyOptionsMenu();
                }
            }
        }

        mCreatedMenus = newMenus;

        return show;
    }

    boolean dispatchPrepareSupportOptionsMenu(Menu menu) {
        boolean show = false;
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f instanceof ActionBarFragment) {
                    if (((ActionBarFragment) f).performPrepareSupportOptionsMenu(menu)) {
                        show = true;
                    }
                }
            }
        }
        return show;
    }

    boolean dispatchSupportOptionsItemSelected(MenuItem item) {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                Fragment f = mAdded.get(i);
                if (f instanceof ActionBarFragment) {
                    if (((ActionBarFragment) f).performSupportOptionsItemSelected(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
