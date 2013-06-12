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
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouteDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
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
        addDataTypeUnchecked(f2, "video/*");

        CONTROL_FILTERS = new ArrayList<IntentFilter>();
        CONTROL_FILTERS.add(f1);
        CONTROL_FILTERS.add(f2);
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

        MediaRouteDescriptor routeDescriptor1 = new MediaRouteDescriptor.Builder(
                FIXED_VOLUME_ROUTE_ID,
                r.getString(R.string.fixed_volume_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolume(VOLUME_MAX)
                .build();

        MediaRouteDescriptor routeDescriptor2 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_ROUTE_ID,
                r.getString(R.string.variable_volume_route_name))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS)
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

                // TODO: Handle queue ids.
                Uri uri = intent.getData();
                long contentPositionMillis = intent.getLongExtra(
                        MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0);
                Bundle metadata = intent.getBundleExtra(MediaControlIntent.EXTRA_ITEM_METADATA);
                Bundle headers = intent.getBundleExtra(
                        MediaControlIntent.EXTRA_ITEM_HTTP_HEADERS);

                Log.d(TAG, mRouteId + ": Received play request, uri=" + uri
                        + ", contentPositionMillis=" + contentPositionMillis
                        + ", metadata=" + metadata
                        + ", headers=" + headers);

                if (uri.toString().contains("hats")) {
                    // Simulate generating an error whenever the uri contains the word 'hats'.
                    Toast.makeText(getContext(), "Route rejected play request: uri=" + uri
                            + ", no hats allowed!", Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onError("Simulated error.  No hats allowed!", null);
                    }
                } else {
                    Toast.makeText(getContext(), "Route received play request: uri=" + uri,
                            Toast.LENGTH_LONG).show();
                    String streamId = generateStreamId();
                    if (callback != null) {
                        MediaItemStatus status = new MediaItemStatus.Builder(
                                MediaItemStatus.PLAYBACK_STATE_PLAYING)
                                .setContentPosition(contentPositionMillis)
                                .build();

                        Bundle result = new Bundle();
                        result.putString(MediaControlIntent.EXTRA_ITEM_ID, streamId);
                        result.putBundle(MediaControlIntent.EXTRA_ITEM_STATUS, status.asBundle());
                        callback.onResult(result);
                    }
                }
                return true;
            }

            if (intent.getAction().equals(ACTION_GET_STATISTICS)
                    && intent.hasCategory(CATEGORY_SAMPLE_ROUTE)) {
                Bundle data = new Bundle();
                data.putInt(DATA_PLAYBACK_COUNT, mPlaybackCount);
                if (callback != null) {
                    callback.onResult(data);
                }
                return true;
            }
            return false;
        }
    }
}