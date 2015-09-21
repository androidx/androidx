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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;

import java.util.List;
import java.util.ArrayList;

/**
 * Activity that allows navigation among the demo activities.
 */
public class MainActivity extends Activity {

    private GuidedStepFragment mGuidedStepFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGuidedStepFragment = new StepFragment();
        GuidedStepFragment.add(getFragmentManager(), mGuidedStepFragment);
    }

    public static class StepFragment extends GuidedStepFragment {
        @Override
        public Guidance onCreateGuidance(Bundle savedInstanceState) {
            String title = getString(R.string.main_title);
            String breadcrumb = getString(R.string.main_breadcrumb);
            String description = "";
            Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
            return new Guidance(title, description, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
            addAction(actions, BrowseActivity.class, R.string.browse, R.string.browse_description);
            addAction(actions, SearchActivity.class, R.string.search, R.string.search_description);
            addAction(actions, DetailsActivity.class, R.string.details, R.string.details_description);
            actions.get(actions.size()-1).getIntent().putExtra(DetailsActivity.EXTRA_ITEM,
                    new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            addAction(actions, SearchDetailsActivity.class, R.string.search_details,
                    R.string.search_details_description);
            actions.get(actions.size()-1).getIntent().putExtra(DetailsActivity.EXTRA_ITEM,
                    new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            addAction(actions, PlaybackOverlayActivity.class, R.string.playback,
                    R.string.playback_description);
            addAction(actions, HorizontalGridTestActivity.class, R.string.hgrid,
                    R.string.hgrid_description);
            addAction(actions, VerticalGridActivity.class, R.string.vgrid,
                    R.string.vgrid_description);
            addAction(actions, GuidedStepActivity.class, R.string.guidedstep,
                    R.string.guidedstep_description);
            addAction(actions, BrowseErrorActivity.class, R.string.browseerror,
                    R.string.browseerror_description);
            addAction(actions, DetailsPresenterSelectionActivity.class,
                    R.string.detail_presenter_options,
                    R.string.detail_presenter_options_description);
        }

        private void addAction(List<GuidedAction> actions, Class cls, int titleRes, int descRes) {
            actions.add(new GuidedAction.Builder()
                    .intent(new Intent(getActivity(), cls))
                    .title(getString(titleRes))
                    .description(getString(descRes))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            Intent intent = action.getIntent();
            if (intent != null) {
                startActivity(intent);
            }
        }

    }
}
