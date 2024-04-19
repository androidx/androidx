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

package androidx.compose.ui.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.TestFile

object UiStubs {

    val Density: TestFile = bytecodeStub(
        filename = "Density.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0xcc05f7d8,
        """
            package androidx.compose.ui.unit

            interface Density
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2VMQQoCMRAbUQR7EOkDBMWThzl7F1dkL4J+oGzr7oDOlHYK
        Pt/KejOQEBISAJgCwKRyAT+Yg9k59knIv7GTV5Qc8CGFvVMSxj5kLSlku2ok
        nVw3nMegVXM0m79lISSORTEKsYZk17eSY2BP3F/H6PLtG3pWW0+WZqb10c7v
        VVvdwh4+IfeLY6cAAAA=
        """,
        """
        androidx/compose/ui/unit/Density.class:
        H4sIAAAAAAAA/4VOTUvDQBB9s9GmjV+pWqg38Qe4benNkyBCoCIoeMlpm6yy
        bbor3U2pt/4uD9KzP0qcqHdn4M17M/DefH69fwAYo0c4V7ZcOlOuZeEWr85r
        WRtZWxPkjbbehLcYREhnaqVkpeyLvJ/OdBFiRITuZO5CZay800GVKqgrglis
        IvamBjoNgEBz3q9NowbMyiGht920E9EXiUiZPfe3m5EYUHMcES4m/z3FQeyb
        /KnLeWDx6OploW9NpQlnD7UNZqGfjDfTSl9b64IKxlnf4gzs4LcETn7wGKc8
        h2y5y93KEWWIM7QzdJAwxV6GfRzkII9DHOUQHqlH9xtDUhD7SQEAAA==
        """
    )

    val PointerEvent: TestFile = bytecodeStub(
        filename = "PointerEvent.kt",
        filepath = "androidx/compose/ui/input/pointer",
        checksum = 0x7fd06e9b,
        """
            package androidx.compose.ui.input.pointer

            import androidx.compose.ui.unit.Density

            interface AwaitPointerEventScope : Density

            class PointerId(val value: Long)

            class PointerInputChange(
                val id: PointerId
            )
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2VMQQoCMRAbUQR7EOkDBMWThzl7F1dkL4J+oGzr7oDOlHYK
        Pt/KejOQEBISAJgCwKRyAT+Yg9k59knIv7GTV5Qc8CGFvVMSxj5kLSlku2ok
        nVw3nMegVXM0m79lISSORTEKsYZk17eSY2BP3F/H6PLtG3pWW0+WZqb10c7v
        VVvdwh4+IfeLY6cAAAA=
        """,
        """
        androidx/compose/ui/input/pointer/AwaitPointerEventScope.class:
        H4sIAAAAAAAA/51Qu04CQRQ9d1F5+AAUFTvjBzhALIxWJGqyCUYjiQ3VsDua
        gWVmw9xF7PguC0PtRxlntaGgspgz5965j3Pm6/vjE8AFjgmX0sRTq+O5iOwk
        tU6JTAtt0oxFarVhNRXdN6n58S+4nSnD/cimqggi1EZyJkUizat4GI5UxEUU
        CKfrZmZGs7hRxml+L2KTUO+NLSfaiHvFMpYsrwnBZFbwyiiHcg4g0Njn5zqP
        Wp7FbUJjuShVgmaQn9JLc7noBC3K3zqEq95/Hfn9Z2ubV6X7oupq5/mYCZW+
        zaaRutOJIpw8ZYb1RD1rp4eJ6hpjWbK2xm15idjwlrZyZ54f/mIDR/5u+7z/
        F5QGKIQoh6iE2MaOp9gNsYfqAORQQ32AwGHf4eAHKmYZqccBAAA=
        """,
        """
        androidx/compose/ui/input/pointer/PointerId.class:
        H4sIAAAAAAAA/5VQTW8SQRh+ZpZdlhVkwS+KH9XWQ4vRpY03TaM2mkBQm2q4
        cBrYSZ0Cs4SdJT3yW7x7MNGYeDDEoz/K+M5CPHkxmXnmfd558rwfv35//wHg
        Me4zPBA6nicqvohGyXSWpDLKVKT0LDPRLFHayHl0sn47cRGMITwXCxFNhD6L
        3g7P5cgU4TB4T5VW5oihsNfd7zM4e/v9MlwUAxTgM7gLMckkA+uWEeBSCRxl
        EpsPKmV42PuPJp4w+GfS9Nd+VKfLUOuNEzNROnotjYiFESTi04VDQzILJQug
        6mPKXyjL2hTFBwzHq2U94A0e8HC1DOjwsBRw32msloe8zV5U6l7Im7zt/Pzo
        8bBwWvvLfFI3C74betbqkNkC1U2bLxdSm0djQyMeJzH1We0pLd9k06GcvxfD
        CWXqvWQkJn0xV5ZvksG7JJuP5CtlydZppo2ayr5KFf0+1zoxwqhEpzig/RXy
        kep2nRRxil14hNvEjuDAThm0vqHU2v6Kyudcc5fQagAf9wivU45UuIyq3RBF
        1o0WipDu2ivKvQG39QWVT/+0Ka8FGxuOnRzvYJfeZ3mTLq4M4HRwtYNrHSp7
        g0I0OthCcwCW4iZuDVBMUU1xO0WQo5ciTFH7Azsv3rCuAgAA
        """,
        """
        androidx/compose/ui/input/pointer/PointerInputChange.class:
        H4sIAAAAAAAA/5VSXU8TURA9d7dfLAXaIlJA8QOUUoQthPiCMSrRZJO1EjC8
        8HTbvSm3H3fJ7m3DI7/FX6CJRuODIT76o4xz2w0IvkiymTNnMnNmdub++v39
        B4BtbDBscxVEoQxO3WbYOwlj4falK9VJX7snoVRaRO7eCD0T3D3mqiWyYAyF
        Nh9wt0vcfddoi6bOwmbIPJNK6ucMtYr//9LBzuohw5IfRi23LXQj4lLFLlcq
        1FzLkPx6qOv9bneHwZJBDjmGxU6ou1K57UHPHaoo3nU9pSMqlc04C4dhpnks
        mp2kdo9HvCcokWGl4l+ffuevyIERadFMeeQx4WAckwx2xfA0Cg5SKDKs3+j/
        8shhegwWbjGk9LGMGZ7eQOBy97SBdEtoL2BwK6s3moGh6CdLeys0D7jmZp+9
        gU2vgRkzZgwYWIfip9KwGnnBJsPu+VnJscqWYxXOzxz6hn7OLp+fbVk19mqi
        lClY81bN/vkhYxVS+8ULlqPs+VQuXcgYqS1mGkwlU70eCKU3OpphYb+vtOwJ
        Tw1kLBtd8fLy+rSy3TAQVOZLJer9XkNE7znlMJT8sMm7hzyShifB5etaF6e/
        IuochP2oKd5IUzOX1Bz+0x2bdLbUcC8lc0XCFWIZwiyhRZgmZqFCzCe0CAtr
        pbFvmKp+Ram69gUzn4aZq2QnYVO2A/OspshWKXZ7VEM4aw5A3qjPGvlZljQq
        ooy5pI1rjkSYrn7GzMcL7cwwOD7UzI8SEs2rEz8Z2sdYJ3xB0XnKWziC7eGO
        h7seFnGPXNz38AAPj8BiLGH5CNkYszEexcjFmI6RiVGOMfcHIp5sr08EAAA=
        """
    )

    val PointerInputScope: TestFile = bytecodeStub(
        filename = "SuspendingPointerInputFilter.kt",
        filepath = "androidx/compose/ui/input/pointer",
        checksum = 0xa05e1a0a,
        """
            package androidx.compose.ui.input.pointer
            import androidx.compose.ui.unit.Density
            import androidx.compose.ui.Modifier

            interface PointerInputScope : Density {
                suspend fun <R> awaitPointerEventScope(
                    block: suspend AwaitPointerEventScope.() -> R
                ): R
            }

            fun Modifier.pointerInput(
                key1: Any?,
                block: suspend PointerInputScope.() -> Unit
            ): Modifier = Modifier
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2VMQQoCMRAbUQR7EOkDBMWThzl7F1dkL4J+oGzr7oDOlHYK
        Pt/KejOQEBISAJgCwKRyAT+Yg9k59knIv7GTV5Qc8CGFvVMSxj5kLSlku2ok
        nVw3nMegVXM0m79lISSORTEKsYZk17eSY2BP3F/H6PLtG3pWW0+WZqb10c7v
        VVvdwh4+IfeLY6cAAAA=
        """,
        """
        androidx/compose/ui/input/pointer/PointerInputScope.class:
        H4sIAAAAAAAA/51Ty24TMRS9nsljklKYDlDSFtqSlpdQmRBg04SKClo1VYAq
        idh05UzcyOnEjsae0O7yLSz4CBYo6pIf4S8Qd/JQSxsRxML28fXx9T328Y9f
        374DwEt4SOAFFY1A8saJ68l2RyrmhtzlohNqtyO50CxwD4ZjKQpWPdlhSSAE
        7BbtUtenoul+rLeYp5NgElidlC4UXLvvmFBcnyYhTmCefqZcj/LudJkY5iVQ
        e1w+ltrnwm112+5RKDzNpVDu7gjlC+N1TwYy1Fww5b6VAkFII0LhSflyXQUC
        P4uVzavxrWmHFTfK029ne6KUwsaUQosbtUphq/B0QlnTNI62TlS6XpZB020x
        XQ8oRy1UCKnpUNeH0Pdp3WdIW/sbTeqIiay5cSHvmaYNqinGjHbXRO+QqEtF
        HRAgxxg/4dEsh6jxnIDq97JpI2MMm2We46jZ/d4A9HvWUabfyxs5sv/aNhaN
        PTMbs/o928w/smOLaxZxYo6RSzhpx4rQnplLOgknliE5Kxc/+5IwrNTe2dc3
        EbLT0dF5Aq/+4dGuWBqVZSfuu+hdJJEKgXjdlx5KdsbXc+5OApv/bxn8PdPc
        TaLrXh6zdk50VJgU4wJqp4M0K9VQdZhocNG8KHSX+wifHWsCqSpvCqrDAP/c
        UiXEE9qsJLpccfTH9rkZ0FCXVw9oQNsME/1BS1dlGHgMj8CMC6M9n67kS+Ab
        QQw1JCLjxAgkwQIT7uPMgBRkcUzgahrHNWyzBk5mBh6bGVDWB/0qPMCxjNFr
        EIfZQzBLcL0EN0pgwxxCcEpwE24dAlFwG+YPYUbBHQUZBUkFCwoWFSwpuKvg
        noJlBSsKrN90t5jzEwUAAA==
        """,
        """
        androidx/compose/ui/input/pointer/SuspendingPointerInputFilterKt.class:
        H4sIAAAAAAAA/61VW28bRRT+Zu34VkPcDSmJE9KUmObSpusYym2jiBJRycIN
        BZe85Gm8nroTr2esnd0ofetv4RcgnhAPKOKR38JvQJzZOKnTBAehPuyZc/nm
        3OYc+8+/f/sdwCfYZviKq26kZffYC/RgqI3wEulJNUxib6ilikXktRMzFKor
        Ve/pqaZpzY9lSOy3cR6MoXLIj7gXctXzvuscioC0GYbycAzPMFxrXRXsie7K
        51JEfutNJ36rr+NQKu/waOA9T1QQS62M93jENfz1yQ4Z/nrbIbc3r3R4sWHj
        bWoHeij8zTO3gY50EksljLerFTEJt363zwE/Khn7O/69y5ntXF/uSktHPe9Q
        xJ2IS0qbK6VjflrCno73kjAkVG0SiiC8EwqC5bbjF9LsFFBiWBrrSlqb4qHX
        VHFE92Vg8igzzAYvRNAfhXnKIz4QBGRYXbuiy681beuk56/vl/Eupkt4BxWG
        qU6og34BLsPipJrzeI+huEsGrih/hsnvXTtH+mXcwvtFzGKOwa3ZSmsXp3Xp
        umZn++LlFl2+XB3D8nWTy3DzDPJExLzLY046Z3CUocVklhQtAQPrW8Yh47G0
        XJ24LsU1J69qpZNXJWfOSY8KkZF4/lWckbn6NZmrTp1t0NcoVJxqdo7VM43V
        Sra6UmBu1nXqObfkFlKuUM+7OTdF1Kf++CnnFIqWVko2dIPZrNyz7MdLevi/
        dmOsW/+yHb59jRHkm+NY0MxpdRb42cvUx93/9u5527/bk37RHvTp8bO7uisY
        pluUy14y6Ijomd0KW7cOeLjPI2nlkbLYlj3F4yQifuGHhBIfiKY6kkaS+dHr
        7aLVe9N6vicXYOWmUiLaDbkxgsRSWydRICg/CjA/crF/yT224CCL07GZxxRy
        JH1M0vekt6Mzs+He+BU377szRH/B/Almf7azRf8EIDDFxTQeEr98CkcVC6m7
        GSziA7JbzsUS3fg0vZfHZ6ObBTo/t/bMSKCupNTyGXxBtETSFi3cLcrty/R6
        Az6dLdLfpmyXD5Bp4k4THzaxgloTH+FuE6tYOwAzWMfGAW4YTBksGCwa3DNw
        De4bbBo8SEXPIPcPJkHsLNsGAAA=
        """
    )

    val Alignment: TestFile = bytecodeStub(
        filename = "Alignment.kt",
        filepath = "androidx/compose/ui",
        checksum = 0x72950571,
        """
            package androidx.compose.ui
            class Alignment {
                companion object {
                    val TopStart = Alignment()
                }
            }
            """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2VMQQoCMRAbUQR7EOkDBMWThzl7F1dkL4J+oGzr7oDOlHYK
        Pt/KejOQEBISAJgCwKRyAT+Yg9k59knIv7GTV5Qc8CGFvVMSxj5kLSlku2ok
        nVw3nMegVXM0m79lISSORTEKsYZk17eSY2BP3F/H6PLtG3pWW0+WZqb10c7v
        VVvdwh4+IfeLY6cAAAA=
        """,
        """
        androidx/compose/ui/Alignment＄Companion.class:
        H4sIAAAAAAAA/5WTz08TQRTHv7PdtstSpeWH8kNFpEqL2gXiDWOEGpMmBRMg
        vXAw0+1Yp93Okp1pw7En/xD/AjlpPBjC0T/K+LasQDQRvbx57/ve583svJ3v
        P75+A/AMFYYVrlpRKFvHnh/2jkItvL70tgLZVj2hTLFKIlcyVFkwhnyHD7gX
        cNX23jQ7wjdZpBgyz6WS5gVDqlRu5JBGxoWNLINt3kvNUK7/4x6bDONtYQ7C
        o33DI8OwVLqGJWK5HkZtryNMM+JSaY8rFRpuqJ32dkOz2w8Cqrr71zZZ3GSY
        4b4vtC5eOUHRP8ohj5yLCRQY1kv1bmgCqbzOoOdJZUSkeOC9Eu94PzBV2tFE
        fd+E0Q6PuiLaLDdcWPFFTBX9y+Tb3ijLUPm/bgyFX8COMLzFDSfN6g1SNEsW
        m7HYgIF1ST+WcbRGXmudYft0OO1as5Zr5U+HruVY54FjOWcfUrOnww1rjW1n
        HevsY8bKW3uFfGreWrMpctzT4bztpPOZuNMGi/s7lxNavHY+YxfjZchdJCpd
        gu1q2BIME3WpxG6/1xTRAW8GpEzWQ58HDR7JOE7Ehb2+MrInamogtSRp63LS
        1LqmlIiqAddaUOjuh/3IF69lTM4lZOMPDus0IDu+NaTIo1+XPm+FIi++RlrT
        q5/hnJBjoUQ2MxJtlMnmzgswBpfWAsZJsUZwJYHtL5j89BubvsLaCbuaZG8A
        eUYVU8khntJqJYeYPhmNNoZvnYsJHHszpNHzw2OK3BE0gYeYw5PR5o+oEfCS
        9NtUO3uIVA1zNczXsIA75OJuDfeweAimcR9Lh8hquBoPNDIayxpFjXGN3E/q
        H9VdNgQAAA==
        """,
        """
        androidx/compose/ui/Alignment.class:
        H4sIAAAAAAAA/4VSXU8TURA9d7ct7bJKqVIpHwJSpaCyQExMhJhgjUmTUhMh
        JISn2+213nZ7l+y9bXjkt/gLRB5IJDHER3+UcbYU8COBl5mdc+fMnJnZn7++
        fQfwAisM01w1olA2Dj0/7ByEWnhd6W0Gsqk6QpkhMIZsi/e4F3DV9N7XW8In
        1GZIbUglzWsGu7S46yKJlIMEhhgS5pPUDDPVGyuvM4xx3xdaF5vC7IQH24ZH
        pugfMMyVFm/lpi8ZLu7AycDCXQI3/GAgauHGAsUygVzJUA1hlGG1VG2Hhqhe
        q9fxpDIiUjzw3oqPvBuYcqi0ibq+CaMtHrVFtH4x7z0HOdxnyFwVY7hF+HXf
        dRd5PIh1jztkaG3z1TBqei1h6hGXSntcqdBwQ7naq4Wm1g0Cmnv0UumWMLzB
        DSfM6vRsOieLTSY2YGBtwg9lHNGRrcYqQ/H8yHWsccuxsudHjpW2xs+PZu01
        a4W9Yvab5I/PKStrxblrLK7gXqlebhuGyQ9dZWRHVFRPalkPxOa1Prp5OWwI
        hpGqVKLW7dRFtMMphyFXDX0e7PJIxvEAdCtKiagccK0FkZ3tsBv54p2M3wqD
        Prv/dcEqLSpBA1koxMsjjYsUpchPkp+If4R/MJt8sh8tUeTFeyGfXDpF+rhf
        6OkgOU59Rta9SECGSiFbwHAficlT/Rcg8RUjX/or/pObRvaqzTL6N0D+DLk9
        doqxExTOYO2dYuIEI8d/cYepl43nFMXSc6QoT8Mt97WVSDDwkvApypreh13B
        wwpmKpjFHH3iUQXzKO6DaTzGk30kNByNBY2URv43qNs4vuQDAAA=
        """
    )

    /**
     * Simplified Modifier.composed stub
     */
    val composed = bytecodeStub(
        filename = "ComposedModifier.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xc6ba0d09,
        """
            package androidx.compose.ui

            import androidx.compose.runtime.Composable

            fun Modifier.composed(
                inspectorInfo: () -> Unit = {},
                factory: @Composable Modifier.() -> Modifier
            ): Modifier = this
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIKSs1NzU1KLfIu4dLkEsZQV5opJOQMYaf45qdkpmWClfJxsZSk
        FpcIsYUASe8SJQYtBgBxwST5ewAAAA==
        """,
        """
        androidx/compose/ui/ComposedModifierKt＄composed＄1.class:
        H4sIAAAAAAAA/5VU6U4TURT+7rR0GYoti7KIO2ILyrTg3oaENBAnFEwEmxh+
        3XYGuHR6x8xMG/zHK/gKPoFoIokmhvjThzKeO20NbqCTzJ2Tc77v7He+fvv4
        GcBdPGYocGl5rrD2jbrbfOn6ttESRrkjWmuuJbaF7a0GU12rNVWIgzGsVhpu
        4Ahp7LWbhpCB7UnuGBXerFm8eNK23ZL1QLjSN1a6Ur7Usz+XIiguFhkm/u4s
        jijD5dMdxhFjiJUEuVtkiGRzVYZo1sxVU0hA19GHflIEu8JnWKj8d8GUYEzI
        ttuwGUayucoeb3PD4XLHeFrbs+tBMYU0kjo0DDL0n6gtjmGGhLm+sbm0Xl5m
        GPip8BTO40ISIxglUKnuhOmrjENXE8p8LknSJMNgj7hmB9ziAaeUtGY7QkNk
        6kiqAwysoYQIGfeFkvIkWQWGyeODhH58oGsZjT6Z44MJLc+e6F/exLSEpjDz
        lHiJS1e+arotn5pIzqb/rVFx3GbI/OiWZW/zlhMwvM7+sdM94llLcoa9UDR/
        n0Pu9IgpzMFgGP61hrkGpRstuxbNd6ji1rlT5Z7gNcfeVAdDuiKkvd5q1myv
        q0mZUtpe2eG+b9NSpZdl3XF9IXdoQLuuxZDcEDuSBy2PwPqG2/Lq9opQzPFn
        LRmIpl0VviBXS1K6AQ9rQ56G3UeNpwuGcTV9mmCUXtoI0syTNEUImg1iM5Ej
        pA7DmS/QmepoMRByBtU6dhmzIYZeBdboyiuYUsRPEFmHmFkiYqZLnFfrpILP
        fMDQO4y9PYWf6AZOUNq9wKOEVk//J2gvjnDxPS4dhoo+3KNTJ1gHMIb7YZ13
        qP4HYZAIHobfAh6Ffym6/8S6soWIiasmrpm4jhsmNeOmiWnc2gLzkUWO7D5m
        fMz6SH8Hu1pp5uIEAAA=
        """,
        """
        androidx/compose/ui/ComposedModifierKt.class:
        H4sIAAAAAAAA/7VUUU8bRxD+9mzss2OIsUlCHEJo4yRgSM4maZvWhAShIJ1q
        3CqmvPC0+NZk8XkP3Z0ReYn4C33sa39B1aeoDxXqY6X+paqz53MggHClqjrd
        7MzOzLffzs7un3//9juAZ1hjeMiV43vSObLaXu/AC4TVl9b6QHU2PUd2pPC/
        DdNgDPl9fsgtl6s967vdfdGm2QSDGSc6DO/nG5fBDWHqja4XulJZ+4c9q9NX
        7VB6KrA2Yq06wl+rL1wNz/DXfyOwMvT/oGRYXx3FZ+Xx1astXu1eHb2f+w3P
        37P2Rbjrc0lLc6W8kA9oNL2w2XddikqthG9lsGoiwzB7hrJUofAVdy1bhT6l
        y3aQxjWGG+23ot2N87/nPu8JCmR4NN84f8L1MzMtDbJXX9jOYRwTWeRwnWGc
        cA8o0PNt1fFMTDKkO1zb70wUGSbKmlv5tEdmR+15blSXjAypUUh+uGLZER3e
        d0OGH//n7rQvVm/kAdf+3fX7WL9yLY07dOfsZmtrrbn+muHppUtcCVHP4S5m
        M5jBvU8b5pJdp/FZDmNIZWHgPsPksAibIuQODzntwegdJug5YVpktAAD62rF
        IOeR1FqVNKfGwE+OZ7Mnx1lj2hgORv5UHfyl5/mT45JRZRX6lydMiiiZhWTB
        qCarieWZ/FhpOrLYQFZTf/ycMsx0JE290DJDYUj0bN/gknndLA8u1tDvq1D2
        RFxIvuuKuu7dOPn1USjoPnlqiLL17kAHFM/X/UmXGi+57jmC4XpDKtHs93aF
        v6UBNRmvzd1t7kttx5OZltxTPOz7pN95M6Bhq0MZSHKvnT4ADOXz3o93+ZOw
        8VbI291NfhAvkLOVEv66y4NAkDvb8vp+W2xI7bsdQ25fWA416oCkPl0ab+uW
        IOsrst6QrY94qlLIfkB+sVAguVSYIln5JYp+TjKla48MviZ9bhCPG7gZ4U1h
        ErfIr7UipinjmygvjXqcadK4Qn8xERtnZD5DdEqkazIvCHpMA91Nvv8J2V8x
        d4LPG5XFpQ8oD8i8IEko4xGriYhJir40vWkpslbJzhLYTMRsGi+jpC/xisYN
        mn9A8A93kLDxyMa8jQVUbCxiycZjPNkBC2ChuoNMgLEANwNMUuECLAcoBnga
        4FmAL/4BKp/c6X8HAAA=
        """
    )
}
