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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ComposableCoroutineCreationDetector]. */
class ComposableCoroutineCreationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableCoroutineCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableCoroutineCreationDetector.CoroutineCreationDuringComposition)

    private val coroutineBuildersStub: TestFile =
        bytecodeStub(
            filename = "Builders.common.kt",
            filepath = "kotlinx/coroutines",
            checksum = 0x8bc08fcf,
            """
        package kotlinx.coroutines

        object CoroutineScope

        fun CoroutineScope.async(
            block: suspend CoroutineScope.() -> Unit
        ) {}

        fun CoroutineScope.launch(
            block: suspend CoroutineScope.() -> Unit
        ) {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2XMsQoCMRAE0BXh4LZzm2usLBQsgt+g5bX2Ipc9CCa7R7JB
        P98IWjkw3ZsBgDUArFp7+Ab3uL2Lzxr8y02aFi3schULid2sSt2Vi42GB6SH
        WgzyYVmrBeFCm3MN0XMut7ZNKg2ecPiHbo76JPzp0ai/aIw8tesdHOENLed0
        U5sAAAA=
        """,
            """
        kotlinx/coroutines/Builders_commonKt.class:
        H4sIAAAAAAAA/61TXU8TQRQ9M/1guxQpKyBUxSpVvoQtxAeTEhIkkmxENIK8
        8GCm27Vsu501+0HgjX9i4i/QN+ODIT76o4x3tl0ENRGNL3fOvXPumTt37nz9
        9ukzgAeoMVQ7fuS58si0/cCPI1c6ofkodr2mE4SvbL/b9eWTaACModQWh8L0
        hGyZzxptx6ZohiEnwmNpM2zObv1GaSOFO7b/xqn3KWb7sGu+jqUdub4Mzc0+
        WqnP7TG8/w9Cq4uX0Eg5FymSQCyUypmI+VK6UX2tvrD1cwcoqEqe3vKDltl2
        okYgXKpDSOlHolfTth9tx55XZ8ivRgduuKahwDB1rn5XRk4ghWdaMgoo3bXD
        AQwyjNkHjt3p5z8Xgeg6RGSYmf21jnORHSXSorqKGMIVHUUM0ys1PN/uaBhh
        GKyqMqr9Z5u+RKMYKn96OLqdJ8g7YCj29FN3JE196kSiKSJBXN49zND8MWUK
        yoCBdRTgtHnkKkSjyZvLDG9PTyr66YnOS1znEzyBEz3IS6mj8fJDcsq8xuZ5
        ja/MlDLlaY0ZWYM8Qze0BLFazsgb2QlWy9eyX97luTagbEmj5MK/5qoyV5i6
        gZHe9HxbKn8YMqJMpZTHR5FDz+/LVGD3OOm+kf7Hpd5/XOpEDNkNv+kwDG+R
        5HbcbTjBrmh4jirDt4W3JwJX+f1gYcdtSRHFAeHqi5jO7zqWPHRDl7bPZmv9
        x9wy6Dt+HNjOpqvyJ/s5e72Mc0QsgyOr3pDWSeSQRwZz5K2Tz2kdmjf0jygt
        GAbZDwltnmyeOlaEhgXC4z0irmI0ERrCCMZo/37CHsCiinEKaMmoKDtJSX91
        UvHCSdcufxLHUmJnYdJqUXSCbjm5j4yFsoXrFm7gpoUp3LJQwe19sBB3ML0P
        PUQuRDXEaIiREHdD3EvcmRD5EOPfAUeuoPKFBQAA
        """,
            """
        kotlinx/coroutines/CoroutineScope.class:
        H4sIAAAAAAAA/4WSTW/TQBCG390kjuMGGspHE8pXaQ/AoW4rblRIbQSSpRAk
        UkWqeto4q7KJvYvsddRjTvwQ/kHFoRJIKIIbPwoxawIcOOCVZuadnX3WM/b3
        H5++AHiKbYbNqbGJ0udhbDJTWKVlHnZ/h4PYvJN1MIbWRMxEmAh9Fr4eTWRs
        66gweAdKK/ucofLo8bCJGrwAVdQZqvatyhm2ev+lP2PwD+Kk5ATg7rAf9QfH
        h/3uiyauIGhQ8qpDmewsnEg7yoTSeSi0NlZYZSjuG9svkoRQ15YXhq+kFWNh
        BeV4OqtQt8yZhjNgYFPKnyundika7zFsL+ZBwNs84C2KFnP/23veXsz3+S47
        qvv86wePt7ir3WeOsHZUqGQss3wnNmlq9M7UMmy8KbRVqYz0TOVqlMjDv29J
        Q+masWRY7VHv/SIdyexYUA2xeiYWyVBkyullMhiYIovlS+VEZwke/oPFHs2n
        WjbVceMif4+UR75FntOqleo+qdC1Tr725BL+Rbn9YFkMgmySbf4qQINQgI+V
        P4fXqdo9K5/BTy7R/IjVizLB8bC0d7FV/lX0GQiwdopKhOsRbkS4iVsUYj1C
        G51TsBy3sUH7OYIcd3J4PwGm9PkckgIAAA==
        """
        )

    private val flowStub: TestFile =
        bytecodeStub(
            filename = "Flow.kt",
            filepath = "kotlinx/coroutines/flow",
            checksum = 0x40e0a7,
            """
        package kotlinx.coroutines.flow

        class Flow<out T>
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2XMsQoCMRAE0BXh4LZzm2usLBQsgt+g5bX2Ipc9CCa7R7JB
        P98IWjkw3ZsBgDUArFp7+Ab3uL2Lzxr8y02aFi3schULid2sSt2Vi42GB6SH
        WgzyYVmrBeFCm3MN0XMut7ZNKg2ecPiHbo76JPzp0ai/aIw8tesdHOENLed0
        U5sAAAA=
        """,
            """
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAA/31QwW4TMRSc5012m22g21IghVJ6LDmwbYWEVKpKgFQpUgCJ
        Rrnk5CRLcbOxpbW39Ljfwh9wQuqhWnHko6o+h5xA4jJvZjx+fs+/b69vALzC
        LmF7Zlyu9FU6MYUpndKZTb/k5lt6yhCBCHvHg6P+hbyUaS71efppfJFN3JuT
        fy1C8rcXoUEIj5VW7oQQ7L0YthEiitHECqHhvipL2On/bwZuu74MpB8yJ6fS
        SfbE/DLgHchDywMINGP/Snm1z2x6QOjWVTsWHRHXVSwShrrq1FU3XKmrhHbp
        UOyLd81f30ORBP7GITcZkO8V+bdfzhxP+d5MM8Jan+f6WM7HWTGQ45ydjb6Z
        yHwoC+X10mydqXMtXVkwj89MWUyyU+UPtj6X2ql5NlRWcfKt1sZJp4y2OIDg
        D1nu4P+H8QmrdKGBZvcnWj+YCDxlDBdmA9uM7T8BxFjlGuDZIhVgZ1G38Jzr
        a860OXNvhKCH+z2s9ZBgnSk2eniAzRHI4iEejdCwWLV4bNGxiO4ApHDXcycC
        AAA=
        """
        )

    private val flowBuildersStub: TestFile =
        bytecodeStub(
            filename = "Builders.kt",
            filepath = "kotlinx/coroutines/flow",
            checksum = 0xa1c50396,
            """
        package kotlinx.coroutines.flow

        fun <T> flowOf(
            value: T
        ): Flow<T> = Flow()
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2XMsQoCMRAE0BXh4LZzm2usLBQsgt+g5bX2Ipc9CCa7R7JB
        P98IWjkw3ZsBgDUArFp7+Ab3uL2Lzxr8y02aFi3schULid2sSt2Vi42GB6SH
        WgzyYVmrBeFCm3MN0XMut7ZNKg2ecPiHbo76JPzp0ai/aIw8tesdHOENLed0
        U5sAAAA=
        """,
            """
        kotlinx/coroutines/flow/BuildersKt.class:
        H4sIAAAAAAAA/4VRXW8SQRQ9dxcW2KJs8aultVXqB31xW+JTS0jUpHEjtokl
        JIanAbZkYNlJdmexj/wWf4FvJpoY4qM/yniXYkwk2mT33HvPnHtn5syPn1++
        AXiOp4TqWOlAhpduX0Uq0TL0Y/ciUB/cl4kMBn4Uv9E5EMEZialwAxEO3bPe
        yO8zaxKsVHp2QajXWn8Ljvdb/5p9wnBMaDbaR6ttzVq7fU1vgxVNHrDXUtHQ
        Hfm6FwkZxq4IQ6WFlorzU6VPkyBg1fb/RuVQ4Gs0ZCh1k2DW9jtFrKFow8YN
        QnYqgsQnlFePSVhfntF962sxEFowZ0ymJltLKRRSAIHGaWLw4qVMswPOBofs
        2Xxm2/wbG4Zt5M3KrjOfVfLlTNl4bRxQNZOfzxyjbjlmhYnvHy3DyaSddcLO
        dc5Sm7D2+wWfjTUh80oN+CKlFmtPk0nPj9qiFyyupvoi6IhIpvWSLJzLYSh0
        EnG+9S4JtZz4XjiVseTlF39cJtjnKon6/olM2zaX0s6KEIcwkMGVIZvIwoKJ
        B1zVmSeO+a+w33/GzU+pVXjIaC14C1XG4pUGJTgc9xaaHB4tVflF/XiBu3jC
        8YjZdd6l3IXp4ZaH2x7u4K6He9jweP9KFxRjC9tdZOP0ux9jJ0YphvULZwN9
        KR0DAAA=
        """
        )

    private val flowCollectStub: TestFile =
        bytecodeStub(
            filename = "Collect.kt",
            filepath = "kotlinx/coroutines/flow",
            checksum = 0xf321f548,
            """
        package kotlinx.coroutines.flow

        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.launch

        fun <T> Flow<T>.launchIn(
            scope: CoroutineScope
        ) = scope.launch {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2XMsQoCMRAE0BXh4LZzm2usLBQsgt+g5bX2Ipc9CCa7R7JB
        P98IWjkw3ZsBgDUArFp7+Ab3uL2Lzxr8y02aFi3schULid2sSt2Vi42GB6SH
        WgzyYVmrBeFCm3MN0XMut7ZNKg2ecPiHbo76JPzp0ai/aIw8tesdHOENLed0
        U5sAAAA=
        """,
            """
        kotlinx/coroutines/flow/CollectKt＄launchIn＄1.class:
        H4sIAAAAAAAA/61WbVMTVxR+7gYIxEQiVetbJWoqEDBBa6stKYoh1C0xWAJY
        S1t7s1zCwmY33Re03/jcn9JPttOqU2faTD/2R3V67maTIIkGp2Vm757ce+55
        znPelr//+f0PANfhMUztWK6hm08ymmVbnqubwslsGtbjTM4yDKG5i27S4J6p
        balm8moYjOG3QuPK/hvbu9WMbrrCNrmRKXlOTZgbBV4tb/CZprZU2SRDrm6Z
        TmYhkK5lC10cyDXFkmbVxEwXwJxlkuBxaSN7pamwauruzOxMYZvv8ozBzUpm
        qbxNJGivxfRwbofRx3D+za6HMcAwkNUJdJbh0ngPN2cm1hhWemm1yBwuI8RM
        mk2Oq73RoxjEkQj6EWXoc7d0hyH9dmAMR5KaVa0ZQppkSPQCZYjp5q61I4Lg
        Mlwe78zOROdW27XX5mtelL3KPeHyDe5yusA2GSKB2+kdlzYMenR6iCgjb1mV
        Hq1d8um25bTknO7GmWJbsOxKZlu4ZZvrlH9umpbLG7VQ9AyDlw1B8JfepGa5
        UpO0JjpJESGbLuiak1Fb4qIbRoLhZEW4uaXlpdUVtZh/VFot3c8X5/PzDMfH
        uwQtiou4FMEFJBn6yS8hA6BGcRljQ1AwzjAcwC8LxzNcCZJiOOpu2dbjJXOB
        64ZnC4YT3XJE5TOFKxFMIk1lsK/hwphmGFSLpZW5Yi5PGX+lG6O4hg+GcBXX
        GUbbVlWKdIUbJYqQyD/RRE0GKoyPGDIaN4yEayXGbHKyKsYSZbFp2SIx1qik
        scRj3d1KtAI4iJsHPC7JIFaCgv8kghugwIeTts+ZYaRbtQ1otiBX/AbtOO7Z
        Wr3bYPPtzWZTsz0tZw9MPsl4KEK5vtOeXq+5GgaVUf8uNzxiPdCILcOD8f8+
        j7v3c+V/sNwx6bs2wS3cliH4PIoRvCOlAoNSuypbtLcHUvUaw81u+Trc4LrQ
        EySM5Sg+w13p2wrDsSapfcNMqe6G6BPN5DIkF1Ar70ghRIdPdClR2ykbRGus
        vheN1PciSlzxX6eUeH3vjDLNLvYN1vfiSkqZDt3968fbUp2oHcly0zJ/qFqe
        Q58taXelu9OvfgfCoPIYbA5Ghvmu+fTvLNAyc4hYyw6tYIu+RjlrQ8jOtKj5
        17ity5m6IheGoZJeMbnrD6YYzQtt5x6vBWfDBbJV9KplYQc7Z5c9KpeqUM1d
        3dFpa649hmmUHzy9z21eFfRBeUUtqpqmsHMGdxxBP4fzpmZYDs0UStCWRR+x
        SMnybE0s6BLydGB0rQMQ05Tgfoow/d9E+aIxTGs/PdSkdLJNUgYhkoBwqn/y
        OWI/+4neofVkYxtHMSyzT1Icx+jMIFlBhGQqbrosjbiBkfQLvP8g9SsmfvKv
        4Bzaf8cnXyDzCz58+hI3Ho58/BzZP2UxoUprH5RzYZgkHaWdEDmXoCdJT9OT
        BOk0PJHSp5glTyz6NUDv8/SuSQ+pkjBKwjHZgaQmXZv1DQLRl1AeTj1Hro78
        0xbJROOsRTKKBd+0lCRdJaA71wIbDcBiSoDchKR2CiDvBJDx1OTUMyzWoRDt
        Z7h3EDbego2jiCUfNo77+2DVDo4+7KhviLbicwT7BckS9lYAG0tN1lGaIp7P
        sHoQM9bCjBFmg2qMMGf9QvmefkVoT/E1TsH2c7MJPfAnBMd/n6SMA9+SdJzq
        4oRDG++uyxtyOS2XM3I561CNnHPwHtbI+IN1hFR8qeKhiq+wruJrfKOSkUfr
        9A8SvgNfR5+DsgPNwYaDpX8BZeuV7iUMAAA=
        """,
            """
        kotlinx/coroutines/flow/CollectKt.class:
        H4sIAAAAAAAA/41UW08TURD+zrb0snJpKyAgVoEqV9lS7xSbKKZxY0ViGxLD
        g55ul7Lt9qzZS+Wxf8kniSamz/4o4+y2FUEEX2bmzJz55puZs/vj59fvAO4j
        zzDXtFzTEEeKZtmW5xpCd5QD0/qkbFumqWvuKzcKxpBo8DZXTC7qyptqg/xR
        hBhiJveEdqgKhhdLpX8hFUnkz4tuD8yyZn3U88t7DB+2Kpuls7XyhYvBt1Yr
        lXzhf0sslCy7rjR0t2pzQzgKF8JyuWtYZO9Y7o5nmjSXyJZ7aDiFGOIM6R6w
        0mi3FEO4ui24qajCtSnd0JworjBMaIe61uzn73Kbt3S6yLC49Hc7f3jKPkid
        eA1jBKMyhjHGMOT4ZGNIMqxdup7MYAeZjSiu+sQNYbgF6nMwtNPzEGR4QbtB
        1QlMyhjHtdNdHhBkbyLFvpWLYpohcw6d555h1nTbea9ZrZYl/AdznWj0aDEU
        z93dmb2ULq4dML2BtIxZ3GQYzfi7yZw8vvTFb8/f+eUcGJIDGq91l9e4y8kn
        tdoh+laYL+K+AANr+oZEwSPDt7Jk1TYYNrudlNztyFJCCtTUQM2kE93OjJRl
        8+FYt5OQVmKpcEp6KWVDuUgiTIEhHyHHAvAKg9zf7nrTZQhvWzWdYaxETHe8
        VlW3K7xqkidVsjRu7nHb8M99Z7xs1AV3PZvszFuPtt3SVdE2HIPCv9/ls5M3
        zzCsCqHb2yZ3HJ2OctnybE0vGj7cdB9irwfwRx42ICGM3kSmMYQIQrhHp13y
        SqRnV1LyMRKrqZQvv2H8HfuCqS5mjnHrsz8/+gGBsogA4nhA9mQvD3OYD3Bn
        kcQCxR8Gt6N4RHpEIkcsKOrLEB6TlOk0HqRM4UlwOYdN0k/JnyFqt/cRUnFH
        xaKKJSyrWMGqijXc3QdzsA5lHxGHPjtkqS0H8w6SDhZ+AcPOvm8nBQAA
        """
        )

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.*
                import kotlinx.coroutines.flow.*

                @Composable
                fun Test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda = @Composable {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda2: @Composable () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    })
                    LambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }
            """
                ),
                Stubs.Composable,
                coroutineBuildersStub,
                flowStub,
                flowBuildersStub,
                flowCollectStub,
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:10: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:11: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:12: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:17: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:18: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:22: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.async {}
                                   ~~~~~
src/androidx/compose/runtime/foo/test.kt:23: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    CoroutineScope.launch {}
                                   ~~~~~~
src/androidx/compose/runtime/foo/test.kt:24: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                    flowOf(Unit).launchIn(CoroutineScope)
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:33: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:34: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:35: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:38: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:39: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:40: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:46: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:47: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:48: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:52: Error: Calls to async should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.async {}
                                       ~~~~~
src/androidx/compose/runtime/foo/test.kt:53: Error: Calls to launch should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        CoroutineScope.launch {}
                                       ~~~~~~
src/androidx/compose/runtime/foo/test.kt:54: Error: Calls to launchIn should happen inside a LaunchedEffect and not composition [CoroutineCreationDuringComposition]
                        flowOf(Unit).launchIn(CoroutineScope)
                                     ~~~~~~~~
21 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.*
                import kotlinx.coroutines.flow.*

                fun test() {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                val lambda2: () -> Unit = {
                    CoroutineScope.async {}
                    CoroutineScope.launch {}
                    flowOf(Unit).launchIn(CoroutineScope)
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    })
                    lambdaParameter {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }

                    val localLambda2: () -> Unit = {
                        CoroutineScope.async {}
                        CoroutineScope.launch {}
                        flowOf(Unit).launchIn(CoroutineScope)
                    }
                }
            """
                ),
                Stubs.Composable,
                coroutineBuildersStub,
                flowStub,
                flowBuildersStub,
                flowCollectStub,
            )
            .run()
            .expectClean()
    }
}
