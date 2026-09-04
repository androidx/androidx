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
class DefaultsNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = DefaultsNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            DefaultsNamingDetector.CAMEL_CASE_PROPERTY_ISSUE,
            DefaultsNamingDetector.REDUNDANT_PREFIX_ISSUE,
        )

    private val RuntimeStubs =
        kotlin(
                """
            package androidx.compose.runtime
            annotation class Stable
            annotation class Immutable
            annotation class Composable
            """
            )
            .indented()

    @Test
    fun camelCaseProperty_valid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val shape: String = "shape"
                        val shape2 = "shape2"
                        const val CONSTANT = 1
                        const val PascalConstant = 2
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_invalid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val Shape: String = "shape"
                        val Shape2 = "shape2"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:4: Error: Properties on Defaults objects should be camelCase [DefaultsCamelCaseWithoutConst]
                    val Shape: String = "shape"
                        ~~~~~
                src/androidx/compose/material3/CardDefaults.kt:5: Error: Properties on Defaults objects should be camelCase [DefaultsCamelCaseWithoutConst]
                    val Shape2 = "shape2"
                        ~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/androidx/compose/material3/CardDefaults.kt line 4: Rename to 'shape':
                @@ -4 +4
                -     val Shape: String = "shape"
                +     val shape: String = "shape"
                Fix for src/androidx/compose/material3/CardDefaults.kt line 5: Rename to 'shape2':
                @@ -5 +5
                -     val Shape2 = "shape2"
                +     val shape2 = "shape2"
                """
                    .trimIndent()
            )
    }

    @Test
    fun camelCaseProperty_constantTypes_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.ui.unit

                    import androidx.compose.runtime.Immutable

                    @Immutable
                    inline class Dp(val value: Float)
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.foundation.layout

                    import androidx.compose.runtime.Stable

                    @Stable
                    class PaddingValues
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.ui.unit.Dp
                    import androidx.compose.foundation.layout.PaddingValues

                    object CardDefaults {
                        val IconSize: Dp = Dp(24f)
                        val ContentPadding: PaddingValues = PaddingValues()
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseFunction_composable_valid() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.runtime

                    annotation class Composable
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.runtime.Composable

                    object CardDefaults {
                        @Composable
                        fun Shape() {}

                        @Composable
                        fun Title(): Unit {}
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_moreConstantTypes_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.ui.graphics

                    import androidx.compose.runtime.Immutable

                    @Immutable
                    value class StrokeCap(val value: Int)
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.ui.graphics.StrokeCap

                    object CardDefaults {
                        val MaxItems: Int = 10
                        val AspectRatio: Float = 1.5f
                        val ItemRange: IntRange = 1..10
                        val StrokeCap: StrokeCap = StrokeCap(0)
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_nonPublic_valid() {
        lint()
            .files(
                kotlin(
                    """
                    package androidx.compose.material3

                    object CardDefaults {
                        private val PrivateProp = "private"
                        internal val InternalProp = "internal"

                        private fun PrivateFunc() {}
                        internal fun InternalFunc() {}
                    }

                    internal object InternalCardDefaults {
                        val PublicPropInInternalObject = "public"
                        fun PublicFuncInInternalObject() {}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_tokensDirectory_valid() {
        lint()
            .files(
                kotlin(
                    "src/commonMain/kotlin/androidx/compose/material3/tokens/CardDefaults.kt",
                    """
                    package androidx.compose.material3.tokens

                    object CardDefaults {
                        val UpperCaseProperty = "invalid but in tokens directory"
                        fun UpperCaseFunction() {}
                    }
                    """,
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_shapeConstantTypes_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.ui.graphics

                    import androidx.compose.runtime.Stable

                    @Stable
                    interface Shape
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.foundation.shape

                    import androidx.compose.ui.graphics.Shape
                    import androidx.compose.runtime.Immutable

                    abstract class CornerBasedShape : Shape

                    class RoundedCornerShape : CornerBasedShape()

                    @Immutable
                    interface CornerSize
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.foundation.shape.CornerBasedShape
                    import androidx.compose.foundation.shape.RoundedCornerShape
                    import androidx.compose.foundation.shape.CornerSize

                    object CardDefaults {
                        val ExtraSmall: CornerBasedShape = RoundedCornerShape()
                        val Medium: RoundedCornerShape = RoundedCornerShape()
                        val ExtraLargeCornerSize: CornerSize = object : CornerSize {}
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_colorConstantType_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.ui.graphics

                    import androidx.compose.runtime.Immutable

                    @Immutable
                    value class Color(val value: ULong)
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.ui.graphics.Color

                    object CardDefaults {
                        val ContainerColor: Color = Color(0)
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_springSpecConstantType_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.animation.core

                    import androidx.compose.runtime.Immutable

                    @Immutable
                    class SpringSpec<T>
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.animation.core.SpringSpec

                    object CardDefaults {
                        val DefaultSpringSpec: SpringSpec<Float> = SpringSpec()
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseFunction_invalid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        fun Shape(): String = "shape"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:4: Error: Functions on Defaults objects should be camelCase [DefaultsCamelCaseWithoutConst]
                    fun Shape(): String = "shape"
                        ~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/androidx/compose/material3/CardDefaults.kt line 4: Rename to 'shape':
                @@ -4 +4
                -     fun Shape(): String = "shape"
                +     fun shape(): String = "shape"
                """
                    .trimIndent()
            )
    }

    @Test
    fun redundantComponentPrefix_valid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val shape: String = "shape"
                        fun elevation(): String = "elevation"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun redundantComponentPrefix_invalid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val cardShape: String = "shape"
                        fun cardElevation(): String = "elevation"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:4: Error: Redundant component prefix: 'cardShape' starts with 'card' [DefaultsRedundantPrefix]
                    val cardShape: String = "shape"
                        ~~~~~~~~~
                src/androidx/compose/material3/CardDefaults.kt:5: Error: Redundant component prefix: 'cardElevation' starts with 'card' [DefaultsRedundantPrefix]
                    fun cardElevation(): String = "elevation"
                        ~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/androidx/compose/material3/CardDefaults.kt line 4: Rename to 'shape':
                @@ -4 +4
                -     val cardShape: String = "shape"
                +     val shape: String = "shape"
                Fix for src/androidx/compose/material3/CardDefaults.kt line 5: Rename to 'elevation':
                @@ -5 +5
                -     fun cardElevation(): String = "elevation"
                +     fun elevation(): String = "elevation"
                """
                    .trimIndent()
            )
    }

    @Test
    fun redundantDefaultPrefix_invalid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        val defaultShape: String = "shape"
                        fun defaultElevation(): String = "elevation"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CardDefaults.kt:4: Error: Redundant 'default' prefix: 'defaultShape' should be 'shape' [DefaultsRedundantPrefix]
                    val defaultShape: String = "shape"
                        ~~~~~~~~~~~~
                src/androidx/compose/material3/CardDefaults.kt:5: Error: Redundant 'default' prefix: 'defaultElevation' should be 'elevation' [DefaultsRedundantPrefix]
                    fun defaultElevation(): String = "elevation"
                        ~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/androidx/compose/material3/CardDefaults.kt line 4: Rename to 'shape':
                @@ -4 +4
                -     val defaultShape: String = "shape"
                +     val shape: String = "shape"
                Fix for src/androidx/compose/material3/CardDefaults.kt line 5: Rename to 'elevation':
                @@ -5 +5
                -     fun defaultElevation(): String = "elevation"
                +     fun elevation(): String = "elevation"
                """
                    .trimIndent()
            )
    }

    @Test
    fun monolithicDefaults_variantPrefix_invalid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object ButtonDefaults {
                        val elevatedColors: String = "colors"
                        fun outlinedColors(): String = "colors"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/ButtonDefaults.kt:4: Error: Should use dedicated defaults object (e.g., ElevatedButtonDefaults.colors) instead of prefixed member 'elevatedColors' in monolithic defaults [DefaultsRedundantPrefix]
                    val elevatedColors: String = "colors"
                        ~~~~~~~~~~~~~~
                src/androidx/compose/material3/ButtonDefaults.kt:5: Error: Should use dedicated defaults object (e.g., OutlinedButtonDefaults.colors) instead of prefixed member 'outlinedColors' in monolithic defaults [DefaultsRedundantPrefix]
                    fun outlinedColors(): String = "colors"
                        ~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun variantDefaults_variantPrefix_valid() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object ElevatedButtonDefaults {
                        val colors: String = "colors"
                        fun elevation(): String = "elevation"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun camelCaseProperty_alignmentAndArrangement_valid() {
        lint()
            .files(
                RuntimeStubs,
                kotlin(
                    """
                    package androidx.compose.ui

                    import androidx.compose.runtime.Stable

                    @Stable
                    fun interface Alignment {
                        fun align()
                    }
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.foundation.layout

                    import androidx.compose.runtime.Stable

                    object Arrangement {
                        @Stable
                        interface Horizontal
                        @Stable
                        interface Vertical
                    }
                    """
                ),
                kotlin(
                    """
                    package androidx.compose.material3

                    import androidx.compose.ui.Alignment
                    import androidx.compose.foundation.layout.Arrangement

                    object CardDefaults {
                        val Center: Alignment = Alignment { }
                        val Start: Arrangement.Horizontal = object : Arrangement.Horizontal {}
                    }
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun nonPublicMembers_ignored() {
        lint()
            .files(
                kotlin(
                        """
                    package androidx.compose.material3

                    object CardDefaults {
                        // Violates checkRedundantComponentPrefix but internal/private -> should be ignored
                        internal val cardColors: String = "colors"
                        private fun cardElevation(): String = "elevation"
                        
                        // Violates checkRedundantDefaultPrefix but internal/private -> should be ignored
                        internal val defaultContentColor: String = "color"
                        
                        // Violates checkVariantPrefix but internal/private -> should be ignored
                        internal val elevatedColors: String = "colors"
                    }
                    """
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }
}
