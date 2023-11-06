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

package androidx.glance.appwidget

import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.layout.Box
import androidx.glance.text.Text
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LambdaActionTest {
    @Test
    fun siblingActionsHaveDifferentKeys() = runTest {
        val lambdas = runTestingComposition {
            Box {
                Text("hello1", modifier = GlanceModifier.clickable {})
                Text("hello2", modifier = GlanceModifier.clickable {})
            }
            Text("hello3", modifier = GlanceModifier.clickable {})
        }.updateLambdaActionKeys()

        assertThat(lambdas.size).isEqualTo(3)
    }
}
