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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * A singleton to present a simple interface for preloading videos.
 *
 * <p>This class is used in {@link android.support.media.instantvideo.widget.InstantVideoView}
 * internally to play the preloaded video.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
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
            sInstance =
                    new InstantVideoPreloadManager(context, new InternalVideoPreloaderFactory());
        }
        return sInstance;
    }

    private final LruCache<Uri, VideoPreloader> mVideoCache =
            new LruCache<Uri, VideoPreloader>(DEFAULT_MAX_VIDEO_COUNT) {
                @Override
                protected void entryRemoved(boolean evicted, Uri key, VideoPreloader oldValue,
                        VideoPreloader newValue) {
                    if (newValue != null) {
                        onEntryRemovedFromCache(key, oldValue);
                    }
                }
            };

    private final Context mAppContext;
    private final VideoPreloaderFactory mVideoPreloaderFactory;

    private int mMaxVideoCount = DEFAULT_MAX_VIDEO_COUNT;

    @VisibleForTesting
    InstantVideoPreloadManager(Context context, VideoPreloaderFactory factory) {
        mAppContext = context.getApplicationContext();
        mVideoPreloaderFactory = factory;
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
        VideoPreloader preloader = mVideoCache.get(videoUri);
        if (preloader == null) {
            mVideoCache.put(videoUri, startVideoPreloading(videoUri));
        } else {
            mVideoCache.put(videoUri, preloader);
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

    private void onEntryRemovedFromCache(Uri videoUri, VideoPreloader preloader) {
        preloader.stop();
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

    private VideoPreloader startVideoPreloading(Uri videoUri) {
        VideoPreloader preloader = mVideoPreloaderFactory.createVideoPreloader(videoUri);
        preloader.start();
        return preloader;
    }

    @VisibleForTesting
    interface VideoPreloaderFactory {
        VideoPreloader createVideoPreloader(Uri videoUri);
    }

    @VisibleForTesting
    interface VideoPreloader {
        void start();
        void stop();
    }

    private static class InternalVideoPreloaderFactory implements VideoPreloaderFactory {
        @Override
        public VideoPreloader createVideoPreloader(Uri videoUri) {
            return new AsyncTaskVideoPreloader(videoUri);
        }
    }

    private static class AsyncTaskVideoPreloader extends AsyncTask<Void, Void, Void>
            implements VideoPreloader {
        private Uri mVideoUri;

        private AsyncTaskVideoPreloader(Uri videoUri) {
            mVideoUri = videoUri;
        }

        @Override
        public void start() {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void stop() {
            cancel(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO: Implement.
            return null;
        }
    }
}
