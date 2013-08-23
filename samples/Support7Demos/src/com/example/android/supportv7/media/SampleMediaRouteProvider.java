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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Bundle;
import android.app.PendingIntent;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouteDescriptor;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;

/**
 * Demonstrates how to create a custom media route provider.
 *
 * @see SampleMediaRouteProviderService
 */
final class SampleMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "SampleMediaRouteProvider";

    private static final String FIXED_VOLUME_ROUTE_ID = "fixed";
    private static final String VARIABLE_VOLUME_ROUTE_ID = "variable";
    private static final int VOLUME_MAX = 10;

    /**
     * A custom media control intent category for special requests that are
     * supported by this provider's routes.
     */
    public static final String CATEGORY_SAMPLE_ROUTE =
            "com.example.android.supportv7.media.CATEGORY_SAMPLE_ROUTE";

    /**
     * A custom media control intent action for special requests that are
     * supported by this provider's routes.
     * <p>
     * This particular request is designed to return a bundle of not very
     * interesting statistics for demonstration purposes.
     * </p>
     *
     * @see #DATA_PLAYBACK_COUNT
     */
    public static final String ACTION_GET_STATISTICS =
            "com.example.android.supportv7.media.ACTION_GET_STATISTICS";

    /**
     * {@link #ACTION_GET_STATISTICS} result data: Number of times the
     * playback action was invoked.
     */
    public static final String DATA_PLAYBACK_COUNT =
            "com.example.android.supportv7.media.EXTRA_PLAYBACK_COUNT";

    /*
     * Set ENABLE_QUEUEING to true to test queuing on MRP. This will make
     * MRP expose the following two experimental hidden APIs:
     *     ACTION_ENQUEUE
     *     ACTION_REMOVE
     */
    public static final boolean ENABLE_QUEUEING = false;

    private static final ArrayList<IntentFilter> CONTROL_FILTERS;
    static {
        IntentFilter f1 = new IntentFilter();
        f1.addCategory(CATEGORY_SAMPLE_ROUTE);
        f1.addAction(ACTION_GET_STATISTICS);

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

        CONTROL_FILTERS = new ArrayList<IntentFilter>();
        CONTROL_FILTERS.add(f1);
        CONTROL_FILTERS.add(f2);
        CONTROL_FILTERS.add(f3);
        if (ENABLE_QUEUEING) {
            CONTROL_FILTERS.add(f4);
            CONTROL_FILTERS.add(f5);
        }
    }

    private static void addDataTypeUnchecked(IntentFilter filter, String type) {
        try {
            filter.addDataType(type);
        } catch (MalformedMimeTypeException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int mVolume = 5;
    private int mEnqueueCount;

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

        MediaRouteDescriptor routeDescriptor1 = new MediaRouteDescriptor.Builder(
                FIXED_VOLUME_ROUTE_ID,
                r.getString(R.string.fixed_volume_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolume(VOLUME_MAX)
                .build();

        MediaRouteDescriptor routeDescriptor2 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_ROUTE_ID,
                r.getString(R.string.variable_volume_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(mVolume)
                .build();

        MediaRouteProviderDescriptor providerDescriptor =
                new MediaRouteProviderDescriptor.Builder()
                .addRoute(routeDescriptor1)
                .addRoute(routeDescriptor2)
                .build();
        setDescriptor(providerDescriptor);
    }

    private final class SampleRouteController extends MediaRouteProvider.RouteController {
        private final String mRouteId;
        private final OverlayDisplayWindow mOverlay;
        private final MediaPlayerWrapper mMediaPlayer;
        private final MediaSessionManager mSessionManager;

        public SampleRouteController(String routeId) {
            mRouteId = routeId;
            mMediaPlayer = new MediaPlayerWrapper(getContext());
            mSessionManager = new MediaSessionManager();
            mSessionManager.setCallback(mMediaPlayer);

            // Create an overlay display window (used for simulating the remote playback only)
            mOverlay = OverlayDisplayWindow.create(getContext(),
                    getContext().getResources().getString(
                            R.string.sample_media_route_provider_remote),
                    1024, 768, Gravity.CENTER);
            mOverlay.setOverlayWindowListener(new OverlayDisplayWindow.OverlayWindowListener() {
                @Override
                public void onWindowCreated(Surface surface) {
                    mMediaPlayer.setSurface(surface);
                }

                @Override
                public void onWindowCreated(SurfaceHolder surfaceHolder) {
                    mMediaPlayer.setSurface(surfaceHolder);
                }

                @Override
                public void onWindowDestroyed() {
                }
            });

            mMediaPlayer.setCallback(new MediaPlayerCallback());
            Log.d(TAG, mRouteId + ": Controller created");
        }

        @Override
        public void onRelease() {
            Log.d(TAG, mRouteId + ": Controller released");
            mMediaPlayer.release();
        }

        @Override
        public void onSelect() {
            Log.d(TAG, mRouteId + ": Selected");
            mOverlay.show();
        }

        @Override
        public void onUnselect() {
            Log.d(TAG, mRouteId + ": Unselected");
            mMediaPlayer.onStop();
            mOverlay.dismiss();
        }

        @Override
        public void onSetVolume(int volume) {
            Log.d(TAG, mRouteId + ": Set volume to " + volume);
            if (mRouteId.equals(VARIABLE_VOLUME_ROUTE_ID)) {
                setVolumeInternal(volume);
            }
        }

        @Override
        public void onUpdateVolume(int delta) {
            Log.d(TAG, mRouteId + ": Update volume by " + delta);
            if (mRouteId.equals(VARIABLE_VOLUME_ROUTE_ID)) {
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
                }
                Log.d(TAG, mSessionManager.toString());
                return success;
            }

            if (action.equals(ACTION_GET_STATISTICS)
                    && intent.hasCategory(CATEGORY_SAMPLE_ROUTE)) {
                Bundle data = new Bundle();
                data.putInt(DATA_PLAYBACK_COUNT, mEnqueueCount);
                if (callback != null) {
                    callback.onResult(data);
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
            if (sid == null || mSessionManager.stop(sid)) {
                Log.d(TAG, "handleEnqueue");
                return handleEnqueue(intent, callback);
            }
            return false;
        }

        private boolean handleEnqueue(Intent intent, ControlRequestCallback callback) {
            if (intent.getData() == null) {
                return false;
            }

            mEnqueueCount +=1;

            boolean enqueue = intent.getAction().equals(MediaControlIntent.ACTION_ENQUEUE);
            Uri uri = intent.getData();
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            long pos = intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0);
            Bundle metadata = intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_METADATA);
            Bundle headers = intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_HTTP_HEADERS);
            PendingIntent receiver = (PendingIntent)intent.getParcelableExtra(
                    MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER);

            Log.d(TAG, mRouteId + ": Received " + (enqueue?"enqueue":"play") + " request"
                    + ", uri=" + uri
                    + ", sid=" + sid
                    + ", pos=" + pos
                    + ", metadata=" + metadata
                    + ", headers=" + headers
                    + ", receiver=" + receiver);
            MediaQueueItem item = mSessionManager.enqueue(sid, uri, receiver);
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
            String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
            MediaQueueItem item = mSessionManager.remove(sid, iid);
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
            String iid = intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
            long pos = intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0);
            Log.d(TAG, mRouteId + ": Received seek request, pos=" + pos);
            MediaQueueItem item = mSessionManager.seek(sid, iid, pos);
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
            MediaQueueItem item = mSessionManager.getStatus(sid, iid);
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
            boolean success = mSessionManager.pause(sid);
            if (callback != null) {
                if (success) {
                    callback.onResult(null);
                } else {
                    callback.onError("Failed to pause, sid=" + sid, null);
                }
            }
            return success;
        }

        private boolean handleResume(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = mSessionManager.resume(sid);
            if (callback != null) {
                if (success) {
                    callback.onResult(null);
                } else {
                    callback.onError("Failed to resume, sid=" + sid, null);
                }
            }
            return success;
        }

        private boolean handleStop(Intent intent, ControlRequestCallback callback) {
            String sid = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
            boolean success = mSessionManager.stop(sid);
            if (callback != null) {
                if (success) {
                    callback.onResult(null);
                } else {
                    callback.onError("Failed to stop, sid=" + sid, null);
                }
            }
            return success;
        }

        private void handleFinish(boolean error) {
            MediaQueueItem item = mSessionManager.finish(error);
            if (item != null) {
                handleStatusChange(item);
            }
        }

        private void handleStatusChange(MediaQueueItem item) {
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

        private final class MediaPlayerCallback extends MediaPlayerWrapper.Callback {
            @Override
            public void onError() {
                handleFinish(true);
            }

            @Override
            public void onCompletion() {
                handleFinish(false);
            }

            @Override
            public void onStatusChanged() {
                handleStatusChange(null);
            }

            @Override
            public void onSizeChanged(int width, int height) {
                mOverlay.updateAspectRatio(width, height);
            }
        }
    }
}