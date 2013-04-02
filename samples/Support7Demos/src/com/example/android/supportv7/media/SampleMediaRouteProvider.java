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
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

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
            "com.example.android.supportv4.media.CATEGORY_SAMPLE_ROUTE";

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
            "com.example.android.supportv4.media.ACTION_GET_STATISTICS";

    /**
     * {@link #ACTION_GET_STATISTICS} result data: Number of times the
     * playback action was invoked.
     */
    public static final String DATA_PLAYBACK_COUNT =
            "com.example.android.supportv4.media.EXTRA_PLAYBACK_COUNT";

    private static final IntentFilter[] CONTROL_FILTERS;
    static {
        CONTROL_FILTERS = new IntentFilter[2];

        CONTROL_FILTERS[0] = new IntentFilter();
        CONTROL_FILTERS[0].addCategory(CATEGORY_SAMPLE_ROUTE);
        CONTROL_FILTERS[0].addAction(ACTION_GET_STATISTICS);

        CONTROL_FILTERS[1] = new IntentFilter();
        CONTROL_FILTERS[1].addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        CONTROL_FILTERS[1].addAction(MediaControlIntent.ACTION_PLAY);
        CONTROL_FILTERS[1].addDataScheme("http");
        CONTROL_FILTERS[1].addDataScheme("https");
        addDataTypeUnchecked(CONTROL_FILTERS[1], "video/*");
    }

    private static void addDataTypeUnchecked(IntentFilter filter, String type) {
        try {
            filter.addDataType(type);
        } catch (MalformedMimeTypeException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int mVolume = 5;
    private int mPlaybackCount;

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

        RouteDescriptor routeDescriptor1 = new RouteDescriptor(
                FIXED_VOLUME_ROUTE_ID,
                r.getString(R.string.fixed_volume_route_name));
        routeDescriptor1.setControlFilters(CONTROL_FILTERS);
        routeDescriptor1.setIconResource(R.drawable.media_route_icon);
        routeDescriptor1.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);
        routeDescriptor1.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED);
        routeDescriptor1.setVolume(VOLUME_MAX);

        RouteDescriptor routeDescriptor2 = new RouteDescriptor(
                VARIABLE_VOLUME_ROUTE_ID,
                r.getString(R.string.variable_volume_route_name));
        routeDescriptor2.setControlFilters(CONTROL_FILTERS);
        routeDescriptor2.setIconResource(R.drawable.media_route_icon);
        routeDescriptor2.setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);
        routeDescriptor2.setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE);
        routeDescriptor2.setVolumeMax(VOLUME_MAX);
        routeDescriptor2.setVolume(mVolume);

        ProviderDescriptor providerDescriptor = new ProviderDescriptor();
        providerDescriptor.setRoutes(new RouteDescriptor[] {
            routeDescriptor1, routeDescriptor2
        });
        setDescriptor(providerDescriptor);
    }

    private String generateStreamId() {
        return UUID.randomUUID().toString();
    }

    private final class SampleRouteController extends MediaRouteProvider.RouteController {
        private final String mRouteId;

        public SampleRouteController(String routeId) {
            mRouteId = routeId;
            Log.d(TAG, mRouteId + ": Controller created");
        }

        @Override
        public void onRelease() {
            Log.d(TAG, mRouteId + ": Controller released");
        }

        @Override
        public void onSelect() {
            Log.d(TAG, mRouteId + ": Selected");
        }

        @Override
        public void onUnselect() {
            Log.d(TAG, mRouteId + ": Unselected");
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

        private void setVolumeInternal(int volume) {
            if (volume >= 0 && volume <= VOLUME_MAX) {
                mVolume = volume;
                Log.d(TAG, mRouteId + ": New volume is " + mVolume);
                publishRoutes();
            }
        }

        @Override
        public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
            Log.d(TAG, mRouteId + ": Received control request " + intent);
            if (intent.getAction().equals(MediaControlIntent.ACTION_PLAY)
                    && intent.hasCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    && intent.getData() != null) {
                mPlaybackCount +=1;

                Uri uri = intent.getData();
                int queueBehavior = intent.getIntExtra(
                        MediaControlIntent.EXTRA_STREAM_QUEUE_BEHAVIOR,
                        MediaControlIntent.STREAM_QUEUE_BEHAVIOR_PLAY_NOW);
                int position = intent.getIntExtra(
                        MediaControlIntent.EXTRA_STREAM_POSITION, 0);
                Bundle metadata = intent.getBundleExtra(MediaControlIntent.EXTRA_STREAM_METADATA);
                Bundle headers = intent.getBundleExtra(
                        MediaControlIntent.EXTRA_STREAM_HTTP_HEADERS);
                String streamId = generateStreamId();

                Log.d(TAG, mRouteId + ": Received play request, uri=" + uri
                        + ", queueBehavior=" + queueBehavior
                        + ", position=" + position
                        + ", metadata=" + metadata
                        + ", headers=" + headers);
                Toast.makeText(getContext(), "Route received play request: uri=" + uri,
                        Toast.LENGTH_LONG).show();
                if (callback != null) {
                    Bundle result = new Bundle();
                    result.putString(MediaControlIntent.EXTRA_STREAM_ID, streamId);
                    callback.onResult(ControlRequestCallback.REQUEST_SUCCEEDED, result);
                }
                return true;
            }

            if (intent.getAction().equals(ACTION_GET_STATISTICS)
                    && intent.hasCategory(CATEGORY_SAMPLE_ROUTE)) {
                Bundle data = new Bundle();
                data.putInt(DATA_PLAYBACK_COUNT, mPlaybackCount);
                if (callback != null) {
                    callback.onResult(ControlRequestCallback.REQUEST_SUCCEEDED, data);
                }
                return true;
            }
            return false;
        }
    }
}