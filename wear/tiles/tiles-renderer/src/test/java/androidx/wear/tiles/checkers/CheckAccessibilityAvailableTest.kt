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

import androidx.wear.tiles.TilesTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TilesTestRunner::class)
class CheckAccessibilityAvailableTest {
    @Test
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    fun check_throwsWithNoSemantics() {
        val entry = buildTimelineEntry(
            androidx.wear.tiles.LayoutElementBuilders.Box.Builder().build())

        var exception: CheckerException? = null

        try {
            CheckAccessibilityAvailable().check(entry)
        } catch (ex: CheckerException) {
            exception = ex
        }

        assertThat(exception).isNotNull()
    }

    @Test
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    fun check_doesntThrowIfSemanticsPresent() {
        val entry =
            buildTimelineEntry(
                androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                    .setModifiers(
                        androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                            .setSemantics(
                                androidx.wear.tiles.ModifiersBuilders.Semantics.Builder()
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
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    fun check_doesntThrowIfSemanticsPresentOnNestedElement() {
        val entry =
            buildTimelineEntry(
                androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                    .addContent(
                        androidx.wear.tiles.LayoutElementBuilders.Box.Builder()
                            .setModifiers(
                                androidx.wear.tiles.ModifiersBuilders.Modifiers.Builder()
                                    .setSemantics(
                                        androidx.wear.tiles.ModifiersBuilders.Semantics.Builder()
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

    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    private fun buildTimelineEntry(
        layout: androidx.wear.tiles.LayoutElementBuilders.LayoutElement
    ) =
        androidx.wear.tiles.TimelineBuilders.TimelineEntry.Builder()
            .setLayout(
                androidx.wear.tiles.LayoutElementBuilders.Layout.Builder().setRoot(layout).build()
            )
            .build()
}
