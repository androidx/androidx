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

package androidx.xr.compose.testing.samples

import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Dp

internal class SubspaceTestingActivity : ComponentActivity()

internal val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

/**
 * Converts a float to a [Dp] value.
 *
 * Mainly used in Compose for XR internal unit tests.
 *
 * @return a [Dp] object representing the same value in Dp.
 */
internal fun Float.toDp(): Dp {
    return Dp(this / Resources.getSystem().displayMetrics.density)
}

/**
 * Converts an integer to a [Dp] value.
 *
 * Mainly used in Compose for XR internal unit tests.
 *
 * @return a [Dp] object representing the same value in Dp.
 */
internal fun Int.toDp(): Dp {
    return Dp(this.toFloat() / Resources.getSystem().displayMetrics.density)
}
