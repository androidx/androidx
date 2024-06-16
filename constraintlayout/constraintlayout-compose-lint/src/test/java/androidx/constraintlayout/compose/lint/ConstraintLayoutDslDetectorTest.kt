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

    private val ConstraintSetScopeStub =
        bytecodeStub(
            filename = "ConstraintSetScope.kt",
            filepath = COMPOSE_CONSTRAINTLAYOUT_FILE_PATH,
            checksum = 0xb5f243fa,
            source =
                """
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
        """
                    .trimIndent(),
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMudSTMxLKcrPTKnQS87PKy4pSszM
        K8lJrMwvLQEK5BbkF6cKCfnml2Tm5wUnp+alBifnF6R6l3AJcrGnViTmFuSk
        CrGFpBaXeJcoMWgxAACjh5JrZAAAAA==
        """,
            """
        androidx/constraintlayout/compose/ConstrainedLayoutReference.class:
        H4sIAAAAAAAA/6VRXU8TURA9d/u1rEW2lUoBxQ9QSlEWiG8giWJMNqloqOGF
        p9vda7ntdpfs3jb41t/iL9BEo/HBND76o4xzy4oCvvkyZ87cmTNzZ378/PoN
        wCOsMWzz0I8j6Z84XhQmKuYyVAF/G/UVBXrHUSKc3d8Pwm+MX/bFGxGL0BMF
        MAa7wwfcCXjYdl62OsJTBWQY8tsylGqHoVJrXEzYWjlgWGxEcdvpCNXS0onD
        wzBSXElq5uxFaq8fBFsMhvRNmAwL3UgFMnQ6g55DE4o45IHjhiqmUuklBVjU
        yTsSXjetfcVj3hOUyLD8jwn+ijS1SJtmKqKISQtXcJUhU9M8B9tCFiWG8mWJ
        Ikxcm4CBaYasOpIJw07jf7ZJ3821hXJ9hunayuWGDKVGuoYXQnGfK6431Btk
        6JhMmwltwMC6FD+Rmq2T52/QnUfDomVUDcuwR0PLMLPV0XDTWGdPJ8t525gj
        7/u7vGFn9ktnzLRGw7msmbVzWmOTaeXK2fiqKVTTi47FWlcxzO/3QyV7wg0H
        MpGtQDz5c05az27kC4apBv16r99rifg1pxy91sjjwQGPpeZpcOmi1tktz4la
        zagfe+K51DWzac3Bpe7YoBNlx2sp64sRLhPLExYIDcIcMQM1Ys8IDUJ7tTzx
        BVP1zyjXVz+h8mGcuZLW5bGOOvnXT3MJZ/TeyTvVXyW/wNIGJVQxm8o7+jaE
        ufpHVN6f00SqWTxNSDXPT/pgbO/jIeFjis5R3vwhMi5uuLjpYgG3yMVtF3dw
        9xAswSKWDpFPMJPgXgIz0X41wewvvckEVQIEAAA=
        """,
            """
        androidx/constraintlayout/compose/ConstraintSetScope＄ConstrainedLayoutReferences.class:
        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
        """,
            """
        androidx/constraintlayout/compose/ConstraintSetScope.class:
        H4sIAAAAAAAA/7VVXU8bRxQ9s/5kMY5xAuEjTUlME/MR1qE0bQNNC25JljpA
        7Qo14qXD7sRZWO+inTWCN9T+gv6F/oJWaiFqpAjlsT+q6p21IcFYeUDtg2fu
        nHvvOXdn5o7//uev1wDmUGOY454d+I69b1i+J8OAO17o8gO/GRLQ2PWlMMpn
        jpoIa5a/K1JgDLltvscNl3t1Y21rW1hhCjGG5ILjOeEjhlhxYiODBJI64kgx
        xMMXjmR4ULmM4DwRe2I/NG2Ga8WJylvpWhg4Xp381zuxpabj2iJIoU9HVlVw
        41R5plN5xrHTyJEG390VHmncK16UuKjaVpjPII+rSuQaQ7YuPBHwUNhlv+mF
        DMzMYBDXe6BhiGoomu/nGVE8owzp0G85M/gAGQXeZOizAkHUVfFcLvsBQ724
        Wek8BarzMjs8fgYJuxIFk4oIhGcJSbtbqPhB3dgW4ZYKkQb3PD/koUNJxqof
        rjZdl6Jiji3TKDDc3PFD1/GM7b2GQSIi8LhrmJ76IOlYMoWPGAasF8LaaSev
        84A3BAUy3C1e/KYup0GX6y6KOu5ggmH9v/7kFKZOb3gzdFxjMQj4AaH36JJQ
        7sHac4aJbrtvTnQBMzBQ0jGD+wyV4uU6oNtJRw32sY5pzDFc7RJBV4Zb9Dly
        /LR7Vi4p36XnMuhVN1PD5wyJ8VZ791faJ/9UhNzmIacStMZejJ4bpoYeNYC6
        YofwfUetSmTZtDE/nxze0bUhTddyJ4e6llZGWlcmYWrK9bfRLC2HTg5ntRJ7
        yDJLiTe/JrWctlLIJUaSpWRVK5Evn0uN6PlkmuUpqpS+TWMU1rOSzukjWqn3
        iVbN5mJkxX9481NW+YhVlTLLVIHV/6OLRt/jpoa4yDezQy9IvOzbguFKhdJW
        m40tEXzPt1xC8hXf4u4GDxy1boOjVXp2nIYwvT1HOgQtvm1VhvFO71njnQvL
        mB69YmWXS6kq02t+M7DEsqMEhtsUGxfocZ8uQzw6X029eWTFyab/ABq/pZWh
        Tp7mxOQx0r+ToaFCYzICY3hKY6YVgB7oNOfVFWsnrxCZRvPYK2SfHeNKvv8I
        A5N/YHhhcjT+458YHj3CjSN8+FsHb+Id3rE27y9k3SJFxWtShOIdmMqPv8Tk
        K0w/m5yaev0Ss8f45DxZEumIbLCV0CZTVgEPyL/ajrtN8xr9Uqy1yA3jU3zW
        ZRMenudnHZswH/HHsE6jTtg0xZrox3dR1gqqNFcJX6DYLzYRM/HIxJcmvsIi
        mVgyUcbXm2AS32B5E30SusRjiaTEYGSMSTyRKET2LYneyDD/BWglTVUgCAAA
        """
        )

    private val MotionSceneScopeStub =
        bytecodeStub(
            filename = "MotionSceneScope.kt",
            filepath = COMPOSE_CONSTRAINTLAYOUT_FILE_PATH,
            checksum = 0x499473bb,
            source =
                """
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
        """
                    .trimIndent(),
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMudSTMxLKcrPTKnQS87PKy4pSszM
        K8lJrMwvLQEK5BbkF6cKCfnml2Tm5wUnp+alBifnF6R6l3AJcrGnViTmFuSk
        CrGFpBaXeJcoMWgxAACjh5JrZAAAAA==
        """,
            """
        androidx/constraintlayout/compose/MotionSceneScope＄ConstrainedLayoutReferences.class:
        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
        """,
            """
        androidx/constraintlayout/compose/MotionSceneScope.class:
        H4sIAAAAAAAA/61VW08bRxT+Zm1ssxjHOOHqNCWBNuYS1qH0FmhacEtY11wK
        KWrES4fdCVlY76KdNYI31H/Qp773F7RSC1EjRSiP/VFVz6yXJBgrUqLI0pkz
        35z5vpkz56z//e+f5wBm8JBhmnt24Dv2oWH5ngwD7nihy4/8RkhAfd+Xwlj2
        Q8f3NizhiQ3L3xdpMIb8Lj/ghsu9HWN1e1dYYRoJhtSc4znhfYZEaWwziw6k
        dCSRZkiGTxzJMFN7e7lZovXEYWjaDNdKY7VXwhth4Hg7tN7fii00HNcWQRrd
        OnJKv3iuO9Wq69gZ5EmC7+8LjyTulC4rXBaNBWazKOCq0rhGKdmhIwc8FLZp
        V/yGFzIwM4s+9HdCwwDD9ZL5ZqYhxVRkyIR+czGLD5BV4A2GbisQRL4uHstF
        P2AQpa1a6yPQSd8+waOV80hh16JQ0hCB8CwhKbkjNT/YMXZFuK1CpME9zw+5
        opDGih+uNFyXohKOLTMYYbix54eu4xm7B3WDtEXgcdcwPXUd6VgyjY8Yeq0n
        wtqLN6/xgNcFBTLcLl2+UZvXoMq6jZKOjzHGsPJ+L5zGxHlxU224xnwQ8CNC
        71CJ0N6j1ccMY+0yb461AbMwUNYxhbsM1dK7FH+7N4466xMdk5hhuNomgoqF
        W3QZOXreOEvvJN6m2bLoUhWp4UuGjtFmV+deFn5c9j21uAiWRchtHnI6kVY/
        SNBHhynTqQyoPfYIP3TUrEyeTVn67ey4pGsDmq7lz451LaOcTHMkUEH5nhjO
        0XTg7HhaK7N7rHuh48XvKS2vVUfyqaFUOb2ulWmtkM8M6YVUhhUoqtx5i2wU
        plcz+a4hrZxd0tZz+QR5yZ9e/JJTa8RKUEcLpI43zdSh195/ixXfsEwv3Mo2
        tUc5TlZ8WzBcqdGmlUZ9WwQP+bZLSKHmW9zd5IGj5jFYXKeHcerC9A4c6RA0
        /6qLGUZbV1/25IWwrOnRO1dcLqU6l77hNwJLLDpKYDCm2LxEj7tULUl6Yo1+
        c+iPvL5oTFI66T+CbI1mhqoIGjvGT5H5MwpbJpuKwDRWyGabAeiETmNB1WK8
        uUpkGo3Dz5B7dIorhZ4T9I7/hcG58WLy578xWDzB9RN8+EcLb+E13uGY91fy
        biIR8Zqkpnh7JwqjTzH+DJOPxicmnj/F9Ck+vUiWQm9E1tfcEJMpbwSf0fpq
        HHeLxjV1Jdac5AfxOb5ok4R7F/lZSxJmI/4EfiCrEzZJsVX0YD3a9T02aPyR
        8K8o9v4WEia+NvGNiXkskIuKiW/x3RaYxCIebCEnoUssSaQk5iKnT2JYwpQY
        iaY3Jboip/o/WrVkd0EIAAA=
        """,
            """
        androidx/constraintlayout/compose/MotionSceneScopeKt.class:
        H4sIAAAAAAAA/2WQz04iQRDGvxoUEVdEFBXc7GG9M2C8eTIrJhNhdiO7xoSD
        aYYOaRi6zUwP0RvZR9nH2IMhHvehNlYbjQle6s+vv1R91f/+/30EcILPhBOh
        h4lRw3s/Mjq1iVDaxuLBZJbB9M6k0u8aq4zuRVLLXmTu5KVdAxHKYzETfiz0
        yP8+GMuIaY5Q/RWety+CsH1+G55127c/rri7IVQ67/KeTZQenRJqb8sby8sL
        WCMcdUwy8sfSDtxL6gutjRXOTOqHxoZZHPOQ7c7E2FhpvyutGAormHnTWY4v
        JBfWXQCBJq7w+PFeuarJ1bBF+LKY54uLedErl76Wyot53WvSzdPvn09/8h5z
        pzomN2Bn+SMaE0vY/OasC22vRZxJwuFVpq2aykDPVKoGsTx7d00o9kyWRPJC
        xSytvUqvPwjRgoeVF9t1rCLP3YEzjwJqnPPM191VTOovcR+HnFvMi6zf6CMX
        4FOAzQAlbAUoYztABTt9UIpdVPvwUqym2HsG4yzy1wsCAAA=
        """
        )

    @Test
    fun createRefsForArgumentTest() {
        lint()
            .files(
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
                """
                        .trimIndent()
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
