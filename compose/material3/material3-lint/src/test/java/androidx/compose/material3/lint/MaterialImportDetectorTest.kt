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

@RunWith(JUnit4::class)

/** Test for [MaterialImportDetector]. */
class MaterialImportDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MaterialImportDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(MaterialImportDetector.UsingMaterialAndMaterial3Libraries)

    private val MaterialButtonStub =
        bytecodeStub(
            filename = "Button.kt",
            filepath = "androidx/compose/material",
            checksum = 0x3ab9ae7,
            """
            package androidx.compose.material

            fun Button() {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material/ButtonKt.class:
        H4sIAAAAAAAA/yVOu07DQBCcPScOMY84hJdTUkGDE0RHBUhIFgEkQGlSXeIT
        usS+Q/Y5SplfoqVAqfkoxB3eYnZ2Zla7P79f3wCuEBFOuUoLLdNVPNP5hy5F
        nHMjCsmz+LYyRqsH0wIRwjlf8jjj6j1+ns7FzKoewa8zBO/sfEzojhbaZFLF
        j8LwlBt+TWD50rPHyEHbAQi0cIRZcyUdG1iWDgm9zdoPNuuAhazvh5t1nw3I
        WZfkttr1sYuFITTudCoInZFU4qnKp6J449PMKsGrroqZuJduiF4qZWQuxrKU
        1r1RShtupFYlhmBooH4nQhO+7Yd2ilAXff5bRxadBRtmOHZf4wAntg+t2rKL
        WxN4CdoJggTb2Emwi70EHYQTUIku9idgJZolen+absHWdwEAAA==
        """
        )

    private val ExperimentalMaterialApiStub =
        bytecodeStub(
            filename = "ExperimentalMaterialApi.kt",
            filepath = "androidx/compose/material",
            checksum = 0x4808c29,
            """
            package androidx.compose.material

            @RequiresOptIn(
                "This material API is experimental and is likely to change or to be removed in" +
                    " the future."
            )
            @Retention(AnnotationRetention.BINARY)
            annotation class ExperimentalMaterialApi
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material/ExperimentalMaterialApi.class:
        H4sIAAAAAAAA/5VSTW8TMRB93pAmTflIC5SkodRwaG/dtnDjtEWAVkpplSCk
        KicnGRo3u+uw9kbpLTf+EwcUceRHIWZFQyJRhLiM38x79ryx/f3Hl68AXuCZ
        wKFK+qnR/YnfM/HIWPJj5SjVKvJfT0YMYkqcik6ui8FIlyAEqpdqrPxIJRf+
        afeSeq6EgsDOoqqSxDjltEn84DcsoSiw2RwaF+nEb9GnTKdkT0cuTF4KlGKy
        Vl2QwPn7gbZybkQGZ6HknJb8SLad1yI9pOhKOiN7A25L0qR50iWZUmzGxKJE
        ugHJj5nLUtoX2J63X3LYIsenMmIXxbGKMvawd4NuMcnyjpXj8F3QOheQzRvn
        X9bu/kNyZiLdu8ptvGoG7bbA+tzGCTnVV04x58XjAr+fyMNqHiAghlyf6Dw7
        YNQ/FKjPpuWKV/MqXrVR/vbZq82mR96BOJ5Nc8GRwPPmfz8+d+dmW39h94dO
        oNI2WdqjNzriW6y3Mp4rpg/a6m5Eiwu0Ao1rLkzGf7C7bBG3uNVKPh4K2OHo
        4Qkkr285rzC3RriNO7j7C95DFesoYqODQoj7IR6EeIhNhngUooZ6B8JiC40O
        PIvHFtt4yoet8W7+0ihbrP4EIBEHSxkDAAA=
        """
        )

    private val Material3ButtonStub =
        bytecodeStub(
            filename = "Button.kt",
            filepath = "androidx/compose/material3",
            checksum = 0x314468f6,
            """
            package androidx.compose.material3

            fun Button() {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material3/ButtonKt.class:
        H4sIAAAAAAAA/yVOu07DQBCcPScOMY84hJfT0kCDE6CjAiQkiwASoDSpLvEJ
        HbHvkH2OUuaXaClQaj4KcYe3mJ2dmdXuz+/XN4BLRIRjrtJCy3QZz3T+oUsR
        59yIQvLsIr6pjNHq3rRAhPCdL3iccfUWP03fxcyqHsGvMwTv5HRM6I7m2mRS
        xQ/C8JQbfkVg+cKz18hB2wEINHeEWXMpHRtYlg4JvfXKD9argIWs74frVZ8N
        yFnn5Lba9bGzuSE0bnUqCJ2RVOKxyqeieOXTzCrBi66KmbiTboieK2VkLsay
        lNa9VkobbqRWJYZgaKB+J0ITvu37dopQF33+WwcWnQUbZjh0X2MPR7YPrdqy
        ixsTeAnaCYIEm9hKsI2dBB2EE1CJLnYnYCWaJXp/zsaw43gBAAA=
        """
        )

    private val RippleStub =
        bytecodeStub(
            filename = "Ripple.kt",
            filepath = "androidx/compose/material/ripple",
            checksum = 0x691c7742,
            """
            package androidx.compose.material.ripple

            fun rememberRipple() {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material/ripple/RippleKt.class:
        H4sIAAAAAAAA/yVOTU/CQBB9s+VDqkIRv8ovkIsF4s2TMTFpRE3QcOG00I1Z
        aHdJuxCO/CWvHgxnf5Rxl87hvZl5M3nv9+/7B8AdQkKPqyTXMtlGc52tdCGi
        jBuRS55GuVytUhGND/Rs6iBCsOAbHqVcfUZvs4WY261HaOYiE9lM5OUtwbvp
        TQjt0VKbVKroRRiecMPvCSzbeNabHDQcgEBL1zArbqXr+rZLBoTOflfz9zuf
        BaxbC/a7LuuTk4bkvhql2e3SECqPOrG2rZFU4nXtknzwmQviv+t1PhdP0g3h
        eK2MzMREFtKqD0ppw43UqsAADBWUcUJUUbN8YacQZdHXQbq06CTYY4Yrlxrn
        uLY8sNu6fTyawovRiOHHOMZJjFM0Y7QQTEEF2jibghWoFuj8AynVobWGAQAA
        """
        )

    private val IconsStub =
        bytecodeStub(
            filename = "Icons.kt",
            filepath = "androidx/compose/material/icons",
            checksum = 0xe246828f,
            """
            package androidx.compose.material.icons

            object Icons
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material/icons/Icons.class:
        H4sIAAAAAAAA/42Sz27TQBDGv90kjuMGmhZoEwq00CL+HHBbcaNCKhVIloyR
        aBWp6mljr9pNbC+yN1GPOfEgvEHFoRKVUAQ3HgoxayI4cGEtzcw3O/vbnZF/
        /PzyFcBzbDE8FHlSaJWc+7HOPuhS+pkwslAi9VWs89IPrG2CMXSGYiL8VOSn
        /rvBUMamiRqDs6dyZV4y1B4/6bfRgOOhjiZD3ZypkuFR+F83vGBw9+K0Ynng
        FuAG0eHRfnTwuo1r8FqUvM6wGeri1B9KMyiEosMiz7URRllQpE00TlNCLYUj
        bQjmv5VGJMIIyvFsUqOumTUta8DARpQ/V1ZtU5TsMGzNpp7Hu9zjHYpmU/f7
        R96dTXf5NnvVdPm3Tw7vcFu7yyzBrV7/bGQY1t6Pc6MyGeQTVapBKvf/vo3G
        caATybAYqlxG42wgiyNBNQzLoY5F2hc0EdLzpHeox0Us3ygrenNw/x8sdmgq
        9aqVnh0S+XukHPId8py+RqXWSfm2YfKNp5dwL6rtjXkxCHKfbPt3AVqEAlws
        /Dm8StV2LVyBH1+i/RmLF1WC40Fl72Kz+qdo+ARYPkEtwI0ANwPcwgqFWA3Q
        Re8ErMRtrNF+Ca/EnRLOL4cbczOQAgAA
        """
        )

    private val PullRefreshStub =
        bytecodeStub(
            filename = "PullRefresh.kt",
            filepath = "androidx/compose/material/pullrefresh",
            checksum = 0xfa59248b,
            """
            package androidx.compose.material.pullrefresh

            fun pullRefresh() {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicCotKcnP8y7hMuNSxalMr6A0J6coNa0otThDiDcAyAmCcID6
        dLgUcOsryiwoyEkV4ggC00DValxSOFUbI7mGl4s5LT9fiC0ktbjEu0SJQYsB
        AAY211PcAAAA
        """,
            """
        androidx/compose/material/pullrefresh/PullRefreshKt.class:
        H4sIAAAAAAAA/01OTU/CQBSct+VDikIRv8ov0IsF9ebJmJg04kfQcOG00FUX
        2i5pt4Qjf8mrB8PZH2XclYO+w7x5M/OS+fr++ARwAZ9wztMoUzJaBhOVzFUu
        goRrkUkeB/MijjPxkon8LXg0fLDht7oKInhTvuBBzNPX4GE8FROjOoT6/C9I
        cI5PhoRWf6Z0LNPgTmgecc0vCSxZOKYCWahZAIFmljBjLqVlXcOiHqG9XlXc
        9cplHutUvPWqw7pkrTOyX41/1U5nmlC6VpEgNPsyFfdFMhbZMx/HRnGfVJFN
        xI20hz8oUi0TMZS5NO5VmirNtVRpjh4YSth08lFGxex9c/nYDL3/WgcGrQUT
        Zji01bGHI7N7Rq2ax60RnBC1EG6IOrZD7KARoglvBMrRwu4ILEc5R/sHIVOo
        LJIBAAA=
        """
        )

    @Test
    fun material_imports() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.material.Button
                import androidx.compose.material.ExperimentalMaterialApi
                import androidx.compose.material3.Button
                import androidx.compose.material.ripple.rememberRipple
                import androidx.compose.material.icons.Icons
                import androidx.compose.material.pullrefresh.pullRefresh

                fun test() {}
            """
                ),
                MaterialButtonStub,
                ExperimentalMaterialApiStub,
                Material3ButtonStub,
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
    fun material_wildcardImports() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.material.*
                import androidx.compose.material3.*
                import androidx.compose.material.ripple.*
                import androidx.compose.material.icons.*
                import androidx.compose.material.pullrefresh.*

                fun test() {}
            """
                ),
                MaterialButtonStub,
                ExperimentalMaterialApiStub,
                Material3ButtonStub,
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
