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

package com.example.android.leanback;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import androidx.collection.LruCache;
import androidx.leanback.widget.PlaybackSeekDataProvider;

import java.util.Iterator;
import java.util.Map;

/**
 *
 * Base class that implements PlaybackSeekDataProvider using AsyncTask.THREAD_POOL_EXECUTOR with
 * prefetching.
 */
public abstract class PlaybackSeekAsyncDataProvider extends PlaybackSeekDataProvider {

    static final String TAG = "SeekAsyncProvider";

    long[] mSeekPositions;
    // mCache is for the bitmap requested by user
    final LruCache<Integer, Bitmap> mCache;
    // mPrefetchCache is for the bitmap not requested by user but prefetched by heuristic
    // estimation. We use a different LruCache so that items in mCache will not be evicted by
    // prefeteched items.
    final LruCache<Integer, Bitmap> mPrefetchCache;
    final SparseArray<LoadBitmapTask> mRequests = new SparseArray<>();
    int mLastRequestedIndex = -1;

    protected boolean isCancelled(Object task) {
        return ((AsyncTask) task).isCancelled();
    }

    protected abstract Bitmap doInBackground(Object task, int index, long position);

    class LoadBitmapTask extends AsyncTask<Object, Object, Bitmap> {

        int mIndex;
        ResultCallback mResultCallback;

        LoadBitmapTask(int index, ResultCallback callback) {
            mIndex = index;
            mResultCallback = callback;
        }

        @Override
        protected Bitmap doInBackground(Object[] params) {
            return PlaybackSeekAsyncDataProvider.this
                    .doInBackground(this, mIndex, mSeekPositions[mIndex]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mRequests.remove(mIndex);
            Log.d(TAG, "thumb Loaded " + mIndex);
            if (mResultCallback != null) {
                mCache.put(mIndex, bitmap);
                mResultCallback.onThumbnailLoaded(bitmap, mIndex);
            } else {
                mPrefetchCache.put(mIndex, bitmap);
            }
        }

    }

    public PlaybackSeekAsyncDataProvider() {
        this(16, 24);
    }

    public PlaybackSeekAsyncDataProvider(int cacheSize, int prefetchCacheSize) {
        mCache = new LruCache<Integer, Bitmap>(cacheSize);
        mPrefetchCache = new LruCache<Integer, Bitmap>(prefetchCacheSize);
    }

    public void setSeekPositions(long[] positions) {
        mSeekPositions = positions;
    }

    @Override
    public long[] getSeekPositions() {
        return mSeekPositions;
    }

    @Override
    public void getThumbnail(int index, ResultCallback callback) {
        Integer key = index;
        Bitmap bitmap = mCache.get(key);
        if (bitmap != null) {
            callback.onThumbnailLoaded(bitmap, index);
        } else {
            bitmap = mPrefetchCache.get(key);
            if (bitmap != null) {
                mCache.put(key, bitmap);
                mPrefetchCache.remove(key);
                callback.onThumbnailLoaded(bitmap, index);
            } else {
                LoadBitmapTask task = mRequests.get(index);
                if (task == null || task.isCancelled()) {
                    // no normal task or prefetch for the position, create a new task
                    task = new LoadBitmapTask(index, callback);
                    mRequests.put(index, task);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    // update existing ResultCallback which might be normal task or prefetch
                    task.mResultCallback = callback;
                }
            }
        }
        if (mLastRequestedIndex != index) {
            if (mLastRequestedIndex != -1) {
                prefetch(mLastRequestedIndex, index > mLastRequestedIndex);
            }
            mLastRequestedIndex = index;
        }
    }

    protected void prefetch(int hintIndex, boolean forward) {
        for (Iterator<Map.Entry<Integer, Bitmap>> it =
                mPrefetchCache.snapshot().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Bitmap> entry = it.next();
            if (forward ? entry.getKey() < hintIndex : entry.getKey() > hintIndex) {
                mPrefetchCache.remove(entry.getKey());
            }
        }
        int inc = forward ? 1 : -1;
        for (int i = hintIndex; (mRequests.size() + mPrefetchCache.size()
                < mPrefetchCache.maxSize()) && (inc > 0 ? i < mSeekPositions.length : i >= 0);
                i += inc) {
            Integer key = i;
            if (mCache.get(key) == null && mPrefetchCache.get(key) == null) {
                LoadBitmapTask task = mRequests.get(i);
                if (task == null) {
                    task = new LoadBitmapTask(key, null);
                    mRequests.put(i, task);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < mRequests.size(); i++) {
            LoadBitmapTask task = mRequests.valueAt(i);
            task.cancel(true);
        }
        mRequests.clear();
        mCache.evictAll();
        mPrefetchCache.evictAll();
        mLastRequestedIndex = -1;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Requests<");
        for (int i = 0; i < mRequests.size(); i++) {
            b.append(mRequests.keyAt(i));
            b.append(",");
        }
        b.append("> Cache<");
        for (Iterator<Integer> it = mCache.snapshot().keySet().iterator(); it.hasNext();) {
            Integer key = it.next();
            if (mCache.get(key) != null) {
                b.append(key);
                b.append(",");
            }
        }
        b.append(">");
        b.append("> PrefetchCache<");
        for (Iterator<Integer> it = mPrefetchCache.snapshot().keySet().iterator(); it.hasNext();) {
            Integer key = it.next();
            if (mPrefetchCache.get(key) != null) {
                b.append(key);
                b.append(",");
            }
        }
        b.append(">");
        return b.toString();
    }
}
