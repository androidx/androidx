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
package androidx.activity.result

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.random.Random.Default.nextInt

/**
 * A registry that stores [activity result callbacks][ActivityResultCallback] for
 * [registered calls][ActivityResultCaller.registerForActivityResult].
 *
 * You can create your own instance for testing by overriding [onLaunch] and calling
 * [dispatchResult] immediately within it, thus skipping the actual
 * [Activity.startActivityForResult] call.
 *
 * When testing, make sure to explicitly provide a registry instance whenever calling
 * [ActivityResultCaller.registerForActivityResult], to be able to inject a test instance.
 */
abstract class ActivityResultRegistry {
    private val rcToKey = mutableMapOf<Int, String>()
    private val keyToRc = mutableMapOf<String, Int>()
    private val keyToLifecycleContainers = mutableMapOf<String, LifecycleContainer>()
    private val launchedKeys = mutableListOf<String>()
    @Transient private val keyToCallback = mutableMapOf<String, CallbackAndContract<*>>()
    private val parsedPendingResults = mutableMapOf<String, Any?>()
    /** Storage for the set of key to ActivityResult instances that have yet to be delivered */
    private val pendingResults = Bundle()

    /**
     * Start the process of executing an [ActivityResultContract] in a type-safe way, using the
     * provided [contract][ActivityResultContract].
     *
     * @param requestCode request code to use
     * @param contract contract to use for type conversions
     * @param input input required to execute an ActivityResultContract.
     * @param options Additional options for how the Activity should be started.
     */
    @MainThread
    abstract fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    )

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * [ActivityResultCaller.registerForActivityResult].
     *
     * @param key a unique string key identifying this call
     * @param lifecycleOwner a [LifecycleOwner] that makes this call.
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    fun <I, O> register(
        key: String,
        lifecycleOwner: LifecycleOwner,
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        val lifecycle = lifecycleOwner.lifecycle
        check(!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "LifecycleOwner $lifecycleOwner is attempting to register while current state is " +
                "${lifecycle.currentState}. LifecycleOwners must call register before they " +
                "are STARTED."
        }
        registerKey(key)
        val lifecycleContainer = keyToLifecycleContainers[key] ?: LifecycleContainer(lifecycle)
        val observer = LifecycleEventObserver { _, event ->
            if (Lifecycle.Event.ON_START == event) {
                keyToCallback[key] = CallbackAndContract(callback, contract)
                if (parsedPendingResults.containsKey(key)) {
                    @Suppress("UNCHECKED_CAST")
                    val parsedPendingResult = parsedPendingResults[key] as O
                    parsedPendingResults.remove(key)
                    callback.onActivityResult(parsedPendingResult)
                }
                val pendingResult =
                    BundleCompat.getParcelable(pendingResults, key, ActivityResult::class.java)
                if (pendingResult != null) {
                    pendingResults.remove(key)
                    callback.onActivityResult(
                        contract.parseResult(pendingResult.resultCode, pendingResult.data)
                    )
                }
            } else if (Lifecycle.Event.ON_STOP == event) {
                keyToCallback.remove(key)
            } else if (Lifecycle.Event.ON_DESTROY == event) {
                unregister(key)
            }
        }
        lifecycleContainer.addObserver(observer)
        keyToLifecycleContainers[key] = lifecycleContainer
        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                val innerCode =
                    checkNotNull(keyToRc[key]) {
                        "Attempting to launch an unregistered ActivityResultLauncher with " +
                            "contract $contract and input $input. You must ensure the " +
                            "ActivityResultLauncher is registered before calling launch()."
                    }
                launchedKeys.add(key)
                try {
                    onLaunch(innerCode, contract, input, options)
                } catch (e: Exception) {
                    launchedKeys.remove(key)
                    throw e
                }
            }

            override fun unregister() {
                this@ActivityResultRegistry.unregister(key)
            }

            override val contract: ActivityResultContract<I, *>
                get() = contract
        }
    }

    /**
     * Register a new callback with this registry.
     *
     * This is normally called by a higher level convenience methods like
     * [ActivityResultCaller.registerForActivityResult].
     *
     * When calling this, you must call [ActivityResultLauncher.unregister] on the returned
     * [ActivityResultLauncher] when the launcher is no longer needed to release any values that
     * might be captured in the registered callback.
     *
     * @param key a unique string key identifying this call
     * @param contract the contract specifying input/output types of the call
     * @param callback the activity result callback
     * @return a launcher that can be used to execute an ActivityResultContract.
     */
    fun <I, O> register(
        key: String,
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        registerKey(key)
        keyToCallback[key] = CallbackAndContract(callback, contract)
        if (parsedPendingResults.containsKey(key)) {
            @Suppress("UNCHECKED_CAST") val parsedPendingResult = parsedPendingResults[key] as O
            parsedPendingResults.remove(key)
            callback.onActivityResult(parsedPendingResult)
        }
        val pendingResult =
            BundleCompat.getParcelable(pendingResults, key, ActivityResult::class.java)
        if (pendingResult != null) {
            pendingResults.remove(key)
            callback.onActivityResult(
                contract.parseResult(pendingResult.resultCode, pendingResult.data)
            )
        }
        return object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                val innerCode =
                    checkNotNull(keyToRc[key]) {
                        "Attempting to launch an unregistered ActivityResultLauncher with " +
                            "contract $contract and input $input. You must ensure the " +
                            "ActivityResultLauncher is registered before calling launch()."
                    }
                launchedKeys.add(key)
                try {
                    onLaunch(innerCode, contract, input, options)
                } catch (e: Exception) {
                    launchedKeys.remove(key)
                    throw e
                }
            }

            override fun unregister() {
                this@ActivityResultRegistry.unregister(key)
            }

            override val contract: ActivityResultContract<I, *>
                get() = contract
        }
    }

    /**
     * Unregister a callback previously registered with [register]. This shouldn't be called
     * directly, but instead through [ActivityResultLauncher.unregister].
     *
     * @param key the unique key used when registering a callback.
     */
    @MainThread
    internal fun unregister(key: String) {
        if (!launchedKeys.contains(key)) {
            // Only remove the key -> requestCode mapping if there isn't a launch in flight
            val rc = keyToRc.remove(key)
            if (rc != null) {
                rcToKey.remove(rc)
            }
        }
        keyToCallback.remove(key)
        if (parsedPendingResults.containsKey(key)) {
            Log.w(LOG_TAG, "Dropping pending result for request $key: ${parsedPendingResults[key]}")
            parsedPendingResults.remove(key)
        }
        if (pendingResults.containsKey(key)) {
            val pendingResult =
                BundleCompat.getParcelable(pendingResults, key, ActivityResult::class.java)
            Log.w(LOG_TAG, "Dropping pending result for request $key: $pendingResult")
            pendingResults.remove(key)
        }
        val lifecycleContainer = keyToLifecycleContainers[key]
        if (lifecycleContainer != null) {
            lifecycleContainer.clearObservers()
            keyToLifecycleContainers.remove(key)
        }
    }

    /**
     * Save the state of this registry in the given [Bundle]
     *
     * @param outState the place to put state into
     */
    fun onSaveInstanceState(outState: Bundle) {
        outState.putIntegerArrayList(
            KEY_COMPONENT_ACTIVITY_REGISTERED_RCS,
            ArrayList(keyToRc.values)
        )
        outState.putStringArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS, ArrayList(keyToRc.keys))
        outState.putStringArrayList(KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS, ArrayList(launchedKeys))
        outState.putBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS, Bundle(pendingResults))
    }

    /**
     * Restore the state of this registry from the given [Bundle]
     *
     * @param savedInstanceState the place to restore from
     */
    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }
        val rcs = savedInstanceState.getIntegerArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_RCS)
        val keys = savedInstanceState.getStringArrayList(KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS)
        if (keys == null || rcs == null) {
            return
        }
        val restoredLaunchedKeys =
            savedInstanceState.getStringArrayList(KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS)
        if (restoredLaunchedKeys != null) {
            launchedKeys.addAll(restoredLaunchedKeys)
        }
        val restoredPendingResults =
            savedInstanceState.getBundle(KEY_COMPONENT_ACTIVITY_PENDING_RESULTS)
        if (restoredPendingResults != null) {
            pendingResults.putAll(restoredPendingResults)
        }
        for (i in keys.indices) {
            val key = keys[i]
            // Developers may have already registered with this same key by the time we restore
            // state, which caused us to generate a new requestCode that doesn't match what we're
            // about to restore. Clear out the new requestCode to ensure that we use the
            // previously saved requestCode.
            if (keyToRc.containsKey(key)) {
                val newRequestCode = keyToRc.remove(key)
                // On the chance that developers have already called launch() with this new
                // requestCode, keep the mapping around temporarily to ensure the result is
                // properly delivered to both the new requestCode and the restored requestCode
                if (!pendingResults.containsKey(key)) {
                    rcToKey.remove(newRequestCode)
                }
            }
            bindRcKey(rcs[i], keys[i])
        }
    }

    /**
     * Dispatch a result received via [Activity.onActivityResult] to the callback on record, or
     * store the result if callback was not yet registered.
     *
     * @param requestCode request code to identify the callback
     * @param resultCode status to indicate the success of the operation
     * @param data an intent that carries the result data
     * @return whether there was a callback was registered for the given request code which was or
     *   will be called.
     */
    @MainThread
    fun dispatchResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val key = rcToKey[requestCode] ?: return false
        doDispatch(key, resultCode, data, keyToCallback[key])
        return true
    }

    /**
     * Dispatch a result object to the callback on record.
     *
     * @param requestCode request code to identify the callback
     * @param result the result to propagate
     * @return true if there is a callback registered for the given request code, false otherwise.
     */
    @MainThread
    fun <O> dispatchResult(requestCode: Int, result: O): Boolean {
        val key = rcToKey[requestCode] ?: return false
        val callbackAndContract = keyToCallback[key]
        if (callbackAndContract?.callback == null) {
            // Remove any pending result
            pendingResults.remove(key)
            // And add these pre-parsed pending results in their place
            parsedPendingResults[key] = result
        } else {
            @Suppress("UNCHECKED_CAST")
            val callback = callbackAndContract.callback as ActivityResultCallback<O>
            if (launchedKeys.remove(key)) {
                callback.onActivityResult(result)
            }
        }
        return true
    }

    private fun <O> doDispatch(
        key: String,
        resultCode: Int,
        data: Intent?,
        callbackAndContract: CallbackAndContract<O>?
    ) {
        if (callbackAndContract?.callback != null && launchedKeys.contains(key)) {
            val callback = callbackAndContract.callback
            val contract = callbackAndContract.contract
            callback.onActivityResult(contract.parseResult(resultCode, data))
            launchedKeys.remove(key)
        } else {
            // Remove any parsed pending result
            parsedPendingResults.remove(key)
            // And add these pending results in their place
            pendingResults.putParcelable(key, ActivityResult(resultCode, data))
        }
    }

    private fun registerKey(key: String) {
        val existing = keyToRc[key]
        if (existing != null) {
            return
        }
        val rc = generateRandomNumber()
        bindRcKey(rc, key)
    }

    /**
     * Generate a random number between the initial value (00010000) inclusive, and the max integer
     * value. If that number is already an existing request code, generate another until we find one
     * that is new.
     *
     * @return the number
     */
    private fun generateRandomNumber(): Int {
        return generateSequence {
                nextInt(Int.MAX_VALUE - INITIAL_REQUEST_CODE_VALUE + 1) + INITIAL_REQUEST_CODE_VALUE
            }
            .first { number -> !rcToKey.containsKey(number) }
    }

    private fun bindRcKey(rc: Int, key: String) {
        rcToKey[rc] = key
        keyToRc[key] = rc
    }

    private class CallbackAndContract<O>(
        val callback: ActivityResultCallback<O>,
        val contract: ActivityResultContract<*, O>
    )

    private class LifecycleContainer(val lifecycle: Lifecycle) {
        private val observers = mutableListOf<LifecycleEventObserver>()

        fun addObserver(observer: LifecycleEventObserver) {
            lifecycle.addObserver(observer)
            observers.add(observer)
        }

        fun clearObservers() {
            observers.forEach { observer -> lifecycle.removeObserver(observer) }
            observers.clear()
        }
    }

    private companion object {
        private const val KEY_COMPONENT_ACTIVITY_REGISTERED_RCS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_RCS"
        private const val KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS =
            "KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS"
        private const val KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS =
            "KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS"
        private const val KEY_COMPONENT_ACTIVITY_PENDING_RESULTS =
            "KEY_COMPONENT_ACTIVITY_PENDING_RESULT"
        private const val LOG_TAG = "ActivityResultRegistry"

        // Use upper 16 bits for request codes
        private const val INITIAL_REQUEST_CODE_VALUE = 0x00010000
    }
}
