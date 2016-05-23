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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.app.media.VideoExampleActivity;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.ActivityOptionsCompat;
import android.widget.Toast;

import java.util.List;

/**
 * The last screen of the Wizard gives to options to either watch the rented movie now or later. Due
 * to keep this example simple and focused on the usage of the GuidedStepFragment, clicking on
 * either action will end the wizard. You might however start a new Activity playing the movie.
 */
public class WizardExample4thStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_WATCH = 1;
    private static final int ACTION_ID_LATER = ACTION_ID_WATCH + 1;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(mMovie.getTitle(),
                getString(R.string.wizard_example_rental_period),
                mMovie.getBreadcrump(), null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_WATCH)
                .editable(false)
                .title(R.string.wizard_example_watch_now)
                .build();
        actions.add(action);
        action = new GuidedAction.Builder(getActivity())
                .id(ACTION_ID_LATER)
                .editable(false)
                .title(R.string.wizard_example_later)
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == ACTION_ID_WATCH) {
            finishGuidedStepFragments();
            Intent intent = new Intent(getActivity().getBaseContext(),
                    VideoExampleActivity.class);
            startActivity(intent);
        } else if (action.getId() == ACTION_ID_LATER) {
            Toast.makeText(getActivity(), getString(R.string.wizard_example_later_clicked),
                    Toast.LENGTH_SHORT).show();
            finishGuidedStepFragments();
        }
    }
}
