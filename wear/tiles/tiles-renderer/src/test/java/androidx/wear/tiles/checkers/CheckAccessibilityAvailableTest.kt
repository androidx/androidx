/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.checkers

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TilesTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TilesTestRunner::class)
class CheckAccessibilityAvailableTest {
    @Test
    fun check_throwsWithNoSemantics() {
        val entry = buildTimelineEntry(LayoutElementBuilders.Box.Builder().build())

        var exception: CheckerException? = null

        try {
            CheckAccessibilityAvailable().check(entry)
        } catch (ex: CheckerException) {
            exception = ex
        }

        assertThat(exception).isNotNull()
    }

    @Test
    fun check_doesntThrowIfSemanticsPresent() {
        val entry =
            buildTimelineEntry(
                LayoutElementBuilders.Box.Builder()
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setSemantics(
                                ModifiersBuilders.Semantics.Builder()
                                    .setContentDescription("Hello World")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

        var exception: CheckerException? = null

        try {
            CheckAccessibilityAvailable().check(entry)
        } catch (ex: CheckerException) {
            exception = ex
        }

        assertThat(exception).isNull()
    }

    @Test
    fun check_doesntThrowIfSemanticsPresentOnNestedElement() {
        val entry =
            buildTimelineEntry(
                LayoutElementBuilders.Box.Builder()
                    .addContent(
                        LayoutElementBuilders.Box.Builder()
                            .setModifiers(
                                ModifiersBuilders.Modifiers.Builder()
                                    .setSemantics(
                                        ModifiersBuilders.Semantics.Builder()
                                            .setContentDescription("Hello World")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

        var exception: CheckerException? = null

        try {
            CheckAccessibilityAvailable().check(entry)
        } catch (ex: CheckerException) {
            exception = ex
        }

        assertThat(exception).isNull()
    }

    private fun buildTimelineEntry(layout: LayoutElementBuilders.LayoutElement) =
        TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
            .build()
}
