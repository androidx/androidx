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
package com.example.android.leanback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.DetailsFragmentBackgroundController;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.MediaPlayerGlue;
import androidx.leanback.media.PlaybackGlue;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;

public class NewDetailsFragment extends androidx.leanback.app.DetailsFragment {

    private static final String TAG = "leanback.DetailsFragment";
    private static final String ITEM = "item";

    private static final int NUM_ROWS = 3;
    private ArrayObjectAdapter mRowsAdapter;
    private PhotoItem mPhotoItem;
    final CardPresenter cardPresenter = new CardPresenter();

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private boolean TEST_SHARED_ELEMENT_TRANSITION = true;
    private boolean TEST_BACKGROUND_PLAYER;

    private static final long TIME_TO_LOAD_OVERVIEW_ROW_MS = 1000;
    private static final long TIME_TO_LOAD_RELATED_ROWS_MS = 2000;

    private Action mActionPlay;
    private Action mActionRent;
    private Action mActionBuy;

    private FullWidthDetailsOverviewSharedElementHelper mHelper;
    private BackgroundHelper mBackgroundHelper; // used to download bitmap async.
    private final DetailsFragmentBackgroundController mDetailsBackground =
            new DetailsFragmentBackgroundController(this);

    void setupTrailerVideo() {
        MediaPlayerGlue mediaPlayerGlue = new MediaPlayerGlue(getActivity());
        mDetailsBackground.setupVideoPlayback(mediaPlayerGlue);
        mediaPlayerGlue.setMode(MediaPlayerGlue.REPEAT_ONE);
        mediaPlayerGlue.setArtist("A Googler");
        mediaPlayerGlue.setTitle("Diving with Sharks Trailer");
        mediaPlayerGlue.setMediaSource(Uri.parse("android.resource://com.example.android.leanback/"
                + "raw/browse"));
    }

    void setupMainVideo() {
        Context context = getActivity();
        MediaPlayerAdapter adapter = new MediaPlayerAdapter(context);
        PlaybackTransportControlGlue<MediaPlayerAdapter> mediaPlayerGlue =
                new PlaybackTransportControlGlue(context, adapter);
        mDetailsBackground.setupVideoPlayback(mediaPlayerGlue);
        mediaPlayerGlue.setSubtitle("A Googler");
        mediaPlayerGlue.setTitle("Diving with Sharks");
        mediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(
                "https://storage.googleapis.com/android-tv/Sample videos/April Fool's "
                        + "2013/Explore Treasure Mode with Google Maps.mp4"));
        mediaPlayerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
            @Override
            public void onPreparedStateChanged(PlaybackGlue glue) {
                super.onPreparedStateChanged(glue);
                PlaybackTransportControlGlue controlGlue = (PlaybackTransportControlGlue) glue;
                controlGlue.setSeekProvider(new PlaybackSeekDiskDataProvider(
                        controlGlue.getDuration(), 1000,
                        "/sdcard/seek/frame_%04d.jpg"));
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mBackgroundHelper = new BackgroundHelper(getActivity());
        mDetailsBackground.enableParallax();
        if (TEST_BACKGROUND_PLAYER) {
            if (MovieData.sStatus == MovieData.STATUS_INIT) {
                // not own/rented, play trailer
                setupTrailerVideo();
            } else {
                // bought or rented, play the main content
                setupMainVideo();
                // hide details main ui
                mDetailsBackground.switchToVideo();
            }
        }

        final Context context = getActivity();
        setBadgeDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_title,
                context.getTheme()));
        setTitle("Leanback Sample App");
        if (!TEST_BACKGROUND_PLAYER) {
            setOnSearchClickedListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), SearchActivity.class);
                    startActivity(intent);
                }
            });
        }

        mActionPlay = new Action(ACTION_PLAY, "Play");
        mActionRent = new Action(ACTION_RENT, "Rent", "$3.99", ResourcesCompat.getDrawable(
                context.getResources(), R.drawable.ic_action_a, context.getTheme()));
        mActionBuy = new Action(ACTION_BUY, "Buy $9.99");

        ClassPresenterSelector ps = new ClassPresenterSelector();
        FullWidthDetailsOverviewRowPresenter dorPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                final Context context = getActivity();
                DetailsOverviewRow dor = (DetailsOverviewRow) mRowsAdapter.get(0);
                if (action.getId() == ACTION_BUY) {
                    // on the UI thread, we can modify actions adapter directly
                    SparseArrayObjectAdapter actions = (SparseArrayObjectAdapter)
                            dor.getActionsAdapter();
                    actions.set(ACTION_PLAY, mActionPlay);
                    actions.clear(ACTION_RENT);
                    actions.clear(ACTION_BUY);
                    boolean previousRented = MovieData.sStatus == MovieData.STATUS_RENTED;
                    MovieData.sStatus = MovieData.STATUS_OWN;
                    dor.setItem(getDisplayTitle(mPhotoItem.getTitle()));
                    dor.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                            R.drawable.details_img_16x9, context.getTheme()));
                    if (TEST_BACKGROUND_PLAYER) {
                        if (!previousRented) {
                            setupMainVideo();
                            mDetailsBackground.switchToVideo();
                        }
                    } else {
                        Intent intent = new Intent(context, PlaybackActivity.class);
                        getActivity().startActivity(intent);
                    }
                } else if (action.getId() == ACTION_RENT) {
                    // on the UI thread, we can modify actions adapter directly
                    SparseArrayObjectAdapter actions = (SparseArrayObjectAdapter)
                            dor.getActionsAdapter();
                    actions.set(ACTION_PLAY, mActionPlay);
                    actions.clear(ACTION_RENT);
                    MovieData.sStatus = MovieData.STATUS_RENTED;
                    dor.setItem(getDisplayTitle(mPhotoItem.getTitle()));
                    if (TEST_BACKGROUND_PLAYER) {
                        setupMainVideo();
                        mDetailsBackground.switchToVideo();
                    } else {
                        Intent intent = new Intent(context, PlaybackActivity.class);
                        getActivity().startActivity(intent);
                    }
                } else if (action.getId() == ACTION_PLAY) {
                    if (TEST_BACKGROUND_PLAYER) {
                        mDetailsBackground.switchToVideo();
                    } else {
                        Intent intent = new Intent(context, PlaybackActivity.class);
                        getActivity().startActivity(intent);
                    }
                }
            }
        });

        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        mRowsAdapter = new ArrayObjectAdapter(ps);
        updateAdapter();

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
                if (item instanceof PhotoItem) {
                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(DetailsActivity.EXTRA_ITEM, (PhotoItem) item);

                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            getActivity(),
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                }
            }
        });
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });

        if (TEST_SHARED_ELEMENT_TRANSITION) {
            mHelper = new FullWidthDetailsOverviewSharedElementHelper();
            mHelper.setSharedElementEnterTransition(getActivity(),
                    DetailsActivity.SHARED_ELEMENT_NAME);
            dorPresenter.setListener(mHelper);
            dorPresenter.setParticipatingEntranceTransition(false);
        } else {
            dorPresenter.setParticipatingEntranceTransition(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    public void setBackgroundVideo(boolean backgroundVideo) {
        TEST_BACKGROUND_PLAYER = backgroundVideo;
    }

    public void setItem(PhotoItem photoItem) {
        mPhotoItem = photoItem;
        updateAdapter();
    }

    static String getDisplayTitle(String title) {
        switch (MovieData.sStatus) {
            case MovieData.STATUS_OWN:
                title = title + "(Owned)";
                break;
            case MovieData.STATUS_RENTED:
                title = title + "(Rented)";
                break;
            case MovieData.STATUS_INIT:
            default:
        }
        return title;
    }

    void updateAdapter() {
        if (mRowsAdapter == null) {
            return;
        }
        mRowsAdapter.clear();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Context context = getActivity();
                if (context == null) {
                    return;
                }

                DetailsOverviewRow dor = new DetailsOverviewRow(
                        getDisplayTitle(mPhotoItem.getTitle()));
                dor.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),
                        mPhotoItem.getImageResourceId(), context.getTheme()));
                SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();
                switch (MovieData.sStatus) {
                    case MovieData.STATUS_INIT:
                        adapter.set(ACTION_RENT, mActionRent);
                        adapter.set(ACTION_BUY, mActionBuy);
                        break;
                    case MovieData.STATUS_OWN:
                        adapter.set(ACTION_PLAY, mActionPlay);
                        break;
                    case MovieData.STATUS_RENTED:
                        adapter.set(ACTION_PLAY, mActionPlay);
                        adapter.set(ACTION_BUY, mActionBuy);
                        break;
                }
                // one line text with icon
                Drawable d = ResourcesCompat.getDrawable(context.getResources(),
                        R.drawable.ic_action_a, context.getTheme());
                adapter.set(202, new Action(202, "Top", null, d));
                dor.setActionsAdapter(adapter);
                mRowsAdapter.add(0, dor);
                setSelectedPosition(0, true);
                if (TEST_SHARED_ELEMENT_TRANSITION) {
                    if (mHelper != null && !mHelper.getAutoStartSharedElementTransition()) {
                        mHelper.startPostponedEnterTransition();
                    }
                }
            }
        }, TIME_TO_LOAD_OVERVIEW_ROW_MS);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                for (int i = 0; i < NUM_ROWS; ++i) {
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                    listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
                    listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
                    listRowAdapter.add(new PhotoItem("Android TV", R.drawable.gallery_photo_3));
                    listRowAdapter.add(new PhotoItem("Leanback", R.drawable.gallery_photo_4));
                    HeaderItem header = new HeaderItem(i, "Row " + i);
                    mRowsAdapter.add(new ListRow(header, listRowAdapter));
                }
            }
        }, TIME_TO_LOAD_RELATED_ROWS_MS);
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore background drawable in onStart():
        mBackgroundHelper.loadBitmap(R.drawable.spiderman,
                new BackgroundHelper.BitmapLoadCallback() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                    }
                });
    }

    @Override
    public void onStop() {
        mDetailsBackground.setCoverBitmap(null);
        super.onStop();
    }
}
