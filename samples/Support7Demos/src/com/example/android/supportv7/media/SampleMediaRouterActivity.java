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
import android.content.res.Resources;
import android.app.PendingIntent;
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
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.app.MediaRouteControllerDialogFragment;
import android.support.v7.app.MediaRouteDiscoveryFragment;
import android.support.v7.app.MediaRouteDialogFactory;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaRouter.ProviderInfo;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaItemStatus;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
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
    private static final String TAG = "SampleMediaRouterActivity";
    private static final String DISCOVERY_FRAGMENT_TAG = "DiscoveryFragment";

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
    private boolean mNeedResume;
    private boolean mSeeking;
    private SampleMediaRouteControllerDialog mControllerDialog;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            // update Ui every 1 second
            mHandler.postDelayed(this, 1000);
        }
    };

    private final SessionManager mSessionManager = new SessionManager("app");
    private Player mPlayer;

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
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteRemoved: route=" + route);
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: route=" + route);

            mPlayer = Player.create(SampleMediaRouterActivity.this, route);
            mPlayer.updatePresentation();
            mSessionManager.setPlayer(mPlayer);
            mSessionManager.unsuspend();

            registerRCC();
            updateUi();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            unregisterRCC();

            PlaylistItem item = getCheckedPlaylistItem();
            if (item != null) {
                long pos = item.getPosition() + (mSessionManager.isPaused() ?
                        0 : (SystemClock.elapsedRealtime() - item.getTimestamp()));
                mSessionManager.suspend(pos);
            }
            mPlayer.updatePresentation();
            mPlayer.release();
            mControllerDialog = null;
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteVolumeChanged: route=" + route);
        }

        @Override
        public void onRoutePresentationDisplayChanged(
                MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRoutePresentationDisplayChanged: route=" + route);
            mPlayer.updatePresentation();
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
                    "[streaming] "+mediaNames[i], Uri.parse(mediaUris[i]), "video/mp4"));
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
                                Uri.fromFile(list[i]), "video/mp4"));
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

        tabName = getResources().getString(R.string.info_tab_text);
        TabSpec spec3=tabHost.newTabSpec(tabName);
        spec3.setIndicator(tabName);
        spec3.setContent(R.id.tab3);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        tabHost.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String arg0) {
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
                if (mSessionManager.isPaused()) {
                    mSessionManager.resume();
                } else {
                    mSessionManager.pause();
                }
            }
        });

        mStopButton = (ImageButton)findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSessionManager.stop();
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PlaylistItem item = getCheckedPlaylistItem();
                if (fromUser && item != null && item.getDuration() > 0) {
                    long pos = progress * item.getDuration() / 100;
                    mSessionManager.seek(item.getItemId(), pos);
                    item.setPosition(pos);
                    item.setTimestamp(SystemClock.elapsedRealtime());
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

        // Build the PendingIntent for the remote control client
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mEventReceiver = new ComponentName(getPackageName(),
                SampleMediaButtonReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mEventReceiver);
        mMediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        // Create and register the remote control client
        registerRCC();

        // Set up playback manager and player
        mPlayer = Player.create(SampleMediaRouterActivity.this,
                mMediaRouter.getSelectedRoute());
        mSessionManager.setPlayer(mPlayer);
        mSessionManager.setCallback(new SessionManager.Callback() {
            @Override
            public void onStatusChanged() {
                updateUi();
            }

            @Override
            public void onItemChanged(PlaylistItem item) {
            }
        });

        updateUi();
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
                    if (mSessionManager.isPaused()) {
                        mSessionManager.resume();
                    } else {
                        mSessionManager.pause();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                {
                    Log.d(TAG, "Received Play event from RemoteControlClient");
                    if (mSessionManager.isPaused()) {
                        mSessionManager.resume();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                {
                    Log.d(TAG, "Received Pause event from RemoteControlClient");
                    if (!mSessionManager.isPaused()) {
                        mSessionManager.pause();
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_MEDIA_STOP:
                {
                    Log.d(TAG, "Received Stop event from RemoteControlClient");
                    mSessionManager.stop();
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
    }

    @Override
    public void onPause() {
        // pause media player for local playback case only
        if (!mPlayer.isRemotePlayback() && !mSessionManager.isPaused()) {
            mNeedResume = true;
            mSessionManager.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        // resume media player for local playback case only
        if (!mPlayer.isRemotePlayback() && mNeedResume) {
            mSessionManager.resume();
            mNeedResume = false;
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        // Unregister the remote control client
        unregisterRCC();

        mSessionManager.stop();
        mPlayer.release();
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
        mediaRouteActionProvider.setDialogFactory(new MediaRouteDialogFactory() {
            @Override
            public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                return new MediaRouteControllerDialogFragment() {
                    @Override
                    public MediaRouteControllerDialog onCreateControllerDialog(
                            Context context, Bundle savedInstanceState) {
                        mControllerDialog = new SampleMediaRouteControllerDialog(
                                context, mSessionManager, mPlayer);
                        return mControllerDialog;
                    }
                };
            }
        });

        // Return true to show the menu.
        return true;
    }

    private void updateProgress() {
        // Estimate content position from last status time and elapsed time.
        // (Note this might be slightly out of sync with remote side, however
        // it avoids frequent polling the MRP.)
        int progress = 0;
        PlaylistItem item = getCheckedPlaylistItem();
        if (item != null) {
            int state = item.getState();
            long duration = item.getDuration();
            if (duration <= 0) {
                if (state == MediaItemStatus.PLAYBACK_STATE_PLAYING
                        || state == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
                    mSessionManager.updateStatus();
                }
            } else {
                long position = item.getPosition();
                long timeDelta = mSessionManager.isPaused() ? 0 :
                        (SystemClock.elapsedRealtime() - item.getTimestamp());
                progress = (int)(100.0 * (position + timeDelta) / duration);
            }
        }
        mSeekBar.setProgress(progress);
    }

    private void updateUi() {
        updatePlaylist();
        updateRouteDescription();
        updateButtons();
        if (mControllerDialog != null) {
            mControllerDialog.updateUi();
        }
    }

    private void updatePlaylist() {
        mPlayListItems.clear();
        for (PlaylistItem item : mSessionManager.getPlaylist()) {
            mPlayListItems.add(item);
        }
        mPlayListView.invalidate();
    }

    private void updateRouteDescription() {
        RouteInfo route = mMediaRouter.getSelectedRoute();
        mInfoTextView.setText("Currently selected route:"
                + "\nName: " + route.getName()
                + "\nProvider: " + route.getProvider().getPackageName()
                + "\nDescription: " + route.getDescription());
    }

    private void updateButtons() {
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
        // show pause or resume icon depending on current state
        mPauseResumeButton.setImageResource(mSessionManager.isPaused() ?
                R.drawable.ic_media_play : R.drawable.ic_media_pause);
        // only enable seek bar when duration is known
        PlaylistItem item = getCheckedPlaylistItem();
        mSeekBar.setEnabled(item != null && item.getDuration() > 0);
        if (mRemoteControlClient != null) {
            mRemoteControlClient.setPlaybackState(mSessionManager.isPaused() ?
                    RemoteControlClient.PLAYSTATE_PAUSED :
                        RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    private PlaylistItem getCheckedPlaylistItem() {
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
        public final String mMime;

        public MediaItem(String name, Uri uri, String mime) {
            mName = name;
            mUri = uri;
            mMime = mime;
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
                        mSessionManager.add(item.mUri, item.mMime);
                    }
                }
            });

            return v;
        }
    }

    private final class PlaylistAdapter extends ArrayAdapter<PlaylistItem> {
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

            final PlaylistItem item = getItem(position);

            TextView tv = (TextView)v.findViewById(R.id.item_text);
            tv.setText(item.toString());

            ImageButton b = (ImageButton)v.findViewById(R.id.item_action);
            b.setImageResource(R.drawable.ic_menu_delete);
            b.setTag(item);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item != null) {
                        mSessionManager.remove(item.getItemId());
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
