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

package androidx.ui.core

import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.test.runOnUiThreadIR
import androidx.ui.framework.test.TestActivity
import androidx.ui.input.EditorValue
import androidx.ui.layout.rtl
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class CoreTextFieldTest {
    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
    }

    @Test
    fun testCorrectLayoutDirection() {
        val latch = CountDownLatch(1)
        var layoutDirection: LayoutDirection? = null
        rule.runOnUiThreadIR {
            activity.setContent {
                CoreTextField(
                    value = EditorValue("..."),
                    modifier = Modifier.rtl,
                    onValueChange = {}
                ) { result ->
                    layoutDirection = result.layoutInput.layoutDirection
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(layoutDirection).isNotNull()
        assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
    }
}
