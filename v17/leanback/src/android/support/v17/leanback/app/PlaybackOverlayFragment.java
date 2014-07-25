/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.ObjectAdapter.DataObserver;
import android.support.v17.leanback.widget.VerticalGridView;


/**
 * A fragment for displaying playback controls and related content.
 * The {@link android.support.v17.leanback.widget.PlaybackControlsRow} is expected to be
 * at position 0 in the adapter.
 */
public class PlaybackOverlayFragment extends DetailsFragment {

    private int mAlignPosition;

    /**
     * Sets the list of rows for the fragment.
     */
    @Override
    public void setAdapter(ObjectAdapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterObserver(mObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerObserver(mObserver);
        }
        setVerticalGridViewLayout(getVerticalGridView());
    }

    @Override
    void setVerticalGridViewLayout(VerticalGridView listview) {
        if (listview == null || getAdapter() == null) {
            return;
        }
        final int alignPosition = getAdapter().size() > 1 ? mAlignPosition : 0;
        listview.setItemAlignmentOffset(alignPosition);
        listview.setItemAlignmentOffsetPercent(100);
        listview.setWindowAlignmentOffset(0);
        listview.setWindowAlignmentOffsetPercent(100);
        listview.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlignPosition =
            getResources().getDimensionPixelSize(R.dimen.lb_playback_controls_align_bottom);
    }

    private final DataObserver mObserver = new DataObserver() {
        public void onChanged() {
            setVerticalGridViewLayout(getVerticalGridView());
        }
    };
}
