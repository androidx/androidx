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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.IntentSender;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;
import androidx.mediarouter.media.MediaSessionStatus;

import com.example.android.supportv7.R;

import java.util.ArrayList;

/**
 * Demonstrates how to create a custom media route provider.
 *
 * @see SampleMediaRouteProviderService
 */
final class SampleMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "SampleMediaRouteProvider";

    private static final String FIXED_VOLUME_ROUTE_ID = "fixed";
    private static final String VARIABLE_VOLUME_BASIC_ROUTE_ID = "variable_basic";
    private static final String VARIABLE_VOLUME_QUEUING_ROUTE_ID = "variable_queuing";
    private static final String VARIABLE_VOLUME_SESSION_ROUTE_ID = "variable_session";

    private static final int VOLUME_MAX = 10;

    /**
     * A custom media control intent category for special requests that are
     * supported by this provider's routes.
     */
    public static final String CATEGORY_SAMPLE_ROUTE =
            "com.example.android.supportv7.media.CATEGORY_SAMPLE_ROUTE";

    /**
     * A custom media control intent action to take a snapshot.
     * </p>
     *
     * @see #EXTRA_SNAPSHOT
     */
    public static final String ACTION_TAKE_SNAPSHOT =
            "com.example.android.supportv7.media.action.TAKE_SNAPSHOT";

    /**
     * {@link #ACTION_TAKE_SNAPSHOT} result data: a bitmap containing a snapshot
     * of the currently playing media item
     */
    public static final String EXTRA_SNAPSHOT =
            "com.example.android.supportv7.media.extra.SNAPSHOT";

    private static final ArrayList<IntentFilter> CONTROL_FILTERS_BASIC;
    private static final ArrayList<IntentFilter> CONTROL_FILTERS_QUEUING;
    private static final ArrayList<IntentFilter> CONTROL_FILTERS_SESSION;

    static {
        IntentFilter f1 = new IntentFilter();
        f1.addCategory(CATEGORY_SAMPLE_ROUTE);
        f1.addAction(ACTION_TAKE_SNAPSHOT);

        IntentFilter f2 = new IntentFilter();
        f2.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f2.addAction(MediaControlIntent.ACTION_PLAY);
        f2.addDataScheme("http");
        f2.addDataScheme("https");
        f2.addDataScheme("rtsp");
        f2.addDataScheme("file");
        addDataTypeUnchecked(f2, "video/*");

        IntentFilter f3 = new IntentFilter();
        f3.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f3.addAction(MediaControlIntent.ACTION_SEEK);
        f3.addAction(MediaControlIntent.ACTION_GET_STATUS);
        f3.addAction(MediaControlIntent.ACTION_PAUSE);
        f3.addAction(MediaControlIntent.ACTION_RESUME);
        f3.addAction(MediaControlIntent.ACTION_STOP);

        IntentFilter f4 = new IntentFilter();
        f4.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f4.addAction(MediaControlIntent.ACTION_ENQUEUE);
        f4.addDataScheme("http");
        f4.addDataScheme("https");
        f4.addDataScheme("rtsp");
        f4.addDataScheme("file");
        addDataTypeUnchecked(f4, "video/*");

        IntentFilter f5 = new IntentFilter();
        f5.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f5.addAction(MediaControlIntent.ACTION_REMOVE);

        IntentFilter f6 = new IntentFilter();
        f6.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f6.addAction(MediaControlIntent.ACTION_START_SESSION);
        f6.addAction(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        f6.addAction(MediaControlIntent.ACTION_END_SESSION);

        CONTROL_FILTERS_BASIC = new ArrayList<>();
        CONTROL_FILTERS_BASIC.add(f1);
        CONTROL_FILTERS_BASIC.add(f2);
        CONTROL_FILTERS_BASIC.add(f3);

        CONTROL_FILTERS_QUEUING = new ArrayList<>(CONTROL_FILTERS_BASIC);
        CONTROL_FILTERS_QUEUING.add(f4);
        CONTROL_FILTERS_QUEUING.add(f5);

        CONTROL_FILTERS_SESSION = new ArrayList<>(CONTROL_FILTERS_QUEUING);
        CONTROL_FILTERS_SESSION.add(f6);
    }

    private static void addDataTypeUnchecked(IntentFilter filter, String type) {
        try {
            filter.addDataType(type);
        } catch (MalformedMimeTypeException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int mVolume = 5;

    public SampleMediaRouteProvider(Context context) {
        super(context);

        publishRoutes();
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        return new SampleRouteController(routeId);
    }

    private void publishRoutes() {
        Resources r = getContext().getResources();
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN);
        settingsIntent.setClass(getContext(), SampleMediaRouteSettingsActivity.class);
        IntentSender is = PendingIntent.getActivity(getContext(), 99, settingsIntent,
                Intent.FLAG_ACTIVITY_NEW_TASK).getIntentSender();

        MediaRouteDescriptor routeDescriptor1 = new MediaRouteDescriptor.Builder(
                FIXED_VOLUME_ROUTE_ID,
                r.getString(R.string.fixed_volume_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolume(VOLUME_MAX)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor2 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_BASIC_ROUTE_ID,
                r.getString(R.string.variable_volume_basic_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(mVolume)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor3 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_QUEUING_ROUTE_ID,
                r.getString(R.string.variable_volume_queuing_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_QUEUING)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(mVolume)
                .setCanDisconnect(true)
                .build();

        Uri iconUri = Uri.parse("android.resource://com.example.android.supportv7/"
                + R.drawable.ic_android);
        MediaRouteDescriptor routeDescriptor4 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_SESSION_ROUTE_ID,
                r.getString(R.string.variable_volume_session_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_SESSION)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(mVolume)
                .setIconUri(iconUri)
                .build();

        MediaRouteProviderDescriptor providerDescriptor = new MediaRouteProviderDescriptor.Builder()
                .addRoute(routeDescriptor1)
                .addRoute(routeDescriptor2)
                .addRoute(routeDescriptor3)
                .addRoute(routeDescriptor4)
                .build();
        setDescriptor(providerDescriptor);
    }

    private final class SampleRouteController extends MediaRouteProvider.RouteController {
        private final String mRouteId;
        private final SessionManager mSessionManager = new SessionManager("mrp");
        private final Player mPlayer;
        private PendingIntent mSessionReceiver;

        public SampleRouteController(String routeId) {
            mRouteId = routeId;
            mPlayer = Player.create(getContext(), null, null);
            mSessionManager.setPlayer(mPlayer);
            mSessionManager.setCallback(new SessionManager.Callback() {
                @Override
                public void onStatusChanged() {
                }

                @Override
                public void onItemChanged(PlaylistItem item) {
                    handleStatusChange(item);
                }
            });
            setVolumeInternal(mVolume);
            Log.d(TAG, mRouteId + ": Controller created");
        }

        @Override
        public void onRelease() {
            Log.d(TAG, mRouteId + ": Controller released");
            mPlayer.release();
        }

        @Override
        public void onSelect() {
            Log.d(TAG, mRouteId + ": Selected");
            mPlayer.connect(null);
        }

        @Override
        public void onUnselect() {
            Log.d(TAG, mRouteId + ": Unselected");
            mPlayer.release();
        }

        @Override
        public void onSetVolume(int volume) {
            Log.d(TAG, mRouteId + ": Set volume to " + volume);
            if (!mRouteId.equals(FIXED_VOLUME_ROUTE_ID)) {
                setVolumeInternal(volume);
            }
        }

        @Override
        public void onUpdateVolume(int delta) {
            Log.d(TAG, mRouteId + ": Update volume by " + delta);
            if (!mRouteId.equals(FIXED_VOLUME_ROUTE_ID)) {
                setVolumeInternal(mVolume + delta);
            }
        }

        @Override
        public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
            Log.d(TAG, mRouteId + ": Received control request " + intent);
            String action = intent.getAction();
            if (intent.hasCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                boolean success = false;
                if (action.equals(MediaControlIntent.ACTION_PLAY)) {
                    success = handlePlay(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_ENQUEUE)) {
                    success = handleEnqueue(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_REMOVE)) {
                    success = handleRemove(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_SEEK)) {
                    success = handleSeek(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_GET_STATUS)) {
                    success = handleGetStatus(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_PAUSE)) {
                    success = handlePause(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_RESUME)) {
                    success = handleResume(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_STOP)) {
                    success = handleStop(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_START_SESSION)) {
                    success = handleStartSession(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_GET_SESSION_STATUS)) {
                    success = handleGetSessionStatus(intent, callback);
                } else if (action.equals(MediaControlIntent.ACTION_END_SESSION)) {
                    success = handleEndSession(intent, callback);
                }
                Log.d(TAG, mSessionManager.toString());
                return success;
            }

            if (callback != null && action.equals(ACTION_TAKE_SNAPSHOT)
                    && intent.hasCategory(CATEGORY_SAMPLE_ROUTE)) {
                if (mSessionManager.getCurrentItem() != null) {
                    Bundle data = new Bundle();
                    data.putParcelable(EXTRA_SNAPSHOT, mPlayer.getSnapshot());
                    callback.onResult(data);
                } else {
                    callback.onError("Failed to take a snapshot", null);
                }
                return true;
            }
            return false;
        }

        private void setVolumeInternal(int volume) {
            if (volume >= 0 && volume <= VOLUME_MAX) {
                mVolume = volume;
                Log.d(TAG, mRouteId + ": New volume is " + mVolume);
                AudioManager audioManager =
                        (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                publishRoutes();
            }
        }

        private boolean handlePlay(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            if (sid != null && !sid.equals(mSessionManager.getSessionId())) {
                Log.d(TAG, "handlePlay fails because of bad sid="+sid);
                return false;
            }
            if (mSessionManager.hasSession()) {
                mSessionManager.stop();
            }
            return handleEnqueue(intent, callback);
        }

        private boolean handleEnqueue(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            if (sid != null && !sid.equals(mSessionManager.getSessionId())) {
                Log.d(TAG, "handleEnqueue fails because of bad sid="+sid);
                return false;
            }

            Uri uri = intent.getData();
            if (uri == null) {
                Log.d(TAG, "handleEnqueue fails because of bad uri="+uri);
                return false;
            }

            boolean enqueue = intent.getAction().equals(MediaControlIntent.ACTION_ENQUEUE);
            String mime = intent.getType();
            long pos = intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0);
            Bundle metadata = intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_METADATA);
            Bundle headers = intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_HTTP_HEADERS);
            PendingIntent receiver = (PendingIntent)intent.getParcelableExtra(
                    MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER);

            Log.d(TAG, mRouteId + ": Received " + (enqueue?"enqueue":"play") + " request"
                    + ", uri=" + uri
                    + ", mime=" + mime
                    + ", sid=" + sid
                    + ", pos=" + pos
                    + ", metadata=" + metadata
                    + ", headers=" + headers
                    + ", receiver=" + receiver);
            PlaylistItem item = mSessionManager.add(null, uri, mime, receiver);
            if (callback != null) {
                if (item != null) {
                    Bundle result = new Bundle();
                    result.putString(MediaControlIntent.EXTRA_SESSION_ID, item.getSessionId());
                    result.putString(MediaControlIntent.EXTRA_ITEM_ID, item.getItemId());
                    result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS,
                            item.getStatus().asBundle());
                    callback.onResult(result);
                } else {
                    callback.onError("Failed to open " + uri.toString(), null);
                }
            }
            return true;
        }

        private boolean handleRemove(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            if (sid == null || !sid.equals(mSessionManager.getSessionId())) {
                return false;
            }

            String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
            PlaylistItem item = mSessionManager.remove(iid);
            if (callback != null) {
                if (item != null) {
                    Bundle result = new Bundle();
                    result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS,
                            item.getStatus().asBundle());
                    callback.onResult(result);
                } else {
                    callback.onError("Failed to remove" +
                            ", sid=" + sid + ", iid=" + iid, null);
                }
            }
            return (item != null);
        }

        private boolean handleSeek(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            if (sid == null || !sid.equals(mSessionManager.getSessionId())) {
                return false;
            }

            String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
            long pos = intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0);
            Log.d(TAG, mRouteId + ": Received seek request, pos=" + pos);
            PlaylistItem item = mSessionManager.seek(iid, pos);
            if (callback != null) {
                if (item != null) {
                    Bundle result = new Bundle();
                    result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS,
                            item.getStatus().asBundle());
                    callback.onResult(result);
                } else {
                    callback.onError("Failed to seek" +
                            ", sid=" + sid + ", iid=" + iid + ", pos=" + pos, null);
                }
            }
            return (item != null);
        }

        private boolean handleGetStatus(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
            Log.d(TAG, mRouteId + ": Received getStatus request, sid=" + sid + ", iid=" + iid);
            PlaylistItem item = mSessionManager.getStatus(iid);
            if (callback != null) {
                if (item != null) {
                    Bundle result = new Bundle();
                    result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS,
                            item.getStatus().asBundle());
                    callback.onResult(result);
                } else {
                    callback.onError("Failed to get status" +
                            ", sid=" + sid + ", iid=" + iid, null);
                }
            }
            return (item != null);
        }

        private boolean handlePause(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = (sid != null) && sid.equals(mSessionManager.getSessionId());
            mSessionManager.pause();
            if (callback != null) {
                if (success) {
                    callback.onResult(new Bundle());
                    handleSessionStatusChange(sid);
                } else {
                    callback.onError("Failed to pause, sid=" + sid, null);
                }
            }
            return success;
        }

        private boolean handleResume(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = (sid != null) && sid.equals(mSessionManager.getSessionId());
            mSessionManager.resume();
            if (callback != null) {
                if (success) {
                    callback.onResult(new Bundle());
                    handleSessionStatusChange(sid);
                } else {
                    callback.onError("Failed to resume, sid=" + sid, null);
                }
            }
            return success;
        }

        private boolean handleStop(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = (sid != null) && sid.equals(mSessionManager.getSessionId());
            mSessionManager.stop();
            if (callback != null) {
                if (success) {
                    callback.onResult(new Bundle());
                    handleSessionStatusChange(sid);
                } else {
                    callback.onError("Failed to stop, sid=" + sid, null);
                }
            }
            return success;
        }

        private boolean handleStartSession(Intent intent, ControlRequestCallback callback) {
            String sid = mSessionManager.startSession();
            Log.d(TAG, "StartSession returns sessionId "+sid);
            if (callback != null) {
                if (sid != null) {
                    Bundle result = new Bundle();
                    result.putString(MediaControlIntent.EXTRA_SESSION_ID, sid);
                    result.putBundle(MediaControlIntent.EXTRA_SESSION_STATUS,
                            mSessionManager.getSessionStatus(sid).asBundle());
                    callback.onResult(result);
                    mSessionReceiver = (PendingIntent)intent.getParcelableExtra(
                            MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER);
                    handleSessionStatusChange(sid);
                } else {
                    callback.onError("Failed to start session.", null);
                }
            }
            return (sid != null);
        }

        private boolean handleGetSessionStatus(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);

            MediaSessionStatus sessionStatus = mSessionManager.getSessionStatus(sid);
            if (callback != null) {
                if (sessionStatus != null) {
                    Bundle result = new Bundle();
                    result.putBundle(MediaControlIntent.EXTRA_SESSION_STATUS,
                            mSessionManager.getSessionStatus(sid).asBundle());
                    callback.onResult(result);
                } else {
                    callback.onError("Failed to get session status, sid=" + sid, null);
                }
            }
            return (sessionStatus != null);
        }

        private boolean handleEndSession(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = (sid != null) && sid.equals(mSessionManager.getSessionId())
                    && mSessionManager.endSession();
            if (callback != null) {
                if (success) {
                    Bundle result = new Bundle();
                    MediaSessionStatus sessionStatus = new MediaSessionStatus.Builder(
                            MediaSessionStatus.SESSION_STATE_ENDED).build();
                    result.putBundle(MediaControlIntent.EXTRA_SESSION_STATUS, sessionStatus.asBundle());
                    callback.onResult(result);
                    handleSessionStatusChange(sid);
                    mSessionReceiver = null;
                } else {
                    callback.onError("Failed to end session, sid=" + sid, null);
                }
            }
            return success;
        }

        private void handleStatusChange(PlaylistItem item) {
            if (item == null) {
                item = mSessionManager.getCurrentItem();
            }
            if (item != null) {
                PendingIntent receiver = item.getUpdateReceiver();
                if (receiver != null) {
                    Intent intent = new Intent();
                    intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, item.getSessionId());
                    intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, item.getItemId());
                    intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS,
                            item.getStatus().asBundle());
                    try {
                        receiver.send(getContext(), 0, intent);
                        Log.d(TAG, mRouteId + ": Sending status update from provider");
                    } catch (PendingIntent.CanceledException e) {
                        Log.d(TAG, mRouteId + ": Failed to send status update!");
                    }
                }
            }
        }

        private void handleSessionStatusChange(String sid) {
            if (mSessionReceiver != null) {
                Intent intent = new Intent();
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, sid);
                intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS,
                        mSessionManager.getSessionStatus(sid).asBundle());
                try {
                    mSessionReceiver.send(getContext(), 0, intent);
                    Log.d(TAG, mRouteId + ": Sending session status update from provider");
                } catch (PendingIntent.CanceledException e) {
                    Log.d(TAG, mRouteId + ": Failed to send session status update!");
                }
            }
        }
    }
}
