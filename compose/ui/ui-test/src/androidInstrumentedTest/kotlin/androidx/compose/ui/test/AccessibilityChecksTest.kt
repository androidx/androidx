/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalTestApi::class)
class AccessibilityChecksTest {

    @Test
    fun performAccessibilityChecks_findsNoErrors() = runComposeUiTest {
        setContent { Box(Modifier.size(20.dp)) }

        // There are no accessibility checks setup, this should not throw
        onRoot().tryPerformAccessibilityChecks()
    }

    @Composable
    private fun BoxWithoutProblems() {
        Box(Modifier.size(20.dp))
    }
}
