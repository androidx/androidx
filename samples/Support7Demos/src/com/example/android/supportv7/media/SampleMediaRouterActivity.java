/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv7.media;

import com.example.android.supportv7.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.app.PendingIntent;
import android.app.Presentation;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteDiscoveryFragment;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaRouter.ProviderInfo;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaItemStatus;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Display;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


import java.io.File;

/**
 * <h3>Media Router Support Activity</h3>
 *
 * <p>
 * This demonstrates how to use the {@link MediaRouter} API to build an
 * application that allows the user to send content to various rendering
 * targets.
 * </p>
 */
public class SampleMediaRouterActivity extends ActionBarActivity {
    private static final String TAG = "MediaRouterSupport";
    private static final String DISCOVERY_FRAGMENT_TAG = "DiscoveryFragment";
    private static final String ACTION_STATUS_CHANGE =
            "com.example.android.supportv7.media.ACTION_STATUS_CHANGE";

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mSelector;
    private LibraryAdapter mLibraryItems;
    private PlaylistAdapter mPlayListItems;
    private TextView mInfoTextView;
    private ListView mLibraryView;
    private ListView mPlayListView;
    private ImageButton mPauseResumeButton;
    private ImageButton mStopButton;
    private SeekBar mSeekBar;
    private String mStatsInfo;
    private boolean mPaused;
    private boolean mNeedResume;
    private boolean mSeeking;
    private long mLastStatusTime;
    private PlaylistAdapter mSavedPlaylist;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress(getCheckedMediaQueueItem());
            // update Ui every 1 second
            mHandler.postDelayed(this, 1000);
        }
    };

    private final MediaPlayerWrapper mMediaPlayer = new MediaPlayerWrapper(this);
    private final MediaPlayerWrapper.Callback mMediaPlayerCB =
            new MediaPlayerWrapper.Callback()  {
        @Override
        public void onError() {
            mPlayer.onFinish(true);
        }

        @Override
        public void onCompletion() {
            mPlayer.onFinish(false);
        }

        @Override
        public void onSizeChanged(int width, int height) {
            mPlayer.updateSize(width, height);
        }

        @Override
        public void onStatusChanged() {
            if (!mSeeking) {
                updateUi();
            }
        }
    };

    private final RemotePlayer mRemotePlayer = new RemotePlayer();
    private final LocalPlayer mLocalPlayer = new LocalPlayer();
    private Player mPlayer;
    private MediaSessionManager.Callback mPlayerCB;

    private final MediaRouter.Callback mMediaRouterCB = new MediaRouter.Callback() {
        // Return a custom callback that will simply log all of the route events
        // for demonstration purposes.
        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteAdded: route=" + route);
        }

        @Override
        public void onRouteChanged(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteChanged: route=" + route);
            mPlayer.showStatistics();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteRemoved: route=" + route);
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: route=" + route);

            Player player = mPlayer;
            MediaSessionManager.Callback playerCB = mPlayerCB;

            if (route.supportsControlCategory(
                    MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                Intent enqueueIntent = new Intent(MediaControlIntent.ACTION_ENQUEUE);
                enqueueIntent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
                enqueueIntent.setDataAndType(Uri.parse("http://"), "video/mp4");

                Intent removeIntent = new Intent(MediaControlIntent.ACTION_REMOVE);
                removeIntent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);

                // Remote Playback:
                //   If route supports remote queuing, let it manage the queue;
                //   otherwise, manage the queue locally and feed it one item at a time
                if (route.supportsControlRequest(enqueueIntent)
                 && route.supportsControlRequest(removeIntent)) {
                    player = mRemotePlayer;
                } else {
                    player = mLocalPlayer;
                }
                playerCB = mRemotePlayer;
                mRemotePlayer.reset();

            } else {
                // Local Playback:
                //   Use local player and feed media player one item at a time
                player = mLocalPlayer;
                playerCB = mMediaPlayer;
            }

            if (player != mPlayer || playerCB != mPlayerCB) {
                // save current playlist
                PlaylistAdapter playlist = new PlaylistAdapter();
                for (int i = 0; i < mPlayListItems.getCount(); i++) {
                    MediaQueueItem item = mPlayListItems.getItem(i);
                    if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING
                            || item.getState() == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                        long position = item.getContentPosition();
                        long timeDelta = mPaused ? 0 :
                                (SystemClock.elapsedRealtime() - mLastStatusTime);
                        item.setContentPosition(position + timeDelta);
                    }
                    playlist.add(item);
                }

                // switch players
                mPlayer.stop();
                mPaused = false;
                mLocalPlayer.setCallback(playerCB);
                mPlayerCB = playerCB;
                mPlayer = player;
                mPlayer.showStatistics();
                mLocalPlayer.updatePresentation();

                // migrate playlist to new route
                int count = playlist.getCount();
                if (isRemoteQueue()) {
                    // if queuing is managed remotely, only enqueue the first
                    // item, as we need to have the returned session id to
                    // enqueue the rest of the playlist items
                    mSavedPlaylist = playlist;
                    count = 1;
                }
                for (int i = 0; i < count; i++) {
                    final MediaQueueItem item = playlist.getItem(i);
                    mPlayer.enqueue(item.getUri(), item.getContentPosition());
                }
            }
            updateUi();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            mPlayer.showStatistics();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteVolumeChanged: route=" + route);
        }

        @Override
        public void onRoutePresentationDisplayChanged(
                MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRoutePresentationDisplayChanged: route=" + route);
        }

        @Override
        public void onProviderAdded(MediaRouter router, ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderAdded: provider=" + provider);
        }

        @Override
        public void onProviderRemoved(MediaRouter router, ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderRemoved: provider=" + provider);
        }

        @Override
        public void onProviderChanged(MediaRouter router, ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderChanged: provider=" + provider);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received status update: " + intent);
            if (intent.getAction().equals(ACTION_STATUS_CHANGE)) {
                String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
                String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
                MediaItemStatus status = MediaItemStatus.fromBundle(
                    intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_STATUS));

                if (status.getPlaybackState() ==
                        MediaItemStatus.PLAYBACK_STATE_FINISHED) {
                    mPlayer.onFinish(false);
                } else if (status.getPlaybackState() ==
                        MediaItemStatus.PLAYBACK_STATE_ERROR) {
                    mPlayer.onFinish(true);
                    showToast("Error while playing item" +
                            ", sid " + sid + ", iid " + iid);
                } else {
                    if (!mSeeking) {
                        updateUi();
                    }
                }
            }
        }
    };

    private RemoteControlClient mRemoteControlClient;
    private ComponentName mEventReceiver;
    private AudioManager mAudioManager;
    private PendingIntent mMediaPendingIntent;
    private final OnAudioFocusChangeListener mAfChangeListener =
            new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                Log.d(TAG, "onAudioFocusChange: LOSS_TRANSIENT");
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_GAIN");
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the media router service.
        mMediaRouter = MediaRouter.getInstance(this);

        // Create a route selector for the type of routes that we care about.
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE)
                .build();

        // Add a fragment to take care of media route discovery.
        // This fragment automatically adds or removes a callback whenever the activity
        // is started or stopped.
        FragmentManager fm = getSupportFragmentManager();
        DiscoveryFragment fragment = (DiscoveryFragment)fm.findFragmentByTag(
                DISCOVERY_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new DiscoveryFragment(mMediaRouterCB);
            fragment.setRouteSelector(mSelector);
            fm.beginTransaction()
                    .add(fragment, DISCOVERY_FRAGMENT_TAG)
                    .commit();
        } else {
            fragment.setCallback(mMediaRouterCB);
            fragment.setRouteSelector(mSelector);
        }

        // Populate an array adapter with streaming media items.
        String[] mediaNames = getResources().getStringArray(R.array.media_names);
        String[] mediaUris = getResources().getStringArray(R.array.media_uris);
        mLibraryItems = new LibraryAdapter();
        for (int i = 0; i < mediaNames.length; i++) {
            mLibraryItems.add(new MediaItem(
                    "[streaming] "+mediaNames[i], Uri.parse(mediaUris[i])));
        }

        // Scan local external storage directory for media files.
        File externalDir = Environment.getExternalStorageDirectory();
        if (externalDir != null) {
            File list[] = externalDir.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    String filename = list[i].getName();
                    if (filename.matches(".*\\.(m4v|mp4)")) {
                        mLibraryItems.add(new MediaItem("[local] " + filename,
                                Uri.fromFile(list[i])));
                    }
                }
            }
        }

        mPlayListItems = new PlaylistAdapter();

        // Initialize the layout.
        setContentView(R.layout.sample_media_router);

        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();
        String tabName = getResources().getString(R.string.library_tab_text);
        TabSpec spec1=tabHost.newTabSpec(tabName);
        spec1.setContent(R.id.tab1);
        spec1.setIndicator(tabName);

        tabName = getResources().getString(R.string.playlist_tab_text);
        TabSpec spec2=tabHost.newTabSpec(tabName);
        spec2.setIndicator(tabName);
        spec2.setContent(R.id.tab2);

        tabName = getResources().getString(R.string.statistics_tab_text);
        TabSpec spec3=tabHost.newTabSpec(tabName);
        spec3.setIndicator(tabName);
        spec3.setContent(R.id.tab3);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        tabHost.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String arg0) {
                if (arg0.equals(getResources().getString(
                        R.string.statistics_tab_text))) {
                    mPlayer.showStatistics();
                }
                updateUi();
            }
        });

        mLibraryView = (ListView) findViewById(R.id.media);
        mLibraryView.setAdapter(mLibraryItems);
        mLibraryView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mLibraryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateButtons();
            }
        });

        mPlayListView = (ListView) findViewById(R.id.playlist);
        mPlayListView.setAdapter(mPlayListItems);
        mPlayListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mPlayListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateButtons();
            }
        });

        mInfoTextView = (TextView) findViewById(R.id.info);

        mPauseResumeButton = (ImageButton)findViewById(R.id.pause_resume_button);
        mPauseResumeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPaused) {
                    mPlayer.pause();
                } else {
                    mPlayer.resume();
                }
            }
        });

        mStopButton = (ImageButton)findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.stop();
                clearContent();
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MediaQueueItem item = getCheckedMediaQueueItem();
                if (fromUser && item != null && item.getContentDuration() > 0) {
                    long pos = progress * item.getContentDuration() / 100;
                    mPlayer.seek(item.getSessionId(), item.getItemId(), pos);
                    item.setContentPosition(pos);
                    mLastStatusTime = SystemClock.elapsedRealtime();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSeeking = false;
                updateUi();
            }
        });

        // Schedule Ui update
        mHandler.postDelayed(mUpdateSeekRunnable, 1000);

        // Use local playback with media player by default
        mLocalPlayer.onCreate();
        mMediaPlayer.setCallback(mMediaPlayerCB);
        mLocalPlayer.setCallback(mMediaPlayer);
        mPlayerCB = mMediaPlayer;
        mPlayer = mLocalPlayer;

        // Register broadcast receiver to receive status update from MRP
        IntentFilter filter = new IntentFilter();
        filter.addAction(SampleMediaRouterActivity.ACTION_STATUS_CHANGE);
        registerReceiver(mReceiver, filter);

        // Build the PendingIntent for the remote control client
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mEventReceiver = new ComponentName(getPackageName(),
                SampleMediaButtonReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mEventReceiver);
        mMediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        // Create and register the remote control client
        registerRCC();
    }

    private void registerRCC() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Create the RCC and register with AudioManager and MediaRouter
            mAudioManager.requestAudioFocus(mAfChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            mAudioManager.registerMediaButtonEventReceiver(mEventReceiver);
            mRemoteControlClient = new RemoteControlClient(mMediaPendingIntent);
            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
            mMediaRouter.addRemoteControlClient(mRemoteControlClient);
            SampleMediaButtonReceiver.setActivity(SampleMediaRouterActivity.this);
            mRemoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE);
            mRemoteControlClient.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    private void unregisterRCC() {
        // Unregister the RCC with AudioManager and MediaRouter
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setTransportControlFlags(0);
            mAudioManager.abandonAudioFocus(mAfChangeListener);
            mAudioManager.unregisterMediaButtonEventReceiver(mEventReceiver);
            mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
            mMediaRouter.removeRemoteControlClient(mRemoteControlClient);
            SampleMediaButtonReceiver.setActivity(null);
            mRemoteControlClient = null;
        }
    }

    public boolean handleMediaKey(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                {
                    Log.d(TAG, "Received Play/Pause event from RemoteControlClient");
                    if (!mPaused) {
                        mPlayer.pause();
                    } else {
                        mPlayer.resume();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                {
                    Log.d(TAG, "Received Play event from RemoteControlClient");
                    if (mPaused) {
                        mPlayer.resume();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                {
                    Log.d(TAG, "Received Pause event from RemoteControlClient");
                    if (!mPaused) {
                        mPlayer.pause();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_STOP:
                {
                    Log.d(TAG, "Received Stop event from RemoteControlClient");
                    mPlayer.stop();
                    clearContent();
                    return true;
                }
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleMediaKey(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleMediaKey(event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public void onStart() {
        // Be sure to call the super class.
        super.onStart();
        mPlayer.showStatistics();
    }

    @Override
    public void onPause() {
        // pause media player for local playback case only
        if (!isRemotePlayback() && !mPaused) {
            mNeedResume = true;
            mPlayer.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        // resume media player for local playback case only
        if (!isRemotePlayback() && mNeedResume) {
            mPlayer.resume();
            mNeedResume = false;
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        // Unregister the remote control client
        unregisterRCC();

        // Unregister broadcast receiver
        unregisterReceiver(mReceiver);
        mPlayer.stop();
        mMediaPlayer.release();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Be sure to call the super class.
        super.onCreateOptionsMenu(menu);

        // Inflate the menu and configure the media router action provider.
        getMenuInflater().inflate(R.menu.sample_media_router_menu, menu);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider)MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mSelector);

        // Return true to show the menu.
        return true;
    }

    private void updateRouteDescription() {
        RouteInfo route = mMediaRouter.getSelectedRoute();
        mInfoTextView.setText("Currently selected route:"
                + "\nName: " + route.getName()
                + "\nProvider: " + route.getProvider().getPackageName()
                + "\nDescription: " + route.getDescription()
                + "\nStatistics: " + mStatsInfo);
        updateButtons();
        mLocalPlayer.updatePresentation();
    }

    private void clearContent() {
        //TO-DO: clear surface view
    }

    private void updateButtons() {
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
        // show pause or resume icon depending on current state
        mPauseResumeButton.setImageResource(mPaused ?
                R.drawable.ic_media_play : R.drawable.ic_media_pause);
        // only enable seek bar when duration is known
        MediaQueueItem item = getCheckedMediaQueueItem();
        mSeekBar.setEnabled(item != null && item.getContentDuration() > 0);
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setPlaybackState(mPaused ?
                    RemoteControlClient.PLAYSTATE_PAUSED : RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    private void updateProgress(MediaQueueItem queueItem) {
        // Estimate content position from last status time and elapsed time.
        // (Note this might be slightly out of sync with remote side, however
        // it avoids frequent polling the MRP.)
        int progress = 0;
        if (queueItem != null) {
            int state = queueItem.getState();
            long duration = queueItem.getContentDuration();
            if (duration <= 0) {
                if (state == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || state == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    updateUi();
                }
            } else {
                long position = queueItem.getContentPosition();
                long timeDelta = mPaused ? 0 :
                        (SystemClock.elapsedRealtime() - mLastStatusTime);
                progress = (int)(100.0 * (position + timeDelta) / duration);
            }
        }
        mSeekBar.setProgress(progress);
    }

    private void updateUi() {
        updatePlaylist();
        updateButtons();
    }

    private void updatePlaylist() {
        Log.d(TAG, "updatePlaylist");
        final PlaylistAdapter playlist = new PlaylistAdapter();
        // make a copy of current playlist
        for (int i = 0; i < mPlayListItems.getCount(); i++) {
            playlist.add(mPlayListItems.getItem(i));
        }
        // clear mPlayListItems first, items will be added back when we get
        // status back from provider.
        mPlayListItems.clear();
        mPlayListView.invalidate();

        for (int i = 0; i < playlist.getCount(); i++) {
            final MediaQueueItem item = playlist.getItem(i);
            final boolean update = (i == playlist.getCount() - 1);
            mPlayer.getStatus(item, update);
        }
    }

    private MediaItem getCheckedMediaItem() {
        int index = mLibraryView.getCheckedItemPosition();
        if (index >= 0 && index < mLibraryItems.getCount()) {
            return mLibraryItems.getItem(index);
        }
        return null;
    }

    private MediaQueueItem getCheckedMediaQueueItem() {
        int count = mPlayListView.getCount();
        int index = mPlayListView.getCheckedItemPosition();
        if (count > 0) {
            if (index < 0 || index >= count) {
                index = 0;
                mPlayListView.setItemChecked(0, true);
            }
            return mPlayListItems.getItem(index);
        }
        return null;
    }

    private void enqueuePlaylist() {
        if (mSavedPlaylist != null) {
            final PlaylistAdapter playlist = mSavedPlaylist;
            mSavedPlaylist = null;
            // migrate playlist (except for the 1st item) to new route
            for (int i = 1; i < playlist.getCount(); i++) {
                final MediaQueueItem item = playlist.getItem(i);
                mPlayer.enqueue(item.getUri(), item.getContentPosition());
            }
        }
    }

    private boolean isRemoteQueue() {
        return mPlayer == mRemotePlayer;
    }

    private boolean isRemotePlayback() {
        return mPlayerCB == mRemotePlayer;
    }

    private void showToast(String msg) {
        Toast toast = Toast.makeText(SampleMediaRouterActivity.this,
                "[app] " + msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }

    private interface Player {
        void enqueue(final Uri uri, long pos);
        void remove(final MediaQueueItem item);
        void seek(String sid, String iid, long pos);
        void getStatus(final MediaQueueItem item, final boolean update);
        void pause();
        void resume();
        void stop();
        void showStatistics();
        void onFinish(boolean error);
        void updateSize(int width, int height);
    }

    private class LocalPlayer implements Player, SurfaceHolder.Callback {
        private final MediaSessionManager mSessionManager = new MediaSessionManager();
        private String mSessionId;
        // The presentation to show on the secondary display.
        private DemoPresentation mPresentation;
        private SurfaceView mSurfaceView;
        private FrameLayout mLayout;
        private int mVideoWidth;
        private int mVideoHeight;

        public void onCreate() {
            mLayout = (FrameLayout)findViewById(R.id.player);
            mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            holder.addCallback(this);
        }

        public void setCallback(MediaSessionManager.Callback cb) {
            mSessionManager.setCallback(cb);
        }

        @Override
        public void enqueue(final Uri uri, long pos) {
            Log.d(TAG, "LocalPlayer: enqueue, uri=" + uri + ", pos=" + pos);
            MediaQueueItem playlistItem = mSessionManager.enqueue(mSessionId, uri, null);
            mSessionId = playlistItem.getSessionId();
            // Set remote control client title
            if (mPlayListItems.getCount() == 0 && mRemoteControlClient != null) {
                RemoteControlClient.MetadataEditor ed = mRemoteControlClient.editMetadata(true);
                ed.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                        playlistItem.toString());
                ed.apply();
            }
            mPlayListItems.add(playlistItem);
            if (pos > 0) {
                // Seek to initial position if needed
                mPlayer.seek(mSessionId, playlistItem.getItemId(), pos);
            }
            updateUi();
        }

        @Override
        public void remove(final MediaQueueItem item) {
            Log.d(TAG, "LocalPlayer: remove, item=" + item);
            mSessionManager.remove(item.getSessionId(), item.getItemId());
            updateUi();
        }

        @Override
        public void seek(String sid, String iid, long pos) {
            Log.d(TAG, "LocalPlayer: seek, sid=" + sid + ", iid=" + iid);
            mSessionManager.seek(sid, iid, pos);
        }

        @Override
        public void getStatus(final MediaQueueItem item, final boolean update) {
            Log.d(TAG, "LocalPlayer: getStatus, item=" + item + ", update=" + update);
            MediaQueueItem playlistItem =
                    mSessionManager.getStatus(item.getSessionId(), item.getItemId());
            if (playlistItem != null) {
                mLastStatusTime = playlistItem.getStatus().getTimestamp();
                mPlayListItems.add(item);
                mPlayListView.invalidate();
            }
            if (update) {
                clearContent();
                updateButtons();
            }
        }

        @Override
        public void pause() {
            Log.d(TAG, "LocalPlayer: pause");
            mSessionManager.pause(mSessionId);
            mPaused = true;
            updateUi();
        }

        @Override
        public void resume() {
            Log.d(TAG, "LocalPlayer: resume");
            mSessionManager.resume(mSessionId);
            mPaused = false;
            updateUi();
        }

        @Override
        public void stop() {
            Log.d(TAG, "LocalPlayer: stop");
            mSessionManager.stop(mSessionId);
            mSessionId = null;
            mPaused = false;
            // For demo purpose, invalidate remote session when local session
            // is stopped (note this is not necessary, remote player could reuse
            // the same session)
            mRemotePlayer.reset();
            updateUi();
        }

        @Override
        public void showStatistics() {
            Log.d(TAG, "LocalPlayer: showStatistics");
            mStatsInfo = null;
            if (isRemotePlayback()) {
                mRemotePlayer.showStatistics();
            }
            updateRouteDescription();
        }

        @Override
        public void onFinish(boolean error) {
            MediaQueueItem item = mSessionManager.finish(error);
            updateUi();
            if (error && item != null) {
                showToast("Failed to play item " + item.getUri());
            }
        }

        // SurfaceHolder.Callback
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format,
                int width, int height) {
            Log.d(TAG, "surfaceChanged "+width+"x"+height);
            mMediaPlayer.setSurface(holder);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            mMediaPlayer.setSurface(holder);
            updateSize(mVideoWidth, mVideoHeight);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
        }

        @Override
        public void updateSize(int width, int height) {
            if (width > 0 && height > 0) {
                if (mPresentation == null) {
                    int surfaceWidth = mLayout.getWidth();
                    int surfaceHeight = mLayout.getHeight();

                    // Calculate the new size of mSurfaceView, so that video is centered
                    // inside the framelayout with proper letterboxing/pillarboxing
                    ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                    if (surfaceWidth * height < surfaceHeight * width) {
                        // Black bars on top&bottom, mSurfaceView has full layout width,
                        // while height is derived from video's aspect ratio
                        lp.width = surfaceWidth;
                        lp.height = surfaceWidth * height / width;
                    } else {
                        // Black bars on left&right, mSurfaceView has full layout height,
                        // while width is derived from video's aspect ratio
                        lp.width = surfaceHeight * width / height;
                        lp.height = surfaceHeight;
                    }
                    Log.d(TAG, "video rect is "+lp.width+"x"+lp.height);
                    mSurfaceView.setLayoutParams(lp);
                } else {
                    mPresentation.updateSize(width, height);
                }
                mVideoWidth = width;
                mVideoHeight = height;
            } else {
                mVideoWidth = mVideoHeight = 0;
            }
        }

        private void updatePresentation() {
            // Get the current route and its presentation display.
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

            // Dismiss the current presentation if the display has changed.
            if (mPresentation != null && mPresentation.getDisplay() != presentationDisplay) {
                Log.i(TAG, "Dismissing presentation because the current route no longer "
                        + "has a presentation display.");
                mPresentation.dismiss();
                mPresentation = null;
            }

            // Show a new presentation if needed.
            if (mPresentation == null && presentationDisplay != null) {
                Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
                mPresentation = new DemoPresentation(
                        SampleMediaRouterActivity.this, presentationDisplay);
                mPresentation.setOnDismissListener(mOnDismissListener);
                try {
                    mPresentation.show();
                } catch (WindowManager.InvalidDisplayException ex) {
                    Log.w(TAG, "Couldn't show presentation!  Display was removed in "
                            + "the meantime.", ex);
                    mPresentation = null;
                }
            }

            if (mPresentation != null || route.supportsControlCategory(
                    MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                mMediaPlayer.setSurface((SurfaceHolder)null);
                mMediaPlayer.reset();
                mSurfaceView.setVisibility(View.GONE);
                mLayout.setVisibility(View.GONE);
            } else {
                mLayout.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        }

        // Listens for when presentations are dismissed.
        private final DialogInterface.OnDismissListener mOnDismissListener =
                new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dialog == mPresentation) {
                    Log.i(TAG, "Presentation was dismissed.");
                    mPresentation = null;
                    updatePresentation();
                }
            }
        };

        private final class DemoPresentation extends Presentation {
            private SurfaceView mPresentationSurfaceView;

            public DemoPresentation(Context context, Display display) {
                super(context, display);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                // Be sure to call the super class.
                super.onCreate(savedInstanceState);

                // Get the resources for the context of the presentation.
                // Notice that we are getting the resources from the context
                // of the presentation.
                Resources r = getContext().getResources();

                // Inflate the layout.
                setContentView(R.layout.sample_media_router_presentation);

                // Set up the surface view.
                mPresentationSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
                SurfaceHolder holder = mPresentationSurfaceView.getHolder();
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                holder.addCallback(LocalPlayer.this);
            }

            public void updateSize(int width, int height) {
                int surfaceHeight = getWindow().getDecorView().getHeight();
                int surfaceWidth = getWindow().getDecorView().getWidth();
                ViewGroup.LayoutParams lp = mPresentationSurfaceView.getLayoutParams();
                if (surfaceWidth * height < surfaceHeight * width) {
                    lp.width = surfaceWidth;
                    lp.height = surfaceWidth * height / width;
                } else {
                    lp.width = surfaceHeight * width / height;
                    lp.height = surfaceHeight;
                }
                Log.d(TAG, "video rect is " + lp.width + "x" + lp.height);
                mPresentationSurfaceView.setLayoutParams(lp);
            }
        }
    }

    private class RemotePlayer implements Player, MediaSessionManager.Callback {
        private MediaQueueItem mQueueItem;
        private MediaQueueItem mPlaylistItem;
        private String mSessionId;
        private String mItemId;
        private long mPosition;

        public void reset() {
            mQueueItem = null;
            mPlaylistItem = null;
            mSessionId = null;
            mItemId = null;
            mPosition = 0;
        }

        // MediaSessionManager.Callback
        @Override
        public void onStart() {
            resume();
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onStop() {
            stop();
        }

        @Override
        public void onSeek(long pos) {
            // If we're currently performing a Play/Enqueue, do not seek
            // until we get the result back (or we may not have valid session
            // and item ids); otherwise do the seek now
            if (mSessionId != null) {
                seek(mSessionId, mItemId, pos);
            }
            // Set current position to seek-to position, actual position will
            // be updated when next getStatus is completed.
            mPosition = pos;
        }

        @Override
        public void onGetStatus(MediaQueueItem item) {
            if (mQueueItem != null) {
                mPlaylistItem = item;
                getStatus(mQueueItem, false);
            }
        }

        @Override
        public void onNewItem(Uri uri) {
            mPosition = 0;
            play(uri, false, 0);
        }

        // Player API
        @Override
        public void enqueue(final Uri uri, long pos) {
            play(uri, true, pos);
        }

        @Override
        public void remove(final MediaQueueItem item) {
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Intent intent = makeRemoveIntent(item);
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        MediaItemStatus status = MediaItemStatus.fromBundle(
                                data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
                        Log.d(TAG, "Remove request succeeded: status=" + status.toString());
                        updateUi();
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Remove request failed: error=" + error + ", data=" + data);
                    }
                };

                Log.d(TAG, "Sending remove request: intent=" + intent);
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Remove request not supported!");
            }
        }

        @Override
        public void seek(String sid, String iid, long pos) {
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Intent intent = makeSeekIntent(sid, iid, pos);
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        MediaItemStatus status = MediaItemStatus.fromBundle(
                                data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
                        Log.d(TAG, "Seek request succeeded: status=" + status.toString());
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Seek request failed: error=" + error + ", data=" + data);
                    }
                };

                Log.d(TAG, "Sending seek request: intent=" + intent);
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Seek request not supported!");
            }
        }

        @Override
        public void getStatus(final MediaQueueItem item, final boolean update) {
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Intent intent = makeGetStatusIntent(item);
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        if (data != null) {
                            String sid = data.getString(MediaControlIntent.EXTRA_SESSION_ID);
                            String iid = data.getString(MediaControlIntent.EXTRA_ITEM_ID);
                            MediaItemStatus status = MediaItemStatus.fromBundle(
                                    data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
                            Log.d(TAG, "GetStatus request succeeded: status=" + status.toString());
                            //showToast("GetStatus request succeeded " + item.mName);
                            if (isRemoteQueue()) {
                                int state = status.getPlaybackState();
                                if (state == MediaItemStatus.PLAYBACK_STATE_PLAYING
                                        || state == MediaItemStatus.PLAYBACK_STATE_PAUSED
                                        || state == MediaItemStatus.PLAYBACK_STATE_PENDING) {
                                    item.setState(state);
                                    item.setContentPosition(status.getContentPosition());
                                    item.setContentDuration(status.getContentDuration());
                                    mLastStatusTime = status.getTimestamp();
                                    mPlayListItems.add(item);
                                    mPlayListView.invalidate();
                                    // update buttons as the queue count might have changed
                                    if (update) {
                                        clearContent();
                                        updateButtons();
                                    }
                                }
                            } else {
                                if (mPlaylistItem != null) {
                                    mPlaylistItem.setContentPosition(status.getContentPosition());
                                    mPlaylistItem.setContentDuration(status.getContentDuration());
                                    mPlaylistItem = null;
                                    updateButtons();
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "GetStatus request failed: error=" + error + ", data=" + data);
                        //showToast("Unable to get status ");
                        if (isRemoteQueue()) {
                            if (update) {
                                clearContent();
                                updateButtons();
                            }
                        }
                    }
                };

                Log.d(TAG, "Sending GetStatus request: intent=" + intent);
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "GetStatus request not supported!");
            }
        }

        @Override
        public void pause() {
            Intent intent = makePauseIntent();
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        Log.d(TAG, "Pause request succeeded");
                        if (isRemoteQueue()) {
                            mPaused = true;
                            updateUi();
                        }
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Pause request failed: error=" + error);
                    }
                };

                Log.d(TAG, "Sending pause request");
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Pause request not supported!");
            }
        }

        @Override
        public void resume() {
            Intent intent = makeResumeIntent();
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        Log.d(TAG, "Resume request succeeded");
                        if (isRemoteQueue()) {
                            mPaused = false;
                            updateUi();
                        }
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Resume request failed: error=" + error);
                    }
                };

                Log.d(TAG, "Sending resume request");
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Resume request not supported!");
            }
        }

        @Override
        public void stop() {
            Intent intent = makeStopIntent();
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        Log.d(TAG, "Stop request succeeded");
                        if (isRemoteQueue()) {
                            // Reset mSessionId, so that next Play/Enqueue
                            // starts a new session
                            mQueueItem = null;
                            mSessionId = null;
                            mPaused = false;
                            updateUi();
                        }
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Stop request failed: error=" + error);
                    }
                };

                Log.d(TAG, "Sending stop request");
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Stop request not supported!");
            }
        }

        @Override
        public void showStatistics() {
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Intent intent = makeStatisticsIntent();
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        Log.d(TAG, "Statistics request succeeded: data=" + data);
                        if (data != null) {
                            int playbackCount = data.getInt(
                                    SampleMediaRouteProvider.DATA_PLAYBACK_COUNT, -1);
                            mStatsInfo = "Total playback count: " + playbackCount;
                        } else {
                            showToast("Statistics query did not return any data");
                        }
                        updateRouteDescription();
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, "Statistics request failed: error=" + error + ", data=" + data);
                        showToast("Unable to query statistics, error: " + error);
                        updateRouteDescription();
                    }
                };

                Log.d(TAG, "Sent statistics request: intent=" + intent);
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, "Statistics request not supported!");
            }

        }

        @Override
        public void onFinish(boolean error) {
            updateUi();
        }

        @Override
        public void updateSize(int width, int height) {
            // nothing to do
        }

        private void play(final Uri uri, boolean enqueue, final long pos) {
            // save the initial seek position
            mPosition = pos;
            MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
            Intent intent = makePlayIntent(uri, enqueue);
            final String request = enqueue ? "Enqueue" : "Play";
            if (route.supportsControlRequest(intent)) {
                MediaRouter.ControlRequestCallback callback =
                        new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(Bundle data) {
                        if (data != null) {
                            String sid = data.getString(MediaControlIntent.EXTRA_SESSION_ID);
                            String iid = data.getString(MediaControlIntent.EXTRA_ITEM_ID);
                            MediaItemStatus status = MediaItemStatus.fromBundle(
                                    data.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
                            Log.d(TAG, request + " request succeeded: data=" + data +
                                    ", sid=" + sid + ", iid=" + iid);

                            // perform delayed initial seek
                            if (mSessionId == null && mPosition > 0) {
                                seek(sid, iid, mPosition);
                            }

                            mSessionId = sid;
                            mItemId = iid;
                            mQueueItem = new MediaQueueItem(sid, iid, null, null);

                            if (isRemoteQueue()) {
                                MediaQueueItem playlistItem =
                                        new MediaQueueItem(sid, iid, uri, null);
                                playlistItem.setState(status.getPlaybackState());
                                mPlayListItems.add(playlistItem);
                                updateUi();
                                enqueuePlaylist();
                            }
                        }
                    }

                    @Override
                    public void onError(String error, Bundle data) {
                        Log.d(TAG, request + " request failed: error=" + error + ", data=" + data);
                        showToast("Unable to " + request + uri + ", error: " + error);
                    }
                };

                Log.d(TAG, "Sending " + request + " request: intent=" + intent);
                route.sendControlRequest(intent, callback);
            } else {
                Log.d(TAG, request + " request not supported!");
            }
        }

        private Intent makePlayIntent(Uri uri, boolean enqueue) {
            Intent intent = new Intent(
                    enqueue ? MediaControlIntent.ACTION_ENQUEUE
                            : MediaControlIntent.ACTION_PLAY);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            intent.setDataAndType(uri, "video/mp4");

            // Provide a valid session id, or none (which starts a new session)
            if (mSessionId != null) {
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
            }

            // PendingIntent for receiving status update from MRP
            Intent statusIntent = new Intent(SampleMediaRouterActivity.ACTION_STATUS_CHANGE);
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER,
                    PendingIntent.getBroadcast(SampleMediaRouterActivity.this,
                            0, statusIntent, 0));

            return intent;
        }

        private Intent makeRemoveIntent(MediaQueueItem item) {
            Intent intent = new Intent(MediaControlIntent.ACTION_REMOVE);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, item.getSessionId());
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, item.getItemId());
            return intent;
        }

        private Intent makeSeekIntent(String sid, String iid, long pos) {
            Intent intent = new Intent(MediaControlIntent.ACTION_SEEK);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, sid);
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, iid);
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, pos);
            return intent;
        }

        private Intent makePauseIntent() {
            Intent intent = new Intent(MediaControlIntent.ACTION_PAUSE);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            if (mSessionId != null) {
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
            }
            return intent;
        }

        private Intent makeResumeIntent() {
            Intent intent = new Intent(MediaControlIntent.ACTION_RESUME);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            if (mSessionId != null) {
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
            }
            return intent;
        }

        private Intent makeStopIntent() {
            Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            if (mSessionId != null) {
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
            }
            return intent;
        }

        private Intent makeGetStatusIntent(MediaQueueItem item) {
            Intent intent = new Intent(MediaControlIntent.ACTION_GET_STATUS);
            intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, item.getSessionId());
            intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, item.getItemId());
            return intent;
         }

        private Intent makeStatisticsIntent() {
            Intent intent = new Intent(SampleMediaRouteProvider.ACTION_GET_STATISTICS);
            intent.addCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE);
            return intent;
        }
    }

    public static final class DiscoveryFragment extends MediaRouteDiscoveryFragment {
        private static final String TAG = "DiscoveryFragment";
        private Callback mCallback;

        public DiscoveryFragment() {
            mCallback = null;
        }

        public DiscoveryFragment(Callback cb) {
            mCallback = cb;
        }

        public void setCallback(Callback cb) {
            mCallback = cb;
        }

        @Override
        public Callback onCreateCallback() {
            return mCallback;
        }

        @Override
        public int onPrepareCallbackFlags() {
            // Add the CALLBACK_FLAG_UNFILTERED_EVENTS flag to ensure that we will
            // observe and log all route events including those that are for routes
            // that do not match our selector.  This is only for demonstration purposes
            // and should not be needed by most applications.
            return super.onPrepareCallbackFlags()
                    | MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS;
        }
    }

    private static final class MediaItem {
        public final String mName;
        public final Uri mUri;

        public MediaItem(String name, Uri uri) {
            mName = name;
            mUri = uri;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private final class LibraryAdapter extends ArrayAdapter<MediaItem> {
        public LibraryAdapter() {
            super(SampleMediaRouterActivity.this, R.layout.media_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = getLayoutInflater().inflate(R.layout.media_item, null);
            } else {
                v = convertView;
            }

            final MediaItem item = getItem(position);

            TextView tv = (TextView)v.findViewById(R.id.item_text);
            tv.setText(item.mName);

            ImageButton b = (ImageButton)v.findViewById(R.id.item_action);
            b.setImageResource(R.drawable.ic_menu_add);
            b.setTag(item);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item != null) {
                        mPlayer.enqueue(item.mUri, 0);
                    }
                }
            });

            return v;
        }
    }

    private final class PlaylistAdapter extends ArrayAdapter<MediaQueueItem> {
        public PlaylistAdapter() {
            super(SampleMediaRouterActivity.this, R.layout.media_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = getLayoutInflater().inflate(R.layout.media_item, null);
            } else {
                v = convertView;
            }

            final MediaQueueItem item = getItem(position);

            TextView tv = (TextView)v.findViewById(R.id.item_text);
            tv.setText(item.toString());

            ImageButton b = (ImageButton)v.findViewById(R.id.item_action);
            b.setImageResource(R.drawable.ic_menu_delete);
            b.setTag(item);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item != null) {
                        mPlayer.remove(item);
                    }
                }
            });

            return v;
        }
    }

    /**
     * Trivial subclass of this activity used to provide another copy of the
     * same activity using a light theme instead of the dark theme.
     */
    public static class Light extends SampleMediaRouterActivity {
    }

    /**
     * Trivial subclass of this activity used to provide another copy of the
     * same activity using a light theme with dark action bar instead of the dark theme.
     */
    public static class LightWithDarkActionBar extends SampleMediaRouterActivity {
    }
}
