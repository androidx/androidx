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
 * limitations under the License
 */

package com.example.android.leanback;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;

/**
 * App uses BackgroundHelper for each Activity, it wraps BackgroundManager and provides:
 * 1. AsyncTask to load bitmap in background thread.
 * 2. Using a BitmapCache to cache loaded bitmaps.
 */
public class BackgroundHelper {

    private static final String TAG = "BackgroundHelper";
    private static final boolean DEBUG = false;
    private static final boolean ENABLED = true;

    // Background delay serves to avoid kicking off expensive bitmap loading
    // in case multiple backgrounds are set in quick succession.
    private static final int SET_BACKGROUND_DELAY_MS = 100;

    /**
     * An very simple example of BitmapCache.
     */
    public static class BitmapCache {
        Bitmap mLastBitmap;
        Object mLastToken;

        // Singleton BitmapCache shared by multiple activities/backgroundHelper.
        static BitmapCache sInstance = new BitmapCache();

        private BitmapCache() {
        }

        /**
         * Get cached bitmap by token, returns null if missing cache.
         */
        public Bitmap getCache(Object token) {
            if (token == null ? mLastToken == null : token.equals(mLastToken)) {
                if (DEBUG) Log.v(TAG, "hitCache token:" + token + " " + mLastBitmap);
                return mLastBitmap;
            }
            return null;
        }

        /**
         * Add cached bitmap.
         */
        public void putCache(Object token, Bitmap bitmap) {
            if (DEBUG) Log.v(TAG, "putCache token:" + token + " " + bitmap);
            mLastToken = token;
            mLastBitmap = bitmap;
        }

        /**
         * Add singleton of BitmapCache shared across activities.
         */
        public static BitmapCache getInstance() {
            return sInstance;
        }
    }

    /**
     * Callback class to perform task after bitmap is loaded.
     */
    public abstract static class BitmapLoadCallback {
        /**
         * Called when Bitmap is loaded.
         */
        public abstract void onBitmapLoaded(Bitmap bitmap);
    }

    static class Request {
        Object mImageToken;
        Bitmap mResult;

        Request(Object imageToken) {
            mImageToken = imageToken;
        }
    }

    public BackgroundHelper(Activity activity) {
        if (DEBUG && !ENABLED) Log.v(TAG, "BackgroundHelper: disabled");
        mActivity = activity;
    }

    class LoadBackgroundRunnable implements Runnable {
        Request mRequest;

        LoadBackgroundRunnable(Object imageToken) {
            mRequest = new Request(imageToken);
        }

        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "Executing task");
            new LoadBitmapIntoBackgroundManagerTask().execute(mRequest);
            mRunnable = null;
        }
    }

    class LoadBitmapTaskBase extends AsyncTask<Request, Object, Request> {
        @Override
        protected Request doInBackground(Request... params) {
            boolean cancelled = isCancelled();
            if (DEBUG) Log.v(TAG, "doInBackground cancelled " + cancelled);
            Request request = params[0];
            if (!cancelled) {
                request.mResult = loadBitmap(request.mImageToken);
            }
            return request;
        }

        @Override
        protected void onPostExecute(Request request) {
            if (DEBUG) Log.v(TAG, "onPostExecute");
            BitmapCache.getInstance().putCache(request.mImageToken, request.mResult);
        }

        @Override
        protected void onCancelled(Request request) {
            if (DEBUG) Log.v(TAG, "onCancelled");
        }

        private Bitmap loadBitmap(Object imageToken) {
            if (imageToken instanceof Integer) {
                final int resourceId = (Integer) imageToken;
                if (DEBUG) Log.v(TAG, "load resourceId " + resourceId);
                Drawable drawable = ContextCompat.getDrawable(mActivity, resourceId);
                if (drawable instanceof BitmapDrawable) {
                    return ((BitmapDrawable) drawable).getBitmap();
                }
            }
            return null;
        }
    }

    class LoadBitmapIntoBackgroundManagerTask extends LoadBitmapTaskBase {
        @Override
        protected void onPostExecute(Request request) {
            super.onPostExecute(request);
            mBackgroundManager.setBitmap(request.mResult);
        }
    }

    class LoadBitmapCallbackTask extends LoadBitmapTaskBase {
        BitmapLoadCallback mCallback;

        LoadBitmapCallbackTask(BitmapLoadCallback callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(Request request) {
            super.onPostExecute(request);
            if (mCallback != null) {
                mCallback.onBitmapLoaded(request.mResult);
            }
        }
    }

    final Activity mActivity;
    BackgroundManager mBackgroundManager;
    LoadBackgroundRunnable mRunnable;

    // Allocate a dedicated handler because there may be no view available
    // when setBackground is invoked.
    static Handler sHandler = new Handler();

    void createBackgroundManagerIfNeeded() {
        if (mBackgroundManager == null) {
            mBackgroundManager = BackgroundManager.getInstance(mActivity);
        }
    }

    /**
     * Attach BackgroundManager to activity window.
     */
    public void attachToWindow() {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "attachToWindow " + mActivity);
        createBackgroundManagerIfNeeded();
        mBackgroundManager.attach(mActivity.getWindow());
    }

    /**
     * Attach BackgroundManager to a view inside activity.
     */
    public void attachToView(View backgroundView) {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "attachToView " + mActivity + " " + backgroundView);
        createBackgroundManagerIfNeeded();
        mBackgroundManager.attachToView(backgroundView);
    }

    /**
     * Sets a background bitmap. It will look up the cache first if missing, an AsyncTask will
     * will be launched to load the bitmap.
     */
    public void setBackground(Object imageToken) {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "set imageToken " + imageToken + " to " + mActivity);
        createBackgroundManagerIfNeeded();
        if (imageToken == null) {
            mBackgroundManager.setDrawable(null);
            return;
        }
        Bitmap cachedBitmap = BitmapCache.getInstance().getCache(imageToken);
        if (cachedBitmap != null) {
            mBackgroundManager.setBitmap(cachedBitmap);
            return;
        }
        if (mRunnable != null) {
            sHandler.removeCallbacks(mRunnable);
        }
        mRunnable = new LoadBackgroundRunnable(imageToken);
        sHandler.postDelayed(mRunnable, SET_BACKGROUND_DELAY_MS);
    }

    /**
     * Clear Drawable.
     */
    public void clearDrawable() {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "clearDrawable to " + mActivity);
        createBackgroundManagerIfNeeded();
        mBackgroundManager.clearDrawable();
    }

    /**
     * Directly sets a Drawable as background.
     */
    public void setDrawable(Drawable drawable) {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "setDrawable " + drawable + " to " + mActivity);
        createBackgroundManagerIfNeeded();
        mBackgroundManager.setDrawable(drawable);
    }

    /**
     * Load bitmap in background and pass result to BitmapLoadCallback.
     */
    public void loadBitmap(Object imageToken, BitmapLoadCallback callback) {
        Bitmap cachedBitmap = BitmapCache.getInstance().getCache(imageToken);
        if (cachedBitmap != null) {
            if (callback != null) {
                callback.onBitmapLoaded(cachedBitmap);
                return;
            }
        }
        new LoadBitmapCallbackTask(callback).execute(new Request(imageToken));
    }
}
