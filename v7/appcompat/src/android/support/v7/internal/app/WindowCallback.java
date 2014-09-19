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

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Interface which allows us to intercept and control calls to {@link android.view.Window.Callback}.
 * Used by ActionBarActivityDelegates.
 *
 * @hide
 */
public interface WindowCallback {

    boolean onMenuItemSelected(int featureId, MenuItem menuItem);

    boolean onCreatePanelMenu(int featureId, Menu menu);

    boolean onPreparePanel(int featureId, View menuView, Menu menu);

    void onPanelClosed(int featureId, Menu menu);

    boolean onMenuOpened(int featureId, Menu menu);

    ActionMode startActionMode(ActionMode.Callback callback);

    View onCreatePanelView(int featureId);

}
