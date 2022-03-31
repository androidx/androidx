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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteControllerDialog;
import androidx.mediarouter.app.MediaRouteControllerDialogFragment;
import androidx.mediarouter.app.MediaRouteDialogFactory;
import androidx.mediarouter.app.MediaRouteDiscoveryFragment;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouter.ProviderInfo;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import androidx.mediarouter.media.MediaRouterParams;

import com.example.android.supportv7.R;

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
@SuppressWarnings("deprecation")
public class SampleMediaRouterActivity extends AppCompatActivity {
    private static final String TAG = "SampleMediaRouter";
    private static final String DISCOVERY_FRAGMENT_TAG = "DiscoveryFragment";
    private static final boolean ENABLE_DEFAULT_CONTROL_CHECK_BOX = false;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mSelector;
    private PlaylistAdapter mPlayListItems;
    private TextView mInfoTextView;
    private ListView mPlayListView;
    CheckBox mUseDefaultControlCheckBox;
    private ImageButton mPauseResumeButton;
    private SeekBar mSeekBar;

    final Handler mHandler = new Handler();

    private final Runnable mUpdateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            // update Ui every 1 second
            mHandler.postDelayed(this, 1000);
        }
    };

    final SessionManager mSessionManager = new SessionManager("app");
    Player mPlayer;

    private final MediaRouter.Callback mMediaRouterCB = new MediaRouter.Callback() {
        // Return a custom callback that will simply log all of the route events
        // for demonstration purposes.
        @Override
        public void onRouteAdded(@NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRouteAdded: route=" + route);
        }

        @Override
        public void onRouteChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRouteChanged: route=" + route);
        }

        @Override
        public void onRouteRemoved(@NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRouteRemoved: route=" + route);
        }

        @Override
        public void onRouteSelected(@NonNull MediaRouter router,
                @NonNull RouteInfo selectedRoute, int reason, @NonNull RouteInfo requestedRoute) {
            Log.d(TAG, "onRouteSelected: requestedRoute=" + requestedRoute
                    + ", route=" + selectedRoute + ", reason=" + reason);

            mPlayer = Player.create(SampleMediaRouterActivity.this, selectedRoute, mMediaSession);
            if (isPresentationApiSupported()) {
                mPlayer.updatePresentation();
            }
            mSessionManager.setPlayer(mPlayer);
            if (reason != MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                mSessionManager.stop();
            } else {
                mSessionManager.unsuspend();
            }

            updateUi();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onRouteUnselected(@NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            mMediaSession.setActive(false);

            PlaylistItem item = getCheckedPlaylistItem();
            if (item != null) {
                long pos = item.getPosition() + (mSessionManager.isPaused() ?
                        0 : (SystemClock.elapsedRealtime() - item.getTimestamp()));
                mSessionManager.suspend(pos);
            }
            if (isPresentationApiSupported()) {
                mPlayer.updatePresentation();
            }
            mPlayer.release();
        }

        @Override
        public void onRouteVolumeChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRouteVolumeChanged: route=" + route);
        }

        @Override
        public void onRoutePresentationDisplayChanged(
                @NonNull MediaRouter router, @NonNull RouteInfo route) {
            Log.d(TAG, "onRoutePresentationDisplayChanged: route=" + route);
            if (isPresentationApiSupported()) {
                mPlayer.updatePresentation();
            }
        }

        @Override
        public void onProviderAdded(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderAdded: provider=" + provider);
        }

        @Override
        public void onProviderRemoved(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderRemoved: provider=" + provider);
        }

        @Override
        public void onProviderChanged(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
            Log.d(TAG, "onRouteProviderChanged: provider=" + provider);
        }

        private boolean isPresentationApiSupported() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
        }
    };

    MediaSessionCompat mMediaSession;
    private ComponentName mEventReceiver;
    private PendingIntent mMediaPendingIntent;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Need overlay permission for emulating remote display.
        if (Build.VERSION.SDK_INT >= 23 && !Api23Impl.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
        }

        // Get the media router service.
        mMediaRouter = MediaRouter.getInstance(this);

        mMediaRouter.setRouterParams(getRouterParams());

        // Create a route selector for the type of routes that we care about.
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE)
                .build();

        mMediaRouter.setOnPrepareTransferListener((fromRoute, toRoute) -> {
            Log.d(TAG, "onPrepareTransfer: from=" + fromRoute.getId()
                    + ", to=" + toRoute.getId());
            return CallbackToFutureAdapter.getFuture(completer -> {
                mHandler.postDelayed(() -> completer.set(null), 3000);
                return "onPrepareTransfer";
            });
        });

        // Add a fragment to take care of media route discovery.
        // This fragment automatically adds or removes a callback whenever the activity
        // is started or stopped.
        FragmentManager fm = getSupportFragmentManager();
        DiscoveryFragment fragment = (DiscoveryFragment) fm.findFragmentByTag(
                DISCOVERY_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new DiscoveryFragment();
            fm.beginTransaction()
                    .add(fragment, DISCOVERY_FRAGMENT_TAG)
                    .commit();
        }
        fragment.setCallback(mMediaRouterCB);
        fragment.setRouteSelector(mSelector);

        // Populate an array adapter with streaming media items.
        String[] mediaNames = getResources().getStringArray(R.array.media_names);
        String[] mediaUris = getResources().getStringArray(R.array.media_uris);
        String[] mediaMimes = getResources().getStringArray(R.array.media_mimes);
        LibraryAdapter libraryItems = new LibraryAdapter();
        for (int i = 0; i < mediaNames.length; i++) {
            libraryItems.add(new MediaItem(
                    "[streaming] " + mediaNames[i], Uri.parse(mediaUris[i]), mediaMimes[i]));
        }

        // Scan local external storage directory for media files.
        File externalDir = Environment.getExternalStorageDirectory();
        if (externalDir != null) {
            File[] list = externalDir.listFiles();
            if (list != null) {
                for (File file : list) {
                    String filename = file.getName();
                    if (filename.matches(".*\\.(m4v|mp4)")) {
                        libraryItems.add(new MediaItem("[local] " + filename,
                                Uri.fromFile(file), "video/mp4"));
                    }
                }
            }
        }

        mPlayListItems = new PlaylistAdapter();

        // Initialize the layout.
        setContentView(R.layout.sample_media_router);

        TabHost tabHost = findViewById(R.id.tabHost);
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
        tabHost.setOnTabChangedListener(arg0 -> updateUi());

        ListView libraryView = findViewById(R.id.media);
        libraryView.setAdapter(libraryItems);
        libraryView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        libraryView.setOnItemClickListener((parent, view, position, id) -> updateButtons());

        mPlayListView = findViewById(R.id.playlist);
        mPlayListView.setAdapter(mPlayListItems);
        mPlayListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mPlayListView.setOnItemClickListener((parent, view, position, id) -> updateButtons());

        mInfoTextView = findViewById(R.id.info);

        mUseDefaultControlCheckBox = findViewById(R.id.custom_control_view_checkbox);
        if (ENABLE_DEFAULT_CONTROL_CHECK_BOX) {
            mUseDefaultControlCheckBox.setVisibility(View.VISIBLE);
        }
        mPauseResumeButton = findViewById(R.id.pause_resume_button);
        mPauseResumeButton.setOnClickListener(v -> {
            if (mSessionManager.isPaused()) {
                mSessionManager.resume();
            } else {
                mSessionManager.pause();
            }
        });

        ImageButton stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(v -> mSessionManager.stop());

        mSeekBar = findViewById(R.id.seekbar);
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
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateUi();
            }
        });

        // Schedule Ui update
        mHandler.postDelayed(mUpdateSeekRunnable, 1000);

        // Build the PendingIntent for the remote control client
        mEventReceiver = new ComponentName(getPackageName(),
                SampleMediaButtonReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mEventReceiver);
        mMediaPendingIntent = PendingIntent.getBroadcast(this, /* requestCode = */0,
            mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create and register the remote control client
        createMediaSession();
        mMediaRouter.setMediaSessionCompat(mMediaSession);

        // Set up playback manager and player
        mPlayer = Player.create(SampleMediaRouterActivity.this,
                mMediaRouter.getSelectedRoute(), mMediaSession);

        mSessionManager.setPlayer(mPlayer);
        mSessionManager.setCallback(new SessionManager.Callback() {
            @Override
            public void onStatusChanged() {
                updateUi();
            }

            @Override
            public void onItemChanged(PlaylistItem item) { }
        });

        updateUi();
    }

    private void createMediaSession() {
        // Create the MediaSession
        mMediaSession = new MediaSessionCompat(this, "SampleMediaRouter", mEventReceiver,
                mMediaPendingIntent);
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                if (mediaButtonEvent != null) {
                    return handleMediaKey(
                            mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
                }
                return super.onMediaButtonEvent(null);
            }

            @Override
            public void onPlay() {
                mSessionManager.resume();
            }

            @Override
            public void onPause() {
                mSessionManager.pause();
            }
        });

        SampleMediaButtonReceiver.setActivity(SampleMediaRouterActivity.this);
    }

    /**
     * Handle media key events.
     */
    public boolean handleMediaKey(@Nullable KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
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
    public boolean onKeyDown(int keyCode, @Nullable KeyEvent event) {
        return handleMediaKey(event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @Nullable KeyEvent event) {
        return handleMediaKey(event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public void onStart() {
        // Be sure to call the super class.
        super.onStart();
    }

    @Override
    public void onDestroy() {
        mSessionManager.stop();
        mPlayer.release();
        mMediaSession.release();
        mMediaRouter.removeCallback(mMediaRouterCB);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Be sure to call the super class.
        super.onCreateOptionsMenu(menu);

        // Inflate the menu and configure the media router action provider.
        getMenuInflater().inflate(R.menu.sample_media_router_menu, menu);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        if (mediaRouteActionProvider != null) {
            mediaRouteActionProvider.setRouteSelector(mSelector);
            mediaRouteActionProvider.setDialogFactory(new MediaRouteDialogFactory() {
                @NonNull
                @Override
                public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                    return new ControllerDialogFragment(SampleMediaRouterActivity.this,
                        mUseDefaultControlCheckBox);
                }
            });
        }

        // Return true to show the menu.
        return true;
    }

    void updateStatusFromSessionManager() {
        if (mPlayer != null && mSessionManager != null) {
            mSessionManager.updateStatus();
        }
    }

    void updateProgress() {
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

    void updateUi() {
        updatePlaylist();
        updateRouteDescription();
        updateButtons();
        if (mPlayer != null && mSessionManager != null) {
            PlaylistItem currentItem = mSessionManager.getCurrentItem();
            if (currentItem != null) {
                mPlayer.updateMetadata(currentItem);
                int currentItemState = Player.STATE_IDLE;
                switch(currentItem.getState()) {
                    case MediaItemStatus.PLAYBACK_STATE_PLAYING:
                        currentItemState = Player.STATE_PLAYING;
                        break;
                    case MediaItemStatus.PLAYBACK_STATE_PAUSED:
                        currentItemState = Player.STATE_PAUSED;
                        break;
                    case MediaItemStatus.PLAYBACK_STATE_PENDING:
                    case MediaItemStatus.PLAYBACK_STATE_BUFFERING:
                        currentItemState = Player.STATE_PREPARING_FOR_PLAY;
                        break;
                }
                mPlayer.publishState(currentItemState);
            }
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
                + "\nProvider: " + route.getProvider().getPackageName());
    }

    private void updateButtons() {
        // show pause or resume icon depending on current state
        mPauseResumeButton.setImageResource(mSessionManager.isPaused() ?
                R.drawable.ic_media_play : R.drawable.ic_media_pause);
        // only enable seek bar when duration is known
        PlaylistItem item = getCheckedPlaylistItem();
        mSeekBar.setEnabled(item != null && item.getDuration() > 0);
    }

    PlaylistItem getCheckedPlaylistItem() {
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

    @NonNull
    public MediaRouterParams getRouterParams() {
        return new MediaRouterParams.Builder()
                .setDialogType(MediaRouterParams.DIALOG_TYPE_DEFAULT)
                .setTransferToLocalEnabled(true) // Phone speaker will be shown when casting.
                .build();
    }

    /**
     * Media route discovery fragment.
     */
    public static final class DiscoveryFragment extends MediaRouteDiscoveryFragment {
        private MediaRouter.Callback mCallback;

        public void setCallback(@Nullable MediaRouter.Callback cb) {
            mCallback = cb;
        }

        @Nullable
        @Override
        public MediaRouter.Callback onCreateCallback() {
            return mCallback;
        }

        @Override
        public int onPrepareCallbackFlags() {
            return super.onPrepareCallbackFlags();
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

        @NonNull
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

            TextView tv = v.findViewById(R.id.item_text);
            tv.setText(item.mName);

            ImageButton b = v.findViewById(R.id.item_action);
            b.setImageResource(R.drawable.ic_menu_add);
            b.setTag(item);
            b.setOnClickListener(v1 -> mSessionManager.add(item.mName, item.mUri, item.mMime));

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

            TextView tv = v.findViewById(R.id.item_text);
            tv.setText(item.toString());

            ImageButton b = v.findViewById(R.id.item_action);
            b.setImageResource(R.drawable.ic_menu_delete);
            b.setTag(item);
            b.setOnClickListener(v1 -> mSessionManager.remove(item.getItemId()));

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

    /**
     * This will show dynamic group dialog when ther user clicks the media route button.
     */
    public static class DynamicGroupActivity extends SampleMediaRouterActivity {
        @NonNull
        @Override
        public MediaRouterParams getRouterParams() {
            return new MediaRouterParams.Builder(super.getRouterParams())
                    .setDialogType(MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP)
                    .build();
        }
    }

    /**
     * This pops up the output switcher if run on Android R+
     */
    public static class OutputSwitcherActivity extends SampleMediaRouterActivity {
        @NonNull
        @Override
        public MediaRouterParams getRouterParams() {
            return new MediaRouterParams.Builder(super.getRouterParams())
                    .setOutputSwitcherEnabled(true)
                    .build();
        }
    }

    /**
     * It doesn't use MediaRouter2
     */
    public static class LegacyMediaRouterActivity extends SampleMediaRouterActivity {
        @NonNull
        @Override
        public MediaRouterParams getRouterParams() {
            return new MediaRouterParams.Builder(super.getRouterParams())
                    .setMediaTransferReceiverEnabled(false)
                    .build();
        }
    }

    public static class ControllerDialogFragment extends MediaRouteControllerDialogFragment {
        private SampleMediaRouterActivity mSampleMediaRouterActivity;
        private MediaRouteControllerDialog mControllerDialog;
        private CheckBox mUseDefaultControlCheckBox;

        public ControllerDialogFragment() {
            super();
        }

        public ControllerDialogFragment(@NonNull SampleMediaRouterActivity activity,
                @Nullable CheckBox customControlViewCheckBox) {
            mSampleMediaRouterActivity = activity;
            mUseDefaultControlCheckBox = customControlViewCheckBox;
        }

        @NonNull
        @Override
        public MediaRouteControllerDialog onCreateControllerDialog(
                @NonNull Context context, @Nullable Bundle savedInstanceState) {
            mSampleMediaRouterActivity.updateStatusFromSessionManager();
            mControllerDialog =
                    mUseDefaultControlCheckBox != null && mUseDefaultControlCheckBox.isChecked()
                    ? super.onCreateControllerDialog(context, savedInstanceState)
                    : new MyMediaRouteControllerDialog(context);
            mControllerDialog.setOnDismissListener(dialog -> mControllerDialog = null);
            return mControllerDialog;
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean canDrawOverlays(Context context) {
            return Settings.canDrawOverlays(context);
        }
    }
}
