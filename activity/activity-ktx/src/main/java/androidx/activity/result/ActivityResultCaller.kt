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

package androidx.activity.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * A version of [ActivityResultCaller.registerForActivityResult]
 * that additionally takes an input right away, producing a launcher that doesn't take any
 * additional input when called.
 *
 * @see ActivityResultCaller.registerForActivityResult
 */
public fun <I, O> ActivityResultCaller.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    registry: ActivityResultRegistry,
    callback: (O) -> Unit
): ActivityResultLauncher<Unit> {
    val resultLauncher = registerForActivityResult(contract, registry) { callback(it) }
    return ActivityResultCallerLauncher(resultLauncher, contract, input)
}

/**
 * A version of [ActivityResultCaller.registerForActivityResult]
 * that additionally takes an input right away, producing a launcher that doesn't take any
 * additional input when called.
 *
 * @see ActivityResultCaller.registerForActivityResult
 */
public fun <I, O> ActivityResultCaller.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    callback: (O) -> Unit
): ActivityResultLauncher<Unit> {
    val resultLauncher = registerForActivityResult(contract) { callback(it) }
    return ActivityResultCallerLauncher(resultLauncher, contract, input)
}

internal class ActivityResultCallerLauncher<I, O>(
    val launcher: ActivityResultLauncher<I>,
    val callerContract: ActivityResultContract<I, O>,
    val callerInput: I
) : ActivityResultLauncher<Unit>() {
    val resultContract: ActivityResultContract<Unit, O> by lazy {
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

    override fun getContract(): ActivityResultContract<Unit, O> {
        return resultContract
    }
}
