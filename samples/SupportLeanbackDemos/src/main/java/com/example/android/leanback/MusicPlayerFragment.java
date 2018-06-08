/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.leanback.app.PlaybackFragment;
import androidx.leanback.app.PlaybackFragmentGlueHost;
import androidx.leanback.media.MediaControllerAdapter;
import androidx.leanback.media.PlaybackBannerControlGlue;
import androidx.leanback.media.PlaybackBaseControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The fragment which contains the MediaSessionService through binding to it.
 * Also this fragment contains a specialized glue with repeat and shuffle operation.
 */
public class MusicPlayerFragment extends PlaybackFragment implements
        MediaSessionService.MediaPlayerListener {

    /**
     * For this app, when the player is prepared, the music item will not be played automatically,
     * so we will fire pause() operation here through attached glue in case current play back
     * state is playing.
     */
    @Override
    public void onPrepared() {
        mGlue.pause();
    }

    /**
     * This control glue is extended from {@link PlaybackBannerControlGlue} for two additional
     * operation (shuffle and repeat)
     * Also, a secondary control row will be added in this glue to hold these two operations.
     *
     * @param <T> T extends MediaControllerAdapter
     */
    private class PlaybackBannerMusicPlayerControlGlue<T extends MediaControllerAdapter> extends
            PlaybackBannerControlGlue<T> {

        // Two more action (Repeat and Shuffle) is added to demonstrate the usage of the API defined
        // in MediaControllerAdapter
        private PlaybackControlsRow.RepeatAction mRepeatAction;
        private PlaybackControlsRow.ShuffleAction mShuffleAction;

        private PlaybackBannerMusicPlayerControlGlue(Context context, int[] fastForwardSpeeds,
                int[] rewindSpeeds, T impl) {
            super(context, fastForwardSpeeds, rewindSpeeds, impl);
        }

        /**
         * Create secondary control row to hold repeat and shuffle action.
         *
         * @param secondaryActionsAdapter The adapter you need to add the {@link Action}s to.
         */
        @Override
        protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
            final long supportedActions = getSupportedActions();

            if ((supportedActions & ACTION_REPEAT) != 0 && mRepeatAction == null) {
                secondaryActionsAdapter.add(
                        mRepeatAction = new PlaybackControlsRow.RepeatAction(getContext()));
            } else if ((supportedActions & ACTION_REPEAT) == 0
                    && mRepeatAction != null) {
                secondaryActionsAdapter.remove(mRepeatAction);
                mRepeatAction = null;
            }

            if ((supportedActions & ACTION_SHUFFLE) != 0 && mShuffleAction == null) {
                secondaryActionsAdapter.add(
                        mShuffleAction = new PlaybackControlsRow.ShuffleAction(getContext()));
            } else if ((supportedActions & ACTION_SHUFFLE) == 0
                    && mShuffleAction != null) {
                secondaryActionsAdapter.remove(mShuffleAction);
                mShuffleAction = null;
            }
        }

        /**
         * Media art, title and subtitle will be updated every time when this callback
         * function is called.
         */
        @Override
        public void onMetadataChanged() {
            super.onMetadataChanged();
            setArt(getPlayerAdapter().getMediaArt(getActivity()));
            setTitle(getPlayerAdapter().getMediaTitle());
            setSubtitle(getPlayerAdapter().getMediaSubtitle());
        }

        @Override
        public long getSupportedActions() {
            long supportedActions = super.getSupportedActions();
            // In our case, if fast forward action or rewind action are not supported by adapter
            // "Fake Fast Forward" and "Fake Rewind" will be executed when user press fast forward
            // or rewind button.
            return supportedActions
                    | PlaybackBaseControlGlue.ACTION_FAST_FORWARD
                    | PlaybackBaseControlGlue.ACTION_REWIND;
        }

        /**
         * The callback function to dispatch the action.
         *
         * @param action Action performed by app user.
         */
        @Override
        public void onActionClicked(Action action) {
            // In our customized glue, only shuffle and repeat these two actions will be
            // processed specifically. Other actions will be handled by super method.
            super.onActionClicked(action);

            // when the action is an instance of repeat action
            if (action instanceof PlaybackControlsRow.RepeatAction) {
                PlaybackControlsRow.RepeatAction repeatAction =
                        ((PlaybackControlsRow.RepeatAction) action);
                repeatAction.nextIndex();
                int index = (repeatAction).getIndex();
                if (getPlayerAdapter() != null) {
                    getPlayerAdapter().setRepeatAction(index);
                }
                notifyItemChanged(
                        (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter(),
                        action);
            }

            // when the action is an instance of shuffle action
            if (action instanceof PlaybackControlsRow.ShuffleAction) {
                PlaybackControlsRow.ShuffleAction shuffleAction =
                        ((PlaybackControlsRow.ShuffleAction) action);
                shuffleAction.nextIndex();
                int index = (shuffleAction).getIndex();
                if (getPlayerAdapter() != null) {
                    getPlayerAdapter().setShuffleAction(index);
                }
                notifyItemChanged(
                        (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter(), action);
            }
        }
    }

    private PlaybackBannerMusicPlayerControlGlue<MediaControllerAdapter> mGlue;
    private MediaControllerCompat mController;
    private MediaControllerAdapter mAdapter;
    private MediaSessionCompat mMediaSession;
    private MediaSessionService mPlaybackService;
    private List<MusicItem> mSongMetaDataList = new ArrayList<>();

    // when the service is bound, fragment will get media session's instance
    private ServiceConnection mPlaybackServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            int[] fastForwardSpeed = new int[]{2, 3, 4, 5};
            int[] rewindSpeed = new int[]{2, 3, 4, 5};

            // Bind the service.
            MediaSessionService.LocalBinder binder = (MediaSessionService.LocalBinder) iBinder;
            mPlaybackService = binder.getService();

            // Register this fragment as the UI callback for backend service.
            mPlaybackService.registerCallback(MusicPlayerFragment.this);

            // When the service is created, the video player/ audio manager will be initialized
            // The following method will create the media item list and set the data source to
            // the first item in current media list. Parameter false means, the original media
            // item lists will be removed.
            mPlaybackService.setMediaList(mSongMetaDataList, false);

            // Set FastForward and Rewind Speed Factors.
            mPlaybackService.setFastForwardSpeedFactors(fastForwardSpeed);
            mPlaybackService.setRewindSpeedFactors(rewindSpeed);

            // Get media session through service
            mMediaSession = mPlaybackService.getMediaSession();
            // The adapter is created using current MediaSession
            mController = new MediaControllerCompat(MusicPlayerFragment.this.getActivity(),
                    mMediaSession);
            mAdapter = new MediaControllerAdapter(mController);
            mGlue = new PlaybackBannerMusicPlayerControlGlue<>(getActivity(), fastForwardSpeed,
                    rewindSpeed,
                    mAdapter);

            // register this callback in service, so current UI can toggle UI pause
            // to control control row's status
            mGlue.setHost(new PlaybackFragmentGlueHost(MusicPlayerFragment.this));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPlaybackService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String json = inputStreamToString(
                getActivity().getResources().openRawResource(R.raw.media));
        MusicItem[] musicItems = new Gson().fromJson(json, MusicItem[].class);
        for (MusicItem i : musicItems) {
            mSongMetaDataList.add(i);
        }
        Intent serviceIntent = new Intent(getActivity(), MediaSessionService.class);
        getActivity().bindService(serviceIntent, mPlaybackServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    /**
     * Helper function to read the content from a given {@link InputStream} and return it as a
     * {@link String}.
     *
     * @param inputStream The {@link InputStream} which should be read.
     * @return Returns <code>null</code> if the the {@link InputStream} could not be read. Else
     * returns the content of the {@link InputStream} as {@link String}.
     */
    private static String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }
    }
}
