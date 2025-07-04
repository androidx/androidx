/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.databridge.client

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.KeyUpdateCallback
import androidx.privacysandbox.databridge.core.KeyUpdateCallbackWithExecutor
import androidx.privacysandbox.databridge.core.Type
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import java.lang.ClassCastException
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * This class provides APIs for the app process to access the data.
 *
 * An interface will be added to [AppOwnedSdkSandboxInterfaceCompat] which will be used by the SDK
 * Runtime to access and modify the data
 */
public abstract class DataBridgeClient private constructor() {

    /**
     * Get value for a specific key. In case the key is not set, null is returned.
     *
     * @param key: key for which the value is requested
     * @return A [Result] object holding the value for the input key. This Result either contains
     *   the successful value or indicates failure with a java.io.IOException or ClassCastException
     */
    public abstract suspend fun getValue(key: Key): Result<Any?>

    /**
     * Get value for specific keys. In case any of he keys is not set, null is returned for those
     * keys.
     *
     * @param keys: Set of keys for which the values is requested
     * @return A map containing the key associated with a [Result] instance with the corresponding
     *   value. This Result either contains the successful value or indicates failure with a
     *   java.io.IOException or ClassCastException
     */
    public abstract suspend fun getValues(keys: Set<Key>): Map<Key, Result<Any?>>

    /**
     * Set the value for the specific key.
     *
     * @param key: Key for which value is to be set
     * @param value: Value for the key to be set
     * @throws java.io.IOException when an exception is encountered writing data to the disk
     * @throws ClassCastException when there is a mismatch in the key type and the type of the value
     */
    public abstract suspend fun setValue(key: Key, value: Any?)

    /**
     * Set the value for the specific key. This operation is atomic.
     *
     * @param keyValueMap: A map of the key and associated values to be set
     * @throws java.io.IOException when an exception is encountered writing data to the disk
     * @throws ClassCastException when there is a mismatch in any the key type and the type of the
     *   value
     */
    public abstract suspend fun setValues(keyValueMap: Map<Key, Any?>)

    /**
     * Removes the key-value pair of the given key.
     *
     * @param key: Key which needs to be removed
     * @throws java.io.IOException when an exception is encountered removing the data associated
     *   with the key in the disk
     */
    public abstract suspend fun removeValue(key: Key)

    /**
     * Removes the key-value pairs of the given keys. This operation is atomic.
     *
     * @param keys: Key which needs to be removed
     * @throws java.io.IOException when an exception is encountered removing the data associated
     *   with the key in the disk
     */
    public abstract suspend fun removeValues(keys: Set<Key>)

    /**
     * Registers a callback that will be triggered whenever the value of any key in the provided set
     * is updated.
     *
     * @param keys: Set of keys for which updates are required
     * @param executor: The executor from which the callback is executed.
     * @param callback: The callback which will be called when there is update in any of the keys in
     *   the provided set
     */
    public abstract fun registerKeyUpdateCallback(
        keys: Set<Key>,
        executor: Executor,
        callback: KeyUpdateCallback,
    )

    /**
     * Unregisters the callback. This will ensure that the keys registered to the callback will not
     * receive any further updates
     *
     * @param callback: The callback which should be unregistered
     */
    public abstract fun unregisterKeyUpdateCallback(callback: KeyUpdateCallback)

    public companion object {
        private var instance: DataBridgeClient? = null
        private val lock = Any()

        private val dataBridgeName = "androidx.privacysandbox.databridge"

        /**
         * Get an instance of [DataBridgeClient]. It also enables the SDK Runtime to access and
         * modify data for requests from SDK Runtime enabled SDKs.
         *
         * @param context: Application Context. If anything other [Context] instance is passed, it
         *   will be converted to application context using [Context.getApplicationContext]
         * @return DataBridgeClient instance
         */
        @JvmStatic
        public fun getInstance(context: Context): DataBridgeClient {
            return getInstance(context, dataBridgeName)
        }

        @JvmStatic
        @VisibleForTesting
        internal fun getInstance(context: Context, fileName: String): DataBridgeClient {
            synchronized(lock) {
                return instance
                    ?: DataBridgeClientImpl(context.applicationContext, fileName).also {
                        instance = it
                    }
            }
        }

        @JvmStatic
        @VisibleForTesting
        internal fun resetInstanceForTesting() {
            instance = null
        }
    }

    private class DataBridgeClientImpl(val context: Context, fileName: String) :
        DataBridgeClient() {
        // TODO(b/410523895): Investigate the filename of DataBridge's dataStore and the effects of
        // concurrent access.
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = fileName)

        init {
            registerAppOwnedSdkSandboxInterface(context, this)
        }

        private val keyGuard = Any()

        @GuardedBy("keyGuard") private val keysLock = mutableMapOf<Key, Any>()

        @GuardedBy("keyToKeyUpdateCallbackWithExecutorMap")
        private val keyToKeyUpdateCallbackWithExecutorMap =
            mutableMapOf<Key, MutableList<KeyUpdateCallbackWithExecutor>>()

        override suspend fun getValue(key: Key): Result<Any?> {
            return getValues(setOf(key))[key]!!
        }

        override suspend fun getValues(keys: Set<Key>): Map<Key, Result<Any?>> {
            val preferences = context.dataStore.data.first()

            return keys.associateWith { key ->
                try {
                    when (key.type) {
                        Type.INT -> {
                            val value = preferences[intPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.LONG -> {
                            val value = preferences[longPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.FLOAT -> {
                            val value = preferences[floatPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.DOUBLE -> {
                            val value = preferences[doublePreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.BOOLEAN -> {
                            val value = preferences[booleanPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.STRING -> {
                            val value = preferences[stringPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.STRING_SET -> {
                            val value = preferences[stringSetPreferencesKey(key.name)]
                            Result.success(value)
                        }
                        Type.BYTE_ARRAY -> {
                            val value = preferences[byteArrayPreferencesKey(key.name)]
                            Result.success(value)
                        }
                    }
                } catch (exception: Exception) {
                    Result.failure<Any>(exception)
                }
            }
        }

        override suspend fun setValue(key: Key, value: Any?) {
            setValues(mapOf(key to value))
        }

        override suspend fun setValues(keyValueMap: Map<Key, Any?>) {
            context.dataStore.edit { preferences ->
                keyValueMap.forEach {
                    when (it.key.type) {
                        Type.INT -> preferences[intPreferencesKey(it.key.name)] = it.value as Int
                        Type.LONG -> preferences[longPreferencesKey(it.key.name)] = it.value as Long
                        Type.FLOAT ->
                            preferences[floatPreferencesKey(it.key.name)] = it.value as Float
                        Type.DOUBLE ->
                            preferences[doublePreferencesKey(it.key.name)] = it.value as Double
                        Type.BOOLEAN ->
                            preferences[booleanPreferencesKey(it.key.name)] = it.value as Boolean
                        Type.STRING ->
                            preferences[stringPreferencesKey(it.key.name)] = it.value as String
                        Type.STRING_SET ->
                            preferences[stringSetPreferencesKey(it.key.name)] =
                                parseStringSetData(it.value)
                        Type.BYTE_ARRAY ->
                            preferences[byteArrayPreferencesKey(it.key.name)] =
                                it.value as ByteArray
                    }
                }
            }
            keyValueMap.forEach { sendKeyUpdates(it.key, it.value) }
        }

        override suspend fun removeValue(key: Key) {
            removeValues(setOf(key))
        }

        override suspend fun removeValues(keys: Set<Key>) {
            context.dataStore.edit { preferences ->
                keys.forEach {
                    when (it.type) {
                        Type.INT -> preferences.remove(intPreferencesKey(it.name))
                        Type.LONG -> preferences.remove(longPreferencesKey(it.name))
                        Type.FLOAT -> preferences.remove(floatPreferencesKey(it.name))
                        Type.DOUBLE -> preferences.remove(doublePreferencesKey(it.name))
                        Type.BOOLEAN -> preferences.remove(booleanPreferencesKey(it.name))
                        Type.STRING -> preferences.remove(stringPreferencesKey(it.name))
                        Type.STRING_SET -> preferences.remove(stringSetPreferencesKey(it.name))
                        Type.BYTE_ARRAY -> preferences.remove(byteArrayPreferencesKey(it.name))
                    }
                }
            }
            keys.forEach { sendKeyUpdates(it, null) }
        }

        override fun registerKeyUpdateCallback(
            keys: Set<Key>,
            executor: Executor,
            callback: KeyUpdateCallback,
        ) {
            keys.forEach { key -> registerCallback(key, callback, executor) }
        }

        override fun unregisterKeyUpdateCallback(callback: KeyUpdateCallback) {
            val keysToRemoveFromMap = mutableListOf<Key>()

            keyToKeyUpdateCallbackWithExecutorMap.forEach { (key, keyUpdateCallbackWithExecutorList)
                ->
                synchronized(getLockForKey(key)) {
                    keyUpdateCallbackWithExecutorList.removeAll { it.keyUpdateCallback == callback }

                    if (keyUpdateCallbackWithExecutorList.isEmpty()) {
                        keysToRemoveFromMap.add(key)
                    }
                }
            }

            synchronized(keyGuard) {
                keysToRemoveFromMap.forEach { key ->
                    keyToKeyUpdateCallbackWithExecutorMap.remove(key)
                }
            }
        }

        private fun <T> parseStringSetData(value: T): Set<String> {
            if (value !is Iterable<*>) {
                throw ClassCastException()
            }

            return value.map { it as String }.toSet()
        }

        private fun registerAppOwnedSdkSandboxInterface(
            context: Context,
            instance: DataBridgeClient,
        ) {
            val dataBridgeProxy = DataBridgeProxy(instance)

            val sdkSandboxManagerCompat = SdkSandboxManagerCompat.from(context)

            val appOwnedSdkSandboxInterfaceCompat =
                AppOwnedSdkSandboxInterfaceCompat(
                    name = dataBridgeName,
                    version = 1,
                    binder = dataBridgeProxy,
                )
            sdkSandboxManagerCompat.registerAppOwnedSdkSandboxInterface(
                appOwnedSdkSandboxInterfaceCompat
            )
        }

        private fun registerCallback(key: Key, callback: KeyUpdateCallback, executor: Executor) {
            synchronized(getLockForKey(key)) {
                val keyUpdateCallbackWithExecutor =
                    KeyUpdateCallbackWithExecutor(callback, executor)
                keyToKeyUpdateCallbackWithExecutorMap
                    .getOrPut(key) { mutableListOf() }
                    .add(keyUpdateCallbackWithExecutor)

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    val result = getValue(key)
                    executor.execute {
                        if (result.isSuccess) {
                            callback.onKeyUpdated(key, result.getOrNull())
                        }
                    }
                }
            }
        }

        private fun sendKeyUpdates(key: Key, value: Any?) {
            if (
                !keyToKeyUpdateCallbackWithExecutorMap.containsKey(key) ||
                    !keysLock.containsKey(key)
            ) {
                return
            }

            synchronized(keysLock[key]!!) {
                keyToKeyUpdateCallbackWithExecutorMap[key]?.forEach { keyUpdateCallbackWithExecutor
                    ->
                    keyUpdateCallbackWithExecutor.executor.execute {
                        keyUpdateCallbackWithExecutor.keyUpdateCallback.onKeyUpdated(key, value)
                    }
                }
            }
        }

        private fun getLockForKey(key: Key): Any {
            synchronized(keyGuard) {
                if (!keysLock.containsKey(key)) {
                    keysLock[key] = Any()
                }
            }
            return keysLock[key]!!
        }
    }
}
