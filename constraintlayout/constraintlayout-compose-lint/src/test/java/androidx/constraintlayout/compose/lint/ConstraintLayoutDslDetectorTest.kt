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

package androidx.constraintlayout.compose.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val COMPOSE_CONSTRAINTLAYOUT_FILE_PATH = "androidx/constraintlayout/compose"

@RunWith(JUnit4::class)
class ConstraintLayoutDslDetectorTest : LintDetectorTest() {
    override fun getDetector() = ConstraintLayoutDslDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ConstraintLayoutDslDetector.IncorrectReferencesDeclarationIssue)

    private val ConstraintSetScopeStub = bytecodeStub(
        filename = "ConstraintSetScope.kt",
        filepath = COMPOSE_CONSTRAINTLAYOUT_FILE_PATH,
        checksum = 0x912b8878,
        source = """
            package androidx.constraintlayout.compose
    
            class ConstraintSetScope {
                private var generatedCount = 0
                private fun nextId() = "androidx.constraintlayout.id" + generatedCount++

                fun createRefsFor(vararg ids: Any): ConstrainedLayoutReferences =
                    ConstrainedLayoutReferences(arrayOf(*ids))
                
                inner class ConstrainedLayoutReferences internal constructor(
                    private val ids: Array<Any>
                ) {
                    operator fun component1(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(0) { nextId() })
                    operator fun component2(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(1) { nextId() })
                    operator fun component3(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(2) { nextId() })
                    operator fun component4(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(3) { nextId() })
                    operator fun component5(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(4) { nextId() })
                    operator fun component6(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(5) { nextId() })
                    operator fun component7(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(6) { nextId() })
                    operator fun component8(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(7) { nextId() })
                    operator fun component9(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(8) { nextId() })
                    operator fun component10(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(9) { nextId() })
                    operator fun component11(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(10) { nextId() })
                    operator fun component12(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(11) { nextId() })
                    operator fun component13(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(12) { nextId() })
                    operator fun component14(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(13) { nextId() })
                    operator fun component15(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(14) { nextId() })
                    operator fun component16(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(15) { nextId() })
                }
            }

            class ConstrainedLayoutReference(val id: Any)
        """.trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJ2KM3AJcjFnlqRmFuQkyrEFpJaXOJdosSg
                xQAASc3A6SsAAAA=
                """,
        """
                androidx/constraintlayout/compose/ConstrainedLayoutReference.class:
                H4sIAAAAAAAA/6VRXU8TURA9d/u1rEW2lUoBxQ9QSlEXiG8giWJMmhQ01PSl
                T7e713Lb7S7ZvW3wrb/FX6CJRuODaXz0RxnnlhUFfPNlzpy5M2fmzvz4+fUb
                gMd4yLDDAy8KpXfiuGEQq4jLQPn8bThQFOgfh7Fw9n4/CK8+eTkUb0QkAlfk
                wBjsLh9yx+dBx3nZ7gpX5ZBiyO7IQKpdhlKlfjFhe63JsFwPo47TFaqtpWOH
                B0GouJLUzDkI1cHA97cZDOmZMBmWeqHyZeB0h32HJhRRwH2nFqiISqUb52BR
                J/dIuL2k9hWPeF9QIsPqPyb4K9LQIh2aKY88pi1cwVWGVEXzDGwLaRQYipcl
                8jBxbQoGZhnS6kjGDLv1/9kmfTfTEarmMcxW1i43ZCjUkzXsC8U9rrjeUH+Y
                omMybXIMrEehE6nZBnneJp14PMpbRtmwDHs8sgwzXR6PtowN9my6mLWNBfK+
                v8saduqwcMZMazxaSJtpO6M1thjpo3Q2uWoI1XDDY/GopxgWDweBkn1RC4Yy
                lm1fPP1zSdrMXugJhpk6ffhg0G+L6DWnHL3R0OV+k0dS8yS4clHr7IznRK1G
                OIhc8ULqmvmkpnmpOzbpOmm9HBT1sQjvE8sS5ggNwgwxA6vEnhMahPZ6ceoL
                ZqqfUayuf0LpwySzktRlsYE18q+f5hLOARPvVL+aXGHSoIAy5hN5BzoKZKof
                UXp/ThOJZv40IdE8P+n6xN7DA8InFF2gvMUWUjXcqOEmWSxpc6uG27jTAotx
                F8stZGPMxViJYcbaL8eY/wVNn9c9/QMAAA==
                """,
        """
                androidx/constraintlayout/compose/ConstraintSetScope＄ConstrainedLayoutReferences.class:
                H4sIAAAAAAAA/62Y3VLbRhTH/ysbfwgDxjGO4xjqEIcYY2JsDCGE0pAEGoMh
                FKc0Kf0StkIERs54RYbcMb3IG7QP0F70tp1pJpl2ppPhsi/Qt8n0yCggAco4
                RAPePbva/Z/fnj2yVv737V//AChgi2FZUquNulLdzVbqKtcakqJqNel5fUej
                ju2ndS5n7xxeKMtauVJ/KicPu+RqqTl4RX4sN2S1InMvGENwU3omZWuSupG9
                v74pVzQvXAyeKUVVtGmGUqp0Frc310rHdW8OrjJEUjYXLpfqjY3spqyt6zo8
                K6lqXZM0hZSzS3VtaadWu8ngUqrcBz9D31ZdqylqdvPZdpbcyg1VqmWLqtag
                uUqFVtbO0FN5Ile2jMnLUkPalmkgw9XUSQRTT1kX2SCoADrQKSKALoqH9kTh
                yRGG8bOFI4BuhPwQcI4WkdK129Ajwo0Iw7lTQhKAH1F9/AUGt+6bYeVMnt+3
                /xRQsTlTlVUtx3ArNfghPk4Ikt5FY18q9VqN1tHcvplGQ3rOFyixPmEIbMha
                SeJaUa3Ku3b5UAzgEvpFJHCZoXCWZXtxhaFDqtAqeVKVd7VilWH+jLk8eDI7
                AriKlIgBDDJMfUzMvBiiTD0lIw9yZFhEBtcYFpJKUkoOU/DuN2ZrXB7+sJ1O
                Hm1zkjaaFRkEhTYkas6AvINu8rqbI+lRB6VHrdIFB6ULVukxB6XHrNLjDkqP
                W6WvOyh93So94aD0hFX6hoPSN3Tp9qOUp6/tkmP3z8gx8ZyT4rlj4nknxfPH
                xEedFB89Jl5wUrxwTHzMSfGxY+LjToo3783ukvFUXJQ1qSppEj0ohe1nLjrY
                Mb3w0jcyne+EXUVvUbYK1RxjvW/2YqIQFUQh+GZPpH8h2C0KPtdBn6/Lt//C
                HX2zlxdG2O0e35u9UCAoxHwhd4g6Rlz7v3iEoHveH/TEhBHvvf0XAtk+k+03
                2aLJbjfZAZPdYbI7TXaXyQ6a7G6THTLZ50x22GT3mOzIO3slYl7Tw/0f3bQu
                N0WiTQ8S3R1gmP7oM8t7tpKezSf3/tqWRrNWdlRN2ZaL6jOFK+s1eeboxEpn
                tjv1qszQVSLRpZ3tdbnxQKIxDKFSvSLVVqWGoreNTn9Z2VAlbadBdvK47uHJ
                1eKgo6xJla1F6akhESiqqty4U5M416nFcn2nUZHnFP1a+KBxV17f2Zjd1WQ6
                ItdVhguGp9UT/MjRydNNuSggpB9EKco/UMtDtQ8IBvXDLLW7qd1GvS5I1Fqj
                0Xr+9mRC4msE00MvEU6/wvl05iVifzTF1qnspEEeiOhCO8JUV6gvcTARFxEH
                mpbugDUt3b2AanO+F7Jxu+gcNKgXfdSte/+JJLxU59J/IlZyfUpVeCk+9BrJ
                X+Efiud/Q2d83O0abxt+jTQmPX8j8+iC5xWyv9MkFx5TGYTwFjEvRpkXA/Ne
                CoDOG6El6tS9FJQE1Un6XKHPO+4EjRuha200wos8WXrgcsZaTGEj3IINrttp
                3KCBGyTcIOEGLbhjLeKO2+C2OY0bMnBDhBsi3JAF93qLuBM2uB6nccMGbphw
                w4QbtuDeaBF30gbX6zRuxMCNEG6EcCMW3Jst4k7Z4Pqcxo0auFHCjRJu1IL7
                aYu404e4Pxu4+SZu8OzZcPF03hh5ytH7aoxYY/RGGDvkvUTjPmvy9pl486fy
                3rLjPXs62PDGDd448caJN27hnWmR97Yd79nzwYa3z+DtI94+4u2z8N5pkfeu
                Ha/fad6EwZsg3gTxJiy8sy3yztnxik7z9hu8/cTbT7z9Ft7PW+S9Z8fb7jRv
                0uBNEm+SeJMW3mKLvPN2vAGneQcM3gHiHWj+mXkXWuQt2fF2OM2bMnhTxJsi
                2pSFd7FF3iU73k6nedMGb5p408SbtvDeb5F32Y63y2nejMGbId4M8WYsvF+0
                xOvGBpUitQRS+I74n0A/IH8Pher/yoszy+Kp7y3iQvNNVEyXE++sOXEokUuc
                PtrpX5zJVT7xWNpqip/2E/4cXVvQxHRJzF3OZQoTkzky8pOFG2J6VsQmLe4p
                LXqFQlReg6uIB0V8SSVW9eKrIh7i0RoYx9dYW8N5jjjHNxz+ZunhiHL0cnzL
                cZdjjuMexzxHiWOJY5mjwDHOMcExyTHFMc1xi+P2/ypvybWDGQAA
                """,
        """
                androidx/constraintlayout/compose/ConstraintSetScope.class:
                H4sIAAAAAAAA/7VUXU8bRxQ9s/5kMY5xAgGcpiTQxDaEdShN20DTglvKUgeo
                qVAjXjrsTpyF9S7aWSN4Q+0v6F/oL2ilFqJGilAe+6Oq3jELKcbKA2pf7tw5
                984583Hv/PX3n68BzKDOMMM9O/Ade9+wfE+GAXe80OUHfiskoLnrS2FUzwPr
                Ily3/F2RAmPIbfM9brjcaxirW9vCClOIMSTnHM8JnzDEiqWNDBJI6ogjxRAP
                XziS4VHtKoKzROyJ/dC0GW4US7W30uth4HgNit/sxBZajmuLIIU+HVm1g1tn
                ylOdylOOnUaONPjurvBI40HxssRl1UhhNoM8riuRGwzZhvBEwENhV/2WFzIw
                M4NB3OyBhiHaQ9F8N8+I4ikwpEP/NJjBe8go8DZDnxUIoq6L53LRDxgaxc1a
                5yvQPq9yw+PnkLBr7WRSEYHwLCHpdsdqftAwtkW4pVKkwT3PD3no0CJjxQ9X
                Wq5LWTHHlmmMMdze8UPX8YztvaZBIiLwuGuYnjqQdCyZwgcMA9YLYe1Ei9d4
                wJuCEhnuFy+fqctrUHHdR1HHPZQY1v7rI6cwcVbhrdBxjfkg4AeEPqAiobUH
                q88ZSt1u3yx1ATMwUNExhYcMteLVOqDbS7cb7EMdk5hhuN4lg0qGW3QcOX7W
                PctXlO/Scxn0qsrU8ClDYvy0vftr0cs/FSG3echpC1pzL0bfDVOG2pDtELTv
                qFmFPJvu5KeTw3u6NqTpWu7kUNfSyknryiVMDbn+CM3SdOjkcFqrsMcss5B4
                80tSy2nLY7nESLKSrGsViuVzqRE9n0yzPGVV0nfJttN6ltM5fUSr9C5p9Wwu
                Rl78+zc/ZlWMWNVWphltE/X/o4EK7whTL1zmm9qhzyNe9W3BcK1Gy1ZazS0R
                fMe3XELyNd/i7gYPHDWPwEKdfhynKUxvz5EOQfNvu5RhvDN63nMX0jKmRx9Y
                1eVSqp3p634rsMSiowSGI4qNS/R4SHUQV69MI3135MXJp++f7DLNDBrpdpEo
                HyP9GzkaviGbbIMx1MhmThPQA53GvKquaPEykWk0jr5C9tkxruX7jzBQ/h3D
                c+VC/Ic/MFw4wq0jvP9rB2/iX7yjEe/P5N0hRcVrUobiHZjIj79E+RUmn5Un
                Jl6/xPQxPrpIlkS6TTZ4uiAiU94YHlH8aZR3l8aVqNDVJDeMj/FJl0t4fJGf
                dVzCbJs/hlWyOmGTlLuEfqy1V5n4lsY64XOU+9kmYiaemPicLL5QZt7EAqqb
                YBJf4qtN9EnoEosSSYnBtjMq8bXEWNu/I9Hbdpb+AeegSxAbCAAA
                """
    )

    private val MotionSceneScopeStub = bytecodeStub(
        filename = "MotionSceneScope.kt",
        filepath = COMPOSE_CONSTRAINTLAYOUT_FILE_PATH,
        checksum = 0xc89561d0,
        source = """
            package androidx.constraintlayout.compose

            import androidx.constraintlayout.compose.ConstrainedLayoutReference

            private const val UNDEFINED_NAME_PREFIX = "androidx.constraintlayout"

            class MotionSceneScope {
                /**
                 * Count of generated ConstraintSet & Transition names.
                 */
                private var generatedCount = 0

                /**
                 * Count of generated ConstraintLayoutReference IDs.
                 */
                private var generatedIdCount = 0

                private fun nextId() = UNDEFINED_NAME_PREFIX + "id" + generatedIdCount++

                fun createRefsFor(vararg ids: Any): ConstrainedLayoutReferences =
                    ConstrainedLayoutReferences(arrayOf(*ids))

                inner class ConstrainedLayoutReferences internal constructor(
                    private val ids: Array<Any>
                ) {
                    operator fun component1(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(0) { nextId() })
                    operator fun component2(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(1) { nextId() })
                    operator fun component3(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(2) { nextId() })
                    operator fun component4(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(3) { nextId() })
                    operator fun component5(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(4) { nextId() })
                    operator fun component6(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(5) { nextId() })
                    operator fun component7(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(6) { nextId() })
                    operator fun component8(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(7) { nextId() })
                    operator fun component9(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(8) { nextId() })
                    operator fun component10(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(9) { nextId() })
                    operator fun component11(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(10) { nextId() })
                    operator fun component12(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(11) { nextId() })
                    operator fun component13(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(12) { nextId() })
                    operator fun component14(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(13) { nextId() })
                    operator fun component15(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(14) { nextId() })
                    operator fun component16(): ConstrainedLayoutReference =
                        ConstrainedLayoutReference(ids.getOrElse(15) { nextId() })
                }
            }
        """.trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJ2KM3AZc6lmJiXUpSfmVKhl5yfV1xSlJiZ
                V5KTWJlfWgIUyC3IL04VEvLNL8nMzwtOTs1LDU7OL0j1LuES5GJPrUjMLchJ
                FWILSS0u8S5RYtBiAACRUOp0ZAAAAA==
                """,
        """
                androidx/constraintlayout/compose/MotionSceneScope＄ConstrainedLayoutReferences.class:
                H4sIAAAAAAAA/62YW1PbRhTH/ysbX4QBQ4AQLgGKCRfjGN+4l4YQKAZDKE5p
                UnoTtkIERma8giFvTB/yDdoP0D70tZ1pJpl2psPw2Ld+oUyPjAiSgzIO0YBX
                Z9dn/+e3Z4/slf9989c/AJLYY1iT1HypqOSPo7miyrWSpKhaQXpePNRoYP+g
                yOXoalFTimo2J6tyNlc8kEPzF55yPlN23ZCfyiVZzcncC8YQ3JWOpGhBUnei
                D7d35ZzmhYvBM6OoijbLsDyY+fCg01uZStXpoU2G1kGbN/oyxdJOdFfWtnV5
                HpVUtahJuiiPrhW1tcNCYZrBpeS5D36G23tFraCo0d2j/SjRyCVVKkTTqlai
                uUqO1lXL0JJ7Juf2jMnrUknal8mRYWDwXQTTSFYX2SGoAOpQLyKABsqG9kzh
                oVGG5HWSEUAjmvwQcIOWMKgr16BFhButDDeuSEgAfrTp/rcY3HpkhvVrxH3f
                zlMyxfI8VVa1GMO9waEqItgLkl6HsSe5YqFAqyhv3VypJD3nK1RS3QyBHVnL
                SFxLq3n52K4W0gH04hMRPehjiH/4or3oZ6iTcrRGHlLlYy2dZ1i6Vg0PvVsV
                AQxgUMQdDDHMfEy+vAhThV5RiefVERExgrsM6ZASkkIRStzD0kKBy5EP2ePQ
                5QaHaItZmkFQaCvazHsfdyxIXA9yKZxwTDhhFU46Jpy0CqccE05ZhcccEx6z
                Co87JjxuFZ5wTHjCKjzpmPCkLlx7WeL04bzs0N0yWiEdc046ViEdd046XiGd
                cE46USGddE46WSGdck46VSE95px0+T5szBjfeKuyJuUlTaIvQWH/yEWHNaY3
                XvrMpTObcKzoPapPIR9jrOv0pF0U2gRRCJ6eiPQvBBtFwec6H/M1+M5euNtO
                T+LCKLvf4js9aQoEhXZfk7uJBkZdZ794hKB72R/0tAuj3qWzFwLZPpPtN9mi
                ya412QGTXWey6012g8kOmuxGk91ksm+Y7GaT3WKyWy/sjVbzmh6f/eimdbkp
                EzV6kuieAMPsR59H3rOVdO6q3Pm7exrN2ThUNWVfTqtHCle2C/Lc5TmUzmLz
                xbzM0JAhybXD/W259EgiH4amTDEnFTalkqL3jUF/VtlRJe2wRHaoUvftedQS
                oC6rSbm9VenAkAikVVUuzRckznVmMVs8LOXkRUV/r/m880DePtxZONZkOvgW
                VYZbRqTNd/gRoxOlmypRQJN+wKQc/0A9D119QDCoH1Kp30j9Ghp1QaLeFnnr
                1dsy0iS+RnA4/BLNw69wc3jkJdr/KIttU1tPTh7cRAPJNtM1R2M95xPRgU6g
                bOkBWNnSwwvIl+d7IRs3i85BTl24TcN69J9IwkvX2PCfaM+4PqVL81pn+DVC
                v8If7oz/hvrOMbdrrCbyGsOY8vyNkSe3PK8Q/Z0mufCU2iCEN2j3IsG8uLPs
                pQTovK20RBBZF2aIswMhevXT64K7h/xGKWE15OFFnCw9cTFjLaa0EW7SBtft
                NG6XgdtFuF2E22XBTVWJO2aDW+M0breB20243YTbbcEdrxJ3wgbX4zRur4Hb
                S7i9hNtrwZ2sEnfKBtfrNG6fgdtHuH2E22fBna4Sd8YG1+c0br+B20+4/eU/
                M+6nVeLOvsX92cCNl3GD16+Gjqt5ByjSDNXAAJEO0PPewFveXvL7rMx728Qb
                v5L3nh3v9cvBhnfI4B0i3iH9+dTCO1cl73073uvXgw1v2OANE2+YeMMW3vkq
                eR/Y8fqd5o0YvBHijRBvxMK7UCXvoh2v6DRv1OCNEm+UeKMW3s+r5F2y4611
                mjdm8MaIN0a8MQtvukreZTvegNO8CYM3QbwJ4k1YeFeq5M3Y8dY5zZsyeFPE
                myLelIV3tUreNTveeqd5xw3eceIdJ95xC+/DKnnX7XgbnOadNHgniXeSeCct
                vF9UxevGDrUi9QRS+I74n0E/IH8Pha7/ZVfn1sUrnlrElfJTqDic7bmwFsVw
                T6znKl9nf0WmMPGep9JeWfqqn+QX6b0VTRzOiLG+2EhqcipGRnxqbFQcXhCx
                S8s6oOVuUHKyW3Cl8SiNL6nFpt58lcZjPNkC4/gaW1u4ydHJ8Q2Hv9x6ONo4
                uji+5XjAscixxLHMkeFY41jnSHKMcUxwTHHMcMxy3OO4/z+8pxUSTxkAAA==
                """,
        """
                androidx/constraintlayout/compose/MotionSceneScope.class:
                H4sIAAAAAAAA/61VW08bRxT+Zm1ssxjHOOHqNCWBNuYS1qH0FmhScEpZ11wK
                LWrES4fdCVlY76KdNYI31H/Qp773F7RSC1EjRSiP/VFVzqyXpBgrUqLI0pkz
                35z5vpkz56z//e+f5wBmsMEwzT078B370LB8T4YBd7zQ5Ud+IySgvu9LYSz7
                oeN7G5bwxIbl74s0GEN+lx9ww+XejrG6vSusMI0EQ2rO8ZzwPkOiNLaZRQdS
                OpJIMyTDJ45kmKm9vdws0XriMDRthmulsdpr4Y0wcLwdWu9vxRYajmuLII1u
                HTmlXzzXnWrVdewM8iTB9/eFRxJ3SpcVLovGArNZFHBVaVyjlOzQkQMeCtu0
                K37DCxmYmUUf+juhYYDhesl8M9OQYioyZEK/uZjFB8gq8AZDtxUIIl8Xj+Wi
                HzCI0lat9RHopG+f4NHKeaSwa1EoaYhAeJaQlNyRmh/sGLsi3FYh0uCe54dc
                UUhjxQ9XGq5LUQnHlhmMMNzY80PX8Yzdg7pB2iLwuGuYnrqOdCyZxkcMvdYT
                Ye3Fm9d4wOuCAhluly7fqM1rUGXdRknHxxhjWHm/F05j4ry4qTZcYz4I+BGh
                d6hEaO/R6mOGsXaZN8fagFkYKOuYwl2Gauldir/dG0ed9YmOScwwXG0TQcXC
                LbqMHD1vnKV3Em/TbFl0qYrU8CVDx2izq3OvCj8u+55aXATLIuQ2DzmdSKsf
                JOijw5ShhmR7BB06alYmz6YE/XZ2XNK1AU3X8mfHupZRTqY5EqigfE8M52g6
                cHY8rZXZPda90PHi95SW16oj+dRQqpxe18q0VshnhvRCKsMKFFXuvEU2CtOr
                mXzXkFbOLmnruXyCvORPL37JqTViJaijBVLHm2Z0dKy9/+4qvmGZHreVbWqP
                0pus+LZguFKjTSuN+rYIfuDbLiGFmm9xd5MHjprHYHGd3sSpC9M7cKRD0Pzr
                BmYYbV191Y4XwrKmR09ccbmU6lz6ht8ILLHoKIHBmGLzEj3uUqEk6Yk1+s2h
                P/L6ojFJ6aS/B7Lf0cygkTKMjvFTZP6MwmpkUxGYxjLZbDMAndBpLKgyjDdX
                iUyjcfgZco9OcaXQc4Le8b8wODdeTP78NwaLJ7h+gg//aOEt/I93OOb9lbyb
                SES8Jqkp3t6JwuhTjD/D5KPxiYnnTzF9ik8vkqXQG5H1NTfEZMobwWe0vhLH
                3aJxNW4ANckP4nN80SYJ9y7ys5YkzEb8CayR1QmbpFgTPfg+2lXFOo0/Ev4V
                xd7fQsLEAxNfk8W8MgsmKni4BSbxDRa3kJPQJb6VSEnMRU6fxLDEksRINL0p
                0RU55kuxHmQmPAgAAA==
                """,
        """
                androidx/constraintlayout/compose/MotionSceneScopeKt.class:
                H4sIAAAAAAAA/2WQTU8CMRCG3y7yISqC32DiQe+sGm6ejGKyEVYDSkw4mLI0
                pLC0ZLdL9Eb8Kf4MD4Z49EcZp0ZjopfpzNN3Zt72/ePlFUANuww1rvqRlv0H
                N9AqNhGXyoT8USeGwHiiY+E2tZFatQOhRDvQE3FpsmAMxSGfcjfkauBe9YYi
                IJpi2Lz1z+sXnl8/v/dPm/X76xZVdwxrjV9520RSDU4Yyj/Lq3+X55BlOGjo
                aOAOhenZm9jlSmnDrZnY9bXxkzCkIaXGSJtQKrcpDO9zw4k542mKXshsoEFs
                ZBOH+IO02SFl/SOGvfksk5/P8k6xsF8ozmcV55DdvT3dvD1nHOJWdcxoDNb/
                /kF1ZBhWzqxrrkyHh4lg2G0lysix8NRUxrIXitNfwwz5tk6iQFzIkKTlb2nn
                nxBHcLBgzaOCNDJUbVvzyGGHzgzxReCLlL/iFulAXbSB9EtdpDwse1ihiIKH
                VRQ9lLDWBYuxjo0unBjpGJufnYegJwYCAAA=
                """
    )

    @Test
    fun createRefsForArgumentTest() {
        lint().files(
            kotlin(
                """
                    package example
                    
                    import androidx.constraintlayout.compose.*
                    
                    fun Test() {
                        val scopeApplier: ConstraintSetScope.() -> Unit = {
                            val (box, text) = createRefsFor("box", "text")
                            val (box1, text1, image1) = createRefsFor("box", "text")
                            val (box2, text2) = createRefsFor("box", "text", "image")
                    
                            val ids = arrayOf("box", "text")
                            val (box3, text3, image3) = createRefsFor(*ids)
                        }                   
                    }

                    fun Test2() {
                        val scopeApplier: MotionSceneScope.() -> Unit = {
                            val (box, text) = createRefsFor("box", "text")
                            val (box1, text1, image1) = createRefsFor("box", "text")
                            val (box2, text2) = createRefsFor("box", "text", "image")
                    
                            val ids = arrayOf("box", "text")
                            val (box3, text3, image3) = createRefsFor(*ids)
                        }                   
                    }
                """.trimIndent()
            ),
            ConstraintSetScopeStub,
            MotionSceneScopeStub
        )
            .run()
            .expect(
                """src/example/test.kt:8: Error: Arguments of createRefsFor (2) do not match assigned variables (3) [IncorrectReferencesDeclaration]
        val (box1, text1, image1) = createRefsFor("box", "text")
                                    ~~~~~~~~~~~~~
src/example/test.kt:9: Error: Arguments of createRefsFor (3) do not match assigned variables (2) [IncorrectReferencesDeclaration]
        val (box2, text2) = createRefsFor("box", "text", "image")
                            ~~~~~~~~~~~~~
src/example/test.kt:19: Error: Arguments of createRefsFor (2) do not match assigned variables (3) [IncorrectReferencesDeclaration]
        val (box1, text1, image1) = createRefsFor("box", "text")
                                    ~~~~~~~~~~~~~
src/example/test.kt:20: Error: Arguments of createRefsFor (3) do not match assigned variables (2) [IncorrectReferencesDeclaration]
        val (box2, text2) = createRefsFor("box", "text", "image")
                            ~~~~~~~~~~~~~
4 errors, 0 warnings"""
            )
    }
}
