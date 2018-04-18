/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv4.media;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.android.supportv4.R;

import java.util.List;

/**
 * A class that shows the Media Queue to the user.
 */
public class QueueFragment extends Fragment {

    private static final String TAG = "QueueFragment";

    private ImageButton mSkipNext;
    private ImageButton mSkipPrevious;
    private ImageButton mPlayPause;

    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat.TransportControls mTransportControls;
    private MediaControllerCompat mMediaController;
    private PlaybackStateCompat mPlaybackState;

    private QueueAdapter mQueueAdapter;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected: session token " + mMediaBrowser.getSessionToken());

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            try {
                mMediaController = new MediaControllerCompat(getActivity(),
                        mMediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create MediaControllerCompat.", e);
            }
            mTransportControls = mMediaController.getTransportControls();
            mMediaController.registerCallback(mSessionCallback);

            ((MediaBrowserSupport) getActivity()).setMediaController(mMediaController);
            mPlaybackState = mMediaController.getPlaybackState();

            List<MediaSessionCompat.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
            onPlaybackStateChanged(mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");
            mMediaController.unregisterCallback(mSessionCallback);
            mTransportControls = null;
            mMediaController = null;
            ((MediaBrowserSupport) getActivity()).setMediaController((MediaControllerCompat) null);
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaControllerCompat.Callback mSessionCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onSessionDestroyed() {
            Log.d(TAG, "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state == null) {
                return;
            }
            Log.d(TAG, "Received playback state change to state " + state.getState());
            mPlaybackState = state;
            QueueFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            Log.d(TAG, "onQueueChanged " + queue);
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
        }
    };

    public static QueueFragment newInstance() {
        return new QueueFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mSkipPrevious = (ImageButton) rootView.findViewById(R.id.skip_previous);
        mSkipPrevious.setEnabled(false);
        mSkipPrevious.setOnClickListener(mButtonListener);

        mSkipNext = (ImageButton) rootView.findViewById(R.id.skip_next);
        mSkipNext.setEnabled(false);
        mSkipNext.setOnClickListener(mButtonListener);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);

        mQueueAdapter = new QueueAdapter(getActivity());

        ListView mListView = (ListView) rootView.findViewById(R.id.list_view);
        mListView.setAdapter(mQueueAdapter);
        mListView.setFocusable(true);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaSessionCompat.QueueItem item = mQueueAdapter.getItem(position);
                mTransportControls.skipToQueueItem(item.getQueueId());
            }
        });

        mMediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaBrowserServiceSupport.class),
                mConnectionCallback, null);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mSessionCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }


    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged " + state);
        if (state == null) {
            return;
        }
        mQueueAdapter.setActiveQueueItemId(state.getActiveQueueItemId());
        mQueueAdapter.notifyDataSetChanged();
        boolean enablePlay = false;
        StringBuilder statusBuilder = new StringBuilder();
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                statusBuilder.append("playing");
                enablePlay = false;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                statusBuilder.append("paused");
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                statusBuilder.append("ended");
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                statusBuilder.append("error: ").append(state.getErrorMessage());
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                statusBuilder.append("buffering");
                break;
            case PlaybackStateCompat.STATE_NONE:
                statusBuilder.append("none");
                enablePlay = false;
                break;
            case PlaybackStateCompat.STATE_CONNECTING:
                statusBuilder.append("connecting");
                break;
            default:
                statusBuilder.append(mPlaybackState);
        }
        statusBuilder.append(" -- At position: ").append(state.getPosition());
        Log.d(TAG, statusBuilder.toString());

        if (enablePlay) {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_24dp));
        } else {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_24dp));
        }

        mSkipPrevious.setEnabled((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                != 0);
        mSkipNext.setEnabled((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0);

        Log.d(TAG, "Queue From MediaController *** Title "
                + mMediaController.getQueueTitle() + "\n: Queue: " + mMediaController.getQueue()
                + "\n Metadata " + mMediaController.getMetadata());
    }

    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int state = mPlaybackState == null ?
                    PlaybackStateCompat.STATE_NONE : mPlaybackState.getState();
            switch (v.getId()) {
                case R.id.play_pause:
                    Log.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING) {
                        pauseMedia();
                    }
                    break;
                case R.id.skip_previous:
                    Log.d(TAG, "Start button pressed, in state " + state);
                    skipToPrevious();
                    break;
                case R.id.skip_next:
                    skipToNext();
                    break;
            }
        }
    };

    private void playMedia() {
        if (mTransportControls != null) {
            mTransportControls.play();
        }
    }

    private void pauseMedia() {
        if (mTransportControls != null) {
            mTransportControls.pause();
        }
    }

    private void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    private void skipToNext() {
        if (mTransportControls != null) {
            mTransportControls.skipToNext();
        }
    }
}
