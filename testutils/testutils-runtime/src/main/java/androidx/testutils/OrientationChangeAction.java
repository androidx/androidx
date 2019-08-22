/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.hamcrest.Matcher;

import java.util.Collection;

/**
 * An Espresso ViewAction that changes the orientation of the screen. Use like this:
 * <code>onView(isRoot()).perform(orientationPortrait());</code> or this: <code>onView(isRoot())
 * .perform(orientationLandscape());</code>
 */
public class OrientationChangeAction implements ViewAction {
    private final int mOrientation;

    private OrientationChangeAction(int orientation) {
        this.mOrientation = orientation;
    }

    /**
     * Rotate the screen to the Landscape orientation.
     *
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static ViewAction orientationLandscape() {
        return new OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * Rotate the screen to the Portrait orientation.
     *
     * @return The ViewAction to use in {@link
     * androidx.test.espresso.ViewInteraction#perform(ViewAction...)}
     */
    public static ViewAction orientationPortrait() {
        return new OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public Matcher<View> getConstraints() {
        return isRoot();
    }

    @Override
    public String getDescription() {
        return "change orientation to " + mOrientation;
    }


    @Override
    public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();
        Activity activity = getActivity(view.getContext());
        if (activity == null && view instanceof ViewGroup) {
            ViewGroup v = (ViewGroup) view;
            int c = v.getChildCount();
            for (int i = 0; i < c && activity == null; ++i) {
                activity = getActivity(v.getChildAt(i).getContext());
            }
        }
        activity.setRequestedOrientation(mOrientation);

        Collection<Activity> resumedActivities =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                        Stage.RESUMED);
        if (resumedActivities.isEmpty()) {
            throw new RuntimeException("Could not change orientation");
        }
    }

    private Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
