/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.coordinatorlayout.testutils;

import com.google.android.material.appbar.AppBarLayout;
/**
 * Allows tests to determine if an AppBarLayout with a CollapsingToolbarLayout is expanded,
 * animating, or collapsed.
 */
public abstract class AppBarStateChangedListener implements AppBarLayout.OnOffsetChangedListener {
    public enum State { UNKNOWN, ANIMATING, EXPANDED, COLLAPSED }

    private State mExistingState = State.UNKNOWN;

    @Override
    public final void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // Collapsed
        if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            setStateAndNotify(appBarLayout, State.COLLAPSED);

        // Expanded
        } else if (verticalOffset == 0) {
            setStateAndNotify(appBarLayout, State.EXPANDED);

        // Animating
        } else {
            setStateAndNotify(appBarLayout, State.ANIMATING);
        }
    }

    private void setStateAndNotify(AppBarLayout appBarLayout, State state) {
        if (mExistingState != state) {
            onStateChanged(appBarLayout, state);
        }
        mExistingState = state;
    }

    public abstract void onStateChanged(AppBarLayout appBarLayout, State state);
}
