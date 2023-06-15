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

@file:Suppress("UnstableApiUsage")

package androidx.compose.material3.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [MaterialImportDetector].
 */
class MaterialImportDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MaterialImportDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(MaterialImportDetector.UsingMaterialAndMaterial3Libraries)

    private val ButtonStub = bytecodeStub(
        filename = "Button.kt",
        filepath = "androidx/compose/material",
        checksum = 0x94880e7a,
        """
            package androidx.compose.material

            fun Button() {}
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DVvFzMafn5QmwhqcUl3iVKDFoMAB7yTT20AAAA
        """,
        """
        androidx/compose/material/ButtonKt.class:
        H4sIAAAAAAAA/yVOu07DQBCcPSdOYh5xCC+npIIGJ4iOCpCQLAJIgNKkusQn
        dMS+Q/Y5SplfoqVAqfkoxB3eYnZ2Zla7P79f3wAuERFOuEoLLdNVPNf5hy5F
        nHMjCsmz+KYyRqt70wIRwne+5HHG1Vv8NHsXc6t6BL/OELzTswmhN15ok0kV
        PwjDU274FYHlS88eIwdtByDQwhFmzZV0bGhZOiL0N2s/2KwDFrKBH27WAzYk
        Z12Q2+rUx84XhtC41akgdMdSiccqn4nilc8yqwQvuirm4k66IXqulJG5mMhS
        WvdaKW24kVqVGIGhgfqdCE34th/YKUJd9PlvHVp0FmyY4ch9jX0c2z6yassu
        tqfwEnQSBAm2sJ1gB7sJuginoBI97E3BSjRL9P8AKS93K3cBAAA=
        """
    )

    private val RippleStub = bytecodeStub(
        filename = "Ripple.kt",
        filepath = "androidx/compose/material/ripple",
        checksum = 0x2f218395,
        """
            package androidx.compose.material.ripple

            fun rememberRipple() {}
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DVvFzMafn5QmwhqcUl3iVKDFoMAB7yTT20AAAA
        """,
        """
        androidx/compose/material/ripple/RippleKt.class:
        H4sIAAAAAAAA/yVOTU/CQBB9s+WzKhTxq/wCuVgw3jwZE5NG1AQNF04L3ZiF
        dpe0C+HIX/LqwXD2Rxl36Rzem5k3k/d+/75/ANwhJPS5SnItk20019lKFyLK
        uBG55GmUy9UqFdH4QM+mDiIEC77hUcrVZ/Q2W4i53XqEVi4ykc1EXt4SvOv+
        hNAZLbVJpYpehOEJN/yewLKNZ73JQcMBCLR0DbPiVrpuYLtkSOjudzV/v/NZ
        wHq1YL/rsQE56ZbcV7M0u1kaQuVRJ9a2PZJKvK5dkg8+c0H8d73O5+JJuiEc
        r5WRmZjIQlr1QSltuJFaFRiCoYIyTogqapbP7RSiLPo6SBcWnQR7zHDpUuMM
        V5aHdlu3j40pvBjNGH6MIxzHOEErRhvBFFSgg9MpWIFqge4/mpQXSIYBAAA=
        """
        )

    private val IconsStub = bytecodeStub(
        filename = "Icons.kt",
        filepath = "androidx/compose/material/icons",
        checksum = 0x1643e419,
        """
            package androidx.compose.material.icons

            object Icons
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DVvFzMafn5QmwhqcUl3iVKDFoMAB7yTT20AAAA
        """,
        """
        androidx/compose/material/icons/Icons.class:
        H4sIAAAAAAAA/42SzW7TQBSFz0wSx3UDDeWnCQUKtIifBW4rdlRIpQLJkjES
        rSKhrib2qExizyB7EnWZFQ/CG1QsKoGEItjxUIg7JoIFG2zp3nvu3Pk8c+Qf
        Pz9/BfAEWwz3hM5Ko7LTMDXFe1PJsBBWlkrkoUqNrsLIxTYYQ3ckpiLMhT4J
        Xw9HMrVtNBi8PaWVfcbQePBw0EELXoAm2gxN+05VDPfj//rCUwZ/L81rVgDu
        AH6UHB7tJwcvOriAYImaFxk2Y1OehCNph6VQtFlobaywyoESY5NJnhPqUjw2
        lmDhK2lFJqygHi+mDbo1c8F3AQxsTP1T5dQ2VdkOw9Z8FgS8xwPepWo+879/
        4L35bJdvs+dtn3/76PEud7O7zBH8+vSPx5Zh/c1EW1XISE9VpYa53P97NrLj
        wGSSYSVWWiaTYijLI0EzDKuxSUU+EOQI6UUzODSTMpUvlRP9BXjwDxY75Eqz
        vkrfmUT5FimPcpcyp7dVqw1Sobsw5dajc/hn9fLtxTAIcodi5/cAlghFLmH5
        z+Y1mnbP8hfwt+fofMLKWd3guFvHm9is/ykynwCrx2hEuBzhSoSruEYl1iL0
        0D8Gq3Ad67ReIahwo4L3C+qkwQ6QAgAA
        """
        )

    private val PullRefreshStub = bytecodeStub(
        filename = "PullRefresh.kt",
        filepath = "androidx/compose/material/pullrefresh",
        checksum = 0x20bedb6d,
        """
            package androidx.compose.material.pullrefresh

            fun pullRefresh() {}
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DVvFzMafn5QmwhqcUl3iVKDFoMAB7yTT20AAAA
        """,
        """
        androidx/compose/material/pullrefresh/PullRefreshKt.class:
        H4sIAAAAAAAA/01OTU/CQBSct+XLolDEr/IL9GJBvXkyJiaN+BE0XDgtdNWF
        tkvaLeHIX/LqwXD2Rxl35aDvMG/ezLxkvr4/PgFcwCec8zTKlIyWwUQlc5WL
        IOFaZJLHwbyI40y8ZCJ/Cx4NH2z4ra6CCN6UL3gQ8/Q1eBhPxcSoDqE+/wsS
        nOOTIaHVnykdyzS4E5pHXPNLAksWjqlAFmoWQKCZJcyYS2lZ17CoR2ivVxV3
        vXKZxzoVb73qsC5Z64zsV+NftdOZJpSuVSQIzb5MxX2RjEX2zMexUdwnVWQT
        cSPt4Q+KVMtEDGUujXuVpkpzLVWaoweGEjadfJRRMXvfXD42Q++/1oFBa8GE
        GQ5tdezhyOyeUavmsTaCE2IrhBuiju0QO2iEaMIbgXK0sDsCy1HO0f4B7wOm
        OJIBAAA=
        """
        )

    @Test
    fun imports() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.material.Button
                import androidx.compose.material.ripple.rememberRipple
                import androidx.compose.material.icons.Icons
                import androidx.compose.material.pullrefresh.pullRefresh

                fun test() {}
            """
            ),
            ButtonStub,
            RippleStub,
            IconsStub,
            PullRefreshStub
        )
            .run()
            .expect(
                """
src/foo/test.kt:4: Warning: Using a material import while also using the material3 library [UsingMaterialAndMaterial3Libraries]
                import androidx.compose.material.Button
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun wildcardImports() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.material.*
                import androidx.compose.material.ripple.*
                import androidx.compose.material.icons.*
                import androidx.compose.material.pullrefresh.*

                fun test() {}
            """
            ),
            ButtonStub,
            RippleStub,
            IconsStub,
            PullRefreshStub
        )
            .run()
            .expect(
                """
src/foo/test.kt:4: Warning: Using a material import while also using the material3 library [UsingMaterialAndMaterial3Libraries]
                import androidx.compose.material.*
                       ~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }
}
/* ktlint-enable max-line-length */
