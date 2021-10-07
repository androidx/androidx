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

package androidx.glance.appwidget.action

import android.app.Activity
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.action.LaunchActivityAction

internal class LaunchActivityIntentAction(val intent: Intent) : LaunchActivityAction

/**
 * Creates an [Action] that launches an [Activity] from the given [Intent] when triggered. The
 * intent should specify a component with [Intent.setClass] or [Intent.setComponent].
 *
 * This action is supported by app widgets only.
 */
public fun launchActivityAction(intent: Intent): Action = LaunchActivityIntentAction(intent)
