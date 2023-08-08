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
import android.os.Bundle
import android.os.Parcel
import android.os.ResultReceiver
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.remote.interactions.RemoteInteractionsUtil.isCurrentDeviceAWatch
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

// Disabling max line length is needed for the link to work properly in the KDoc.
/* ktlint-disable max-line-length */
/**
 * Support for opening android intents on other devices.
 *
 *
 * The following example opens play store for the given app on another device:
 *
 * ```
 * val remoteActivityHelper = RemoteActivityHelper(context, executor)
 *
 * val result = remoteActivityHelper.startRemoteActivity(
 *     new Intent(Intent.ACTION_VIEW).setData(
 *         Uri.parse("http://play.google.com/store/apps/details?id=com.example.myapp")
 *     ),
 *     nodeId
 * )
 * ```
 *
 * [startRemoteActivity] returns a [ListenableFuture], which is completed after the intent has
 * been sent or failed if there was an issue with sending the intent.
 *
 * nodeId is the opaque string that represents a
 * [node](https://developers.google.com/android/reference/com/google/android/gms/wearable/Node)
 * in the Android Wear network. For the given device, it can obtained by `NodeClient.getLocalNode()`
 * and the list of nodes to which this device is currently connected can be obtained by
 * `NodeClient.getConnectedNodes()`. More information about this can be found
 * [here](https://developers.google.com/android/reference/com/google/android/gms/wearable/NodeClient).
 *
 * @param context The [Context] of the application for sending the intent.
 * @param executor [Executor] used for getting data to be passed in remote intent. If not
 * specified, default will be `Executors.newSingleThreadExecutor()`.
 */
/* ktlint-enable max-line-length */
public class RemoteActivityHelper(
    private val context: Context,
    private val executor: Executor = Executors.newSingleThreadExecutor()
) {
    public companion object {
        @SuppressWarnings("ActionValue")
        public const val ACTION_REMOTE_INTENT: String =
            "com.google.android.wearable.intent.action.REMOTE_INTENT"

        private const val EXTRA_INTENT: String = "com.google.android.wearable.intent.extra.INTENT"

        private const val EXTRA_NODE_ID: String = "com.google.android.wearable.intent.extra.NODE_ID"

        private const val EXTRA_RESULT_RECEIVER: String =
            "com.google.android.wearable.intent.extra.RESULT_RECEIVER"

        /**
         * Result code passed to [ResultReceiver.send] when a remote intent was sent successfully.
         */
        public const val RESULT_OK: Int = 0

        /** Result code passed to [ResultReceiver.send] when a remote intent failed to send.  */
        public const val RESULT_FAILED: Int = 1

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
        public fun getTargetNodeId(intent: Intent): String? =
            intent.getStringExtra(EXTRA_NODE_ID)

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

        /** Re-package a result receiver as a vanilla version for cross-process sending  */
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

    /**
     * Used for testing only, so we can set mock NodeClient.
     */
    @VisibleForTesting
    internal var nodeClient: NodeClient = Wearable.getNodeClient(context)

    /**
     * Start an activity on another device. This api currently supports sending intents with
     * action set to [android.content.Intent.ACTION_VIEW], a data uri populated using
     * [android.content.Intent.setData], and with the category
     * [android.content.Intent.CATEGORY_BROWSABLE] present. If the current device is a watch,
     * the activity will start on the companion phone device. Otherwise, the activity will
     * start on all connected watch devices.
     *
     * @param targetIntent   The intent to open on the remote device. Action must be set to
     *                       [android.content.Intent.ACTION_VIEW], a data uri must be populated
     *                       using [android.content.Intent.setData], and the category
     *                       [android.content.Intent.CATEGORY_BROWSABLE] must be present.
     * @param targetNodeId   Wear OS node id for the device where the activity should be
     *                       started. If null, and the current device is a watch, the
     *                       activity will start on the companion phone device. Otherwise,
     *                       the activity will start on all connected watch devices.
     * @return The [ListenableFuture] which resolves if starting activity was successful or
     * throws [Exception] if any errors happens. If there's a problem with starting remote
     * activity, [RemoteIntentException] will be thrown.
     */
    @JvmOverloads
    public fun startRemoteActivity(
        targetIntent: Intent,
        targetNodeId: String? = null,
    ): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture {
            require(Intent.ACTION_VIEW == targetIntent.action) {
                "Only ${Intent.ACTION_VIEW} action is currently supported for starting a" +
                    " remote activity"
            }
            requireNotNull(targetIntent.data) {
                "Data Uri is required when starting a remote activity"
            }
            require(targetIntent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true) {
                "The category ${Intent.CATEGORY_BROWSABLE} must be present on the intent"
            }

            startCreatingIntentForRemoteActivity(
                targetIntent, targetNodeId, it, nodeClient,
                object : Callback {
                    override fun intentCreated(intent: Intent) {
                        context.sendBroadcast(intent)
                    }

                    override fun onFailure(exception: Exception) {
                        it.setException(exception)
                    }
                }
            )
        }
    }

    private fun startCreatingIntentForRemoteActivity(
        intent: Intent,
        nodeId: String?,
        completer: CallbackToFutureAdapter.Completer<Void>,
        nodeClient: NodeClient,
        callback: Callback
    ) {
        if (isCurrentDeviceAWatch(context)) {
            callback.intentCreated(
                createIntent(
                    intent,
                    RemoteIntentResultReceiver(completer, numNodes = 1),
                    nodeId,
                    DEFAULT_PACKAGE
                )
            )
            return
        }

        if (nodeId != null) {
            nodeClient.getCompanionPackageForNode(nodeId)
                .addOnSuccessListener(
                    executor
                ) { taskPackageName ->
                    val packageName = taskPackageName ?: DEFAULT_PACKAGE

                    if (packageName.isEmpty()) {
                        callback.onFailure(NotFoundException("Device $nodeId is not connected"))
                    } else {
                        callback.intentCreated(
                            createIntent(
                                intent,
                                RemoteIntentResultReceiver(completer, numNodes = 1),
                                nodeId,
                                packageName
                            )
                        )
                    }
                }.addOnFailureListener(executor) { callback.onFailure(it) }
            return
        }

        nodeClient.connectedNodes.addOnSuccessListener(
            executor
        ) { connectedNodes ->
            if (connectedNodes.size == 0) {
                callback.onFailure(NotFoundException("No devices connected"))
            } else {
                val resultReceiver = RemoteIntentResultReceiver(completer, connectedNodes.size)
                for (node in connectedNodes) {
                    nodeClient.getCompanionPackageForNode(node.id).addOnSuccessListener(
                        executor
                    ) { taskPackageName ->
                        val packageName = taskPackageName ?: DEFAULT_PACKAGE
                        callback.intentCreated(
                            createIntent(intent, resultReceiver, node.id, packageName)
                        )
                    }.addOnFailureListener(executor) { callback.onFailure(it) }
                }
            }
        }.addOnFailureListener(executor) { callback.onFailure(it) }
    }

    /**
     * Creates [android.content.Intent] with action specifying remote intent. If any of
     * additional extras are specified, they will be added to it. If specified, [ResultReceiver]
     * will be re-packed to be parcelable. If specified, packageName will be set.
     */
    @VisibleForTesting
    internal fun createIntent(
        extraIntent: Intent?,
        resultReceiver: ResultReceiver?,
        nodeId: String?,
        packageName: String? = null
    ): Intent {
        val remoteIntent = Intent(ACTION_REMOTE_INTENT)
        // Put the extra when non-null value is passed in
        extraIntent?.let { remoteIntent.putExtra(EXTRA_INTENT, extraIntent) }
        resultReceiver?.let {
            remoteIntent.putExtra(
                EXTRA_RESULT_RECEIVER,
                getResultReceiverForSending(resultReceiver)
            )
        }
        nodeId?.let { remoteIntent.putExtra(EXTRA_NODE_ID, nodeId) }
        packageName?.let { remoteIntent.setPackage(packageName) }
        return remoteIntent
    }

    /**
     * Result code passed to [ResultReceiver.send] for the status of remote intent.
     */
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
        private var numNodes: Int
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
