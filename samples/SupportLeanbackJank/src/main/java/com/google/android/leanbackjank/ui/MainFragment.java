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
 * limitations under the License
 */

package com.google.android.leanbackjank.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.ShadowOverlayHelper;

import com.google.android.leanbackjank.IntentDefaults;
import com.google.android.leanbackjank.IntentKeys;
import com.google.android.leanbackjank.R;
import com.google.android.leanbackjank.data.VideoProvider;
import com.google.android.leanbackjank.model.VideoInfo;
import com.google.android.leanbackjank.presenter.CardPresenter;
import com.google.android.leanbackjank.presenter.GridItemPresenter;
import com.google.android.leanbackjank.presenter.HeaderItemPresenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseFragment {

    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Define defaults.
        int categoryCount = IntentDefaults.CATEGORY_COUNT;
        int entriesPerCat = IntentDefaults.ENTRIES_PER_CATEGORY;
        boolean disableShadows = IntentDefaults.DISABLE_SHADOWS;
        int cardWidth = IntentDefaults.CARD_WIDTH;
        int cardHeight = IntentDefaults.CARD_HEIGHT;
        int whichVideo = IntentDefaults.WHICH_VIDEO;
        boolean useSingleBitmap = IntentDefaults.USE_SINGLE_BITMAP;

        Intent intent = getActivity().getIntent();
        if (intent.getExtras() != null) {
            categoryCount = intent.getIntExtra(IntentKeys.CATEGORY_COUNT, categoryCount);
            entriesPerCat = intent.getIntExtra(IntentKeys.ENTRIES_PER_CATEGORY, entriesPerCat);
            disableShadows = intent.getBooleanExtra(IntentKeys.DISABLE_SHADOWS, disableShadows);
            cardWidth = intent.getIntExtra(IntentKeys.CARD_WIDTH, cardWidth);
            cardHeight = intent.getIntExtra(IntentKeys.CARD_HEIGHT, cardHeight);
            whichVideo = intent.getIntExtra(IntentKeys.WHICH_VIDEO, whichVideo);
            useSingleBitmap = intent.getBooleanExtra(IntentKeys.USE_SINGLE_BITMAP, useSingleBitmap);
        }

        loadVideoData(categoryCount, entriesPerCat, disableShadows, useSingleBitmap, cardWidth,
                cardHeight);
        setBackground();
        setupUIElements();

        if (whichVideo != IntentKeys.NO_VIDEO) {
            int resource = 0;
            /* For info on how to generate videos see:
             * https://docs.google.com/document/d/1HV8O-Nm4rc2DwVwiZmT4Wa9pf8XttWndg9saGncTRGw
             */
            if (whichVideo == IntentKeys.VIDEO_2160P_60FPS) {
                resource = R.raw.bbb_sunflower_2160p_60fps;
            } else if (whichVideo == IntentKeys.VIDEO_1080P_60FPS) {
                resource = R.raw.testvideo_1080p_60fps;
            } else if (whichVideo == IntentKeys.VIDEO_480P_60FPS) {
                resource = R.raw.bbb_480p;
            } else if (whichVideo == IntentKeys.VIDEO_360P_60FPS) {
                resource = R.raw.bbb_360p;
            }
            Uri uri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/"
                    + resource);
            Intent videoIntent = new Intent(Intent.ACTION_VIEW, uri, getActivity(),
                    VideoActivity.class);
            startActivity(videoIntent);
        }
    }

    private void setBackground() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mBackgroundManager.setDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.default_background, null));
    }

    private void setupUIElements() {
        setBadgeDrawable(ResourcesCompat.getDrawable(
                getActivity().getResources(), R.drawable.app_banner, null));
        // Badge, when set, takes precedent over title
        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        // set headers background color
        setBrandColor(ResourcesCompat.getColor(getResources(), R.color.jank_yellow, null));
        // set search icon color
        setSearchAffordanceColor(
                ResourcesCompat.getColor(getResources(), R.color.search_opaque, null));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new HeaderItemPresenter();
            }
        });
    }

    private void loadVideoData(int categoryCount, int entriesPerCat, boolean disableShadows,
            boolean useSingleBitmap, int cardWidth, int cardHeight) {
        ListRowPresenter listRowPresenter = new ListRowPresenter() {
            @Override
            protected ShadowOverlayHelper.Options createShadowOverlayOptions() {
                Resources res = getResources();
                ShadowOverlayHelper.Options options = new ShadowOverlayHelper.Options();
                options.dynamicShadowZ(res.getDimension(R.dimen.shadow_unfocused_z),
                        res.getDimension(R.dimen.shadow_focused_z));
                return options;
            }
        };
        listRowPresenter.setShadowEnabled(!disableShadows);
        // see b/64451726, leanback bug causes child rounded corner is incorrectly disabled when
        // shadow is disabled. To make the test data consistent and comparable, by default treat
        // the rounded corner option same as shadow option.
        boolean disableRoundedCorner = disableShadows;
        listRowPresenter.enableChildRoundedCorners(!disableRoundedCorner);
        mRowsAdapter = new ArrayObjectAdapter(listRowPresenter);
        HashMap<String, List<VideoInfo>> data = VideoProvider.buildMedia(categoryCount,
                entriesPerCat, cardWidth, cardHeight, getActivity(), useSingleBitmap);
        CardPresenter cardPresenter = new CardPresenter(cardWidth, cardHeight);

        int i = 0;
        for (Map.Entry<String, List<VideoInfo>> entry : data.entrySet()) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            for (VideoInfo videoInfo : entry.getValue()) {
                listRowAdapter.add(videoInfo);
            }
            HeaderItem header = new HeaderItem(i++, entry.getKey());
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        ArrayObjectAdapter settingsListAdapter = new ArrayObjectAdapter(new GridItemPresenter());
        for (int j = 0; j < entriesPerCat; j++) {
            settingsListAdapter.add("Settings " + j);
        }
        HeaderItem settingsHeader = new HeaderItem(i++, "Settings");
        mRowsAdapter.add(new ListRow(settingsHeader, settingsListAdapter));

        setAdapter(mRowsAdapter);
    }
}
