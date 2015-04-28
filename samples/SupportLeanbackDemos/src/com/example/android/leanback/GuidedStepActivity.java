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

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;

import java.util.List;
import java.util.ArrayList;

/**
 * Activity that showcases different aspects of GuidedStepFragments.
 */
public class GuidedStepActivity extends Activity {

    private static final int CONTINUE = 1;
    private static final int BACK = 2;

    private static final int OPTION_CHECK_SET_ID = 10;
    private static final int DEFAULT_OPTION = 0;
    private static final String[] OPTION_NAMES = { "Option A", "Option B", "Option C" };
    private static final String[] OPTION_DESCRIPTIONS = { "Here's one thing you can do",
            "Here's another thing you can do", "Here's one more thing you can do" };
    private static final int[] OPTION_DRAWABLES = { R.drawable.ic_guidedstep_option_a,
            R.drawable.ic_guidedstep_option_b, R.drawable.ic_guidedstep_option_c };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GuidedStepFragment.add(getFragmentManager(), new FirstStepFragment());
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addCheckedAction(List<GuidedAction> actions, int iconResId, Context context,
            String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .title(title)
                .description(desc)
                .checkSetId(OPTION_CHECK_SET_ID)
                .iconResourceId(iconResId, context)
                .build());
    }

    /**
     * The first fragment is instantiated via XML, so it must be public.
     */
    public static class FirstStepFragment extends GuidedStepFragment {
        @Override
        public int onProvideTheme() {
            return R.style.Theme_Example_Leanback_GuidedStep_First;
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_first_title);
            String breadcrumb = getString(R.string.guidedstep_first_breadcrumb);
            String description = getString(R.string.guidedstep_first_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, CONTINUE, "Continue", "Let's do it");
            addAction(actions, BACK, "Cancel", "Nevermind");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            FragmentManager fm = getFragmentManager();
            if (action.getId() == CONTINUE) {
                GuidedStepFragment.add(fm, new SecondStepFragment());
            } else {
                getActivity().finish();
            }
        }
    }

    private static class SecondStepFragment extends GuidedStepFragment {

        private int mSelectedOption = DEFAULT_OPTION;

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public GuidanceStylist onCreateGuidanceStylist() {
            return new GuidanceStylist() {
                @Override
                public int onProvideLayoutId() {
                    return R.layout.guidedstep_second_guidance;
                }
            };
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            String desc = "The description can be quite long as well.  ";
            desc += "Just be sure to set multilineDescription to true in the GuidedAction.";
            actions.add(new GuidedAction.Builder()
                    .title("Note that Guided Actions can have titles that are quite long.")
                    .description(desc)
                    .multilineDescription(true)
                    .infoOnly(true)
                    .enabled(false)
                    .build());
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                addCheckedAction(actions, OPTION_DRAWABLES[i], getActivity(), OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i]);
                if (i == DEFAULT_OPTION) {
                    actions.get(actions.size() -1).setChecked(true);
                }
            }
            addAction(actions, CONTINUE, "Continue", "");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == CONTINUE) {
                FragmentManager fm = getFragmentManager();
                GuidedStepFragment.add(fm, new ThirdStepFragment(mSelectedOption));
            } else {
                mSelectedOption = getSelectedActionPosition()-1;
            }
        }

    }

    private static class ThirdStepFragment extends GuidedStepFragment {
        private final int mOption;

        public ThirdStepFragment(int option) {
            mOption = option;
        }

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_third_title);
            String breadcrumb = getString(R.string.guidedstep_third_breadcrumb);
            String description = "You chose: " + OPTION_NAMES[mOption];
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, CONTINUE, "Done", "All finished");
            addAction(actions, BACK, "Back", "Forgot something...");
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == CONTINUE) {
                getActivity().finish();
            } else {
                getFragmentManager().popBackStack();
            }
        }

    }

}
