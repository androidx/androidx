/*
 * Copyright 2023 The Android Open Source Project
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
import android.net.Uri
import android.os.OutcomeReceiver
import androidx.wear.remote.interactions.RemoteInteractionsUtil.isCurrentDeviceAWatch
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.Sdk
import com.google.wear.services.remoteinteractions.RemoteInteractionsManager
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Forwards remote interactions to [RemoteInteractionsManager]. */
internal open class RemoteInteractionsManagerCompat(context: Context) : IRemoteInteractionsManager {

    override val isAvailabilityStatusApiSupported =
        isCurrentDeviceAWatch(context) &&
            WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_TIRAMISU_4)

    private val remoteInteractionsManager: RemoteInteractionsManager? =
        if (isAvailabilityStatusApiSupported)
            Sdk.getWearManager(context, RemoteInteractionsManager::class.java)
        else null

    override val isWearSdkApiStartRemoteActivitySupported =
        isCurrentDeviceAWatch(context) &&
            WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_BAKLAVA_0)

    override fun registerRemoteActivityHelperStatusListener(
        executor: Executor,
        listener: Consumer<Int>,
    ) {
        if (isAvailabilityStatusApiSupported) {
            remoteInteractionsManager!!.registerRemoteActivityHelperStatusListener(
                executor,
                listener,
            )
        } else {
            throw UnsupportedOperationException("Should not call wear sdk when not supported.")
        }
    }

    override fun unregisterRemoteActivityHelperStatusListener(listener: Consumer<Int>) {
        if (isAvailabilityStatusApiSupported) {
            remoteInteractionsManager!!.unregisterRemoteActivityHelperStatusListener(listener)
        } else {
            throw UnsupportedOperationException("Should not call wear sdk when not supported.")
        }
    }

    override fun startRemoteActivity(
        dataUri: Uri,
        additionalCategories: List<String>,
        executor: Executor,
        outcomeReceiver: OutcomeReceiver<Void?, Throwable>,
    ) {
        if (isWearSdkApiStartRemoteActivitySupported) {
            remoteInteractionsManager!!.startRemoteActivity(
                dataUri,
                additionalCategories,
                executor,
                outcomeReceiver,
            )
        } else {
            throw UnsupportedOperationException("Should not call wear sdk when not supported.")
        }
    }
}
