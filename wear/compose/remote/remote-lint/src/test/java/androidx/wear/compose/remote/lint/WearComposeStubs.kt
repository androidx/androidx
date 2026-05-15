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

package androidx.wear.compose.remote.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

object WearComposeStubs {
    val WearMaterialLocalTextStyleStub: TestFile =
        kotlin(
            """
            package androidx.wear.compose.material

            import androidx.compose.runtime.ProvidableCompositionLocal
            import androidx.compose.ui.text.TextStyle

            object LocalTextStyle : ProvidableCompositionLocal<TextStyle>() {
                val current: TextStyle = TextStyle()
                override infix fun provides(value: TextStyle): Any = Any()
            }
            """
                .trimIndent()
        )

    val WearMaterialLocalContentColorStub: TestFile =
        kotlin(
            """
            package androidx.wear.compose.material

            import androidx.compose.runtime.ProvidableCompositionLocal
            import androidx.compose.ui.graphics.Color

            object LocalContentColor : ProvidableCompositionLocal<Color>() {
                val current: Color = Color.Black
                override infix fun provides(value: Color): Any = Any()
            }
            """
                .trimIndent()
        )

    val WearMaterial3LocalTextStyleStub: TestFile =
        kotlin(
            """
            package androidx.wear.compose.material3

            import androidx.compose.runtime.ProvidableCompositionLocal
            import androidx.compose.ui.text.TextStyle

            object LocalTextStyle : ProvidableCompositionLocal<TextStyle>() {
                val current: TextStyle = TextStyle()
                override infix fun provides(value: TextStyle): Any = Any()
            }
            """
                .trimIndent()
        )

    val WearMaterial3LocalContentColorStub: TestFile =
        kotlin(
            """
            package androidx.wear.compose.material3

            import androidx.compose.runtime.ProvidableCompositionLocal
            import androidx.compose.ui.graphics.Color

            object LocalContentColor : ProvidableCompositionLocal<Color>() {
                val current: Color = Color.Black
                override infix fun provides(value: Color): Any = Any()
            }
            """
                .trimIndent()
        )
}
