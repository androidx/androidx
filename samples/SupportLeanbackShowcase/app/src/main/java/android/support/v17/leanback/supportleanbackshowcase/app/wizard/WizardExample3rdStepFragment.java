/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.app.wizard;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;

import java.util.List;

/**
 * This is the third screen of the rental wizard which will display a progressbar while waiting for
 * the server to process the rental. The server communication is faked for the sake of this example
 * by waiting four seconds until continuing.
 */
public class WizardExample3rdStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_PROCESSING = 1;
    private final Handler mFakeHttpHandler = new Handler();

    @Override
    public void onStart() {
        super.onStart();

        // Fake Http call by creating some sort of delay.
        mFakeHttpHandler.postDelayed(fakeHttpRequestRunnable, 4000L);
    }

    @Override
    public GuidedActionsStylist onCreateActionsStylist() {
        GuidedActionsStylist stylist = new GuidedActionsStylist() {
            @Override
            public int onProvideItemLayoutId() {
                return R.layout.wizard_progress_action_item;
            }

        };
        return stylist;
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Example_LeanbackWizard_NoSelector;
    }

    @Override
    public void onStop() {
        super.onStop();

        // Make sure to cancel the execution of the Runnable in case the fragment is stopped.
        mFakeHttpHandler.removeCallbacks(fakeHttpRequestRunnable);
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(mMovie.getTitle(),
                getString(R.string.wizard_example_just_a_second),
                mMovie.getBreadcrump(), null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_PROCESSING)
                .title(R.string.wizard_example_processing)
                .infoOnly(true)
                .build();
        actions.add(action);
    }

    private final Runnable fakeHttpRequestRunnable = new Runnable() {
        @Override
        public void run() {
            GuidedStepFragment fragment = new WizardExample4thStepFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    };

}
