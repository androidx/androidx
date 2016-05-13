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

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.supportleanbackshowcase.utils.Constants;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.utils.Utils;
import android.support.v17.leanback.supportleanbackshowcase.models.Song;
import android.support.v17.leanback.supportleanbackshowcase.models.SongList;
import android.support.v17.leanback.widget.*;
import android.support.v17.leanback.widget.AbstractMediaItemPresenter;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

/**
 * This example shows how to play music files and build a simple track list.
 */
public class MusicConsumptionExampleFragment extends PlaybackOverlayFragment implements
        BaseOnItemViewClickedListener, BaseOnItemViewSelectedListener,
        MediaPlayerGlue.OnMediaFileFinishedPlayingListener {

    private static final String TAG = "MusicConsumptionExampleFragment";
    private static final int PLAYLIST_ACTION_ID = 0;
    private static final int FAVORITE_ACTION_ID = 1;
    private ArrayObjectAdapter mRowsAdapter;
    private MediaPlayerGlue mGlue;
    private int mCurrentSongIndex = 0;
    private List<Song> mSongList;
    private boolean mAdapterNotified = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onCreate");

        mGlue = new MediaPlayerGlue(getActivity(), this) {

            @Override protected void onRowChanged(PlaybackControlsRow row) {
                if (mRowsAdapter == null || mAdapterNotified) return;
                //mAdapterNotified = true;
                mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
            }
        };
        mGlue.setOnMediaFileFinishedPlayingListener(this);

        String json = Utils.inputStreamToString(
                getResources().openRawResource(R.raw.music_consumption_example));


        mSongList = new Gson().fromJson(json, SongList.class).getSongs();

        Resources res = getActivity().getResources();

        // For each song add a playlist and favorite actions.
        for(Song song : mSongList) {
            MultiActionsProvider.MultiAction[] mediaRowActions = new
                    MultiActionsProvider.MultiAction[2];
            MultiActionsProvider.MultiAction playlistAction = new
                    MultiActionsProvider.MultiAction(PLAYLIST_ACTION_ID);
            Drawable[] playlistActionDrawables = new Drawable[] {
                    res.getDrawable(R.drawable.ic_playlist_add_white_24dp,
                            getActivity().getTheme()),
                    res.getDrawable(R.drawable.ic_playlist_add_filled_24dp,
                            getActivity().getTheme())};
            playlistAction.setDrawables(playlistActionDrawables);
            mediaRowActions[0] = playlistAction;

            MultiActionsProvider.MultiAction favoriteAction = new
                    MultiActionsProvider.MultiAction(FAVORITE_ACTION_ID);
            Drawable[] favoriteActionDrawables = new Drawable[] {
                    res.getDrawable(R.drawable.ic_favorite_border_white_24dp,
                            getActivity().getTheme()),
                    res.getDrawable(R.drawable.ic_favorite_filled_24dp,
                            getActivity().getTheme())};
            favoriteAction.setDrawables(favoriteActionDrawables);
            mediaRowActions[1] = favoriteAction;
            song.setMediaRowActions(mediaRowActions);
        }

        Song song = mSongList.get(mCurrentSongIndex);
        MediaPlayerGlue.MetaData metaData = new MediaPlayerGlue.MetaData();
        metaData.setArtist(song.getDescription());
        metaData.setTitle(song.getTitle());
        metaData.setCover(getResources().getDrawable(song.getImageResource(getActivity()), null));
        Uri uri = Utils.getResourceUri(getActivity(), song.getFileResource(getActivity()));
        mGlue.setMetaData(metaData);
        mGlue.setMediaSource(uri);
        mGlue.prepareMediaForPlaying();

        addPlaybackControlsRow();
    }

    @Override public void onStart() {
        super.onStart();
        mGlue.enableProgressUpdating(mGlue.hasValidMedia() && mGlue.isMediaPlaying());
    }

    @Override public void onStop() {
        super.onStop();
        mGlue.enableProgressUpdating(false);
        mGlue.reset();
    }

    static class SongPresenter extends AbstractMediaItemPresenter {

        SongPresenter() {
            super();
        }

        SongPresenter(Context context, int themeResId) {
            super(themeResId);
            setHasMediaRowSeparator(true);
        }

        @Override
        protected void onBindMediaDetails(ViewHolder vh, Object item) {

            int favoriteTextColor =  vh.view.getContext().getResources().getColor(
                    R.color.song_row_favorite_color);
            Song song = (Song) item;
            vh.getMediaItemNumberView().setText("" + song.getNumber());

            String songTitle = song.getTitle() + " / " + song.getDescription();
            vh.getMediaItemNameView().setText(songTitle);

            vh.getMediaItemDurationView().setText("" + song.getDuration());

            if (song.isFavorite()) {
                vh.getMediaItemNumberView().setTextColor(favoriteTextColor);
                vh.getMediaItemNameView().setTextColor(favoriteTextColor);
                vh.getMediaItemDurationView().setTextColor(favoriteTextColor);
            } else {
                Context context = vh.getMediaItemNumberView().getContext();
                vh.getMediaItemNumberView().setTextAppearance(context,
                        R.style.TextAppearance_Leanback_PlaybackMediaItemNumber);
                vh.getMediaItemNameView().setTextAppearance(context,
                        R.style.TextAppearance_Leanback_PlaybackMediaItemName);
                vh.getMediaItemDurationView().setTextAppearance(context,
                        R.style.TextAppearance_Leanback_PlaybackMediaItemDuration);
            }
        }
    };

    static class SongPresenterSelector extends PresenterSelector {
        Presenter mRegularPresenter;
        Presenter mFavoritePresenter;

        /**
         * Adds a presenter to be used for the given class.
         */
        public SongPresenterSelector setSongPresenterRegular(Presenter presenter) {
            mRegularPresenter = presenter;
            return this;
        }

        /**
         * Adds a presenter to be used for the given class.
         */
        public SongPresenterSelector setSongPresenterFavorite(Presenter presenter) {
            mFavoritePresenter = presenter;
            return this;
        }

        @Override
        public Presenter[] getPresenters() {
            return new Presenter[]{mRegularPresenter, mFavoritePresenter};
        }

        @Override
        public Presenter getPresenter(Object item) {
            return ( (Song) item).isFavorite() ? mFavoritePresenter : mRegularPresenter;
        }

    }

    static class TrackListHeaderPresenter extends AbstractMediaListHeaderPresenter {

        TrackListHeaderPresenter() {
            super();
        }

        @Override
        protected void onBindMediaListHeaderViewHolder(ViewHolder vh, Object item) {
            vh.getHeaderView().setText("Tracklist");
        }
    };

    private void addPlaybackControlsRow() {
        mRowsAdapter = new ArrayObjectAdapter(new ClassPresenterSelector()
                .addClassPresenterSelector(Song.class, new SongPresenterSelector()
                        .setSongPresenterRegular(new SongPresenter(getActivity(),
                                R.style.Theme_Example_LeanbackMusic_RegularSongNumbers))
                        .setSongPresenterFavorite(new SongPresenter(getActivity(),
                                R.style.Theme_Example_LeanbackMusic_FavoriteSongNumbers)))
                .addClassPresenter(TrackListHeader.class, new TrackListHeaderPresenter())
                .addClassPresenter(PlaybackControlsRow.class,
                        mGlue.createControlsRowAndPresenter()));
        mRowsAdapter.add(mGlue.getControlsRow());
        mRowsAdapter.add(new TrackListHeader());
        mRowsAdapter.addAll(2, mSongList);
        setAdapter(mRowsAdapter);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
    }

    public MusicConsumptionExampleFragment() {
        super();
    }



    @Override public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                        RowPresenter.ViewHolder rowViewHolder, Object row) {

        if (item instanceof  Action) {
            // if the clicked item is a primary or secondary action in the playback controller
            mGlue.onActionClicked((Action) item);
        } else if (row instanceof  Song) {
            // if a media item row is clicked
            Song clickedSong = (Song) row;
            AbstractMediaItemPresenter.ViewHolder songRowVh =
                    (AbstractMediaItemPresenter.ViewHolder) rowViewHolder;

            // if an action within a media item row is clicked
            if (item instanceof MultiActionsProvider.MultiAction) {
                if ( ((MultiActionsProvider.MultiAction) item).getId() == FAVORITE_ACTION_ID) {
                    MultiActionsProvider.MultiAction favoriteAction =
                            (MultiActionsProvider.MultiAction) item;
                    MultiActionsProvider.MultiAction playlistAction =
                            songRowVh.getMediaItemRowActions()[0];
                    favoriteAction.incrementIndex();
                    playlistAction.incrementIndex();;

                    clickedSong.setFavorite(!clickedSong.isFavorite());
                    songRowVh.notifyDetailsChanged();
                    songRowVh.notifyActionChanged(playlistAction);
                    songRowVh.notifyActionChanged(favoriteAction);
                }
            } else if (item == null){
                // if a media item details is clicked, start playing that media item
                onSongDetailsClicked(clickedSong);
            }

        }


    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Object row) {
    }


    public void onSongDetailsClicked(Song song) {
        int nextSongIndex = mSongList.indexOf(song);
        mCurrentSongIndex = nextSongIndex;
        startPlayback();
    }


    @Override public void onMediaFileFinishedPlaying(MediaPlayerGlue.MetaData song) {
        if (mGlue.repeatOne()) {
        } else if (mGlue.useShuffle()) {
            mCurrentSongIndex = (int) (Math.random() * mSongList.size());
        } else {
            mCurrentSongIndex++;
            if (mCurrentSongIndex >= mSongList.size()) {
                mCurrentSongIndex = 0;
                if (!mGlue.repeatAll()) {
                    return;
                }
            }
        }
        startPlayback();
    }

    private void startPlayback() {
        Song song = mSongList.get(mCurrentSongIndex);
        MediaPlayerGlue.MetaData metaData = new MediaPlayerGlue.MetaData();
        metaData.setArtist(song.getDescription());
        metaData.setTitle(song.getTitle());
        metaData.setCover(getResources().getDrawable(song.getImageResource(getActivity()), null));

        Uri uri = Utils.getResourceUri(getActivity(), song.getFileResource(getActivity()));
        mGlue.setMetaData(metaData);

        if (mGlue.setMediaSource(uri)) {
            mGlue.prepareMediaForPlaying();
        }
        mGlue.startPlayback();
    }
}
