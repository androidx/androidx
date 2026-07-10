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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.creation.drawWithContent
import org.junit.Test

class DrawWithContentTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testDrawWithContent() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    box(
                        Modifier.size(100).drawWithContent {
                            mRemoteWriter.rcPaint.setColor(0xFFFF0000.toInt()).commit()
                            drawRect(0f, 0f, 100f, 100f)
                            drawContent()
                            mRemoteWriter.rcPaint.setColor(0xFF0000FF.toInt()).commit()
                            drawCircle(50f, 50f, 25f)
                        }
                    ) {
                        box(Modifier.size(50).background(0xFF00FF00.toInt()))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "DrawWithContent",
            ops,
        )
    }
}
