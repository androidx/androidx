/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PagerCustomKeyTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun keysLambdaIsCalledOnlyOnce() {
        var keyCalls = 0

        rule.setContent {
            HorizontalPager(
                state = rememberPagerState { 1 },
                key = {
                    keyCalls++
                    0
                }
            ) {
                Spacer(Modifier.fillMaxSize())
            }
        }

        rule.runOnIdle {
            Truth.assertThat(keyCalls).isEqualTo(1)
        }
    }
}
