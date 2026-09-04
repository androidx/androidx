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

package androidx.compose.material3.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComposableParameterOrderingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableParameterOrderingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableParameterOrderingDetector.ISSUE)

    // Stubs for types used in tests
    private val ModifierStub =
        kotlin(
                """
            package androidx.compose.ui
            interface Modifier {
                companion object : Modifier
            }
            """
            )
            .indented()

    private val ComposableStub =
        kotlin(
                """
            package androidx.compose.runtime
            annotation class Composable
            """
            )
            .indented()

    @Test
    fun validOrdering() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        onClick: () -> Unit,
                        modifier: Modifier = Modifier,
                        enabled: Boolean = true,
                        colors: String = "colors",
                        content: @Composable () -> Unit
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalidOrdering_modifierNotFirst() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        enabled: Boolean = true,
                        modifier: Modifier = Modifier,
                        onClick: () -> Unit,
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:9: Error: Modifier parameter should be the first optional parameter [ComposableParameterOrdering]
                    modifier: Modifier = Modifier,
                    ~~~~~~~~
                src/androidx/compose/material3/TestButton.kt:9: Error: Parameter 'enabled' (tier PRIMARY_BEHAVIOR_FLAG) should come after 'modifier' (tier MODIFIER) [ComposableParameterOrdering]
                    modifier: Modifier = Modifier,
                    ~~~~~~~~
                src/androidx/compose/material3/TestButton.kt:10: Error: Parameter 'modifier' (tier MODIFIER) should come after 'onClick' (tier REQUIRED_INPUT) [ComposableParameterOrdering]
                    onClick: () -> Unit,
                    ~~~~~~~
                3 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun invalidOrdering_stylingBeforeFlag() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors",
                        enabled: Boolean = true,
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:10: Error: Parameter 'colors' (tier VISUAL_STYLING) should come after 'enabled' (tier PRIMARY_BEHAVIOR_FLAG) [ComposableParameterOrdering]
                    enabled: Boolean = true,
                    ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    private val LayoutStubs =
        kotlin(
                """
            package androidx.compose.foundation.layout
            interface WindowInsets
            """
            )
            .indented()

    private val DialogStubs =
        kotlin(
                """
            package androidx.compose.ui.window
            class DialogProperties
            """
            )
            .indented()

    @Test
    fun validOrdering_windowInsetsBeforeProperties() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                LayoutStubs,
                DialogStubs,
                kotlin(
                        "src/androidx/compose/material3/TestDialog.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import androidx.compose.foundation.layout.WindowInsets
                    import androidx.compose.ui.window.DialogProperties

                    @Composable
                    fun TestDialog(
                        modifier: Modifier = Modifier,
                        windowInsets: WindowInsets = object : WindowInsets {},
                        properties: DialogProperties = DialogProperties(),
                        content: @Composable () -> Unit
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalidOrdering_propertiesBeforeWindowInsets() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                LayoutStubs,
                DialogStubs,
                kotlin(
                        "src/androidx/compose/material3/TestDialog.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import androidx.compose.foundation.layout.WindowInsets
                    import androidx.compose.ui.window.DialogProperties

                    @Composable
                    fun TestDialog(
                        modifier: Modifier = Modifier,
                        properties: DialogProperties = DialogProperties(),
                        windowInsets: WindowInsets = object : WindowInsets {},
                        content: @Composable () -> Unit
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestDialog.kt:12: Error: Parameter 'properties' (tier PLATFORM_WINDOW_CONFIG) should come after 'windowInsets' (tier VISUAL_STYLING) [ComposableParameterOrdering]
                    windowInsets: WindowInsets = object : WindowInsets {},
                    ~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun validOrdering_interspersedFlags() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        label: String = "label",
                        isError: Boolean = false, // Interspersed flag (Tier 5) is allowed after Slot (Tier 5)
                        colors: String = "colors", // Styling (Tier 6) comes after
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalidOrdering_primaryFlagAfterSlot() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        label: String = "label", // Tier 5 (Slot)
                        enabled: Boolean = true, // Tier 4 (Primary flag) - declared too late!
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:10: Error: Parameter 'label' (tier SLOT_CONFIG) should come after 'enabled' (tier PRIMARY_BEHAVIOR_FLAG) [ComposableParameterOrdering]
                    enabled: Boolean = true, // Tier 4 (Primary flag) - declared too late!
                    ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun invalidOrdering_interspersedFlagAfterStyling() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors", // Tier 6 (Styling)
                        isError: Boolean = false, // Tier 5 (Interspersed flag) - declared too late!
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:10: Error: Parameter 'colors' (tier VISUAL_STYLING) should come after 'isError' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    isError: Boolean = false, // Tier 5 (Interspersed flag) - declared too late!
                    ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun validOrdering_behaviorAfterStyling() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class ScrollBehavior

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors", // Tier 6 (Styling)
                        scrollBehavior: ScrollBehavior? = null, // Tier 7 (Auxiliary behavior) - allowed after styling
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun validOrdering_behaviorBeforeStyling() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class ScrollBehavior

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        scrollBehavior: ScrollBehavior? = null,
                        colors: String = "colors",
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalidOrdering_behaviorBeforeSlot() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class ScrollBehavior

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        scrollBehavior: ScrollBehavior? = null,
                        label: @Composable () -> Unit = {},
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:12: Error: Parameter 'scrollBehavior' (tier AUXILIARY_BEHAVIOR) should come after 'label' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    label: @Composable () -> Unit = {},
                    ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun validOrdering_layoutAfterSlot() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class Alignment
                    class Arrangement

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        label: String = "label", // Tier 5 (Slot)
                        alignment: Alignment = Alignment(), // Tier 6 (Layout)
                        horizontalArrangement: Arrangement = Arrangement(), // Tier 6 (Layout)
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalidOrdering_layoutBeforeSlot() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class Alignment

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        alignment: Alignment = Alignment(), // Tier 6 (Layout) - too early!
                        label: String = "label", // Tier 5 (Slot)
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:12: Error: Parameter 'alignment' (tier VISUAL_STYLING) should come after 'label' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    label: String = "label", // Tier 5 (Slot)
                    ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun ignored_ifDeprecatedOverloadExists() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    // Active overload: has suboptimal ordering (colors before enabled)
                    // but it preserves the deprecated sibling signature (modifier, colors)
                    // and appends the new parameter (enabled) at the end. Should be ignored!
                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors",
                        enabled: Boolean = true,
                    ) {}

                    @Deprecated("Use overload with enabled flag")
                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors",
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun flagged_ifNewParametersSuboptimallyOrdered() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    class DialogProperties

                    // Active overload: has two new parameters (properties and enabled)
                    // appended at the end. Their relative order (properties before enabled)
                    // is suboptimal and NOT constrained by the deprecated sibling (modifier, colors).
                    // This should be flagged!
                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors",
                        properties: DialogProperties = DialogProperties(),
                        enabled: Boolean = true,
                    ) {}

                    @Deprecated("Use new overload")
                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        colors: String = "colors",
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TestButton.kt:17: Error: Parameter 'properties' (tier PLATFORM_WINDOW_CONFIG) should come after 'enabled' (tier PRIMARY_BEHAVIOR_FLAG) [ComposableParameterOrdering]
                    enabled: Boolean = true,
                    ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testSources_ignored() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "test/androidx/compose/material3/TestButton.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun TestButton(
                        modifier: Modifier = Modifier,
                        itemCount: Int,
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun heightParameter_tier6() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/AppBar.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyAppBar(
                        modifier: Modifier = Modifier,
                        navigationIcon: @Composable () -> Unit = {},
                        collapsedHeight: Int = 56,
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun heightParameter_beforeSlot_invalid() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/AppBar.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyAppBar(
                        modifier: Modifier = Modifier,
                        collapsedHeight: Int = 56,
                        navigationIcon: @Composable () -> Unit = {},
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/AppBar.kt:10: Error: Parameter 'collapsedHeight' (tier VISUAL_STYLING) should come after 'navigationIcon' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    navigationIcon: @Composable () -> Unit = {},
                    ~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun windowInsetsParameter_tier6() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/layout/WindowInsets.kt",
                        """
                    package androidx.compose.foundation.layout
                    class WindowInsets
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/AppBar.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.layout.WindowInsets
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyAppBar(
                        modifier: Modifier = Modifier,
                        navigationIcon: @Composable () -> Unit = {},
                        customInsets: WindowInsets = WindowInsets(),
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun windowInsetsParameter_beforeSlot_invalid() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/layout/WindowInsets.kt",
                        """
                    package androidx.compose.foundation.layout
                    class WindowInsets
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/AppBar.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.layout.WindowInsets
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyAppBar(
                        modifier: Modifier = Modifier,
                        customInsets: WindowInsets = WindowInsets(),
                        navigationIcon: @Composable () -> Unit = {},
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/AppBar.kt:11: Error: Parameter 'customInsets' (tier VISUAL_STYLING) should come after 'navigationIcon' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    navigationIcon: @Composable () -> Unit = {},
                    ~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun widthParameter_tier6() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/Sheet.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MySheet(
                        modifier: Modifier = Modifier,
                        dragHandle: @Composable () -> Unit = {},
                        sheetMaxWidth: Int = 300,
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun widthParameter_beforeSlot_invalid() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/material3/Sheet.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MySheet(
                        modifier: Modifier = Modifier,
                        sheetMaxWidth: Int = 300,
                        dragHandle: @Composable () -> Unit = {},
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/Sheet.kt:10: Error: Parameter 'sheetMaxWidth' (tier VISUAL_STYLING) should come after 'dragHandle' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    dragHandle: @Composable () -> Unit = {},
                    ~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun scrollState_withPriorState_treatedAsAuxiliaryBehavior() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/ScrollState.kt",
                        """
                    package androidx.compose.foundation
                    class ScrollState
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/TextField.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.ScrollState
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyTextField(
                        state: String,
                        modifier: Modifier = Modifier,
                        label: @Composable () -> Unit = {},
                        scrollState: ScrollState = ScrollState(),
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun scrollState_withoutPriorState_treatedAsPrimaryStateController() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/ScrollState.kt",
                        """
                    package androidx.compose.foundation
                    class ScrollState
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/TabRow.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.ScrollState
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyTabRow(
                        modifier: Modifier = Modifier,
                        scrollState: ScrollState = ScrollState(),
                        label: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun scrollState_withPriorState_beforeSlot_invalid() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/ScrollState.kt",
                        """
                    package androidx.compose.foundation
                    class ScrollState
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/TextField.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.ScrollState
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyTextField(
                        state: String,
                        modifier: Modifier = Modifier,
                        scrollState: ScrollState = ScrollState(),
                        label: @Composable () -> Unit = {},
                        content: @Composable () -> Unit = {},
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/TextField.kt:12: Error: Parameter 'scrollState' (tier AUXILIARY_BEHAVIOR) should come after 'label' (tier SLOT_CONFIG) [ComposableParameterOrdering]
                    label: @Composable () -> Unit = {},
                    ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun scrollState_withPriorState_beforeStyling_valid() {
        lint()
            .files(
                ComposableStub,
                ModifierStub,
                kotlin(
                        "src/androidx/compose/foundation/ScrollState.kt",
                        """
                    package androidx.compose.foundation
                    class ScrollState
                    """,
                    )
                    .indented(),
                kotlin(
                        "src/androidx/compose/material3/TextField.kt",
                        """
                    package androidx.compose.material3

                    import androidx.compose.foundation.ScrollState
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier

                    @Composable
                    fun MyTextField(
                        state: String,
                        modifier: Modifier = Modifier,
                        label: @Composable () -> Unit = {},
                        scrollState: ScrollState = ScrollState(),
                        shape: String = "shape",
                    ) {}
                    """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
