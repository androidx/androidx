/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LazyLayoutStateReadInCompositionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LazyLayoutStateReadInCompositionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            LazyLayoutStateReadInCompositionDetector.FrequentlyChangedStateReadInComposition
        )

    private val lazyGridStateStub =
        bytecodeStub(
            filename = "LazyGridState.kt",
            filepath = "androidx/compose/foundation/lazy/grid",
            checksum = 0x1f30c0f3,
            source =
                """
                    package androidx.compose.foundation.lazy.grid

                    interface LazyGridLayoutInfo {

                    }

                    class LazyGridState {
                        val firstVisibleItemIndex: Int get() = 0
                        val firstVisibleItemScrollOffset: Int get() = 0
                        val layoutInfo: LazyGridLayoutInfo get() = object : LazyGridLayoutInfo {}
                    }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uSST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczPAzLzhdhCUotLvEuUGLQYAAC6oXVDAAAA
        """,
            """
        androidx/compose/foundation/lazy/grid/LazyGridLayoutInfo.class:
        H4sIAAAAAAAA/52OzU4CMRSFzy3Kz/g3qCT4EhaIC4MrN5pJxphI4oZVmemQ
        wtAapkPAFc/lwrD2oYx3ML6Avcnpd3ubc8/X98cngBt0CLfKpktn0rVM3OLN
        FVpmrrSp8sZZmav3jZwuTSpjpkeGWG1c6SObuQaIEM7USvE3O5XPk5lOfAM1
        QjueO58bK5+0V2yl7ghisarxTqqkVQkINOf3tam6HlPaJ3R222YguiIQIVPW
        3W0HokfVcEAYxv8NywF4X/g3GHnl9fXcE4KRK5eJfjC5Jly9lNabhX41hZnk
        +t5a5/feRZ0T4AC/R+Bir+e45LvPxodc9TFqERoRmhFaCBhxFOEYJ2NQgVOc
        jSEKhAXaP+1d5AJ/AQAA
        """,
            """
        androidx/compose/foundation/lazy/grid/LazyGridState＄layoutInfo＄1.class:
        H4sIAAAAAAAA/6VSTW/TQBB946R1Y1zSlo8mfH8E1PaAm8IBQYWACpAlAxJF
        ueS0sbfpNs4ustdVyyl/iRMSB5QzPwoxGxAVx5bLzJu3M7Oz++bHz2/fATzC
        fcJzobPCqOwoSs34kylltGcqnQmrjI5y8fk4GhYqixJGbxjsWmFlJxfHprKx
        3jOdrg8iLB2IQ8Hpehi9HxzI1PqoER6frnfyt6uPOcL8ttLKPiPU1tZ7IXws
        BKijQajbfVUSXib/O/pTwnIyMjZXOnorreBSwZw3Pqzx95AzDWdAoBHzR8pF
        m4yyLmF1OmkE00ngtbwNmk4WgtZ0suVtkjveIjw55Xgnr+cRHp7haT7ahMWh
        tCedCNtr62efI8RVXGNx/7nmwciyBDsmk4RmorR8V40HsvgoBjkzK4lJRd4T
        hXLxHzKMtZbFTi7KUrJwzVc6zU2p9JA/fd9khGDXVEUqXyuX3f5QaavGsqdK
        xeUvtDZ2NmyJLjxeAV6NmSZwO8H+JqPIacR+buMrgi8MPNxiOz8jz+E22/B3
        Akch+zrusA2Y85zCaOPujL2C6+jM6m/gHvsuZyxy1fk+ajGaMZZiLGOFIS7E
        uIhLfVCJy1jtwysdbP0CBEAxIl4DAAA=
        """,
            """
        androidx/compose/foundation/lazy/grid/LazyGridState.class:
        H4sIAAAAAAAA/6VTW08TQRT+ZnvfFtiiaLkoKqiAygJKQpQYlQSzSYUETBPD
        07ad1qHbWbMzJeATv8VnH/SJRBPT+OiPMp5ZGm7yILgP5/LN+c45e87Mr9/f
        fgB4glmGx76sR6Go77q1sP0hVNxthB1Z97UIpRv4H/fcZiTqbpms12Rsal/z
        DBiDs+3v+BQhm+56dZvXdAYJhvSykEI/Z0hMTVcKSCFtI4kMQ1K/F4phsXyJ
        es8YSk2uV0WkdEUoUQ24p3nbk3W+G1fyGMbPCdisRWEQrDcaimuGPooo+3th
        R3uyETIsT01fsJljNnU0UQ6jprvNdTXyhVSuL2WoY6Zy10K91gkCinpxid+d
        DI7qTM5nULQxaCa4dNlmM7jKUCy3Qh0I6b7h2ieST81Z7Z0E3QNmRM4IMLAW
        4bvCeHNk1ecZWt39MdsqWbbldPdtK2uMLOnUIZi1St39BWuOvUr9/JS2HGuj
        6CRGrLnk0ir56ZFkNuWkCcucwrIxliPMPsLyTsGUXGCmkaHG+eseO4uf3rId
        nFjx0/9ZsHNqKbMtSp5cCeucYaAsJF/rtKs8eutTFwyD5bDmBxU/EsbvgaMb
        HalFm3ty57Dbl8dXhKHgScmjlcBXipNrb4adqMZXhWEO95iVv3iYh0UPynxJ
        GhK9L5L3yXPN7kinZg6Q/UqGhSmS6RjMYppk4TAAOdiki8jHiCE/7JGtxOcz
        zNwJpnXE7PsHpn0usx8D5BnmImlzlv+OwXcHuNLF0JczKfInUuR7KWZ6pw7p
        BB7EhWj+8USGqR2T4R4ekd4g/Br97vUtJDyUPAx7GMEomRjzcAM3t8AUxnFr
        C30KtsJthbTCndjIKxQUJpQ5mlS4q9CvMPAH6p7bvDkFAAA=
        """
        )

    private val lazyListStateStub =
        bytecodeStub(
            filename = "LazyListState.kt",
            filepath = "androidx/compose/foundation/lazy",
            checksum = 0x4e68cddf,
            source =
                """
                    package androidx.compose.foundation.lazy

                    interface LazyListLayoutInfo {

                    }

                    class LazyListState {
                        val firstVisibleItemIndex: Int get() = 0
                        val firstVisibleItemScrollOffset: Int get() = 0
                        val layoutInfo: LazyListLayoutInfo get() = object : LazyListLayoutInfo {}
                    }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uSST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczPAzLzhdhCUotLvEuUGLQYAAC6oXVDAAAA
        """,
            """
        androidx/compose/foundation/lazy/LazyListLayoutInfo.class:
        H4sIAAAAAAAA/51OTU/CQBB9syiF+lVUEvwTFlBOnryYNKkxkcRLT0u7mIWy
        a9gtAU/8Lg+Gsz/KOMX4B5xJ3rz5yHvz9f3xCeAWXcKNNMXS6mId53bxZp2K
        p7YyhfTamriU75s4ZUi186nc2MonZmoDECGayZXkC/MaP01mKvcBGoROOre+
        1CZ+VF6yirwjiMWqwXZUQ7sGEGjO87Wuuz6zYkDo7ratUPREKCJm095uOxR9
        qpdDwij9x5/szVbR32LspVfXc08Ix7Za5upBl4pw9VwZrxfqRTs9KdW9Mdbv
        ZV2TzXGA3xC42OM5LrkOWPiQs5mhkSBI0ErQRsgURwmOcZKBHE5xlkE4RA6d
        H25NYfx1AQAA
        """,
            """
        androidx/compose/foundation/lazy/LazyListState＄layoutInfo＄1.class:
        H4sIAAAAAAAA/6VSS2/TQBD+xknrxqSkLY8mvB8BtT3UTQFxKCChCiRLhkoU
        5ZLTxt6m2zi7yF5XLaf8JU5IHFDO/CjErEE8bgUu38x8ntfO5y9fP30G8BD3
        CTtCp7lR6UmYmMk7U8jwwJQ6FVYZHWbi/WkYM8SqsPtWWNnNxKkpbaQPTLfn
        gwhLR+JYcKYehXvDI5lYHzXCgzO3jX829DFHmH+itLLPCLW19X4TPhYC1NEg
        1O2hKghP4/9YeIewHI+NzZQOX0kruEow502Oa3wPctBwAAKNmT9RLtpiL+0R
        VmfTRjCbBl7b26DZdCFoz6bb3ha5z9uER2ff7Nebefrm3z3IR4ewOJK/NSE8
        Xlv/p+lNXMU11vCPCZtjy+feNakktGKl5etyMpT5WzHMmFmJTSKyvsiVi3+Q
        zUhrme9moigki9R6oZPMFEqP+MqHJiUE+6bME/lSuezOm1JbNZF9VSguf661
        sdWeBXrwWG7+DSoR4PRne5O90InCdm7jI4IP7Hi4xThfkedwm7H5PYGjJts6
        7jAGzHlOUnRwt2Kv4Dq6Vf0N3GPb44xFrjo/QC1CK8JShGWssIsLES7i0gBU
        4DJWB/AK57a/AQF0+xNAAwAA
        """,
            """
        androidx/compose/foundation/lazy/LazyListState.class:
        H4sIAAAAAAAA/51TS08UQRD+evY9u8gsii4PRQUVUJkFJZpgTJSEZJIREjCb
        GE6zu73Y7GyPme4l4Inf4tmDnkg0MRuP/ihj9bDhJQdgDvX4ur6q6qrpP39/
        /ALwHHMMc4FsxpFo7rqNqPMpUtxtRV3ZDLSIpBsGn/dcn4QvlN7QgeY5MAZn
        O9gJ6FBuuWv1bd7QOaQYsq+EFPo1Q2p6plZCBlkbaeQY0vqjUAxV/3Kllhgq
        W1yviFjpmlCiHnJP844nm3w3KeIxTJwTsNGIozBca7UU1wwDFOEHe1FXe7IV
        MbyYnrl4H8dEambSj+Itd5vrehwIqdxAykgnJOWuRnq1G4YUtXS5S06FRyWm
        5nMo2xgyI3t2hRZzuMFQ9tuRDoV033EdUHxALVmdnRStmxlRMAIMrE34rjBe
        lazmPEO7tz9uWxXLtpzevm3ljZEnnTkE81alt79gVdnbzO8vWcux1stOatSq
        pl+ukJ8dTeczTpaw3Cksn2AFwuwjrOiUTMkFZhoZbp2/3/Gz+Om12uGJnS5e
        caPOqVXMtSlvejlqcoZBX0i+2u3Uefw+oAYYhvyoEYS1IBbG74Nj612pRYd7
        cuew0TfH/wRDyZOSx8thoBQn196IunGDrwjDHOkza//xMA+L3o350jQfekYk
        H5HnmrWRzsweIP+dDAvTJLMJmMcMydJhAAqwSZdRTBBDftInW6mvZ5iFE0zr
        iDlwAaZ9LvMaBskzzEXS5qz4E0MfDnC9h+FvZ1IUT6Qo9lPM9k8d0ik8TgrR
        /JOJjFA7JsNDPCW9TvhNuu6tTaQ8VDyMeBjFGJkY93AbdzbBFCZwdxMDCrbC
        PYWswv3EKCqUFCaVOZpSeKBwTWHwHwU6zXobBQAA
        """
        )

    @Test
    fun observablePropertiesUsedInComposableFunction() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                @Composable
                fun TestGrid(state: LazyGridState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }

                @Composable
                fun TestList(state: LazyListState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }
            """
                ),
                lazyGridStateStub,
                lazyListStateStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/androidx/compose/foundation/foo/test.kt:10: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = state.firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:11: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = state.firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:12: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = state.layoutInfo
                                     ~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = state.firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = state.firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = state.layoutInfo
                                     ~~~~~~~~~~~~~~~~
0 errors, 6 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Wrap with derivedStateOf:
@@ -10 +10
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Collect with snapshotFlow:
@@ -10 +10
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Wrap with derivedStateOf:
@@ -11 +11
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Collect with snapshotFlow:
@@ -11 +11
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Wrap with derivedStateOf:
@@ -12 +12
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Collect with snapshotFlow:
@@ -12 +12
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -14 +16
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -21 +23
+                 }
            """
                    .trimIndent()
            )
    }

    @Test
    fun observablePropertiesUsedInNonComposableFunction() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun testGrid(state: LazyGridState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }

                fun testList(state: LazyListState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }
            """
                ),
                lazyGridStateStub,
                lazyListStateStub,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }

    @Test
    fun observablePropertiesUsedInComposableFunctionWithReceiver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                @Composable
                fun LazyGridState.TestGrid() {
                    val index = firstVisibleItemIndex
                    val offset = firstVisibleItemScrollOffset
                    val layoutInfo = layoutInfo
                }

                @Composable
                fun LazyListState.TestList() {
                    val index = firstVisibleItemIndex
                    val offset = firstVisibleItemScrollOffset
                    val layoutInfo = layoutInfo
                }
            """
                ),
                lazyGridStateStub,
                lazyListStateStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/foundation/foo/test.kt:10: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:11: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:12: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = layoutInfo
                                     ~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = layoutInfo
                                     ~~~~~~~~~~
0 errors, 6 warnings
            """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Wrap with derivedStateOf:
@@ -10 +10
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Collect with snapshotFlow:
@@ -10 +10
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Wrap with derivedStateOf:
@@ -11 +11
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Collect with snapshotFlow:
@@ -11 +11
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Wrap with derivedStateOf:
@@ -12 +12
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Collect with snapshotFlow:
@@ -12 +12
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -14 +16
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -21 +23
+                 }
            """
                    .trimIndent()
            )
    }

    @Test
    fun observablePropertiesUsedInComposableLambda() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun setContent(content: @Composable () -> Unit) {
                    // no-op
                }

                fun testGrid() {
                    val state = LazyGridState()
                    setContent {
                        val index = state.firstVisibleItemIndex
                        val offset = state.firstVisibleItemScrollOffset
                        val layoutInfo = state.layoutInfo
                    }
                }

                fun testList() {
                     val state = LazyListState()
                    setContent {
                        val index = state.firstVisibleItemIndex
                        val offset = state.firstVisibleItemScrollOffset
                        val layoutInfo = state.layoutInfo
                    }
                }
            """
                ),
                lazyGridStateStub,
                lazyListStateStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/foundation/foo/test.kt:15: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = state.firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:16: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = state.firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = state.layoutInfo
                                         ~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:24: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = state.firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:25: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = state.firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:26: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = state.layoutInfo
                                         ~~~~~~~~~~~~~~~~
0 errors, 6 warnings
            """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/foundation/foo/test.kt line 15: Wrap with derivedStateOf:
@@ -15 +15
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 15: Collect with snapshotFlow:
@@ -15 +15
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 16: Wrap with derivedStateOf:
@@ -16 +16
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 16: Collect with snapshotFlow:
@@ -16 +16
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 24: Wrap with derivedStateOf:
@@ -24 +24
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 24: Collect with snapshotFlow:
@@ -24 +24
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 25: Wrap with derivedStateOf:
@@ -25 +25
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 25: Collect with snapshotFlow:
@@ -25 +25
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Wrap with derivedStateOf:
@@ -26 +26
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Collect with snapshotFlow:
@@ -26 +26
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
            """
                    .trimIndent()
            )
    }

    @Test
    fun observablePropertiesUsedInComposableLambdaWithReceiver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun setListContent(content: @Composable LazyListState.() -> Unit) {
                    // no-op
                }

                fun setGridContent(content: @Composable LazyGridState.() -> Unit) {
                    // no-op
                }

                fun testGrid() {
                    setGridContent {
                        val index = firstVisibleItemIndex
                        val offset = firstVisibleItemScrollOffset
                        val layoutInfo = layoutInfo
                    }
                }

                fun testList() {
                    setListContent {
                        val index = firstVisibleItemIndex
                        val offset = firstVisibleItemScrollOffset
                        val layoutInfo = layoutInfo
                    }
                }
            """
                ),
                lazyGridStateStub,
                lazyListStateStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:20: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = layoutInfo
                                         ~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:26: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:27: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:28: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = layoutInfo
                                         ~~~~~~~~~~
0 errors, 6 warnings
            """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 20: Wrap with derivedStateOf:
@@ -20 +20
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 20: Collect with snapshotFlow:
@@ -20 +20
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Wrap with derivedStateOf:
@@ -26 +26
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Collect with snapshotFlow:
@@ -26 +26
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 27: Wrap with derivedStateOf:
@@ -27 +27
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 27: Collect with snapshotFlow:
@@ -27 +27
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 28: Wrap with derivedStateOf:
@@ -28 +28
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 28: Collect with snapshotFlow:
@@ -28 +28
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
            """
                    .trimIndent()
            )
    }
}
