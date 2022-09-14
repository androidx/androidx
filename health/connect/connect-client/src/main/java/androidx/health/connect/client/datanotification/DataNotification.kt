/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.datanotification

import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.converters.datatype.getRecordType
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

/**
 * Contains information about the changed data.
 *
 * @param dataTypes a set of changed [Record] classes.
 * @see androidx.health.connect.client.HealthConnectClient.registerForDataNotifications
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // Not yet ready for public
class DataNotification private constructor(
    val dataTypes: Set<KClass<out Record>>,
) {

    companion object {
        private const val EXTRA_DATA_TYPE_IDS = "com.google.android.healthdata.extra.DATA_TYPE_IDS"

        /**
         * Extracts the notification data from the given [intent]. The [Intent] is usually received
         * via a [BroadcastReceiver][android.content.BroadcastReceiver].
         *
         * @param intent an [Intent] received in a
         * [BroadcastReceiver][android.content.BroadcastReceiver].
         * @return [DataNotification] if the notification data was successfully extracted, `null`
         * otherwise.
         * @see androidx.health.connect.client.HealthConnectClient.registerForDataNotifications
         */
        @JvmStatic
        fun from(intent: Intent): DataNotification? {
            val dataTypeIds = intent.getIntArrayExtra(EXTRA_DATA_TYPE_IDS) ?: return null

            return DataNotification(
                dataTypes = dataTypeIds.mapTo(HashSet(), ::getRecordType),
            )
        }
    }
}