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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidanceStylist.Guidance;
import androidx.leanback.widget.GuidedAction;

import java.util.List;

/**
 * Activity that showcases different aspects of GuidedStepFragments.
 */
public class DetailsPresenterSelectionActivity extends Activity {

    private static final int OPTION_CHECK_SET_ID = 10;

    private static final long ACTION_ID_SWITCH_LEGACY_ON = 10000;
    private static final long ACTION_ID_SWITCH_LEGACY_OFF = 10001;

    public static boolean USE_LEGACY_PRESENTER = false;

    private static final String[] OPTION_NAMES = { "Use new details presenter", "Use legacy details presenter" };
    private static final String[] OPTION_DESCRIPTIONS = { "Use new details presenter",
            "Use legacy details presenter"};
    private static final long[] OPTION_IDS = {ACTION_ID_SWITCH_LEGACY_OFF, ACTION_ID_SWITCH_LEGACY_ON};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GuidedStepFragment.addAsRoot(this, new SetupFragment(), android.R.id.content);
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder(null)
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addCheckedAction(List<GuidedAction> actions, Context context,
            long id, String title, String desc, boolean checked) {
        actions.add(new GuidedAction.Builder(null)
                .title(title)
                .description(desc)
                .id(id)
                .checkSetId(OPTION_CHECK_SET_ID)
                .checked(checked)
                .build());
    }

    /**
     * Fragment hosted in DetailsPresenterSelectionActivity.
     */
    public static class SetupFragment extends GuidedStepFragment {

        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.guidedstep_second_title);
            String breadcrumb = getString(R.string.guidedstep_second_breadcrumb);
            String description = getString(R.string.guidedstep_second_description);
            Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_main_icon);
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
            for (int i = 0; i < OPTION_NAMES.length; i++) {
                boolean checked = false;
                if (OPTION_IDS[i] == ACTION_ID_SWITCH_LEGACY_ON) {
                    if (USE_LEGACY_PRESENTER) {
                        checked = true;
                    }
                } else if (OPTION_IDS[i] == ACTION_ID_SWITCH_LEGACY_OFF) {
                    if (!USE_LEGACY_PRESENTER) {
                        checked = true;
                    }
                }
                addCheckedAction(actions, getActivity(), OPTION_IDS[i], OPTION_NAMES[i],
                        OPTION_DESCRIPTIONS[i], checked);
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == ACTION_ID_SWITCH_LEGACY_ON) {
                USE_LEGACY_PRESENTER = action.isChecked();
            } else if (action.getId() == ACTION_ID_SWITCH_LEGACY_OFF) {
                USE_LEGACY_PRESENTER = !action.isChecked();
            }
            getActivity().finish();
        }

    }

}
