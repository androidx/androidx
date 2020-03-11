/*
 * Copyright (C) 2020 The Android Open Source Project
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


package androidx.activity.result;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A registry that stores {@link ActivityResultCallback activity result callbacks} for
 * {@link ActivityResultCaller#prepareCall prepared calls}.
 *
 * You can create your own instance for testing by overriding {@link #invoke} and calling
 * {@link #dispatchResult} immediately within it, thus skipping the actual
 * {@link Activity#startActivityForResult} call.
 *
 * When testing, make sure to explicitly provide a registry instance whenever calling
 * {@link ActivityResultCaller#prepareCall}, to be able to inject a test instance.
 */
public abstract class ActivityResultRegistry {
    private static final String KEY_COMPONENT_ACTIVITY_REGISTERED_RCS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_RCS";
    private static final String KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS";
    private static final String KEY_COMPONENT_ACTIVITY_PENDING_RESULTS =
            "KEY_COMPONENT_ACTIVITY_PENDING_RESULT";

    private static final String LOG_TAG = "ActivityResultRegistry";

    private final AtomicInteger mNextRc = new AtomicInteger(0);
    private final Map<Integer, String> mRcToKey = new HashMap<Integer, String>();
    private final Map<String, Integer> mKeyToRc = new HashMap<String, Integer>();

    private final transient Map<String, CallbackAndContract<?>> mKeyToCallback =
            new HashMap<String, CallbackAndContract<?>>();

    private final Bundle/*<String, ActivityResult>*/ mPendingResults = new Bundle();

    /**
     * Start the process of executing an {@link ActivityResultContract} in a type-safe way,
     * using the provided {@link ActivityResultContract contract}.
     *
     * @param requestCode request code to use
     * @param contract contract to use for type conversions
     * @param input input required to execute an ActivityResultContract.
     */
    @MainThread
    public abstract <I, O> void invoke(
            int requestCode,
            @NonNull ActivityResultContract<I, O> contract,
            @SuppressLint("UnknownNullness") I input);

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * {@link ActivityResultCaller#prepareCall}.
     *
     * @param key a unique string key identifying this call
     * @param lifecycleOwner a {@link LifecycleOwner} that makes this call.
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     *
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    @NonNull
    public <I, O> ActivityResultLauncher<I> registerActivityResultCallback(
            @NonNull final String key,
            @NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback) {

        final int requestCode = registerKey(key);
        mKeyToCallback.put(key, new CallbackAndContract<O>(callback, contract));

        Lifecycle lifecycle = lifecycleOwner.getLifecycle();

        final ActivityResult pendingResult = mPendingResults.getParcelable(key);
        if (pendingResult != null) {
            mPendingResults.remove(key);
            if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                callback.onActivityResult(contract.parseResult(
                        pendingResult.getResultCode(),
                        pendingResult.getData()));
            } else {
                lifecycle.addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(
                            @NonNull LifecycleOwner lifecycleOwner,
                            @NonNull Lifecycle.Event event) {
                        if (Lifecycle.Event.ON_CREATE.equals(event)) {
                            callback.onActivityResult(contract.parseResult(
                                    pendingResult.getResultCode(),
                                    pendingResult.getData()));
                        }
                    }
                });
            }
        }

        lifecycle.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
                    @NonNull Lifecycle.Event event) {
                if (Lifecycle.Event.ON_DESTROY.equals(event)) {
                    unregisterActivityResultCallback(key);
                }
            }
        });

        return new ActivityResultLauncher<I>() {
            @Override
            public void launch(I input) {
                invoke(requestCode, contract, input);
            }

            @Override
            public void dispose() {
                unregisterActivityResultCallback(key);
            }
        };
    }

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * {@link ActivityResultCaller#prepareCall}.
     *
     * When calling this, make sure to call {@link #unregisterActivityResultCallback} when the
     * launcher is no longer needed to release any values that might be captured in the
     * registered callback.
     *
     * @param key a unique string key identifying this call
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     *
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    @NonNull
    public <I, O> ActivityResultLauncher<I> registerActivityResultCallback(
            @NonNull final String key,
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback) {
        final int requestCode = registerKey(key);
        mKeyToCallback.put(key, new CallbackAndContract<O>(callback, contract));

        final ActivityResult pendingResult = mPendingResults.getParcelable(key);
        if (pendingResult != null) {
            mPendingResults.remove(key);
            callback.onActivityResult(contract.parseResult(
                    pendingResult.getResultCode(),
                    pendingResult.getData()));
        }

        return new ActivityResultLauncher<I>() {
            @Override
            public void launch(I input) {
                invoke(requestCode, contract, input);
            }

            @Override
            public void dispose() {
                unregisterActivityResultCallback(key);
            }
        };
    }

    /**
     * Unregister a callback previously registered with {@link #registerActivityResultCallback}
     *
     * @param key the unique key used when registering a callback.
     */
    @MainThread
    public void unregisterActivityResultCallback(@NonNull String key) {
        Integer rc = mKeyToRc.remove(key);
        if (rc != null) {
            mRcToKey.remove(rc);
        }
        mKeyToCallback.remove(key);
        if (mPendingResults.containsKey(key)) {
            Log.w(LOG_TAG, "Dropping pending result for request " + key + ": "
                    + mPendingResults.<ActivityResult>getParcelable(key));
        }
    }

    /**
     * Save the state of this registry in the given {@link Bundle}
     *
     * @param outState the place to put state into
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putIntegerArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_RCS,
                new ArrayList<Integer>(mRcToKey.keySet()));
        outState.putStringArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS,
                new ArrayList<String>(mRcToKey.values()));
        outState.putBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS, mPendingResults);
    }

    /**
     * Restore the state of this registry from the given {@link Bundle}
     *
     * @param savedInstanceState the place to restore from
     */
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        ArrayList<Integer> rcs =
                savedInstanceState.getIntegerArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_RCS);
        ArrayList<String> keys =
                savedInstanceState.getStringArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS);
        if (keys == null || rcs == null) {
            return;
        }
        int numKeys = keys.size();
        for (int i = 0; i < numKeys; i++) {
            bindRcKey(rcs.get(i), keys.get(i));
        }
        mNextRc.set(numKeys);
        mPendingResults.putAll(
                savedInstanceState.getBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS));
    }

    /**
     * Dispatch a result received via {@link Activity#onActivityResult} to the callback on record,
     * or store the result if callback was not yet registered.
     *
     * @param requestCode request code to identify the callback
     * @param resultCode status to indicate the success of the operation
     * @param data an intent that carries the result data
     *
     * @return whether there was a callback was registered for the given request code which was
     * or will be called.
     */
    @MainThread
    public boolean dispatchResult(int requestCode, int resultCode, @Nullable Intent data) {
        String key = mRcToKey.get(requestCode);
        if (key == null) {
            return false;
        }
        doDispatch(key, resultCode, data, mKeyToCallback.get(key));
        return true;
    }

    private <O> void doDispatch(String key, int resultCode, @Nullable Intent data,
            CallbackAndContract<O> callbackAndContract) {
        ActivityResultCallback<O> callback = callbackAndContract.mCallback;
        ActivityResultContract<?, O> contract = callbackAndContract.mContract;
        if (callback != null) {
            callback.onActivityResult(contract.parseResult(resultCode, data));
        } else {
            mPendingResults.putParcelable(key, new ActivityResult(resultCode, data));
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY)
    public void clearCallbacks() {
        mKeyToCallback.clear();
    }

    private int registerKey(String key) {
        Integer existing = mKeyToRc.get(key);
        if (existing != null) {
            return existing;
        }
        int rc = mNextRc.getAndIncrement();
        bindRcKey(rc, key);
        return rc;
    }

    private void bindRcKey(int rc, String key) {
        mRcToKey.put(rc, key);
        mKeyToRc.put(key, rc);
    }

    private static class CallbackAndContract<O> {
        final ActivityResultCallback<O> mCallback;
        final ActivityResultContract<?, O> mContract;

        CallbackAndContract(
                ActivityResultCallback<O> callback,
                ActivityResultContract<?, O> contract) {
            mCallback = callback;
            mContract = contract;
        }
    }
}
