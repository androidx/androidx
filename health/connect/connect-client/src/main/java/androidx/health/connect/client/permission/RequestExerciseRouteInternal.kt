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

package androidx.health.connect.client.permission

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.impl.converters.records.toExerciseRoute
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.platform.client.impl.logger.Logger
import androidx.health.platform.client.service.HealthDataServiceConstants

/**
 * An [ActivityResultContract] to request a route associated with an {@code ExerciseSessionRecord}.
 *
 * @see androidx.activity.ComponentActivity.registerForActivityResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RequestExerciseRouteInternal : ActivityResultContract<String?, ExerciseRoute?>() {
    override fun createIntent(context: Context, input: String?): Intent {
        require(!input.isNullOrEmpty()) { "Session identifier is required" }
        return Intent(HealthDataServiceConstants.ACTION_REQUEST_ROUTE).apply {
            putExtra(HealthDataServiceConstants.EXTRA_SESSION_ID, input)
        }
    }

    @Suppress("DEPRECATION") // getParcelableExtra
    override fun parseResult(resultCode: Int, intent: Intent?): ExerciseRoute? {
        val route =
            intent?.getParcelableExtra<androidx.health.platform.client.exerciseroute.ExerciseRoute>(
                HealthDataServiceConstants.EXTRA_EXERCISE_ROUTE
            )
        if (route == null) {
            Logger.debug(HealthConnectClient.HEALTH_CONNECT_CLIENT_TAG, "No route returned.")
            return null
        }
        Logger.debug(HealthConnectClient.HEALTH_CONNECT_CLIENT_TAG, "Returned a route.")
        return toExerciseRoute(route)
    }
}
