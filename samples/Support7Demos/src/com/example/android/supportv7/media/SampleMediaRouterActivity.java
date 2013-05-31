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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mSelector;
    private ArrayAdapter<MediaItem> mMediaItems;
    private TextView mInfoTextView;
    private ListView mMediaListView;
    private Button mPlayButton;
    private Button mStatisticsButton;

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
        if (fm.findFragmentByTag(DISCOVERY_FRAGMENT_TAG) == null) {
            DiscoveryFragment fragment = new DiscoveryFragment();
            fragment.setRouteSelector(mSelector);
            fm.beginTransaction()
                    .add(fragment, DISCOVERY_FRAGMENT_TAG)
                    .commit();
        }

        // Populate an array adapter with fake media items.
        String[] mediaNames = getResources().getStringArray(R.array.media_names);
        String[] mediaUris = getResources().getStringArray(R.array.media_uris);
        mMediaItems = new ArrayAdapter<MediaItem>(this,
                android.R.layout.simple_list_item_single_choice, android.R.id.text1);
        for (int i = 0; i < mediaNames.length; i++) {
            mMediaItems.add(new MediaItem(mediaNames[i], Uri.parse(mediaUris[i])));
        }

        // Initialize the layout.
        setContentView(R.layout.sample_media_router);

        mInfoTextView = (TextView)findViewById(R.id.info);

        mMediaListView = (ListView)findViewById(R.id.media);
        mMediaListView.setAdapter(mMediaItems);
        mMediaListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mMediaListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateButtons();
            }
        });

        mPlayButton = (Button)findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        mStatisticsButton = (Button)findViewById(R.id.statistics_button);
        mStatisticsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showStatistics();
            }
        });
    }

    @Override
    public void onStart() {
        // Be sure to call the super class.
        super.onStart();

        updateRouteDescription();
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
        mInfoTextView.setText("Currently selected route: " + route.getName()
                + " from provider " + route.getProvider().getPackageName()
                + ", description: " + route.getDescription());
        updateButtons();
    }

    private void updateButtons() {
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();

        MediaItem item = getCheckedMediaItem();
        if (item != null) {
            mPlayButton.setEnabled(route.supportsControlRequest(makePlayIntent(item)));
        } else {
            mPlayButton.setEnabled(false);
        }

        mStatisticsButton.setEnabled(route.supportsControlRequest(makeStatisticsIntent()));
    }

    private void play() {
        final MediaItem item = getCheckedMediaItem();
        if (item == null) {
            return;
        }

        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
        Intent intent = makePlayIntent(item);
        if (route.supportsControlRequest(intent)) {
            MediaRouter.ControlRequestCallback callback =
                    new MediaRouter.ControlRequestCallback() {
                @Override
                public void onResult(Bundle data) {
                    String streamId = data != null ? data.getString(
                            MediaControlIntent.EXTRA_ITEM_ID) : null;

                    Log.d(TAG, "Play request succeeded: data=" + data + " , streamId=" + streamId);
                    Toast.makeText(SampleMediaRouterActivity.this,
                            "Now playing " + item.mName,
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String error, Bundle data) {
                    Log.d(TAG, "Play request failed: error=" + error + ", data=" + data);
                    Toast.makeText(SampleMediaRouterActivity.this,
                            "Unable to play " + item.mName + ", error: " + error,
                            Toast.LENGTH_LONG).show();
                }
            };

            Log.d(TAG, "Sending play request: intent=" + intent);
            route.sendControlRequest(intent, callback);
        } else {
            Log.d(TAG, "Play request not supported: intent=" + intent);
            Toast.makeText(SampleMediaRouterActivity.this,
                    "Play not supported for " + item.mName, Toast.LENGTH_LONG).show();
        }
    }

    private void showStatistics() {
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute();
        Intent intent = makeStatisticsIntent();
        if (route.supportsControlRequest(intent)) {
            MediaRouter.ControlRequestCallback callback = new MediaRouter.ControlRequestCallback() {
                @Override
                public void onResult(Bundle data) {
                    Log.d(TAG, "Statistics request succeeded: data=" + data);
                    if (data != null) {
                        int playbackCount = data.getInt(
                                SampleMediaRouteProvider.DATA_PLAYBACK_COUNT, -1);
                        Toast.makeText(SampleMediaRouterActivity.this,
                                "Total playback count: " + playbackCount,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SampleMediaRouterActivity.this,
                                "Statistics query did not return any data",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String error, Bundle data) {
                    Log.d(TAG, "Statistics request failed: error=" + error + ", data=" + data);
                    Toast.makeText(SampleMediaRouterActivity.this,
                            "Unable to query statistics, error: " + error,
                            Toast.LENGTH_LONG).show();
                }
            };

            Log.d(TAG, "Sent statistics request: intent=" + intent);
            route.sendControlRequest(intent, callback);
        } else {
            Log.d(TAG, "Statistics request not supported: intent=" + intent);
            Toast.makeText(SampleMediaRouterActivity.this,
                    "Statistics not supported.", Toast.LENGTH_LONG).show();
        }
    }

    private Intent makePlayIntent(MediaItem item) {
        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setDataAndType(item.mUri, "video/mp4");
        return intent;
    }

    private Intent makeStatisticsIntent() {
        Intent intent = new Intent(SampleMediaRouteProvider.ACTION_GET_STATISTICS);
        intent.addCategory(SampleMediaRouteProvider.CATEGORY_SAMPLE_ROUTE);
        return intent;
    }

    private MediaItem getCheckedMediaItem() {
        int index = mMediaListView.getCheckedItemPosition();
        if (index >= 0 && index < mMediaItems.getCount()) {
            return mMediaItems.getItem(index);
        }
        return null;
    }

    private final class DiscoveryFragment extends MediaRouteDiscoveryFragment {
        @Override
        public Callback onCreateCallback() {
            // Return a custom callback that will simply log all of the route events
            // for demonstration purposes.
            return new MediaRouter.Callback() {
                @Override
                public void onRouteAdded(MediaRouter router, RouteInfo route) {
                    Log.d(TAG, "onRouteAdded: route=" + route);
                }

                @Override
                public void onRouteChanged(MediaRouter router, RouteInfo route) {
                    Log.d(TAG, "onRouteChanged: route=" + route);
                    updateRouteDescription();
                }

                @Override
                public void onRouteRemoved(MediaRouter router, RouteInfo route) {
                    Log.d(TAG, "onRouteRemoved: route=" + route);
                }

                @Override
                public void onRouteSelected(MediaRouter router, RouteInfo route) {
                    Log.d(TAG, "onRouteSelected: route=" + route);
                    updateRouteDescription();
                }

                @Override
                public void onRouteUnselected(MediaRouter router, RouteInfo route) {
                    Log.d(TAG, "onRouteUnselected: route=" + route);
                    updateRouteDescription();
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
