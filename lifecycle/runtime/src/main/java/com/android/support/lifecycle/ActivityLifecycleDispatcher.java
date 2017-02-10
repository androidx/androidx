/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.support.lifecycle.ReportInitializationFragment.ActivityInitializationListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Helper class to dispatch lifecycle events for an activity. Use it only if you activity does not
 * extend {@link android.support.v4.app.FragmentActivity FragmentActivity}.
 * <p>
 * If you decide to use this class, you should carefully read the documentation and call each method
 * on the right time.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ActivityLifecycleDispatcher {
    private static final String FRAGMENT_TAG = "com.android.support.lifecycle.ReportFragment";
    private static final String LOG_TAG = "ActivityLfDispatcher";
    private static final int LOADER_ID = 26130239;

    private static Class<?> sActivityThreadClass;
    private static Class sPauseListenerClass;
    private static Method sCurrentActivityThreadMethod;
    private static Method sRegisterPauseListenerMethod;
    private static Method sUnregisterPauseListenerMethod;

    static {
        loadClassesAndMethods();
    }

    private static void loadClassesAndMethods() {
        //noinspection TryWithIdenticalCatches
        try {
            sActivityThreadClass = Class.forName("android.app.ActivityThread");
            sCurrentActivityThreadMethod = sActivityThreadClass.getMethod("currentActivityThread");
            sCurrentActivityThreadMethod.setAccessible(true);

            sPauseListenerClass = Class.forName("android.app.OnActivityPausedListener");
            sRegisterPauseListenerMethod = sActivityThreadClass.getMethod(
                    "registerOnActivityPausedListener", Activity.class, sPauseListenerClass);
            sUnregisterPauseListenerMethod = sActivityThreadClass.getMethod(
                    "unregisterOnActivityPausedListener", Activity.class, sPauseListenerClass);
            sRegisterPauseListenerMethod.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Log.w(LOG_TAG, "Failed to find a class", e);
        } catch (NoSuchMethodException e) {
            Log.w(LOG_TAG, "Failed to find a method", e);
        }
    }

    private final Activity mActivity;
    final LifecycleRegistry mRegistry;
    // This listener is awesome, but isn't perfect, it is not called in many cases:
    // deliverNewIntents, start in paused state and etc, however, for most straightforward
    // case it is called synchronously after onPause() call.
    private final Object mPauseListener = createPauseListener();
    private Handler mPauseHandler;
    private final EmptyCursor mReportStopCursor = new EmptyCursor() {
        @Override
        public void deactivate() {
            super.deactivate();
            mRegistry.handleLifecycleEvent(Lifecycle.ON_STOP);
        }
    };

    private boolean mNeedToDispatchPause = false;

    private final Runnable mPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
        }
    };

    private final ActivityInitializationListener mInitializationListener =
            new ActivityInitializationListener() {
                @Override
                public void onCreate() {
                    mRegistry.handleLifecycleEvent(Lifecycle.ON_CREATE);
                }

                @Override
                public void onStart() {
                    mRegistry.handleLifecycleEvent(Lifecycle.ON_START);
                }

                @Override
                public void onResume() {
                    mRegistry.handleLifecycleEvent(Lifecycle.ON_RESUME);
                }
            };

    /**
     * @param activity activity, lifecycle of which should be dispatched
     * @param provider {@link LifecycleProvider} for this activity
     */
    public ActivityLifecycleDispatcher(Activity activity, LifecycleProvider provider) {
        mActivity = activity;
        mRegistry = new LifecycleRegistry(provider);
    }

    /**
     * Must be called right after super.onCreate call in {@link Activity#onCreate(Bundle)}.
     */
    public void onActivityPostSuperOnCreate() {
        // loader might have been retained - kill it, kill it!
        mActivity.getLoaderManager().destroyLoader(LOADER_ID);
        ReportInitializationFragment fragment =
                ReportInitializationFragment.getOrCreateFor(mActivity);
        fragment.setActivityListener(mInitializationListener);
    }

    /**
     * Must be the first call in {@link Activity#onResume()} method,
     * even before super.onResume call.
     */
    public void onActivityPreSuperOnResume() {
        dispatchPauseIfNeeded();
        // this listener is called once and then it is removed, so we should set it on every resume.
        registerPauseListener();
    }

    private Object createPauseListener() {
        InvocationHandler handler = new InvocationHandler() {
            Object mObject = new Object();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("onPaused")) {
                    dispatchPauseIfNeeded();
                    return null;
                }
                // all other methods are default object's methods, so pass them to our internal
                // object
                return method.invoke(mObject, args);
            }
        };

        return Proxy.newProxyInstance(LifecycleActivity.class.getClassLoader(),
                new Class[]{sPauseListenerClass}, handler);
    }

    private void invokePauseListenerLifecycleMethod(Method method) {
        //noinspection TryWithIdenticalCatches
        try {
            Object activityThread = sCurrentActivityThreadMethod.invoke(sActivityThreadClass);
            method.invoke(activityThread, mActivity, mPauseListener);
        } catch (IllegalAccessException e) {
            Log.w(LOG_TAG, "Failed to register a pause listener", e);
        } catch (InvocationTargetException e) {
            Log.w(LOG_TAG, "Failed to register a pause listener", e);
        }
    }

    private void registerPauseListener() {
        invokePauseListenerLifecycleMethod(sRegisterPauseListenerMethod);
    }

    private void unregisterPauseListener() {
        invokePauseListenerLifecycleMethod(sUnregisterPauseListenerMethod);
    }

    void dispatchPauseIfNeeded() {
        if (mNeedToDispatchPause) {
            mRegistry.handleLifecycleEvent(Lifecycle.ON_PAUSE);
            // not sure how we ended up here, so make sure to clean up our listener.
            unregisterPauseListener();
            mNeedToDispatchPause = false;
        }
    }

    /**
     * Must be the first call in {@link Activity#onPause()} method, even before super.onPause call.
     */
    public void onActivityPreSuperOnPause() {
        mNeedToDispatchPause = true;
        if (mPauseHandler == null) {
            mPauseHandler = new Handler();
        }
        mPauseHandler.postAtFrontOfQueue(mPauseRunnable);
    }

    /**
     * Must be the first call in {@link Activity#onStop()} method, even before super.onStop call.
     */
    @SuppressWarnings("deprecation")
    public void onActivityPreSuperOnStop() {
        dispatchPauseIfNeeded();
        // clean up internal state associated with that cursor
        mActivity.stopManagingCursor(mReportStopCursor);
        // add it back
        mActivity.startManagingCursor(mReportStopCursor);
    }

    /**
     * Must be the first call in {@link Activity#onDestroy()} method,
     * even before super.OnDestroy call.
     */
    public void onActivityPreSuperOnDestroy() {
        mActivity.getLoaderManager().destroyLoader(LOADER_ID);
        // Create two loaders on the same ID, so first one will be come inactive.
        // After onDestroy method, inactive loaders will be 100% destroyed.
        mActivity.getLoaderManager().initLoader(LOADER_ID, null,
                new EmptyLoaderCallbacks() {
                    @Override
                    public Loader onCreateLoader(int id, Bundle args) {
                        return new ReportOnDestroyLoader(mRegistry, mActivity);
                    }
                });

        mActivity.getLoaderManager().restartLoader(LOADER_ID, null, new EmptyLoaderCallbacks() {
            @Override
            public Loader onCreateLoader(int id, Bundle args) {
                return new Loader(mActivity.getApplicationContext());
            }
        });
    }

    /**
     * @return {@link Lifecycle} for the given activity
     */
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    abstract static class EmptyLoaderCallbacks implements LoaderManager.LoaderCallbacks {
        @Override
        public void onLoadFinished(Loader loader, Object data) {
        }

        @Override
        public void onLoaderReset(Loader loader) {
        }
    }

    static class ReportOnDestroyLoader extends Loader {

        LifecycleRegistry mRegistry;

        ReportOnDestroyLoader(LifecycleRegistry registry, Context context) {
            super(context);
            mRegistry = registry;
        }

        @Override
        protected void onReset() {
            super.onReset();
            if (mRegistry != null) {
                mRegistry.handleLifecycleEvent(Lifecycle.ON_DESTROY);
                mRegistry = null;
            }
        }
    }
}
