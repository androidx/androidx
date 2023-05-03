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
import androidx.window.embedding.ActivityStack

/**
 * Creates an [ActivityStack] instance for testing, which defaults to an [ActivityStack] with
 * cross-process activities.
 *
 * The [activitiesInProcess] can be passed from the activity obtained from
 * [androidx.test.core.app.ActivityScenario] or even mock Activities.
 *
 * @param activitiesInProcess The [Activity] list with the same process of the host task with
 *     empty list as the default value
 * @param isEmpty Indicates whether this `ActivityStack` contains any [Activity] regardless of the
 *     process with `false` as the default value
 * @return An [ActivityStack] instance for testing
 */
@Suppress("FunctionName")
@JvmName("createTestActivityStack")
@JvmOverloads
fun TestActivityStack(
    activitiesInProcess: List<Activity> = emptyList(),
    isEmpty: Boolean = false,
): ActivityStack = ActivityStack(activitiesInProcess, isEmpty)