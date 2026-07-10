/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build.lint

class ExperimentalCompanionWithNonExperimentalElementsDetectorTest :
    AbstractLintDetectorTest(
        useDetector = ExperimentalCompanionWithNonExperimentalElementsDetector(),
        useIssues = listOf(ExperimentalCompanionWithNonExperimentalElementsDetector.ISSUE),
    ) {

    fun testExperimentalCompanionWithFieldsNotMarkedAsExperimental() {
        val input =
            arrayOf(
                ktSample("androidx.KtClassWithAnnotatedCompanion"),
                ktSample("sample.annotation.provider.ExperimentalSampleAnnotation"),
            )

        val expected =
            """
            src/androidx/MyAnnotation.kt:28: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                    @MyAnnotation const val A: Int = 1
                                            ~
            src/androidx/MyAnnotation.kt:32: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                    const val C: Int = 3
                              ~
            src/androidx/MyAnnotation.kt:34: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                    @MyAnnotation fun myFun() {}
                                      ~~~~~
            src/androidx/MyAnnotation.kt:38: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                    fun myFun3() {}
                        ~~~~~~
            src/androidx/MyAnnotation.kt:93: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                        val A: Int = 1
                            ~
            src/androidx/MyAnnotation.kt:97: Error: Elements in an experimental companion object must be annotated as experimental [ExperimentalCompanionElement]
                        var C: Int = 1
                            ~
            6 errors
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Autofix for src/androidx/MyAnnotation.kt line 28: Annotate as experimental:
            @@ -27,0 +28 @@
            +        @ExperimentalSampleAnnotation
            Autofix for src/androidx/MyAnnotation.kt line 32: Annotate as experimental:
            @@ -31,0 +32 @@
            +        @ExperimentalSampleAnnotation
            Autofix for src/androidx/MyAnnotation.kt line 34: Annotate as experimental:
            @@ -33,0 +34 @@
            +        @ExperimentalSampleAnnotation
            Autofix for src/androidx/MyAnnotation.kt line 38: Annotate as experimental:
            @@ -37,0 +38 @@
            +        @ExperimentalSampleAnnotation
            Autofix for src/androidx/MyAnnotation.kt line 93: Annotate as experimental:
            @@ -92,0 +93 @@
            +            @ExperimentalSampleAnnotation
            Autofix for src/androidx/MyAnnotation.kt line 97: Annotate as experimental:
            @@ -96,0 +97 @@
            +            @ExperimentalSampleAnnotation
            """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
