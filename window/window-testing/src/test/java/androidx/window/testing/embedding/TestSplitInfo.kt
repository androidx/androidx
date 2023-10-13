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

package androidx.window.testing.embedding

import android.app.Activity
import android.os.Binder
import android.os.IBinder
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitInfo

/**
 * A convenience method to get a test [SplitInfo] with default values provided. With the default
 * values it returns an empty [ActivityStack] for the primary and secondary stacks. The default
 * [SplitAttributes] are for splitting equally and matching the locale layout.
 *
 * Note: This method should be used for testing local logic as opposed to end to end verification.
 * End to end verification requires a device that supports Activity Embedding.
 *
 * @param primaryActivity the [Activity] for the primary container.
 * @param secondaryActivity the [Activity] for the secondary container.
 * @param splitAttributes the [SplitAttributes].
 */
@ExperimentalWindowApi
fun TestSplitInfo(
    primaryActivity: Activity,
    secondaryActivity: Activity,
    splitAttributes: SplitAttributes = SplitAttributes(),
    token: IBinder = Binder()
): SplitInfo {
    val primaryActivityStack = TestActivityStack(primaryActivity, false)
    val secondaryActivityStack = TestActivityStack(secondaryActivity, false)
    return SplitInfo(primaryActivityStack, secondaryActivityStack, splitAttributes, token)
}

/**
 * A convenience method to get a test [ActivityStack] with default values provided. With the default
 * values, there will be a single [Activity] in the stack and it will be considered not empty.
 *
 * Note: This method should be used for testing local logic as opposed to end to end verification.
 * End to end verification requires a device that supports Activity Embedding.
 *
 * @param testActivity an [Activity] that should be considered in the stack
 * @param isEmpty states if the stack is empty or not. In practice an [ActivityStack] with a single
 * [Activity] but [isEmpty] set to `false` means there is an [Activity] from outside the process
 * in the stack.
 */
@ExperimentalWindowApi
fun TestActivityStack(
    testActivity: Activity,
    isEmpty: Boolean = true,
    token: IBinder = Binder()
): ActivityStack {
    return ActivityStack(
        listOf(testActivity),
        isEmpty,
        token
    )
}
