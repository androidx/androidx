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
 * Base class for implementing a {@link Fragment} that will be used together with
 * {@link ActionBarActivity} to support interactions with the activity's {@link ActionBar}.
 */
public class ActionBarFragment extends Fragment implements ActionBarFragmentCallbacks {
    @Override
    public void onCreateSupportOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public void onPrepareSupportOptionsMenu(Menu menu) {
    }

    @Override
    public void onDestroySupportOptionsMenu() {
    }

    @Override
    public boolean onSupportOptionsItemSelected(MenuItem item) {
        return false;
    }
}
