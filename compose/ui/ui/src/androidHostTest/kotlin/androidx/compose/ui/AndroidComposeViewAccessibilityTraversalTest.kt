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

package androidx.compose.ui

import android.app.Activity
import android.view.View
import androidx.compose.ui.platform.AndroidComposeView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 29)
class AndroidComposeViewAccessibilityTraversalTest {

    @Test
    fun findViewByAccessibilityIdTraversal_doesNotCrash() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = View(activity)
        // With the fix applied, this should not crash (no IllegalArgumentException).
        // It should safely return null because the accessibility ID (17) does not match the view.
        val result = AndroidComposeView.findViewByAccessibilityIdTraversal(17, view)
        assertThat(result).isNull()
    }
}
