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
@file:JvmName("TestActivityStack")

package androidx.window.testing.embedding

import android.app.Activity
import android.os.Binder
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.window.embedding.ActivityStack

/**
 * Creates an [ActivityStack] instance for testing. The default values are an empty list for
 * [activitiesInProcess] but a false value for [isEmpty]. This is the same as being embedded with
 * an [Activity] from another process.
 *
 * @param activitiesInProcess The [Activity] list with the same process of the host task with
 *     empty list as the default value.
 * @param isEmpty Indicates whether this `ActivityStack` contains any [Activity] regardless of the
 *     process with `false` as the default value.
 * @return An [ActivityStack] instance for testing.
 */
@Suppress("FunctionName")
@JvmName("createTestActivityStack")
@JvmOverloads
fun TestActivityStack(
    activitiesInProcess: List<Activity> = emptyList(),
    isEmpty: Boolean = false,
): ActivityStack = ActivityStack(activitiesInProcess, isEmpty, TEST_ACTIVITY_STACK_TOKEN)

@RestrictTo(RestrictTo.Scope.LIBRARY)
@VisibleForTesting
@JvmField
val TEST_ACTIVITY_STACK_TOKEN = Binder()
