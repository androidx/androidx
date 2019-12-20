/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget.drawer;

/**
 * Provides the ability to manipulate a {@link WearableDrawerView WearableDrawerView's} position
 * within a {@link WearableDrawerLayout}.
 */
public class WearableDrawerController {

    private final WearableDrawerLayout mDrawerLayout;
    private final WearableDrawerView mDrawerView;

    WearableDrawerController(WearableDrawerLayout drawerLayout, WearableDrawerView drawerView) {
        mDrawerLayout = drawerLayout;
        mDrawerView = drawerView;
    }

    /**
     * Requests that the {@link WearableDrawerView} be opened.
     */
    public void openDrawer() {
        mDrawerLayout.openDrawer(mDrawerView);
    }

    /**
     * Requests that the {@link WearableDrawerView} be closed.
     */
    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mDrawerView);
    }

    /**
     * Requests that the {@link WearableDrawerView} be peeked.
     */
    public void peekDrawer() {
        mDrawerLayout.peekDrawer(mDrawerView);
    }
}
