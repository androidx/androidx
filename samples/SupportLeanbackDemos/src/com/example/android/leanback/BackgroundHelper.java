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
                Drawable drawable = activity.getDrawable(resourceId);
                if (drawable instanceof BitmapDrawable) {
                    return ((BitmapDrawable) drawable).getBitmap();
                }
            }
            return null;
        }

        private void applyBackground(Activity activity, Bitmap bitmap) {
            BackgroundManager backgroundManager = BackgroundManager.getInstance(activity);
            if (bitmap == null || backgroundManager == null || !backgroundManager.isAttached()) {
                return;
            }
            backgroundManager.setBitmap(bitmap);
        }
    }

    private LoadBackgroundRunnable mRunnable;
    private LoadBitmapTask mTask;

    public void attach(Activity activity) {
        if (!ENABLED) {
            return;
        }
        BackgroundManager.getInstance(activity).attach(activity.getWindow());
    }

    public void setBackground(Activity activity, Object imageToken) {
        if (!ENABLED) {
            return;
        }
        Handler handler = activity.getWindow().getDecorView().getHandler();
        if (mRunnable != null) {
            handler.removeCallbacks(mRunnable);
        }
        mRunnable = new LoadBackgroundRunnable(activity, imageToken);
        handler.postDelayed(mRunnable, SET_BACKGROUND_DELAY_MS);
    }
}
