/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.wear.remote.interactions

import android.content.Context
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.Parcel
import android.os.ResultReceiver
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.remote.interactions.RemoteInteractionsUtil.isCurrentDeviceAWatch
import androidx.wear.remote.interactions.RemoteInteractionsUtil.logDOrNotUser
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

// Disabling max line length is needed for the link to work properly in the KDoc.

/**
 * Support for opening android intents on other devices.
 *
 * The following example opens play store for the given app on another device:
 * ```
 * val remoteActivityHelper = RemoteActivityHelper(context, executor)
 *
 * val result = remoteActivityHelper.startRemoteActivity(
 *     Intent(Intent.ACTION_VIEW)
 *         .setData(
 *             Uri.parse("http://play.google.com/store/apps/details?id=com.example.myapp"))
 *         .addCategory(Intent.CATEGORY_BROWSABLE),
 *     nodeId)
 * ```
 *
 * [startRemoteActivity] returns a [ListenableFuture], which is completed after the intent has been
 * sent or failed if there was an issue with sending the intent.
 *
 * nodeId is the opaque string that represents a
 * [node](https://developers.google.com/android/reference/com/google/android/gms/wearable/Node) in
 * the Android Wear network. For the given device, it can obtained by `NodeClient.getLocalNode()`
 * and the list of nodes to which this device is currently connected can be obtained by
 * `NodeClient.getConnectedNodes()`. More information about this can be found
 * [here](https://developers.google.com/android/reference/com/google/android/gms/wearable/NodeClient).
 *
 * @param context The [Context] of the application for sending the intent.
 * @param executor [Executor] used for getting data to be passed in remote intent. If not specified,
 *   default will be `Executors.newSingleThreadExecutor()`.
 */
public class RemoteActivityHelper
@JvmOverloads
constructor(
    private val context: Context,
    private val executor: Executor = Executors.newSingleThreadExecutor(),
) {
    public companion object {
        @SuppressWarnings("ActionValue")
        public const val ACTION_REMOTE_INTENT: String =
            "com.google.android.wearable.intent.action.REMOTE_INTENT"

        /** The remote activity's availability is unknown. */
        public const val STATUS_UNKNOWN: Int = 0

        /**
         * The remote auth's availability is unknown.
         *
         * On older devices, [STATUS_UNKNOWN] is returned as we can not determine the availability
         * states. To preserve compatibility with existing devices behavior, try
         * [startRemoteActivity] and handle error codes accordingly.
         */
        public const val STATUS_UNAVAILABLE: Int = 1

        /**
         * Indicates that remote activity is temporarily unavailable.
         *
         * There is a known paired device, but it is not currently connected or reachable to handle
         * the remote interaction.
         */
        public const val STATUS_TEMPORARILY_UNAVAILABLE: Int = 2

        /**
         * Indicates that remote activity is available.
         *
         * There is a connected device capable to handle the remote interaction.
         */
        public const val STATUS_AVAILABLE: Int = 3

        private const val EXTRA_INTENT: String = "com.google.android.wearable.intent.extra.INTENT"

        private const val EXTRA_NODE_ID: String = "com.google.android.wearable.intent.extra.NODE_ID"

        private const val EXTRA_RESULT_RECEIVER: String =
            "com.google.android.wearable.intent.extra.RESULT_RECEIVER"

        private var sUseWearSdkImpl: Boolean = false

        /**
         * Result code passed to [ResultReceiver.send] when a remote intent was sent successfully.
         */
        public const val RESULT_OK: Int = 0

        /** Result code passed to [ResultReceiver.send] when a remote intent failed to send. */
        public const val RESULT_FAILED: Int = 1

        /** Permission required to use [startPhoneActivityWithUnlock]. */
        public const val PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE: String =
            "com.google.wear.permission.SEND_CONTINUE_ACTIVITY_ON_PHONE"

        internal const val DEFAULT_PACKAGE = "com.google.android.wearable.app"

        /**
         * Returns the [android.content.Intent] extra specifying remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The remote intent, or null if none was set.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        public fun getTargetIntent(intent: Intent): Intent? =
            intent.getParcelableExtra(EXTRA_INTENT)

        /**
         * Returns the [String] extra specifying node ID of remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The node id, or null if none was set.
         */
        @JvmStatic
        public fun getTargetNodeId(intent: Intent): String? = intent.getStringExtra(EXTRA_NODE_ID)

        /**
         * Returns the [android.os.ResultReceiver] extra of remote intent.
         *
         * @param intent The intent holding configuration.
         * @return The result receiver, or null if none was set.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        internal fun getRemoteIntentResultReceiver(intent: Intent): ResultReceiver? =
            intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)

        /** Re-package a result receiver as a vanilla version for cross-process sending */
        @JvmStatic
        internal fun getResultReceiverForSending(receiver: ResultReceiver): ResultReceiver {
            val parcel = Parcel.obtain()
            receiver.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel)
            parcel.recycle()
            return receiverForSending
        }
    }

    /** Used for testing only, so we can set mock NodeClient. */
    @VisibleForTesting internal var nodeClient: NodeClient = Wearable.getNodeClient(context)

    /** Used for testing only, so we can mock wear sdk dependency. */
    @VisibleForTesting
    internal var remoteInteractionsManager: IRemoteInteractionsManager =
        RemoteInteractionsManagerCompat(context)

    /**
     * Status of whether [RemoteActivityHelper] can [startRemoteActivity], if known.
     *
     * In scenarios of restricted connection or temporary disconnection with a paired device,
     * [startRemoteActivity] will not be available. Please check [availabilityStatus] before calling
     * [startRemoteActivity] to provide better experience for the user.
     *
     * Wear devices start to support determining the availability status from Wear Sdk
     * WEAR_TIRAMISU_4. On older wear devices, it will always return [STATUS_UNKNOWN]. On phone
     * devices, it will always return [STATUS_UNKNOWN].
     *
     * @sample androidx.wear.remote.interactions.samples.RemoteActivityAvailabilitySample
     * @return a [Flow] with a stream of status updates that could be one of [STATUS_UNKNOWN],
     *   [STATUS_UNAVAILABLE], [STATUS_TEMPORARILY_UNAVAILABLE], [STATUS_AVAILABLE].
     */
    public val availabilityStatus: Flow<Int>
        get() {
            if (!isCurrentDeviceAWatch(context)) {
                // Currently, we do not support knowing the startRemoteActivity's availability on a
                // non-watch device.
                return flowOf(STATUS_UNKNOWN)
            }
            if (!remoteInteractionsManager.isAvailabilityStatusApiSupported) {
                return flowOf(STATUS_UNKNOWN)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // This should never be reached as the check above wouldn't pass below T.
                // `Consumer<Int>` requires min API 25 but library min API is 23, this hints to lint
                // that the code below
                // only executes on T+.
                return flowOf(STATUS_UNKNOWN)
            }

            return getRemoteActivityHelperStatusInternal()
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getRemoteActivityHelperStatusInternal(): Flow<Int> {
        return callbackFlow {
            val callback =
                object : Consumer<Int> {
                    override fun accept(value: Int) {
                        // Emit WearSDK values through AndroidX with 1:1 mapping.
                        trySend(value)
                    }
                }

            remoteInteractionsManager.registerRemoteActivityHelperStatusListener(executor, callback)

            awaitClose {
                remoteInteractionsManager.unregisterRemoteActivityHelperStatusListener(callback)
            }
        }
    }

    /**
     * Start an activity on another device.
     *
     * This API currently supports sending intents with action set to
     * [android.content.Intent.ACTION_VIEW], a data URI populated using
     * [android.content.Intent.setData], and with the category
     * [android.content.Intent.CATEGORY_BROWSABLE] present.
     *
     * When [targetNodeId] is unspecified, if the current device is a watch, the activity will start
     * on the companion phone device. Otherwise, the activity will start on all connected watch
     * devices.
     *
     * If the intent passed in sets a different action or does not contain the CATEGORY_BROWSABLE
     * category or does not set a data URI, the call will be rejected and a
     * [kotlin.IllegalArgumentException] thrown.
     *
     * Besides the mandated action and category, the caller must provide a data URI and an optional
     * set of categories to be delivered to the remote device.
     *
     * If any additional attributes of the intent are set (for examples, extras, package,
     * component), they will be stripped from the intent. Only an intent with ACTION_VIEW,
     * CATEGORY_BROWSABLE, any other specified categories, and the provided data URI will be
     * delivered to the remote devices.
     *
     * From Wear 6, the Wear SDK on the Watch will be used for starting remote activities on the
     * connected companion.
     *
     * @param targetIntent The intent to open on the remote device. Action must be set to
     *   [android.content.Intent.ACTION_VIEW], a data URI must be populated using
     *   [android.content.Intent.setData], and the category
     *   [android.content.Intent.CATEGORY_BROWSABLE] must be present.
     * @param targetNodeId Wear OS node id for the device where the activity should be started. If
     *   null, and the current device is a watch, the activity will start on the companion phone
     *   device. Otherwise, the activity will start on all connected watch devices.
     * @return The [ListenableFuture] which resolves if starting activity was successful or throws
     *   [Exception] if any errors happens. If there's a problem with starting remote activity,
     *   [RemoteIntentException] will be thrown.
     */
    @JvmOverloads
    public fun startRemoteActivity(
        targetIntent: Intent,
        targetNodeId: String? = null,
    ): ListenableFuture<Void> {
        if (remoteInteractionsManager.isWearSdkApiStartRemoteActivitySupported) {
            return startRemoteActivity(remoteInteractionsManager, targetIntent, executor)
        }
        return startRemoteActivityLegacy(targetIntent, targetNodeId)
    }

    private fun checkTargetIntentPrecondition(targetIntent: Intent) {
        require(Intent.ACTION_VIEW == targetIntent.action) {
            "Only ${Intent.ACTION_VIEW} action is currently supported for starting a" +
                " remote activity"
        }
        requireNotNull(targetIntent.data) { "Data URI is required when starting a remote activity" }
        require(targetIntent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true) {
            "The category ${Intent.CATEGORY_BROWSABLE} must be present on the intent"
        }
    }

    /**
     * Starts an activity on the companion phone device, requesting a remote unlock if supported and
     * [PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE] is granted.
     *
     * The remote unlock aspect of the request is subject to a strict set of security conditions:
     * * The remote unlock part is only applicable when the request is initiated from a Wear OS
     *   watch.
     * * The calling application must be granted [PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE].
     * * The app must be in the foreground.
     * * The request must be the result of explicit user interaction.
     * * User should have enrolled in the feature.
     * * An active authentication session must exist between the devices. This requires both devices
     *   support the feature, the watch is currently unlocked and near the phone while they are
     *   connected via Bluetooth, and the phone has been unlocked by the user at least once *after*
     *   the watch was unlocked and placed on the user's body.
     *
     * To initiate the launch, this method will generate an intent with an
     * [android.content.Intent.ACTION_VIEW] action, the provided [targetUri], and the caller's
     * [targetCategories] (which must include [android.content.Intent.CATEGORY_BROWSABLE] category).
     *
     * If remote unlock is not supported or conditions are not met (for example, missing permission,
     * or if the authentication session ends), this method falls back to [startRemoteActivity]
     * behavior. In this case, no remote unlock will be requested and the user will have to manually
     * unlock their phone if it is locked before activity is launched.
     *
     * @param targetUri The data URI of the activity to start on the phone. This must not be empty.
     * @param targetCategories The categories of the activity to start on the phone. This must
     *   contain [android.content.Intent.CATEGORY_BROWSABLE]. If not specified, by default it will
     *   be [android.content.Intent.CATEGORY_BROWSABLE].
     * @return A [ListenableFuture] which resolves if the request was successfully sent, or throws
     *   an [Exception] if the request failed or the parameters were invalid.
     */
    @JvmOverloads
    @SuppressWarnings("AsyncSuffixFuture")
    public fun startRemoteActivityAttemptUnlock(
        targetUri: Uri,
        targetCategories: List<String> = listOf(Intent.CATEGORY_BROWSABLE),
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture { completer ->
            try {
                require(targetUri != Uri.EMPTY) { "targetUri cannot be empty" }
                require(targetCategories.contains(Intent.CATEGORY_BROWSABLE)) {
                    "The category ${Intent.CATEGORY_BROWSABLE} must be present on the intent"
                }

                val isSupported =
                    remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
                val hasPermission =
                    context.checkSelfPermission(PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

                if (isSupported && hasPermission) {
                    routeContinueActivityOnPhoneWithUnlock(
                        targetPackage = "",
                        targetAction = Intent.ACTION_VIEW,
                        targetUri,
                        targetCategories,
                        callerPackage = context.packageName,
                        object : OutcomeReceiver<Void?, Throwable> {
                            override fun onResult(result: Void?) {
                                logDOrNotUser("continueActivityOnPhoneWithUnlock", "onResult")
                                completer.set(null)
                            }

                            override fun onError(error: Throwable) {
                                logDOrNotUser("continueActivityOnPhoneWithUnlock", "onError:$error")
                                startRemoteActivityFallback(targetUri, targetCategories, completer)
                            }
                        },
                    )
                    return@getFuture "startRemoteActivityAttemptUnlock"
                }

                startRemoteActivityFallback(targetUri, targetCategories, completer)
            } catch (e: Exception) {
                completer.setException(e)
            }
            "startRemoteActivityAttemptUnlock"
        }
    }

    /**
     * Starts an activity on the companion phone device, requesting a remote unlock.
     *
     * This API requests to start an activity on the companion phone. If the phone is locked, it may
     * also request to initiate unlocking the phone before launching the target application if below
     * system conditions allow it.
     *
     * This API is only available on API 37 and above.
     *
     * To use this function, the following conditions must be met:
     * * Note that this method only works when it's called from the watch.
     * * The calling application must be granted [PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE].
     * * The [targetPackage] must be the same package (or a trusted peer application) on the
     *   connected phone, and its SHA1 certificate fingerprint must match the source package on the
     *   watch. The [callerPackage] must also accurately match the calling app's package name.
     * * The caller application must be running in the foreground when the request is made.
     * * The request must be the direct result of an explicit user interaction (for example, tapping
     *   a "show on phone" button).
     * * User should have enrolled in the feature.
     * * An active authentication session must exist between the devices. This requires both devices
     *   support the feature, the watch is currently unlocked and near the phone while they are
     *   connected via Bluetooth, and the phone has been unlocked by the user at least once *after*
     *   the watch was unlocked and placed on the user's body.
     *
     * There are no restrictions on [targetAction], [targetUri] or [targetCategories] as long as
     * they are not empty.
     *
     * **Exception Conditions:**
     * * [IllegalStateException]: Thrown if [PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE] has not
     *   been granted.
     * * [IllegalArgumentException]: Thrown if any of the required parameters are empty.
     * * [UnsupportedOperationException]: Thrown if this remote unlock API is unsupported on this
     *   device.
     *
     * @param callerPackage The package name of the calling app. This is required.
     * @param targetPackage The package name of the activity to start on the phone. This is
     *   required.
     * @param targetAction The action of the activity to start on the phone. This is required.
     * @param targetUri The data URI of the activity to start on the phone. This is required and
     *   cannot be empty.
     * @param targetCategories The categories of the activity to start on the phone. This is
     *   required.
     * @return A [ListenableFuture] which resolves if the request was successfully sent, or throws
     *   an [Exception] if the request failed or the parameters were invalid. If there's a problem
     *   with starting remote activity, [RemoteIntentException] will be thrown.
     * @throws IllegalStateException If [PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE] has not been
     *   granted.
     * @throws IllegalArgumentException If any of the required parameters are empty.
     * @throws UnsupportedOperationException If this remote unlock API is unsupported on this
     *   device.
     */
    @RequiresApi(37)
    @RequiresPermission(PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE)
    @SuppressWarnings("AsyncSuffixFuture")
    public fun startPhoneActivityWithUnlock(
        callerPackage: String,
        targetPackage: String,
        targetAction: String,
        targetUri: Uri,
        targetCategories: List<String>,
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture { completer ->
            try {
                val hasPermission =
                    context.checkSelfPermission(PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    completer.setException(
                        IllegalStateException(
                            "Caller does not have $PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE permission."
                        )
                    )
                    return@getFuture "startPhoneActivityWithUnlock"
                }

                if (
                    !remoteInteractionsManager
                        .isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
                ) {
                    completer.setException(
                        UnsupportedOperationException(
                            "startPhoneActivityWithUnlock is not supported on this device"
                        )
                    )
                    return@getFuture "startPhoneActivityWithUnlock"
                }

                checkPreconditionForContinueOnPhone(
                    targetPackage,
                    targetAction,
                    targetUri,
                    targetCategories,
                    callerPackage,
                )

                routeContinueActivityOnPhoneWithUnlock(
                    targetPackage,
                    targetAction,
                    targetUri,
                    targetCategories,
                    callerPackage,
                    object : OutcomeReceiver<Void?, Throwable> {
                        override fun onResult(result: Void?) {
                            logDOrNotUser("startPhoneActivityWithUnlock", "onResult")
                            completer.set(null)
                        }

                        override fun onError(error: Throwable) {
                            logDOrNotUser("startPhoneActivityWithUnlock", "onError:$error")
                            completer.setException(error)
                        }
                    },
                )
            } catch (e: Exception) {
                completer.setException(e)
            }
            "startPhoneActivityWithUnlock"
        }
    }

    /**
     * The legacy implementation of startRemoteActivity and will be called when the sdk version is
     * older than API 36 / Wear SDK version 6.
     */
    @VisibleForTesting
    internal fun startRemoteActivityLegacy(
        targetIntent: Intent,
        targetNodeId: String? = null,
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture {
            checkTargetIntentPrecondition(targetIntent)
            startCreatingIntentForRemoteActivity(
                targetIntent,
                targetNodeId,
                it,
                nodeClient,
                object : Callback {
                    override fun intentCreated(intent: Intent) {
                        context.sendBroadcast(intent)
                    }

                    override fun onFailure(exception: Exception) {
                        it.setException(exception)
                    }
                },
            )
        }
    }

    @RequiresApi(36)
    private fun startRemoteActivity(
        @NonNull remoteInteractionsManager: IRemoteInteractionsManager,
        targetIntent: Intent,
        @NonNull executor: Executor,
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture { completer ->
            checkTargetIntentPrecondition(targetIntent)
            remoteInteractionsManager.startRemoteActivity(
                targetIntent.data!!, // Already checked previously so it's safe.
                targetIntent.categories!!.toList(), // Already checked previously so it's safe.
                executor,
                object : OutcomeReceiver<Void?, Throwable> {
                    override fun onResult(result: Void?) {
                        logDOrNotUser("startRemoteActivity", "onResult")
                        completer.set(null)
                    }

                    override fun onError(error: Throwable) {
                        logDOrNotUser("startRemoteActivity", "onError:$error")
                        completer.setException(error)
                    }
                },
            )
            "startRemoteActivity"
        }
    }

    private fun startRemoteActivityFallback(
        targetUri: Uri,
        targetCategories: List<String>,
        completer: CallbackToFutureAdapter.Completer<Void>,
    ) {
        val intent = Intent(Intent.ACTION_VIEW).setData(targetUri)
        targetCategories.forEach { intent.addCategory(it) }
        val future = startRemoteActivity(intent)
        future.addListener(
            {
                try {
                    future.get()
                    completer.set(null)
                } catch (e: ExecutionException) {
                    completer.setException(e.cause ?: e)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            },
            executor,
        )
    }

    private fun startCreatingIntentForRemoteActivity(
        intent: Intent,
        nodeId: String?,
        completer: CallbackToFutureAdapter.Completer<Void>,
        nodeClient: NodeClient,
        callback: Callback,
    ) {
        if (isCurrentDeviceAWatch(context)) {
            callback.intentCreated(
                createIntent(
                    intent,
                    RemoteIntentResultReceiver(completer, numNodes = 1),
                    nodeId,
                    DEFAULT_PACKAGE,
                )
            )
            return
        }

        if (nodeId != null) {
            nodeClient
                .getCompanionPackageForNode(nodeId)
                .addOnSuccessListener(executor) { taskPackageName ->
                    val packageName = taskPackageName ?: DEFAULT_PACKAGE

                    if (packageName.isEmpty()) {
                        callback.onFailure(NotFoundException("Device $nodeId is not connected"))
                    } else {
                        callback.intentCreated(
                            createIntent(
                                intent,
                                RemoteIntentResultReceiver(completer, numNodes = 1),
                                nodeId,
                                packageName,
                            )
                        )
                    }
                }
                .addOnFailureListener(executor) { callback.onFailure(it) }
            return
        }

        nodeClient.connectedNodes
            .addOnSuccessListener(executor) { connectedNodes ->
                if (connectedNodes.size == 0) {
                    callback.onFailure(NotFoundException("No devices connected"))
                } else {
                    val resultReceiver = RemoteIntentResultReceiver(completer, connectedNodes.size)
                    for (node in connectedNodes) {
                        nodeClient
                            .getCompanionPackageForNode(node.id)
                            .addOnSuccessListener(executor) { taskPackageName ->
                                val packageName = taskPackageName ?: DEFAULT_PACKAGE
                                callback.intentCreated(
                                    createIntent(intent, resultReceiver, node.id, packageName)
                                )
                            }
                            .addOnFailureListener(executor) { callback.onFailure(it) }
                    }
                }
            }
            .addOnFailureListener(executor) { callback.onFailure(it) }
    }

    /**
     * Creates [android.content.Intent] with action specifying remote intent. If any of additional
     * extras are specified, they will be added to it. If specified, [ResultReceiver] will be
     * re-packed to be parcelable. If specified, packageName will be set.
     */
    @VisibleForTesting
    internal fun createIntent(
        extraIntent: Intent?,
        resultReceiver: ResultReceiver?,
        nodeId: String?,
        packageName: String? = null,
    ): Intent {
        val remoteIntent = Intent(ACTION_REMOTE_INTENT)
        // Put the extra when non-null value is passed in
        extraIntent?.let { remoteIntent.putExtra(EXTRA_INTENT, extraIntent) }
        resultReceiver?.let {
            remoteIntent.putExtra(
                EXTRA_RESULT_RECEIVER,
                getResultReceiverForSending(resultReceiver),
            )
        }
        nodeId?.let { remoteIntent.putExtra(EXTRA_NODE_ID, nodeId) }
        packageName?.let { remoteIntent.setPackage(packageName) }
        return remoteIntent
    }

    @RequiresApi(37)
    private fun routeContinueActivityOnPhoneWithUnlock(
        targetPackage: String,
        targetAction: String,
        targetUri: Uri,
        targetCategories: List<String>,
        callerPackage: String,
        outcomeReceiver: OutcomeReceiver<Void?, Throwable>,
    ) {
        remoteInteractionsManager.continueActivityOnPhoneWithUnlock(
            targetPackage,
            targetAction,
            targetUri,
            targetCategories,
            callerPackage,
            executor,
            outcomeReceiver,
        )
    }

    private fun checkPreconditionForContinueOnPhone(
        targetPackage: String,
        targetAction: String,
        targetUri: Uri,
        targetCategories: List<String>,
        callerPackage: String,
    ) {
        require(targetAction.isNotEmpty()) { "targetAction cannot be empty" }
        require(targetPackage.isNotEmpty()) { "targetPackage cannot be empty" }
        require(targetUri != Uri.EMPTY) { "targetUri cannot be empty" }
        require(targetCategories.isNotEmpty()) { "targetCategories cannot be empty" }
        require(callerPackage.isNotEmpty()) { "callerPackage cannot be empty" }
    }

    /** Result code passed to [ResultReceiver.send] for the status of remote intent. */
    @IntDef(RESULT_OK, RESULT_FAILED)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class SendResult

    public class RemoteIntentException(message: String) : Exception(message)

    private interface Callback {
        fun intentCreated(intent: Intent)

        fun onFailure(exception: Exception)
    }

    private class RemoteIntentResultReceiver(
        private val completer: CallbackToFutureAdapter.Completer<Void>,
        private var numNodes: Int,
    ) : ResultReceiver(null) {
        private var numFailedResults: Int = 0

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            numNodes--
            if (resultCode != RESULT_OK) numFailedResults++
            // Don't send result if not all nodes have finished.
            if (numNodes > 0) return

            if (numFailedResults == 0) {
                completer.set(null)
            } else {
                completer.setException(
                    RemoteIntentException("There was an error while starting remote activity.")
                )
            }
        }
    }
}
