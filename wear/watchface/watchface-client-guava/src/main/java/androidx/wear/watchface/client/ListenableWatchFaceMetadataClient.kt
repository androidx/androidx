/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.client.WatchFaceMetadataClient.ServiceNotBoundException
import androidx.wear.watchface.client.WatchFaceMetadataClient.ServiceStartFailureException
import androidx.wear.watchface.control.WatchFaceControlService
import com.google.common.util.concurrent.ListenableFuture

/**
 * [ListenableFuture]-based compatibility wrapper around
 * [WatchFaceMetadataClient.createWatchFaceMetadataClient].
 */
@WatchFaceClientExperimental
public class ListenableWatchFaceMetadataClient {
    public companion object {
        /**
         * Constructs a [WatchFaceMetadataClient] for fetching metadata for the specified watch
         * face.
         *
         * @param context Calling application's [Context].
         * @param watchFaceName The [ComponentName] of the watch face to fetch meta data from.
         * @return A [ListenableFuture] which resolves with [WatchFaceMetadataClient] if there is
         * one, otherwise it throws a [ServiceNotBoundException] if the underlying watch face
         * control service can not be bound or a [ServiceStartFailureException] if the watch face
         * dies during startup.
         */
        @Suppress("AsyncSuffixFuture")
        @JvmStatic
        public fun createListenableWatchFaceMetadataClient(
            context: Context,
            watchFaceName: ComponentName
        ): ListenableFuture<WatchFaceMetadataClient> =
            createListenableWatchFaceMetadataClientImpl(
                context,
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                    setPackage(watchFaceName.packageName)
                },
                watchFaceName
            )

        internal fun createListenableWatchFaceMetadataClientImpl(
            context: Context,
            intent: Intent,
            watchFaceName: ComponentName
        ) = ListenableWatchFaceControlClient.launchFutureCoroutine(
            "ListenableWatchFaceMetadataClient.listenableCreateWatchFaceMetadataClient"
        ) {
            WatchFaceMetadataClient.createWatchFaceMetadataClientImpl(
                context,
                intent,
                watchFaceName
            )
        }
    }
}