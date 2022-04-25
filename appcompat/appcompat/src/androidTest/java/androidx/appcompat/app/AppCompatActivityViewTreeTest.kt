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

package androidx.appcompat.app

import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AppCompatActivityViewTreeTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule<AppCompatInflaterDefaultActivity>(
        AppCompatInflaterDefaultActivity::class.java
    )

    @Test
    fun queryViewTreeLifecycleTest() {
        val lfOwner = ViewTreeLifecycleOwner.get(activityRule.activity.window.decorView)
        assertThat(lfOwner).isEqualTo(activityRule.activity)
    }

    @Test
    fun queryViewTreeViewModelStoreTest() {
        val vmsOwner = ViewTreeViewModelStoreOwner.get(activityRule.activity.window.decorView)
        assertThat(vmsOwner).isEqualTo(activityRule.activity)
    }

    @Test
    fun queryViewTreeSavedStateRegistryTest() {
        val ssrOwner = activityRule.activity.window.decorView.findViewTreeSavedStateRegistryOwner()
        assertThat(ssrOwner).isEqualTo(activityRule.activity)
    }
}