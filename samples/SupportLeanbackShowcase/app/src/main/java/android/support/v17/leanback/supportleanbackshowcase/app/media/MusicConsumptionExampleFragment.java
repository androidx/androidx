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

import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.supportleanbackshowcase.utils.Constants;
import android.support.v17.leanback.supportleanbackshowcase.app.media.MediaPlayerGlue;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.app.media.TrackListHeader;
import android.support.v17.leanback.supportleanbackshowcase.utils.Utils;
import android.support.v17.leanback.supportleanbackshowcase.models.Song;
import android.support.v17.leanback.supportleanbackshowcase.models.SongList;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

/**
 * This example shows how to play music files and build a simple track list.
 */
public class MusicConsumptionExampleFragment extends PlaybackOverlayFragment implements
        OnItemViewClickedListener, Song.OnSongRowClickListener,
        MediaPlayerGlue.OnMediaFileFinishedPlayingListener {

    private static final String TAG = "MusicConsumptionExampleFragment";
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
        Song song = mSongList.get(mCurrentSongIndex);
        MediaPlayerGlue.MetaData metaData = new MediaPlayerGlue.MetaData();
        metaData.setArtist(song.getDescription());
        metaData.setTitle(song.getTitle());
        metaData.setCover(getResources().getDrawable(song.getImageResource(getActivity()), null));
        Uri uri = Utils.getResourceUri(getActivity(), song.getFileResource(getActivity()));
        mGlue.setMetaData(metaData);
        mGlue.setMediaSource(uri);

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
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

    private void addPlaybackControlsRow() {
        final PlaybackControlsRowPresenter controlsPresenter = mGlue
                .createControlsRowAndPresenter();
        ClassPresenterSelector selector = new ClassPresenterSelector();
        Song.Presenter songPresenter = new Song.Presenter(getActivity());
        songPresenter.setOnClickListener(this);
        selector.addClassPresenter(Song.class, songPresenter);
        selector.addClassPresenter(TrackListHeader.class,
                                   new TrackListHeader.Presenter(getActivity()));
        selector.addClassPresenter(PlaybackControlsRow.class, controlsPresenter);
        mRowsAdapter = new ArrayObjectAdapter(selector);
        mRowsAdapter.add(mGlue.getControlsRow());
        mRowsAdapter.add(new TrackListHeader());
        mRowsAdapter.addAll(2, mSongList);
        setAdapter(mRowsAdapter);
        setOnItemViewClickedListener(this);
    }

    @Override public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                        RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (!(item instanceof Action)) return;
        mGlue.onActionClicked((Action) item);
    }


    @Override public void onSongRowClicked(Song song) {
        mCurrentSongIndex = mSongList.indexOf(song);
        startPlayback();
    }


    @Override public void onMediaFileFinishedPlaying(MediaPlayerGlue.MetaData song) {
        if (mGlue.repeatOne()) {
            startPlayback();
            return;
        }
        if (mGlue.useShuffle()) {
            mCurrentSongIndex = (int) (Math.random() * mSongList.size());
        } else mCurrentSongIndex++;
        if (mCurrentSongIndex >= mSongList.size()) {
            mCurrentSongIndex = 0;
            if (!mGlue.repeatAll()) return;
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
        mGlue.setMediaSource(uri);
        mGlue.startPlayback();
    }
}
