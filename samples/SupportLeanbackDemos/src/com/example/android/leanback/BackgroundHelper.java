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
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class BackgroundHelper {

    private static final String TAG = "BackgroundHelper";
    private static final boolean DEBUG = false;
    private static final boolean ENABLED = true;

    // Background delay serves to avoid kicking off expensive bitmap loading
    // in case multiple backgrounds are set in quick succession.
    private static final int SET_BACKGROUND_DELAY_MS = 100;

    static class Request {
        Object mImageToken;
        Activity mActivity;
        Bitmap mResult;

        Request(Activity activity, Object imageToken) {
            mActivity = activity;
            mImageToken = imageToken;
        }
    }

    public BackgroundHelper() {
        if (DEBUG && !ENABLED) Log.v(TAG, "BackgroundHelper: disabled");
    }

    class LoadBackgroundRunnable implements Runnable {
        Request mRequest;

        LoadBackgroundRunnable(Activity activity, Object imageToken) {
            mRequest = new Request(activity, imageToken);
        }

        @Override
        public void run() {
            if (mTask != null) {
                if (DEBUG) Log.v(TAG, "Cancelling task");
                mTask.cancel(true);
            }
            if (DEBUG) Log.v(TAG, "Executing task");
            mTask = new LoadBitmapTask();
            mTask.execute(mRequest);
            mRunnable = null;
        }
    };

    class LoadBitmapTask extends AsyncTask<Request, Object, Request> {
        @Override
        protected Request doInBackground(Request... params) {
            boolean cancelled = isCancelled();
            if (DEBUG) Log.v(TAG, "doInBackground cancelled " + cancelled);
            Request request = params[0];
            if (!cancelled) {
                request.mResult = loadBitmap(request.mActivity, request.mImageToken);
            }
            return request;
        }

        @Override
        protected void onPostExecute(Request request) {
            if (DEBUG) Log.v(TAG, "onPostExecute");
            applyBackground(request.mActivity, request.mResult);
            if (mTask == this) {
                mTask = null;
            }
        }

        @Override
        protected void onCancelled(Request request) {
            if (DEBUG) Log.v(TAG, "onCancelled");
        }

        private Bitmap loadBitmap(Activity activity, Object imageToken) {
            if (imageToken instanceof Integer) {
                final int resourceId = (Integer) imageToken;
                if (DEBUG) Log.v(TAG, "load resourceId " + resourceId);
                Drawable drawable = ContextCompat.getDrawable(activity, resourceId);
                if (drawable instanceof BitmapDrawable) {
                    return ((BitmapDrawable) drawable).getBitmap();
                }
            }
            return null;
        }

        private void applyBackground(Activity activity, Bitmap bitmap) {
            BackgroundManager backgroundManager = BackgroundManager.getInstance(activity);
            if (backgroundManager == null || !backgroundManager.isAttached()) {
                return;
            }
            backgroundManager.setBitmap(bitmap);
        }
    }

    private LoadBackgroundRunnable mRunnable;
    private LoadBitmapTask mTask;

    // Allocate a dedicated handler because there may be no view available
    // when setBackground is invoked.
    private Handler mHandler = new Handler();

    public void setBackground(Activity activity, Object imageToken) {
        if (!ENABLED) {
            return;
        }
        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mRunnable = new LoadBackgroundRunnable(activity, imageToken);
        mHandler.postDelayed(mRunnable, SET_BACKGROUND_DELAY_MS);
    }

    static public void attach(Activity activity) {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "attach to activity " + activity);
        BackgroundManager.getInstance(activity).attach(activity.getWindow());
    }

    static public void release(Activity activity) {
        if (!ENABLED) {
            return;
        }
        if (DEBUG) Log.v(TAG, "release from activity " + activity);
        BackgroundManager.getInstance(activity).release();
    }
}
