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

package androidx.health.connect.client.contracts

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.permission.ExerciseRouteRequestAppContract
import androidx.health.connect.client.permission.platform.ExerciseRouteRequestModuleContract
import androidx.health.connect.client.records.ExerciseRoute

/**
 * An [ActivityResultContract] to request a route associated with an
 * [androidx.health.connect.client.records.ExerciseSessionRecord].
 *
 * It receives the session id as input and returns an ExerciseRoute as output, if available.
 *
 * @sample androidx.health.connect.client.samples.ReadExerciseRoute
 */
class ExerciseRouteRequestContract : ActivityResultContract<String, ExerciseRoute?>() {

    private val delegate: ActivityResultContract<String, ExerciseRoute?> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ExerciseRouteRequestModuleContract()
        } else {
            ExerciseRouteRequestAppContract()
        }

    /**
     * Creates an intent to request an [ExerciseRoute]. It receives the exercise session id as
     * [input].
     *
     * @param context the context
     * @param input the exercise session id obtained via
     *   [androidx.health.connect.client.records.ExerciseSessionRecord.metadata]
     * @throws IllegalArgumentException if the [input] is an empty string.
     * @see ActivityResultContract.createIntent
     */
    override fun createIntent(context: Context, input: String): Intent {
        require(input.isNotEmpty()) { "Session identifier can't be empty" }
        return delegate.createIntent(context, input)
    }

    /**
     * Converts the activity result into [ExerciseRoute], to return as output.
     *
     * @return null if the user didn't grant access to the exercise route or if there's no exercise
     *   route for the session id passed on [createIntent].
     * @see ActivityResultContract.parseResult
     */
    override fun parseResult(resultCode: Int, intent: Intent?): ExerciseRoute? {
        return delegate.parseResult(resultCode, intent)
    }
}
