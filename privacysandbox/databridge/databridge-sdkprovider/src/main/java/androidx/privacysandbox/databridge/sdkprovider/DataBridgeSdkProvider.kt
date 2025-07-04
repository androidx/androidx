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

package androidx.privacysandbox.databridge.sdkprovider

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.aidl.IDataBridgeProxy
import androidx.privacysandbox.databridge.core.aidl.IGetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ISetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * This class provides the SDK Runtime enabled SDKs APIs to access and modify the data which is
 * stored on the app process. The APIs in this class will perform IPC calls to the app process.
 */
public abstract class DataBridgeSdkProvider private constructor() {

    /**
     * Get value for specific keys. In case any of any of the keys are not set, null is returned as
     * part of Result object for those keys.
     *
     * @param keys: Set of keys for which the values is requested
     * @return A map containing the key associated with a [Result] instance with the corresponding
     *   value. This Result either contains the successful value or indicates failure with a
     *   java.io.IOException or ClassCastException
     */
    public abstract suspend fun getValues(keys: Set<Key>): Map<Key, Result<Any?>>

    /**
     * Set the value for the specific key. This operation is atomic.
     *
     * @param keyValueMap: A map of the key and associated values to be set
     * @throws IOException when an exception is encountered writing data to the disk
     * @throws ClassCastException when there is a mismatch in any the key type and the type of the
     *   value
     */
    public abstract suspend fun setValues(keyValueMap: Map<Key, Any?>)

    /**
     * Removes the key-value pairs of the given keys. This operation is atomic.
     *
     * @param keys: Key which needs to be removed
     * @throws IOException when an exception is encountered removing the data associated with the
     *   key in the disk
     */
    public abstract suspend fun removeValues(keys: Set<Key>)

    public companion object {
        private var instance: DataBridgeSdkProvider? = null
        private val lock = Any()

        /**
         * Get an instance of [DataBridgeSdkProvider]. This will help the SDK Runtime enabled SDKs
         * to access and modify the data which is stored on the app process.
         *
         * @param sdkContext: SDK context
         * @return DataBridgeClient instance
         * @throws java.lang.IllegalStateException If DataBridgeClient has not been initialized by
         *   the app process
         */
        @JvmStatic
        public fun getInstance(sdkContext: Context): DataBridgeSdkProvider {
            synchronized(lock) {
                if (instance == null) {
                    val sdkSandboxControllerCompat = SdkSandboxControllerCompat.from(sdkContext)
                    val appOwnedSdkSandboxInterface: AppOwnedSdkSandboxInterfaceCompat? =
                        sdkSandboxControllerCompat.getAppOwnedSdkSandboxInterfaces().firstOrNull {
                            it.getName() == "androidx.privacysandbox.databridge"
                        }

                    if (appOwnedSdkSandboxInterface == null) {
                        throw java.lang.IllegalStateException(
                            "DataBridgeClient must be initialized from the app before an instance of DataBridgeSdkProvider can be requested for."
                        )
                    }

                    instance =
                        DataBridgeSdkProviderImpl(
                            IDataBridgeProxy.Stub.asInterface(
                                appOwnedSdkSandboxInterface.getInterface()
                            )
                        )
                }
                return instance!!
            }
        }

        @JvmStatic
        @VisibleForTesting
        internal fun getInstance(dataBridgeProxy: IDataBridgeProxy): DataBridgeSdkProvider {
            synchronized(lock) {
                instance = DataBridgeSdkProviderImpl(dataBridgeProxy)
                return instance!!
            }
        }
    }

    private class DataBridgeSdkProviderImpl(val dataBridgeProxy: IDataBridgeProxy) :
        DataBridgeSdkProvider() {

        override suspend fun getValues(keys: Set<Key>): Map<Key, Result<Any?>> {
            val keyNameToKeyMap: Map<String, Key> = keys.associateBy { it.name }
            val (keyNames, keyTypes) = keys.map { it.name to it.type.toString() }.unzip()
            val callback =
                object : IGetValuesResultCallback.Stub() {
                    private val latch = CountDownLatch(1)
                    private var mResult: Map<Key, Result<Any?>> = emptyMap()

                    override fun getValuesResult(resultInternal: List<ResultInternal>) {
                        val result =
                            resultInternal.associate {
                                val key = keyNameToKeyMap[it.keyName]!!
                                if (it.valueInternal != null) {
                                    key to Result.success(it.valueInternal!!.value)
                                } else {
                                    key to
                                        getResultFailureFromThrowable(
                                            it.exceptionName!!,
                                            it.exceptionMessage!!,
                                        )
                                }
                            }
                        mResult = result
                        latch.countDown()
                    }

                    fun getResult(): Map<Key, Result<Any?>> {
                        latch.await()
                        return mResult
                    }
                }
            dataBridgeProxy.getValues(keyNames, keyTypes, callback)
            return callback.getResult()
        }

        override suspend fun setValues(keyValueMap: Map<Key, Any?>) {
            val callback =
                object : ISetValuesResultCallback.Stub() {
                    private val latch = CountDownLatch(1)
                    private var mExceptionName: String? = null
                    private var mExceptionMessage: String? = null

                    override fun setValuesResult(
                        exceptionName: String?,
                        exceptionMessage: String?,
                    ) {
                        mExceptionName = exceptionName
                        mExceptionMessage = exceptionMessage
                        latch.countDown()
                    }

                    fun throwExceptionIfPresent() {
                        latch.await()
                        if (mExceptionName != null) {
                            throw createAndReturnThrowable(mExceptionName!!, mExceptionMessage!!)
                        }
                    }
                }

            val valueInternal =
                keyValueMap.map {
                    ValueInternal(
                        it.key.type.toString(),
                        isValueNull = (it.value == null),
                        it.value,
                    )
                }

            dataBridgeProxy.setValues(keyValueMap.map { it.key.name }, valueInternal, callback)

            callback.throwExceptionIfPresent()
        }

        override suspend fun removeValues(keys: Set<Key>) {
            val callback =
                object : IRemoveValuesResultCallback.Stub() {
                    private val latch = CountDownLatch(1)
                    private var mExceptionName: String? = null
                    private var mExceptionMessage: String? = null

                    override fun removeValuesResult(
                        exceptionName: String?,
                        exceptionMessage: String?,
                    ) {
                        mExceptionName = exceptionName
                        mExceptionMessage = exceptionMessage
                        latch.countDown()
                    }

                    fun throwExceptionIfPresent() {
                        latch.await()
                        if (mExceptionName != null) {
                            throw createAndReturnThrowable(mExceptionName!!, mExceptionMessage!!)
                        }
                    }
                }

            val (keyNames, keyTypes) = keys.map { it.name to it.type.toString() }.unzip()
            dataBridgeProxy.removeValues(keyNames, keyTypes, callback)
            callback.throwExceptionIfPresent()
        }

        private fun getResultFailureFromThrowable(
            exceptionName: String,
            exceptionMessage: String,
        ): Result<Any?> {
            val throwable = createAndReturnThrowable(exceptionName, exceptionMessage)
            return Result.failure(throwable)
        }

        private fun createAndReturnThrowable(
            exceptionName: String,
            exceptionMessage: String,
        ): Throwable {
            if (exceptionName == java.lang.ClassCastException::class.java.canonicalName) {
                return ClassCastException(exceptionMessage)
            } else if (exceptionName == IOException::class.java.canonicalName) {
                return IOException(exceptionMessage)
            }
            return IllegalStateException(exceptionMessage)
        }
    }
}
