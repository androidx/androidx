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

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class AndroidViewsHandlerDelayedCreationTest {

    @get:Rule val rule = createComposeRule()

    private var originalDelayFlagsEnabled = true

    @Before
    fun setUp() {
        originalDelayFlagsEnabled = AndroidComposeUiFlags.isDelayAndroidViewsHandlerCreationEnabled
    }

    @After
    fun tearDown() {
        AndroidComposeUiFlags.isDelayAndroidViewsHandlerCreationEnabled = originalDelayFlagsEnabled
    }

    @Test
    fun delayedHandler_isMeasuredAndLaidOutWhenAddedToLaidOutComposeView() {
        AndroidComposeUiFlags.isDelayAndroidViewsHandlerCreationEnabled = true

        var androidComposeView: AndroidComposeView? = null
        var includeAndroidView by mutableStateOf(false)

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            Box(Modifier.fillMaxSize()) {
                if (includeAndroidView) {
                    AndroidView(factory = { View(it) }, modifier = Modifier.size(50.dp))
                }
            }
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            assertThat(view.isLaidOut).isTrue()
            assertThat(view.width).isGreaterThan(0)
            assertThat(view.height).isGreaterThan(0)
            assertThat(view.androidViewsHandler).isNull()
            includeAndroidView = true
        }

        rule.runOnIdle {
            val view = androidComposeView!!
            val handler = view.androidViewsHandler
            assertThat(handler).isNotNull()
            assertThat(handler!!.isLaidOut).isTrue()
            assertThat(handler.width).isEqualTo(view.width)
            assertThat(handler.height).isEqualTo(view.height)
        }
    }
}
