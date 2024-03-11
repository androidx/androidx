/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.lifecycle

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.kruth.assertThat
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreationExtrasTest {
    @Test
    fun testInitialCreationExtras() {
        val initial = MutableCreationExtras()
        val key = object : CreationExtras.Key<Bundle> { }
        initial[key] = bundleOf("value" to "initial")
        val mutable = MutableCreationExtras(initial)
        initial[key] = bundleOf("value" to "overridden")
        assertThat(mutable[key]?.getString("value")).isEqualTo("initial")
    }
}
