/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.media.instantvideo.preload;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.LruCache;

/**
 * A singleton to present a simple interface for preloading videos.
 *
 * <p>This class is used in {@link android.support.media.instantvideo.widget.InstantVideoView}
 * internally to play the preloaded video.
 */
public class InstantVideoPreloadManager {
    private static final String TAG = "InstantVideoPreloadMgr";
    private static final boolean DEBUG = false;

    private static final int DEFAULT_MAX_VIDEO_COUNT = 20;

    private static InstantVideoPreloadManager sInstance;

    /**
     * Returns the singleton instance of this class.
     */
    public static synchronized InstantVideoPreloadManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new InstantVideoPreloadManager(context);
        }
        return sInstance;
    }

    private final LruCache<Uri, VideoPreloadTask> mVideoCache =
            new LruCache<Uri, VideoPreloadTask>(DEFAULT_MAX_VIDEO_COUNT) {
                @Override
                protected void entryRemoved(boolean evicted, Uri key, VideoPreloadTask oldValue,
                        VideoPreloadTask newValue) {
                    if (newValue != null) {
                        onEntryRemovedFromCache(key, oldValue);
                    }
                }
            };

    private Context mAppContext;
    private int mMaxVideoCount = DEFAULT_MAX_VIDEO_COUNT;

    @VisibleForTesting
    InstantVideoPreloadManager(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * Starts to preload the video with the given URI.
     *
     * @param videoUri The URI of the video to preload.
     */
    public void preload(@NonNull Uri videoUri) {
        if (videoUri == null) {
            throw new IllegalArgumentException("The video URI shouldn't be null.");
        }
        if (DEBUG) Log.d(TAG, "Preload " + videoUri);
        VideoPreloadTask task = mVideoCache.get(videoUri);
        if (task == null) {
            mVideoCache.put(videoUri, startVideoPreloadTask(videoUri));
        } else {
            mVideoCache.put(videoUri, task);
        }
        if (mVideoCache.size() > mMaxVideoCount) {
            if (DEBUG) {
                Log.d(TAG, "Reached the limit of the video count. Resizing to " + mMaxVideoCount);
            }
            mVideoCache.resize(mMaxVideoCount);
        }
    }

    @VisibleForTesting
    int getCacheSize() {
        return mVideoCache.size();
    }

    private void onEntryRemovedFromCache(Uri videoUri, VideoPreloadTask task) {
        task.cancel(true);
    }

    /**
     * Clears the cache and evict all the videos.
     */
    public void clearCache() {
        mVideoCache.evictAll();
    }

    /**
     * Sets the limit of the total size of the preloaded video contents in bytes.
     *
     * @param size The maximum cache size in bytes.
     */
    public void setMaxCacheSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The maximum cache size should be greater than 0.");
        }
        // TODO: Implement.
    }

    /**
     * Sets the maximum count of videos to preload.
     *
     * @param count The maximum count of the videos to be preloaded.
     */
    public void setMaxPreloadVideoCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The maximum video count should be greater than 0.");
        }
        mMaxVideoCount = count;
        mVideoCache.resize(count);
    }

    private VideoPreloadTask startVideoPreloadTask(Uri videoUri) {
        VideoPreloadTask task = new VideoPreloadTask(videoUri);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    private static class VideoPreloadTask extends AsyncTask<Void, Void, Void> {
        private Uri mVideoUri;

        private VideoPreloadTask(Uri videoUri) {
            mVideoUri = videoUri;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO: Implement.
            return null;
        }
    }
}

