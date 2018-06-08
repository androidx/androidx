/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.swiperefreshlayout.widget;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import org.hamcrest.Matcher;


public class SwipeRefreshLayoutActions {
    public static ViewAction setRefreshing() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(SwipeRefreshLayout.class);
            }

            @Override
            public String getDescription() {
                return "Set SwipeRefreshLayout refreshing state";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view;
                swipeRefreshLayout.setRefreshing(true);

                // Intentionally not waiting until idle here because it will not become idle due to
                // the animation.
            }
        };
    }

    public static ViewAction setSize(final int size) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(SwipeRefreshLayout.class);
            }

            @Override
            public String getDescription() {
                return "Set SwipeRefreshLayout size";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view;
                swipeRefreshLayout.setSize(size);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setEnabled(final boolean enabled) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(SwipeRefreshLayout.class);
            }

            @Override
            public String getDescription() {
                return "Set SwipeRefreshLayout enabled state";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view;
                swipeRefreshLayout.setEnabled(enabled);

                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
