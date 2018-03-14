// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from GuidedStepHalfScreenActivity.java.  DO NOT MODIFY. */

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

package com.example.android.leanback;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;

import java.util.List;

/**
 * Activity that showcases different aspects of GuidedStepSupportFragments in half
 * screen mode. This is achieved by setting the theme for this activity
 * to {@code Theme.Example.Leanback.GuidedStep.Half}.
 */
public class GuidedStepSupportHalfScreenActivity extends FragmentActivity {
    private static final String TAG = "leanback.GuidedStepSupportHalfScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guided_step_activity);
        GuidedStepSupportFragment.addAsRoot(this, new FirstStepFragment(), R.id.lb_guidedstep_host);
    }

    public static class FirstStepFragment extends GuidedStepSupportFragment {

       @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_first_title);
            String breadcrumb = getString(R.string.guidedstep_first_breadcrumb);
            String description = getString(R.string.guidedstep_first_description);
            final Context context = getActivity();
            Drawable icon = ResourcesCompat.getDrawable(context.getResources(),
                    R.drawable.ic_main_icon, context.getTheme());
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CONTINUE)
                    .description("Just do it")
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .description("Never mind")
                    .build());
        }

        public FirstStepFragment() {
            setEntranceTransitionType(GuidedStepSupportFragment.SLIDE_FROM_BOTTOM);
        }

        /**
         * This fragment could be used by an activity using theme
         * {@code Theme.Leanback.GuidedStep.Half} or something else (BrowseActivity).
         * In order to provide a consistent half screen experience under
         * both scenarios, we override onProvideTheme method.
         */
        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_Half;
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
                GuidedStepSupportFragment.add(fm, new SecondStepFragment(), R.id.lb_guidedstep_host);
            } else if (action.getId() == GuidedAction.ACTION_ID_CANCEL){
                finishGuidedStepSupportFragments();
            }
        }
    }

    public static class SecondStepFragment extends GuidedStepSupportFragment {

        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_Half;
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description);
            final Context context = getActivity();
            Drawable icon = ResourcesCompat.getDrawable(context.getResources(),
                    R.drawable.ic_main_icon, context.getTheme());
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            Context context = getActivity();
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_FINISH)
                    .description("Done")
                    .build());
            actions.add(new GuidedAction.Builder(context)
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .description("Never mind")
                    .build());
        }

        @Override
        public void onCreateButtonActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getActivity())
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .description("Cancel")
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            fm.popBackStack();
        }
    }
}
