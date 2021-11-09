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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A registry that stores {@link ActivityResultCallback activity result callbacks} for
 * {@link ActivityResultCaller#registerForActivityResult registered calls}.
 *
 * You can create your own instance for testing by overriding {@link #onLaunch} and calling
 * {@link #dispatchResult} immediately within it, thus skipping the actual
 * {@link Activity#startActivityForResult} call.
 *
 * When testing, make sure to explicitly provide a registry instance whenever calling
 * {@link ActivityResultCaller#registerForActivityResult}, to be able to inject a test instance.
 */
public abstract class ActivityResultRegistry {
    private static final String KEY_COMPONENT_ACTIVITY_REGISTERED_RCS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_RCS";
    private static final String KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS";
    private static final String KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS =
            "KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS";
    private static final String KEY_COMPONENT_ACTIVITY_PENDING_RESULTS =
            "KEY_COMPONENT_ACTIVITY_PENDING_RESULT";
    private static final String KEY_COMPONENT_ACTIVITY_RANDOM_OBJECT =
            "KEY_COMPONENT_ACTIVITY_RANDOM_OBJECT";

    private static final String LOG_TAG = "ActivityResultRegistry";

    // Use upper 16 bits for request codes
    private static final int INITIAL_REQUEST_CODE_VALUE = 0x00010000;
    private Random mRandom = new Random();

    private final Map<Integer, String> mRcToKey = new HashMap<>();
    final Map<String, Integer> mKeyToRc = new HashMap<>();
    private final Map<String, LifecycleContainer> mKeyToLifecycleContainers = new HashMap<>();
    ArrayList<String> mLaunchedKeys = new ArrayList<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final transient Map<String, CallbackAndContract<?>> mKeyToCallback = new HashMap<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Map<String, Object> mParsedPendingResults = new HashMap<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Bundle/*<String, ActivityResult>*/ mPendingResults = new Bundle();

    /**
     * Start the process of executing an {@link ActivityResultContract} in a type-safe way,
     * using the provided {@link ActivityResultContract contract}.
     *
     * @param requestCode request code to use
     * @param contract contract to use for type conversions
     * @param input input required to execute an ActivityResultContract.
     * @param options Additional options for how the Activity should be started.
     */
    @MainThread
    public abstract <I, O> void onLaunch(
            int requestCode,
            @NonNull ActivityResultContract<I, O> contract,
            @SuppressLint("UnknownNullness") I input,
            @Nullable ActivityOptionsCompat options);

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * {@link ActivityResultCaller#registerForActivityResult}.
     *
     * @param key a unique string key identifying this call
     * @param lifecycleOwner a {@link LifecycleOwner} that makes this call.
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     *
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    @NonNull
    public final <I, O> ActivityResultLauncher<I> register(
            @NonNull final String key,
            @NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback) {

        Lifecycle lifecycle = lifecycleOwner.getLifecycle();

        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            throw new IllegalStateException("LifecycleOwner " + lifecycleOwner + " is "
                    + "attempting to register while current state is "
                    + lifecycle.getCurrentState() + ". LifecycleOwners must call register before "
                    + "they are STARTED.");
        }

        registerKey(key);
        LifecycleContainer lifecycleContainer = mKeyToLifecycleContainers.get(key);
        if (lifecycleContainer == null) {
            lifecycleContainer = new LifecycleContainer(lifecycle);
        }
        LifecycleEventObserver observer = new LifecycleEventObserver() {
            @Override
            public void onStateChanged(
                    @NonNull LifecycleOwner lifecycleOwner,
                    @NonNull Lifecycle.Event event) {
                if (Lifecycle.Event.ON_START.equals(event)) {
                    mKeyToCallback.put(key, new CallbackAndContract<>(callback, contract));
                    if (mParsedPendingResults.containsKey(key)) {
                        @SuppressWarnings("unchecked")
                        final O parsedPendingResult = (O) mParsedPendingResults.get(key);
                        mParsedPendingResults.remove(key);
                        callback.onActivityResult(parsedPendingResult);
                    }
                    final ActivityResult pendingResult = mPendingResults.getParcelable(key);
                    if (pendingResult != null) {
                        mPendingResults.remove(key);
                        callback.onActivityResult(contract.parseResult(
                                pendingResult.getResultCode(),
                                pendingResult.getData()));
                    }
                } else if (Lifecycle.Event.ON_STOP.equals(event)) {
                    mKeyToCallback.remove(key);
                } else if (Lifecycle.Event.ON_DESTROY.equals(event)) {
                    unregister(key);
                }
            }
        };
        lifecycleContainer.addObserver(observer);
        mKeyToLifecycleContainers.put(key, lifecycleContainer);

        return new ActivityResultLauncher<I>() {
            @Override
            public void launch(I input, @Nullable ActivityOptionsCompat options) {
                Integer innerCode = mKeyToRc.get(key);
                if (innerCode == null) {
                    throw new IllegalStateException("Attempting to launch an unregistered "
                            + "ActivityResultLauncher with contract " + contract + " and input "
                            + input + ". You must ensure the ActivityResultLauncher is registered "
                            + "before calling launch().");
                }
                mLaunchedKeys.add(key);
                try {
                    onLaunch(innerCode, contract, input, options);
                } catch (Exception e) {
                    mLaunchedKeys.remove(key);
                    throw e;
                }
            }

            @Override
            public void unregister() {
                ActivityResultRegistry.this.unregister(key);
            }

            @NonNull
            @Override
            public ActivityResultContract<I, ?> getContract() {
                return contract;
            }
        };
    }

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * {@link ActivityResultCaller#registerForActivityResult}.
     *
     * When calling this, you must call {@link ActivityResultLauncher#unregister()} on the
     * returned {@link ActivityResultLauncher} when the launcher is no longer needed to
     * release any values that might be captured in the registered callback.
     *
     * @param key a unique string key identifying this call
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     *
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    @NonNull
    public final <I, O> ActivityResultLauncher<I> register(
            @NonNull final String key,
            @NonNull final ActivityResultContract<I, O> contract,
            @NonNull final ActivityResultCallback<O> callback) {
        registerKey(key);
        mKeyToCallback.put(key, new CallbackAndContract<>(callback, contract));

        if (mParsedPendingResults.containsKey(key)) {
            @SuppressWarnings("unchecked")
            final O parsedPendingResult = (O) mParsedPendingResults.get(key);
            mParsedPendingResults.remove(key);
            callback.onActivityResult(parsedPendingResult);
        }
        final ActivityResult pendingResult = mPendingResults.getParcelable(key);
        if (pendingResult != null) {
            mPendingResults.remove(key);
            callback.onActivityResult(contract.parseResult(
                    pendingResult.getResultCode(),
                    pendingResult.getData()));
        }

        return new ActivityResultLauncher<I>() {
            @Override
            public void launch(I input, @Nullable ActivityOptionsCompat options) {
                Integer innerCode = mKeyToRc.get(key);
                if (innerCode == null) {
                    throw new IllegalStateException("Attempting to launch an unregistered "
                            + "ActivityResultLauncher with contract " + contract + " and input "
                            + input + ". You must ensure the ActivityResultLauncher is registered "
                            + "before calling launch().");
                }
                mLaunchedKeys.add(key);
                onLaunch(innerCode, contract, input, options);
            }

            @Override
            public void unregister() {
                ActivityResultRegistry.this.unregister(key);
            }

            @NonNull
            @Override
            public ActivityResultContract<I, ?> getContract() {
                return contract;
            }
        };
    }

    /**
     * Unregister a callback previously registered with {@link #register}. This shouldn't be
     * called directly, but instead through {@link ActivityResultLauncher#unregister()}.
     *
     * @param key the unique key used when registering a callback.
     */
    @MainThread
    final void unregister(@NonNull String key) {
        if (!mLaunchedKeys.contains(key)) {
            // Only remove the key -> requestCode mapping if there isn't a launch in flight
            Integer rc = mKeyToRc.remove(key);
            if (rc != null) {
                mRcToKey.remove(rc);
            }
        }
        mKeyToCallback.remove(key);
        if (mParsedPendingResults.containsKey(key)) {
            Log.w(LOG_TAG, "Dropping pending result for request " + key + ": "
                    + mParsedPendingResults.get(key));
            mParsedPendingResults.remove(key);
        }
        if (mPendingResults.containsKey(key)) {
            Log.w(LOG_TAG, "Dropping pending result for request " + key + ": "
                    + mPendingResults.<ActivityResult>getParcelable(key));
            mPendingResults.remove(key);
        }
        LifecycleContainer lifecycleContainer = mKeyToLifecycleContainers.get(key);
        if (lifecycleContainer != null) {
            lifecycleContainer.clearObservers();
            mKeyToLifecycleContainers.remove(key);
        }
    }

    /**
     * Save the state of this registry in the given {@link Bundle}
     *
     * @param outState the place to put state into
     */
    public final void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putIntegerArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_RCS,
                new ArrayList<>(mKeyToRc.values()));
        outState.putStringArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS,
                new ArrayList<>(mKeyToRc.keySet()));
        outState.putStringArrayList(KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS,
                new ArrayList<>(mLaunchedKeys));
        outState.putBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS,
                (Bundle) mPendingResults.clone());
        outState.putSerializable(KEY_COMPONENT_ACTIVITY_RANDOM_OBJECT, mRandom);
    }

    /**
     * Restore the state of this registry from the given {@link Bundle}
     *
     * @param savedInstanceState the place to restore from
     */
    public final void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
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
        mLaunchedKeys =
                savedInstanceState.getStringArrayList(KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS);
        mRandom = (Random) savedInstanceState.getSerializable(KEY_COMPONENT_ACTIVITY_RANDOM_OBJECT);
        mPendingResults.putAll(
                savedInstanceState.getBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS));
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            // Developers may have already registered with this same key by the time we restore
            // state, which caused us to generate a new requestCode that doesn't match what we're
            // about to restore. Clear out the new requestCode to ensure that we use the
            // previously saved requestCode.
            if (mKeyToRc.containsKey(key)) {
                Integer newRequestCode = mKeyToRc.remove(key);
                // On the chance that developers have already called launch() with this new
                // requestCode, keep the mapping around temporarily to ensure the result is
                // properly delivered to both the new requestCode and the restored requestCode
                if (!mPendingResults.containsKey(key)) {
                    mRcToKey.remove(newRequestCode);
                }
            }
            bindRcKey(rcs.get(i), keys.get(i));
        }
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
    public final boolean dispatchResult(int requestCode, int resultCode, @Nullable Intent data) {
        String key = mRcToKey.get(requestCode);
        if (key == null) {
            return false;
        }
        doDispatch(key, resultCode, data, mKeyToCallback.get(key));
        return true;
    }

    /**
     * Dispatch a result object to the callback on record.
     *
     * @param requestCode request code to identify the callback
     * @param result the result to propagate
     *
     * @return true if there is a callback registered for the given request code, false otherwise.
     */
    @MainThread
    public final <O> boolean dispatchResult(int requestCode,
            @SuppressLint("UnknownNullness") O result) {
        String key = mRcToKey.get(requestCode);
        if (key == null) {
            return false;
        }

        CallbackAndContract<?> callbackAndContract = mKeyToCallback.get(key);
        if (callbackAndContract == null || callbackAndContract.mCallback == null) {
            // Remove any pending result
            mPendingResults.remove(key);
            // And add these pre-parsed pending results in their place
            mParsedPendingResults.put(key, result);
        } else {
            @SuppressWarnings("unchecked")
            ActivityResultCallback<O> callback =
                    (ActivityResultCallback<O>) callbackAndContract.mCallback;
            if (mLaunchedKeys.remove(key)) {
                callback.onActivityResult(result);
            }
        }
        return true;
    }

    private <O> void doDispatch(String key, int resultCode, @Nullable Intent data,
            @Nullable CallbackAndContract<O> callbackAndContract) {
        if (callbackAndContract != null && callbackAndContract.mCallback != null
                && mLaunchedKeys.contains(key)) {
            ActivityResultCallback<O> callback = callbackAndContract.mCallback;
            ActivityResultContract<?, O> contract = callbackAndContract.mContract;
            callback.onActivityResult(contract.parseResult(resultCode, data));
            mLaunchedKeys.remove(key);
        } else {
            // Remove any parsed pending result
            mParsedPendingResults.remove(key);
            // And add these pending results in their place
            mPendingResults.putParcelable(key, new ActivityResult(resultCode, data));
        }
    }

    private void registerKey(String key) {
        Integer existing = mKeyToRc.get(key);
        if (existing != null) {
            return;
        }
        int rc = generateRandomNumber();
        bindRcKey(rc, key);
    }

    /**
     * Generate a random number between the initial value (00010000) inclusive, and the max
     * integer value. If that number is already an existing request code, generate another until
     * we find one that is new.
     *
     * @return the number
     */
    private int generateRandomNumber() {
        int number = mRandom.nextInt((Integer.MAX_VALUE - INITIAL_REQUEST_CODE_VALUE) + 1)
                + INITIAL_REQUEST_CODE_VALUE;
        while (mRcToKey.containsKey(number)) {
            number = mRandom.nextInt((Integer.MAX_VALUE - INITIAL_REQUEST_CODE_VALUE) + 1)
                    + INITIAL_REQUEST_CODE_VALUE;
        }
        return number;
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

    private static class LifecycleContainer {
        final Lifecycle mLifecycle;
        private final ArrayList<LifecycleEventObserver> mObservers;

        LifecycleContainer(@NonNull Lifecycle lifecycle) {
            mLifecycle = lifecycle;
            mObservers = new ArrayList<>();
        }

        void addObserver(@NonNull LifecycleEventObserver observer) {
            mLifecycle.addObserver(observer);
            mObservers.add(observer);
        }

        void clearObservers() {
            for (LifecycleEventObserver observer: mObservers) {
                mLifecycle.removeObserver(observer);
            }
            mObservers.clear();
        }
    }
}
