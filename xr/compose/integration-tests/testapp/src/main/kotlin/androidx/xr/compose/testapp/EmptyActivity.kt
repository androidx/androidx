/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.testapp

import androidx.activity.ComponentActivity

/**
 * A generic blank activity used as a clean-slate host for instrumented testing and Compose
 * integration tests.
 *
 * This activity is crucial for creating a predictable and lightweight environment when launching
 * instrumented test cases via `ActivityScenario` or Jetpack Compose's `createAndroidComposeRule`.
 * It avoids the resource overhead, lifecycle side effects, and UI interference that would occur by
 * launching functional application activities (such as [MainActivity]) during testing.
 *
 * By registering this activity in the integration test application's manifest, it provides the
 * necessary test harness framework hooks to dynamically set, mount, and inspect Compose spatial or
 * standard hierarchies in regression suite executions.
 */
class EmptyActivity : ComponentActivity()
