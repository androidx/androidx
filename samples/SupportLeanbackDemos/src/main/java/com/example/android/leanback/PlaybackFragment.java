/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.PlaybackFragmentGlueHost;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;

/**
 * Example of PlaybackFragment working with a PlaybackControlGlue.
 */
public class PlaybackFragment
        extends androidx.leanback.app.PlaybackFragment
        implements PlaybackActivity.PictureInPictureListener {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    /**
     * Change this to choose a different overlay background.
     */
    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    /**
     * Change the number of related content rows.
     */
    private static final int RELATED_CONTENT_ROWS = 3;

    /**
     * Change this to select hidden
     */
    private static final boolean SECONDARY_HIDDEN = false;

    private static final int ROW_CONTROLS = 0;

    private PlaybackControlGlue mGlue;

    @Override
    public SparseArrayObjectAdapter getAdapter() {
        return (SparseArrayObjectAdapter) super.getAdapter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBackgroundType(BACKGROUND_TYPE);

        createComponents(getActivity());
    }

    private void createComponents(Context context) {
        mGlue = new PlaybackControlGlue(context) {
            @Override
            public int getUpdatePeriod() {
                long totalTime = getControlsRow().getDuration();
                if (getView() == null || getView().getWidth() == 0 || totalTime <= 0) {
                    return 1000;
                }
                return 16;
            }

            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == R.id.lb_control_picture_in_picture) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        getActivity().enterPictureInPictureMode();
                    }
                    return;
                }
                super.onActionClicked(action);
            }

            @Override
            protected void onCreateControlsRowAndPresenter() {
                super.onCreateControlsRowAndPresenter();
                getControlsRowPresenter().setSecondaryActionsHidden(SECONDARY_HIDDEN);
            }
        };

        mGlue.setHost(new PlaybackFragmentGlueHost(this));
        ClassPresenterSelector classPresenterSelector = new ClassPresenterSelector();
        classPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        setAdapter(new SparseArrayObjectAdapter(classPresenterSelector));

        // Add related content rows
        for (int i = 0; i < RELATED_CONTENT_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i);
            getAdapter().set(ROW_CONTROLS + 1 + i, new ListRow(header, listRowAdapter));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PlaybackActivity) getActivity()).registerPictureInPictureListener(this);
    }

    @Override
    public void onStop() {
        ((PlaybackActivity) getActivity()).unregisterPictureInPictureListener(this);
        super.onStop();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            setFadingEnabled(true);
            fadeOut();
        } else {
            setFadingEnabled(mGlue.isPlaying());
        }
    }
}
