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
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * A class that can call [Activity.startActivityForResult]-style APIs without having to manage
 * request codes, and converting request/response to an [Intent]
 */
interface ActivityResultCaller {
    /**
     * Register a request to [start an activity for result][Activity.startActivityForResult],
     * designated by the given [contract][ActivityResultContract].
     *
     * This creates a record in the [registry][ActivityResultRegistry] associated with this caller,
     * managing request code, as well as conversions to/from [Intent] under the hood.
     *
     * This *must* be called unconditionally, as part of initialization path, typically as a field
     * initializer of an Activity or Fragment.
     *
     * @param I the type of the input(if any) required to call the activity
     * @param O the type of output returned as an activity result
     * @param contract the contract, specifying conversions to/from [Intent]s
     * @param callback the callback to be called on the main thread when activity result is
     *   available
     * @return the launcher that can be used to start the activity or dispose of the prepared call.
     */
    fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I>

    /**
     * Register a request to [start an activity for result][Activity.startActivityForResult],
     * designated by the given [contract][ActivityResultContract].
     *
     * This creates a record in the given [registry][ActivityResultRegistry], managing request code,
     * as well as conversions to/from [Intent] under the hood.
     *
     * This *must* be called unconditionally, as part of initialization path, typically as a field
     * initializer of an Activity or Fragment.
     *
     * @param I the type of the input(if any) required to call the activity
     * @param O the type of output returned as an activity result
     * @param contract the contract, specifying conversions to/from [Intent]s
     * @param registry the registry where to hold the record.
     * @param callback the callback to be called on the main thread when activity result is
     *   available
     * @return the launcher that can be used to start the activity or dispose of the prepared call.
     */
    fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I>
}

/**
 * A version of [ActivityResultCaller.registerForActivityResult] that additionally takes an input
 * right away, producing a launcher that doesn't take any additional input when called.
 *
 * @see ActivityResultCaller.registerForActivityResult
 */
fun <I, O> ActivityResultCaller.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    registry: ActivityResultRegistry,
    callback: (@JvmSuppressWildcards O) -> Unit
): ActivityResultLauncher<Unit> {
    val resultLauncher = registerForActivityResult(contract, registry) { callback(it) }
    return ActivityResultCallerLauncher(resultLauncher, contract, input)
}

/**
 * A version of [ActivityResultCaller.registerForActivityResult] that additionally takes an input
 * right away, producing a launcher that doesn't take any additional input when called.
 *
 * @see ActivityResultCaller.registerForActivityResult
 */
fun <I, O> ActivityResultCaller.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    callback: (@JvmSuppressWildcards O) -> Unit
): ActivityResultLauncher<Unit> {
    val resultLauncher = registerForActivityResult(contract) { callback(it) }
    return ActivityResultCallerLauncher(resultLauncher, contract, input)
}

internal class ActivityResultCallerLauncher<I, O>(
    private val launcher: ActivityResultLauncher<I>,
    val callerContract: ActivityResultContract<I, O>,
    val callerInput: I
) : ActivityResultLauncher<Unit>() {
    private val resultContract: ActivityResultContract<Unit, O> by lazy {
        object : ActivityResultContract<Unit, O>() {
            override fun createIntent(context: Context, input: Unit): Intent {
                return callerContract.createIntent(context, callerInput)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): O {
                return callerContract.parseResult(resultCode, intent)
            }
        }
    }

    override fun launch(input: Unit, options: ActivityOptionsCompat?) {
        launcher.launch(callerInput, options)
    }

    override fun unregister() {
        launcher.unregister()
    }

    override val contract: ActivityResultContract<Unit, O> = resultContract
}
