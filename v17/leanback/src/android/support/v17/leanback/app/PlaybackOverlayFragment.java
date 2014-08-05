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

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.ObjectAdapter.DataObserver;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A fragment for displaying playback controls and related content.
 * The {@link android.support.v17.leanback.widget.PlaybackControlsRow} is expected to be
 * at position 0 in the adapter.
 */
public class PlaybackOverlayFragment extends DetailsFragment {

    /**
     * No background.
     */
    public static final int BG_NONE = 0;

    /**
     * A dark translucent background.
     */
    public static final int BG_DARK = 1;

    /**
     * A light translucent background.
     */
    public static final int BG_LIGHT = 2;

    private int mAlignPosition;
    private View mRootView;
    private int mBackgroundType = BG_DARK;
    private int mBgDarkColor;
    private int mBgLightColor;

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
        mBgDarkColor =
                getResources().getColor(R.color.lb_playback_controls_background_dark);
        mBgLightColor =
                getResources().getColor(R.color.lb_playback_controls_background_light);
    }

    /**
     * Sets the background type.
     *
     * @param type One of BG_LIGHT, BG_DARK, or BG_NONE.
     */
    public void setBackgroundType(int type) {
        if (type != BG_LIGHT && type != BG_DARK && type != BG_NONE) {
            throw new IllegalArgumentException("Invalid background type");
        }
        if (type != mBackgroundType) {
            mBackgroundType = type;
            updateBackground();
        }
    }

    /**
     * Returns the background type.
     */
    public int getBackgroundType() {
        return mBackgroundType;
    }

    private void updateBackground() {
        if (mRootView != null) {
            int color = mBgDarkColor;
            switch (mBackgroundType) {
                case BG_DARK: break;
                case BG_LIGHT: color = mBgLightColor; break;
                case BG_NONE: color = Color.TRANSPARENT; break;
            }
            mRootView.setBackground(new ColorDrawable(color));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = super.onCreateView(inflater, container, savedInstanceState);
        updateBackground();
        return mRootView;
    }

    private final DataObserver mObserver = new DataObserver() {
        public void onChanged() {
            setVerticalGridViewLayout(getVerticalGridView());
        }
    };
}
