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
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.android.supportv4.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowserCompat} to connect to the {@link MediaBrowserServiceSupport}.
 * Once connected, the fragment subscribes to get all the children. All
 * {@link MediaBrowserCompat.MediaItem} objects that can be browsed are shown in a ListView.
 */
public class BrowseFragment extends Fragment {

    private static final String TAG = "BrowseFragment";

    public static final String ARG_MEDIA_ID = "media_id";

    // The number of media items per page.
    private static final int PAGE_SIZE = 6;

    public static interface FragmentDataHelper {
        void onMediaItemSelected(MediaBrowserCompat.MediaItem item);
    }

    // The mediaId to be used for subscribing for children using the MediaBrowser.
    private String mMediaId;
    private final List<MediaBrowserCompat.MediaItem> mMediaItems = new ArrayList<>();

    private boolean mCanLoadNewPage;
    private final Set<Integer> mSubscribedPages = new HashSet<Integer>();
    private MediaBrowserCompat mMediaBrowser;
    private BrowseAdapter mBrowserAdapter;

    private MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children,
                Bundle options) {
            int page = options.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
            int pageSize = options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
            if (page < 0 || pageSize != PAGE_SIZE || children == null
                    || children.size() > PAGE_SIZE) {
                return;
            }

            int itemIndex = page * PAGE_SIZE;
            if (itemIndex >= mMediaItems.size()) {
                if (children.size() == 0) {
                    return;
                }
                // An additional page is loaded.
                mMediaItems.addAll(children);
            } else {
                // An existing page is replaced by the newly loaded page.
                for (MediaBrowserCompat.MediaItem item : children) {
                    if (itemIndex < mMediaItems.size()) {
                        mMediaItems.set(itemIndex, item);
                    } else {
                        mMediaItems.add(item);
                    }
                    itemIndex++;
                }

                // If the newly loaded page contains less than {PAGE_SIZE} items,
                // then this page should be the last page.
                if (children.size() < PAGE_SIZE) {
                    while (mMediaItems.size() > itemIndex) {
                        mMediaItems.remove(mMediaItems.size() - 1);
                    }
                }
            }
            mBrowserAdapter.notifyDataSetChanged();
            mCanLoadNewPage = true;
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: parentId=" + parentId);
            mMediaItems.clear();
            mMediaItems.addAll(children);
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onError(String id) {
            Toast.makeText(getActivity(), R.string.error_loading_media,
                    Toast.LENGTH_LONG).show();
        }
    };

    private MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected: session token " + mMediaBrowser.getSessionToken());

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(getActivity(),
                        mMediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create MediaController.", e);
            }
            ((MediaBrowserSupport) getActivity()).setMediaController(mediaController);

            if (mMediaId == null) {
                mMediaId = mMediaBrowser.getRoot();
            }

            if (mMediaItems.size() == 0) {
                loadPage(0);
            }
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended");
            ((MediaBrowserSupport) getActivity()).setMediaController((MediaControllerCompat) null);
        }
    };

    public static BrowseFragment newInstance(String mediaId) {
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_ID, mediaId);
        BrowseFragment fragment = new BrowseFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mBrowserAdapter = new BrowseAdapter(getActivity(), mMediaItems);

        View controls = rootView.findViewById(R.id.controls);
        controls.setVisibility(View.GONE);

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);
                try {
                    FragmentDataHelper listener = (FragmentDataHelper) getActivity();
                    listener.onMediaItemSelected(item);
                } catch (ClassCastException ex) {
                    Log.e(TAG, "Exception trying to cast to FragmentDataHelper", ex);
                }
            }
        });

        Bundle args = getArguments();
        mMediaId = args.getString(ARG_MEDIA_ID, null);

        mMediaBrowser = new MediaBrowserCompat(getActivity(),
                new ComponentName(getActivity(), MediaBrowserServiceSupport.class),
                mConnectionCallback, null);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                if (mCanLoadNewPage && firstVisibleItem + visibleItemCount == totalItemCount) {
                    mCanLoadNewPage = false;
                    loadPage((mMediaItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Do nothing
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaBrowser.disconnect();
        mSubscribedPages.clear();
    }

    private void loadPage(int page) {
        Integer pageInteger = Integer.valueOf(page);
        if (mSubscribedPages.contains(pageInteger)) {
            return;
        }
        mSubscribedPages.add(pageInteger);
        Bundle options = new Bundle();
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, PAGE_SIZE);
        mMediaBrowser.subscribe(mMediaId, options, mSubscriptionCallback);
    }

    // An adapter for showing the list of browsed MediaItem objects
    private static class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        public BrowseAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItems) {
            super(context, R.layout.media_list_item, mediaItems);
        }

        static class ViewHolder {
            ImageView mImageView;
            TextView mTitleView;
            TextView mDescriptionView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.media_list_item, parent, false);
                holder = new ViewHolder();
                holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
                holder.mImageView.setVisibility(View.GONE);
                holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
                holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MediaBrowserCompat.MediaItem item = getItem(position);
            holder.mTitleView.setText(item.getDescription().getTitle());
            holder.mDescriptionView.setText(item.getDescription().getDescription());
            if (item.isPlayable()) {

                holder.mImageView.setImageDrawable(ContextCompat.getDrawable(
                        getContext(), R.drawable.ic_play_arrow_white_24dp));
                holder.mImageView.setVisibility(View.VISIBLE);
            } else {
                holder.mImageView.setVisibility(View.GONE);
            }
            return convertView;
        }
    }
}