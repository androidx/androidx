/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.asynclayoutinflater.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Pools.SynchronizedPool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;

/**
 * <p>Helper class for inflating layouts asynchronously. To use, construct
 * an instance of {@link AsyncLayoutInflater} on the UI thread and call
 * {@link #inflate(int, ViewGroup, OnInflateFinishedListener)}. The
 * {@link OnInflateFinishedListener} will be invoked on the UI thread
 * is no executor is passed, otherwise, it is called on the given executor.
 *
 * <p>This is intended for parts of the UI that are created lazily or in
 * response to user interactions. This allows the UI thread to continue
 * to be responsive & animate while the relatively heavy inflate
 * is being performed.
 *
 * <p>For a layout to be inflated asynchronously it needs to have a parent
 * whose {@link ViewGroup#generateLayoutParams(AttributeSet)} is thread-safe
 * and all the Views being constructed as part of inflation must not create
 * any {@link Handler}s or otherwise call {@link Looper#myLooper()}. If the
 * layout that is trying to be inflated cannot be constructed
 * asynchronously for whatever reason, {@link AsyncLayoutInflater} will
 * automatically fall back to inflating on the UI thread.
 *
 * <p>NOTE that the inflated View hierarchy is NOT added to the parent. It is
 * equivalent to calling {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
 * with attachToRoot set to false. Callers will likely want to call
 * {@link ViewGroup#addView(View)} in the {@link OnInflateFinishedListener}
 * callback at a minimum.
 *
 * <p>This inflater does not support setting a {@link LayoutInflater.Factory}
 * nor {@link LayoutInflater.Factory2}. Similarly it does not support inflating
 * layouts that contain fragments.
 */
public final class AsyncLayoutInflater {
    private static final String TAG = "AsyncLayoutInflater";
    LayoutInflater mInflater;
    Handler mHandler;
    InflateThread mInflateThread;

    public AsyncLayoutInflater(@NonNull Context context) {
        mInflater = new BasicInflater(context);
        mHandler = new Handler(Looper.myLooper(), mHandlerCallback);
        mInflateThread = InflateThread.getInstance();
    }

    /**
     * Triggers view inflation on background thread.
     */
    @UiThread
    public void inflate(@LayoutRes int resid, @Nullable ViewGroup parent,
            @NonNull OnInflateFinishedListener callback) {
        inflateInternal(resid, parent, callback, mInflater, /* callbackExecutor= */ null);
    }

    private void inflateInternal(@LayoutRes int resid, @Nullable ViewGroup parent,
            @NonNull OnInflateFinishedListener callback, LayoutInflater inflater,
            Executor callbackExecutor) {
        if (callback == null) {
            throw new NullPointerException("callback argument may not be null!");
        }
        InflateRequest request = mInflateThread.obtainRequest();
        request.mInflater = inflater;
        request.mHandler = mHandler;
        request.resid = resid;
        request.parent = parent;
        request.callback = callback;
        request.mExecutor = callbackExecutor;
        mInflateThread.enqueue(request);
    }

    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            InflateRequest request = (InflateRequest) msg.obj;
            if (request.view == null) {
                request.view = request.mInflater.inflate(request.resid, request.parent, false);
            }

            if (request.mExecutor != null) {
                request.mExecutor.execute(() -> triggerCallbacks(request, mInflateThread));
            } else {
                triggerCallbacks(request, mInflateThread);
            }
            return true;
        }
    };

    static void triggerCallbacks(InflateRequest request, InflateThread mInflateThread) {
        request.callback.onInflateFinished(request.view, request.resid, request.parent);
        mInflateThread.releaseRequest(request);
    }

    public interface OnInflateFinishedListener {
        void onInflateFinished(@NonNull View view, @LayoutRes int resid,
                @Nullable ViewGroup parent);
    }

    private static class InflateRequest {
        LayoutInflater mInflater;
        Handler mHandler;
        ViewGroup parent;
        int resid;
        View view;
        OnInflateFinishedListener callback;
        Executor mExecutor;

        InflateRequest() {
        }
    }

    private static class BasicInflater extends LayoutInflater {
        private static final String[] sClassPrefixList =
                {"android.widget.", "android.webkit.", "android.app."};

        BasicInflater(Context context) {
            super(context);
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return new BasicInflater(newContext);
        }

        @Override
        protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
            for (String prefix : sClassPrefixList) {
                try {
                    View view = createView(name, prefix, attrs);
                    if (view != null) {
                        return view;
                    }
                } catch (ClassNotFoundException e) {
                    // In this case we want to let the base class take a crack
                    // at it.
                }
            }

            return super.onCreateView(name, attrs);
        }
    }

    private static class InflateThread extends Thread {
        private static final InflateThread sInstance;

        static {
            sInstance = new InflateThread();
            sInstance.setName("AsyncLayoutInflator");
            sInstance.start();
        }

        public static InflateThread getInstance() {
            return sInstance;
        }

        private ArrayBlockingQueue<InflateRequest> mQueue = new ArrayBlockingQueue<>(10);
        private SynchronizedPool<InflateRequest> mRequestPool = new SynchronizedPool<>(10);

        // Extracted to its own method to ensure locals have a constrained liveness
        // scope by the GC. This is needed to avoid keeping previous request references
        // alive for an indeterminate amount of time, see b/33158143 for details
        public void runInner() {
            InflateRequest request;
            try {
                request = mQueue.take();
            } catch (InterruptedException ex) {
                // Odd, just continue
                Log.w(TAG, ex);
                return;
            }

            try {
                request.view = request.mInflater.inflate(request.resid, request.parent, false);
            } catch (RuntimeException ex) {
                // Probably a Looper failure, retry on the UI thread
                Log.w(TAG, "Failed to inflate resource in the background! Retrying on the UI"
                        + " thread", ex);
            }

            // Trigger callback on bg thread if async inflation was successful.
            if (request.view != null && request.mExecutor != null) {
                request.mExecutor.execute(() -> triggerCallbacks(request, this));
            } else {
                Message.obtain(request.mHandler, 0, request).sendToTarget();
            }
        }

        @Override
        public void run() {
            while (true) {
                runInner();
            }
        }

        public InflateRequest obtainRequest() {
            InflateRequest obj = mRequestPool.acquire();
            if (obj == null) {
                obj = new InflateRequest();
            }
            return obj;
        }

        public void releaseRequest(InflateRequest obj) {
            obj.callback = null;
            obj.mInflater = null;
            obj.mHandler = null;
            obj.parent = null;
            obj.resid = 0;
            obj.view = null;
            obj.mExecutor = null;
            mRequestPool.release(obj);
        }

        public void enqueue(InflateRequest request) {
            try {
                mQueue.put(request);
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to enqueue async inflate request", e);
            }
        }
    }
}
