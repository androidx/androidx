/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.runtime.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.compiled
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WrongNavigateRouteDetectorTest(private val testFile: TestFile) : LintDetectorTest() {

    override fun getDetector(): Detector = WrongNavigateRouteDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongNavigateRouteDetector.WrongNavigateRouteType)

    private companion object {
        @JvmStatic @Parameterized.Parameters public fun data() = listOf(SOURCECODE, BYTECODE)
    }

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClass())
                    navController.navigate(route = TestClassComp())
                }
                """
                    )
                    .indented(),
                testFile,
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClassWithArg(10))
                    navController.navigate(route = TestObject)
                    navController.navigate(route = classInstanceRef)
                    navController.navigate(route = classInstanceWithArgRef)
                    navController.navigate(route = Outer)
                    navController.navigate(route = Outer.InnerObject)
                    navController.navigate(route = Outer.InnerClass(123))
                    navController.navigate(route = 123)
                    navController.navigate(route = "www.test.com/{arg}")
                    navController.navigate(route = InterfaceChildClass(true))
                    navController.navigate(route = InterfaceChildObject)
                    navController.navigate(route = AbstractChildClass(true))
                    navController.navigate(route = AbstractChildObject)
                    //classes with companion object to simulate marked with @Serializable
                    navController.navigate(route = TestClassWithArgComp(15))
                    navController.navigate(route = OuterComp.InnerClassComp(15))
                    navController.navigate(route = InterfaceChildClassComp(true))
                    navController.navigate(route = AbstractChildClassComp(true))
                }
                """
                    )
                    .indented(),
                testFile,
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClass)
                    navController.navigate(route = TestClass::class)
                    navController.navigate(route = TestClassWithArg)
                    navController.navigate(route = TestClassWithArg::class)
                    navController.navigate(route = TestInterface)
                    navController.navigate(route = TestInterface::class)
                    navController.navigate(route = InterfaceChildClass)
                    navController.navigate(route = InterfaceChildClass::class)
                    navController.navigate(route = TestAbstract)
                    navController.navigate(route = TestAbstract::class)
                    navController.navigate(route = AbstractChildClass)
                    navController.navigate(route = AbstractChildClass::class)
                    navController.navigate(route = InterfaceChildClass::class)
                    navController.navigate(route = Outer.InnerClass)
                    navController.navigate(route = Outer.InnerClass::class)

                    //classes with companion object to simulate marked with @Serializable
                    navController.navigate(route = TestClassComp)
                    navController.navigate(route = TestClassComp::class)
                    navController.navigate(route = TestClassWithArgComp)
                    navController.navigate(route = TestClassWithArgComp::class)
                    navController.navigate(route = OuterComp.InnerClassComp)
                    navController.navigate(route = OuterComp.InnerClassComp::class)
                    navController.navigate(route = InterfaceChildClassComp)
                    navController.navigate(route = InterfaceChildClassComp::class)
                    navController.navigate(route = AbstractChildClassComp)
                    navController.navigate(route = AbstractChildClassComp::class)
                    navController.navigate(route = TestAbstractComp)
                    navController.navigate(route = TestAbstractComp::class)
                }
                """
                    )
                    .indented(),
                testFile,
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expect(
                """
src/com/example/test.kt:7: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClass)
                                   ~~~~~~~~~
src/com/example/test.kt:8: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClass::class)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:9: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArg)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArg::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestInterface)
                                   ~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestInterface::class)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass)
                                   ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstract)
                                   ~~~~~~~~~~~~
src/com/example/test.kt:16: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstract::class)
                                   ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClass)
                                   ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = Outer.InnerClass)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = Outer.InnerClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:24: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassComp)
                                   ~~~~~~~~~~~~~
src/com/example/test.kt:25: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:26: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArgComp)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:27: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArgComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:28: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = OuterComp.InnerClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:29: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = OuterComp.InnerClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:30: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:31: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:32: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:33: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:34: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstractComp)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:35: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstractComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
27 errors, 0 warnings
                """
            )
    }
}

private val SOURCECODE =
    kotlin(
            """

package androidx.navigation

public open class NavController {

    public fun navigate(resId: Int) {}

    public fun navigate(route: String) {}

    public fun <T : Any> navigate(route: T) {}
}
""" +
                TEST_CLASS
        )
        .indented()

private val BYTECODE =
    compiled(
        "libs/StartDestinationLint.jar",
        SOURCECODE,
        0xb1569275,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuMSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPi90ssc87PKynKz8lJLfIu4RLl4k7Oz9VLrUjMLchJFWILSS0u8S5RYtBi
                AADcysPxVwAAAA==
                """,
        """
                androidx/navigation/AbstractChildClass.class:
                H4sIAAAAAAAA/41QTW9SQRQ9M+8DeAV54Belamv9CGUhtHGnaaQkJiTYRW1Y
                wGrgvdAJj/eSNwPpkt/i2o2JxsSFIS79UcY7j2pcsHAxZ+65c3LunfPz17fv
                AF7iGcNzEQdpIoPrViyWciq0TOJWZ6x0Kia6eyWjoBsJpXJgDAfbtJeh0n/0
                OVgM7msZS33KYDeGRwMGq3E0KMJBzoONPHGRThnYsAgPOwVwFEmqr6RiaPT/
                b5tXNGUa6o4xIvshQ6U/S3Qk49a7UItAaEESPl9a9E1moGAANHZG/WtpWJuq
                4JjhbL2qerzGs7Needzf8Xjeqq1XJ7zNzkpV1+d13rZ+fHC5b19U/rI8qet2
                3vFd43TCcLh1/X8Doq1oCf9cLLtJrNMkisL0xUxTAN0kCBnKfRmH54v5OEwv
                xTiiTrWfTEQ0EKk0/KbpvU8W6SR8Kw3ZvVjEWs7DgVSSXjtxnOhsssIxpWtn
                /66asKniVDtwCQ+InRLndHvNryg0976g9CnTPCY0GqCBQ8J7GxVuoWxipMq4
                Uerw6Wy8WiZdup3mZ5Q+brUpbgQ3NhxPMtzHU7rfZEs6uD2C1cOdHu72aOx9
                KlHrYRf1EZjCHh6MkFMoKzxU8BQeKbgKvkLlN2AWm1jVAgAA
                """,
        """
                androidx/navigation/AbstractChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy27TQBQ9Y6dxYgKkLY+E9yNIbSXqpqrYBCGVIKRIaZEo
                yqYLNLGHdhJ7Bo0nUZdZ8SH8QVdILFDUJR+FuOME2AKb+zr33Ds+199/fP0G
                YA9PGPa4SoyWyVmk+FSecCu1ivaHuTU8tt1TmSbdlOd5V2cfW85wRQ0BGEN9
                xKc8Srk6id4MRyK2AXyG8nOppH3B4G9sDmpYQTlECQFDyZ7KnOFZ/38Wdhja
                G/2xtqlU0WiaRVJZYRRPo1fiA5+ktqsVTZjEVpsDbsbCdDYHITy3eL0V/wHf
                ZwXKsP1v0xhWfxEOhOUJt5xqXjb1SUjmTNUZMLAx1c+ky3YoStoMrfksDL2G
                F3p1iuazysUnvzGf7Xo77GVQ8S4+l72653p3mZuw9fcKBbjNUP0tEx3lkE/p
                9dboNBVme2xJ+K5OBMPVvlTicJINhXnHhylV1vo65umAG+nyZbHWU0qYYoGg
                c4VHemJi8Vo6rPl2oqzMxEDmkpr3ldK2eF2ONmldcgKQ99zV6TvuUxY5Rciv
                bH1B5byAH5AtF8UOHpKtLRpQRQjUGUWXluSn5L0luXZeqOsINxbFBaGILuMK
                YT4eURYWpDu4iyYeFwvvoVX87aQB9daP4few2sNaD+u4RiGu92jmzWOwHA00
                Cc8R5riVo/wTjWwvPCoDAAA=
                """,
        """
                androidx/navigation/AbstractChildClassComp.class:
                H4sIAAAAAAAA/5VSS08TURT+7vQ1HYqUiljABwpiqcgUQlxYQoI1mialCyRN
                hNVtey2XTu+YubcNS36LazdEDYkmhrj0RxnPTCskygIXc849Z77znefPX1+/
                A9jAOkORq3bgy/axq/hAdriRvnK3m9oEvGUqh9JrVzyudcXvvU+BMSxehd8T
                2lzERMgYQ3JTKmm2GOKF/eUGQ6yw3MgggZSDOGyyedBhYPsZOBhLw0KGoOZQ
                aoaV2vWrKlOmjjDbIRml2GewN1veKPXG9XkWQ8EVAVK4ybBWqHV9Qzzu0aDn
                SmVEoLjnvhTveN+jJhVx9FvGD3Z40BVBedjbLQdTmGZIX5AxPPuPZi6LKGeQ
                x0w4llmGhZofdNwjYZoBl0q7XCnfRDzarfum3vc8GsPkn4p3hOFtbjj5rN4g
                RqtmoUiHAjTyLvmPZWiV6NVeY3h9fpJzrLwVfecnjpUdcyw7nj8/mU+tWyX2
                nKVejOeSWWvWKsV+fEha2fju5IVlU8hs3E5kkyEdHdXSlS3/fSVUHlWTrfMB
                jdMEvueJYLVrGOZ2+8rInqiqgdSy6Ynty37pRip+WzBM1KQS9X6vKYI9ThiG
                XM1vca/BAxnaI2emqpQIogELCnbe+P2gJV7J8N/MKE/jnyxYo8HHaUAWZsI9
                UJ1PyEqSvkM6F54s6RjZici7QtYWoS3STvEM6eLcF4yfRgxPR5FAGaskp4co
                3MBEuBB6hWy0P2TpG3K54Z5IJ4qfMf7xSprMEDCisamo1Cg4j2jTyHzD1Ft2
                htufMHcaeWJEHCZkdKZW1Fgp4i5Sw0CF/HeJ8d4BYlXcr2K+igd4SE8sVLGI
                RwdgGkt4fABbY0KjoOFoLGskNbIakxr53/A96KpcBAAA
                """,
        """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41Sy27TQBQ9M3k5bqChPJpQHqVFgrDAbcWOCilEIFkyRqJR
                JNTV2B610zgzkj2JusyKD+EPKhaVQEIR7PgoxLUJD4kusOV759w5c67uGX/7
                /vEzgCe4z/BA6CQzKjn1tJipI2GV0V4/ym0mYjs4VmnyOjqRsW2AMWxeRB7K
                3P460ECFob6vtLLPGCoPe6MWaqi7qKLBULXHKmfoBf/Z8ymDsx+npZoLXkg4
                fngw7IeDFy1cgtuk4mWG7cBkR96JtFEmlM49obWxpWruhcaG0zQlqSvB2FgS
                815JKxJhBdX4ZFYhJ1gRmkUAAxtT/VQVaIdWyS41WMxdl3d4+S3mztd3vLOY
                7/Ed9rzh8C/v67zNC+oew9aFw/3tEbVth2I2MNpmJk1l9nhsGTbeTLVVE+nr
                mcpVlMr+nyHIuYFJJMNqoLQMp5NIZkNBHIa1wMQiHYlMFXhZdA/MNIvlS1WA
                7lJ49I8sdsm+ajlzt3CT8h1CdcptypzeWonuEvIKZyjXHp3DOSu3N5dkoId7
                FFs/CWiSFOBg5ffhdWIXz8on8LfnaH3A6llZ4Ngq421slz8k3RIJrB2i4uOq
                j2s+ruMGLbHuo4PuIViOm9ig/Rxujls56j8AJAtPiM0CAAA=
                """,
        """
                androidx/navigation/AbstractChildObjectComp.class:
                H4sIAAAAAAAA/5VSW2sTQRT+ZnLbbKNN66WJVau2FC/otsU3gxCDwsK6gg0B
                6dNsdmin2czI7iT0MU/+EP9B8aGgIEHf/FHi2TVU0L64y57bfOc7nG/2x8/P
                XwE8xRbDI6Hj1Kj4xNNiqg6FVUZ73SizqRja3pFK4jfRsaTQjN/XwBi2Lmro
                y8yeNxXIEkO1o7SyzxlK9x8MGqig6qKMGkPZHqmM4XHwH7OfMTidYVIwuuA5
                jeOH+/1u2HvZwCW4dSpeZtgMTHroHUsbpULpzBNaG1swZ15obDhJEqJaCUbG
                Epn3WloRCyuoxsfTEqnCclPPDRjYiOonKs92KIp3acB85rq8xYtvPnO+f+Ct
                +WyP77AXNYd/+1jlTZ5D9xi2L1zwb61odDMU057RNjVJItMnI8uw/nairRpL
                X09VpqJEdv8sQgr2TCwZlgOlZTgZRzLtC8IwrAZmKJKBSFWeL4ruvpmkQ/lK
                5Ul7QTz4hxa7JGG52LudK0r+NmVV8k3ynN5KkW1Q5uXqkK88PINzWhzfWYCB
                Du6SbfwGoE5UgIOl8+Y1QufP0hfwd2dofMLyaVHguFfYW9gsflC6KSJYPUDJ
                xxUfV31cw3UKseajhfYBWIYbWKfzDG6GmxmqvwBXhfge3QIAAA==
                """,
        """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41Qz2sTQRT+ZvJjk21qNqnWNPVXrdomBzct3pRiGxACsUJb
                ckhOk+yaTrOZhZ1J6DF/i2cvgiJ4kODRP0p8k4aCkIMs89775n37vTff7z8/
                fgJ4hT2GPaGCJJbBta/EVA6FkbHyW8qEyUcxCJuXMgqakdDaAWPwrsRU+JFQ
                Q/9D/yocGAcphp1VEhehNrcyDjIM2TdSSXPEkN7v1joMqf1apwAHeRdpuIRF
                MmRg3QIKWM+D4w5RzaXUDLX2f275msYMQ3NslUi/y1Bqj2ITSeW/D40IhBFE
                4eNpit7PbMjbAJo7ovtraVGDquCA4WQ+K7u8whdnPnO5t+byXKoynx3yBjtZ
                L2c9XuWN1K9PWe6lz0q3KEfsajqX8bJW6ZBhd+X+/1hEa9EW3qmYNmNlkjiK
                wuTlyJAFzTgIGYptqcLTybgfJheiH9FNuR0PRNQRibR4eemex5NkEL6TFmyd
                TZSR47AjtaTusVKxWYzWOCB/0zQwS6dsDad3c6od5Cg+JXREmFN269+xVt/+
                huKXBWeXov0LeEYfsHnDgoeSdZIqq0bGk+7GUsu3BlPO1L+i+HmlTOGGsJTh
                eL6IO3hB+S317lLvXg+pFjZbuN9CBVtUotrCNh70wDQe4lEPjkZJ47FGQeOJ
                Rk6jrLHxF0rwRpzxAgAA
                """,
        """
                androidx/navigation/InterfaceChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy27TQBQ9Y6d5mABpyyPhXQhSC6JuKhBIRUgQhGQpLRJU
                2XSBJva0ncSeQeNJ1GVWfAh/0BUSCxR1yUch7jgB1mVzX+eee8fn+uev7z8A
                PMVDhmdcJUbL5CRUfCKPuJVahZGywhzyWHSPZZp0U57nXZ19bjvDFXVUwBga
                Qz7hYcrVUfh+MBSxrcBnKL+UStpXDP76Rr+OJZQDlFBhKNljmTM87/3Xxh2G
                znpvpG0qVTicZKF0DMXT8K045OPUdrXKrRnHVptdbkbC7Gz0A3hu82o7/gd+
                ygqUYfN80xiW/xB2heUJt5xqXjbxSUrmTM0ZMLAR1U+ky7YoSjoM7dk0CLym
                F3gNimbT6tkXvzmbbntb7E2l6p19LXsNz/VuMzfh8TkkquAmQ+2vTnSWPT6h
                51uj01SYzZEl6bs6EQyXe1KJvXE2EGafD1KqrPR0zNM+N9Lli2I9UkqYYoGg
                gwUf9djE4p10WOvDWFmZib7MJTW/Vkrb4nk5OiR2ySlA3nN3pw+5S1noJCG/
                9OgbqqcFfI9suSi+wBrZ+rwBNQRAg1F0YUF+Qt5bkOunhbyOcG1enBOK6CIu
                EebjPmVBQbqF22jhQbHwDtrFD08aUG/jAH6E5QgrEVZxhUJcjWjm9QOwHE20
                CM8R5LiRo/wbyZhkAy0DAAA=
                """,
        """
                androidx/navigation/InterfaceChildClassComp.class:
                H4sIAAAAAAAA/5VSXU8TQRQ9sy3dtizSFsVS/ABBLUXYghiNEBKs0TQpNUHS
                RHiatkPZdjtrdqYNj/wWn30hakg0McRHf5TxTqnwoInhYe+dc/fecz9//vr6
                HcAa1hgWuWyGgdc8ciXvey2uvUC6ZalFeMAbonTo+c2Sz5UqBd33NhhDqs37
                3PW5bLlv6m3R0DYiDLP/otkVSl9Q2RhhiG140tObDNH83kKNIZJfqDmwkUgi
                iiRhHrYY2J4DB2MJWLhGrvrQUwxLlStUuk6pWkJvGTbKsccQ32j4w9xPrkA0
                bwSX5GHjBsNKvtIJNBG57X7X9UyM5L77Uhzwnq9LgVQ67DV0EG7zsCPC9fPu
                biYxiSxD4oKM4elV2rmsYt1BDtNmMrcY5ipB2HLbQtdD7knlcikDPSBSbjXQ
                1Z7v0yDSf0reFpo3ueZks7r9CF0AMyJhBGjqHbIfeQYV6dVcYXh9dpxJWllr
                8J0dJ63UaNKKR7NnxzP2qlVkz5n9YiwTS1k5qxj58SFmpaI76QsUp5BcND6S
                ihm6VVPvf6+EaqNSUlXep2HqMPB9ES53NMP0Tk9qryvKsu8pr+6Lrctm6UZK
                QVMwjFc8Kaq9bl2Eu5x8GDKVoMH9Gg89g4dGpyylCAfTFRScfBv0woZ45Zl/
                U8M8tb+yYIWmHqXqYqSnzBrovUTTipG+QzpjjpZ0hLCNOMllQpvkbZFOFk4x
                Wpj+gvETQhbcYSTwDEWSk+deSCFt9kEvw0brI96JIZdr1kR6pPAZ4x//SeOc
                Owxp4riOxDA4i8Gi4XzD5Dt2iqlPuH0ysESoNZOQDYrIUXOrA+5HeEy6RPa7
                xDizj0gZs2XcK2MO8/TE/TIe4OE+mEIeC/uIK6QVCgqOwqIyMKMwoZD7DYsK
                p/5yBAAA
                """,
        """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41STW/TQBB9u0kTxw00LV8J5asUUOkBtxU3KqQSgWQpGIlG
                kVBPG3tJN3F2JXsT9ZgTP4R/UHGoBBKK4MaPQsyaUA4gga2dmfd25u3O2N++
                f/wM4DEeMGwJnWRGJSeBFlM1EFYZHYTayuytiGX7WKXJq/5QxrYKxtAYiqkI
                UqEHwS+2xLDxN42uzO25ThVLDJV9pZV9ylDaetirowrPRxk1hrI9VjnDdud/
                7/KEwduP00LOB3caXhgddg+i9vM6VlCvEdlg2OyYbBAMpe1nQuk8EFobW8jm
                QWRsNElTklrtjIwlseCltCIRVhDHx9MSjYg5U3MGDGxE/IlyaIeiZJcOmM98
                nzd5seYz7+s73pzP9vgOe1b1+Jf3Fd7gLnXP3eWfU6JzG5GYto22mUlTmT0a
                WYb11xNt1ViGeqpy1U/lwe8uaHZtk0iGlY7SMpqM+zLrCsphWOuYWKQ9kSmH
                F6R/aCZZLF8oB1oL4d4fstil+ZWp5Qqtlhso+TvUt8Nr5Dm99P0IbRAK3HDI
                L22fwT8ttu8ukoH72CRb/5mAZYpAhRfOi69RtnuWP4G/OcPFD1g9LQiOe4W9
                TRLuZ2W4RAKXj1AKcSXE1ZBKmxSiFeI61o/ActzATdrPUc9xK4f3Az/F2jnp
                AgAA
                """,
        """
                androidx/navigation/NavController.class:
                H4sIAAAAAAAA/41SW08TQRT+Zttut8utrYJQQeWiFFC2EH2xhERJjEtqNdL0
                hadpuynTbmeT3WnDY3+L/8AnjQ+G+OiPMp7ZNlBAo5vsuX/fnDNzfv769h3A
                c+wzrHLZCgPROnckH4g2VyKQTpUPjgKpwsD3vTANxpDt8AF3fC7bzvtGx2uq
                NBIM5oGQQh0yJIpb9WmkYNpIIs2QVGciYliv/JO9zGCNcx7hiu5WnSEVepHb
                YmAuw3yxcnX2iQqFbJd1zXolCNtOx1ONkAsZOVzKQMUHRE41UNW+75c1U9BX
                noUcw4NuoHwhnc6g5wipvFBy33GlZoxEM0rjDh3WPPOa3TH8Aw95z6NChs3J
                JkYXUP5TW9OYx4KNu7jHkL9dcGOaMZGeZvmg9vJ25rBYq8Xp/O0cQ64ynuid
                p3iLK04xozdI0NMyLTJagG6xS/Fzob0SWa09hvBiuGwbi4ZtZC+GtmFpg34r
                Sdqif9ZaWLwY7hsl9jr145NJyeOVbKJglJJrlnUxzKa2KbVvZs2C8XZUkD6e
                HRVQ1CKdmfCpqmTrk2nfdD812qdrS7DbVfT2R0GLVmCuIqRX7fcaXljjDd/T
                wwdN7td5KLQ/Dm587Eslep4rByISFLp8rVdXi8CQORFtyVU/JIh9EvTDpvdG
                aPzSGF8foSdAWIVBW6y/JHVLS01yhzxH9046tf0F1mcyDDwlacbBJJ6RnB4V
                IAObdA5TcUSDX8T1NP5NoBkDF0bJMVBbM5glqSnmKKcpyqR1VXonn/+KxetE
                JqwJovQlUZoolii/q23df3bcWAGJ/2G1/8q6THknrr5/nd1AKZbb2CNdoegK
                XcmDUyRcPHTxyKUbXiMT6y428PgULMITbJ5iKoIdoRjBjLRNxlaEXIRChJnY
                Lf4G70EG2roEAAA=
                """,
        """
                androidx/navigation/NavControllerKt.class:
                H4sIAAAAAAAA/41UW08TQRg9s1va7VJoy70F5Y6tFxbwLmhCmphsrCVBgiEk
                Jtt2rAvLbLIzbXjkt/gLVB5IJDHER3+U8duV2NCC0IedmTPnnD3z9Zv99fv7
                DwCP8JJh1hH1wHfrh5ZwWm7DUa4vrIrTKvlCBb7n8eCNSoAxZPaclmN5jmhY
                G9U9XiNUZxhscFXyHCltIZUjanyTf2SYLhTLl/lucfmXvUovLvtBw9rjqho4
                rpCWI4SvIpq0Kr6qND2PWJlal/nkNdYpGEgmocFkyHfGe++qT+tBIzIqXJfy
                nEwxRmtXmSzczCKFfqTDUBmGMQplC8GD7sJdFWmjqXgw1xZRpGH3covLA3UZ
                pDCE4TDQCIOxVvNc4apXDHqhuM1w679nSiDPEF+LFClMIGdiHLcY5m5SiQQm
                GWIFu7gdSqdNTGHmCmln5gTmTMyH9Gx531cU2XrLlVN3lEP10A5aOjU1Cx/J
                8AEGth9ONNo8dMPZEs3qywwfzo7y5tmRqY1ppmboHaM2k80QQVtiPz/HDeLl
                Y4aW0QmNEdjTBuOZBIEGgck2aGZ6w7esMOSuPFQCDxnM9smozy9cucV9xTC+
                2RTKPeC2aLnSrXp8vX1DqIQlv84Z0mVX8ErzoMqDLYc4DKm2LSee+c5vBjX+
                2g33cueW212GWKZWiFGJdOTD+0PFe0qrOI0JGvNh+3Zh1EEdWAw59NBKwzNa
                TRAa/mLf0Psl+kOen3OBvgu6HFKEdKmynap0h2oAg92q0U5V9oLKwBgpWaQq
                IeoUzJ5ifOcEt4/Re4qpnUz6BLPHyJ5iPpovHGP06z/T/kjUB5PijJC5jhe0
                Nml3nr6pj8l8New6PMEajRuE36GiFHah2yjauGvjHu7beIBFGxaWdsHC8q/s
                IiVhSCQleiTiEv0SaRmCfRJDEsMSAxKDfwAPYTX9vQUAAA==
                """,
        """
                androidx/navigation/Outer＄InnerClass.class:
                H4sIAAAAAAAA/41U31MbVRT+7m5+bJYENkBbfkRQiZiEtgvYai20CiiyGEIF
                h7HiyyVZw8Kyi7sbpr44PPVP6Iy+OOM4PvFQZxQcO+Ng++bf5Diem90mNVSG
                meSec8+e853vnnPu/euf3/8AcAPrDHnu1DzXqj3QHX5g1XlguY6+2ghML284
                jukt2Nz3k2AM2g4/4LrNnbq+urVjVoMkZIbErOVYwV2GWMEobjDIheJGGnEk
                VcSgMCiWQJnz6gzMSENFVwoS0uQfbFs+w3j5IgRmGLrqZmC0sCiNwaBW3b19
                1zGdYIoAq+7+1wxF4nFRzLGy69X1HTPY8rjl+Dp3HDdoevt6xQ0qDdueEYdJ
                qMT5MkNapMjXzC95ww4YtgoXS2QY5c7azVyQYxp96BfZh6iUgbseeJZDx+8v
                FF+ADK10niudtvmGZddML4kRFaOiHf1t7MLzztxR8Bo1ku/vm06N4VrhLPTZ
                bBEyERxDXoC/wZATpT/P8U3hWBCOC+c7loTjRBo5vCK0a3T4be5vL7g1kyHb
                jjScwKyL802GA0gTpmNaxRTeohOZXzW4TTN2qfCS+n9Os39e+6n3fMs2qapx
                N9g2PYbesyhEprzrBrbl6CtmwGs84GST9g5kul9MLCmxgIZ/l+wPLLEjrlKN
                BvbH08MRVRqQVEk7PVTpJ2mKKikJkl0kZZLdytOHysDp4bQ0yeYzvQlNGpIm
                5ac/JCQttpzSkmK39OyhvNynKaSTo6JIoROZGZlTpKvTitY1FBtgk2zp2SOZ
                AtOhxyNGeob0bqGvZVvwCtEZiilxLSG4TjNxgsH/HdgkFukutieL3ooKP1hw
                ncBzbdv0ru/SZYmFzespW45Zaextmd6nor6irG6V2xvcs8Q+Mg6vNZzA2jMN
                58DyLTLNtXvDkFkPeHV3he9H3vlO73vc43smUftPWLpN0aStuu42vKq5aAmI
                wQhi40w6GiaJ3jJRgl7xfpGmkE6vAq3LtFuk7xJJtXSCVGn4V2R+pp2Ej2nt
                huj4CMWPIkWyTLvLoTd96xGzQZpApVGCRv8QUxcjQzJe+gWZoxZcomkcbcKk
                Q4cIJkvkngePdQazlwbQy0KwImCKWApOqSeQ7g+f4MrjVlBINtUim4rIrkRs
                LgFaCgMYjHKPR8XK5mLffAtFMJgtDR9jOISs0CqDCQS621H62yQFtdwTjN4/
                wau9rx9jXEQeo6gVj3H1GNcfdxwjFzF6gQeteqsG41ENmgx+w43OMihRPMNN
                vB3x+IKkaFe+NPET4rGjiT8hfYe4fDRxCmlFAF2l//fCEgt7Umm2T04qfyOb
                pH27YvlWxfK4hXcpzyrpSUHqnWYN7jVD6XrhIyxR+T5pAhpYI/kZ2W9Tp2Y2
                IRuYNXDHwF28RyreNzCH+U0wHwv4YBM9vvh96ENtrgkfmo+sj14ffT5uNo23
                fOg+cqT/CwA97z/6BwAA
                """,
        """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41US08TURT+7p0+plMe5SFvnxR5KVMQVxATJBqHlGKEYJTV
                bTvCwDCjM7cNS1bu3Lpw6cIVC4kLEk0MStz4o4jnTkdBCMakvec7Z84535nv
                3Pbn8eevAKZxl2FIeNXAd6o7pifqzrqQju+ZSzVpB3nL8+xgqbxpV2QajCG3
                KerCdIW3bv6OagypWcdz5D0GbWR0tQlJpAwkkGZIyA0nZBgu/hfDDIMu/WUZ
                ON46Q+fIaPGErRGljMGiH6ybm7YsB8LxQlN4ni+jhqFZ8mWp5rqUlT3VVkcL
                Nd4Q4ca8X7WjIS3tx/TxGxrcflUTLk14aaR49s1mRp8z5P/FRlSi7NpEl/Tl
                hh0wtJ/vQtSzFTfSxwBXouhWaXllrjT/oAl9MDIU7GdoK275ktLMRVuKqpCC
                Cvl2XaMdMXVk1AEGtkXxHUd5BULVSYYXh7tXDN7DDZ473DW4rkA2trqhQrkW
                /ei10XO4O8UL7H5a59/fp3iOL3TktD5eSEzpuWRfoocV2KOjt9pCJpeiaJow
                I6wTziis2KaYmqH3wm2mMUp3pCTq874nA9917WBiSzL0P6l50tm2La/uhA5p
                NneiI92Sxl5ai45nl2rbZTtYUboqOf2KcFdF4Cg/DjYvS1HZWhQvYz9/tvdj
                EYhtm6b5i6QpuhHzrghDm1xj2a8FFfuho1r0xi1Wzw2HSVpPIlK+V22L7C3y
                UmSbySbpaTLybpNnqv2o6NgB9H0CHBNxMjBAj4GmRgIy1Eo1zVKER8XX42Kt
                vfVj9OgkXYvTTzOTzGiLeU9K2/cuKGXoQGfMZJHlZLvHxj8gmdgb/wb+Dklt
                b/wQ/GliLxq8QGcCPK1HzboaBXEzhbroy0gdqBtNvx8COnr+SNEdFQDZL+DP
                DtD7CQP7UUDDFJ1KR44xtJCqdyK+cforUqMxXCZ5rqxBs3DVwjWL3u4GQQxa
                yGNoDSzETQyvwQjVZyREKkRHBLpC5CKQpfMXu5HXfeAEAAA=
                """,
        """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VRW2sTQRg9M5vLZhNtGi9NjK2XptpUcNviUy1CDQoLMYW2
                BCRPk2SIk2xmYXcS+pgnf4j/oPhQUJCgb/4o8dttNA9S3GG/M9/tfDNnfv76
                8g3ACzxjqAjdDwPVP3e1mKqBMCrQ7vHEyDALxlAciqlwfaEH7nF3KHsmC4sh
                c6i0Mq8YrO16u4A0Mg5SyDKkzAcVMVSb17K+ZLAPe37S74DHTbbXOj07ajXe
                FHADTo6CNxk2m0E4cIfSdEOhdOQKrQOT8ERuKzCtie8T1WpzFBgic99JI/rC
                CIrx8dSi27HY5GIDBjai+LmKvV3a9fcY6vNZweFl7vDifOZw27J/fOTl+Wyf
                77IDbqVeZ23+/VOGF3ncsM9iGsfTWoYNX0R0yXziXKnCULv2xrVlUxYPGLb+
                U/lH50ekfktMG4E2YeD7Mnw+ojnVk4k2aiw9PVWR6vryaCkM6d8I+pJhpam0
                bE3GXRmeCaphKDWDnvDbIlSxvwgWlieT1OycBpOwJ9+qOFdZzGn/MwV79EKp
                RNZK/GCENfIyhEVCTiudeFvkubH4hOmdS9gXSfrJohio4inZwlUBckQF2Mj/
                bV6j6vjLfwV/f4nCZ6xcJAEL22RLlH5I/zqd4zHhBmE9GbGJHcIDolkl4lIH
                lodbHm57uIO7tMWahzIqHbAI91DtIB3BiXA/QibCeoSN33FlSFUiAwAA
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp＄Companion.class:
                H4sIAAAAAAAA/51TTW/TQBB9a6d2YkJJUz4SoHwGSBHUTQUIqQgJgpAipa0E
                KJce0CZZyib2Gq3XUY858UP4Bz0hcUBRj/woxKwTqLgglcvMm3nzZr0z3h8/
                v30H8AhNhidcDXUih4eh4hN5wI1MVLiXGaHbSfyp0VGKUMTTNA+t4YpKfDCG
                yohPeBhxdRDu9UdiYHy4DN4zqaR5zuA213tlLMELUIDPUDAfZcrwtPt/R24z
                tJrdcWIiqcLRJA6lIoniUfhKfOBZZNqJSo3OBibRO1yPhd5e7wVw7NGrjcEJ
                +T7OWYaN03VjWPkt2BGGD7nhlHPiiUvDZNaUrAEDG1P+UNpok9CwxdCYTYPA
                qTmBUyE0mxaPP7u12XTL2WQv/aJz/MVzKo6t3WK2w4PTzMjHFYa1fyp8rDEs
                /y1jKP0ZLi1zl0/ozkYnUST0xtjQwtrJUDCc60oldrO4L/Q73o8oU+0mAx71
                uJY2XiTLJ90FrTl4m2R6IF5Ly9XfZMrIWPRkKqn4hVKJyb8wRYs2VLBjI+/Y
                v4Vuf4ui0M6R/NL9ryge5fRtsl6efIwG2fK8ACUEQIUROrMQPyTvLMTlo3wn
                VnBxnpwLcnQWy8S5uENRldiruIbrqOfoBvm7+cE3cS9/LzQL0lT24Xaw0kG1
                g1WcJ4gLHep9aR8sRQ114lMEKS6n8H4BDr3L2mwDAAA=
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp.class:
                H4sIAAAAAAAA/5VUW0/cVhD+jvfmNQt4IRcCmyYt23S5BHNLSoE0XFKK6QIp
                pDSE9uGw64LB2NT2ovSlylN+QqT2pVIf+sRDorZQFamiyVt/U1V1jm2WW4S0
                0u45M+OZb74zM+f889+ffwEYxNcM3dwuu45ZfqrZfMdc477p2Np8xTfcSWdr
                O6/bNkkW9zyhpsAY1A2+wzWL22va/OqGUfJTiDEkR03b9D9miBf0jiWGWKFj
                KYMEUgrikBlkUyCNu2sMTM9AQV0aEjLk76+bHkNPsRYiIwx1a4avVzEpnc6g
                lOibYxu230fAJWf7O4Y+4lMrdnvRcde0DcNfdblpexq3bccPojxtzvHnKpY1
                Ig6XVOgMVxgyIlW+bHzDK5bP4BZqS6jrxbM1HamRcwbNuCTYtFKpfWfRd02b
                ynKp0HECOrTS+a6etU1UTKtsuCm8o+CGaFfLafzCUffuyXiXms23tw27zHC7
                cB7+fMYInUi2Iy8SvM+QE225yPED4VgQjpMXO3YKx64McrgupNtUgHXurU86
                ZYMhexyp276xJs7YGw4pTaGGfgV9GKATGd9WuEVzeLnwll48YchfNBI0D3zV
                MqiyCcdfN1yGpvMoxGu0ZEW35G4t3c2LhdvkksKImOjipuMTkraxs6WZdCzX
                5pb2IBy/SWLku5WS77iz3N2kIoUX8Z6CUVDmdBWMYaimITumQXUfw7i4wBNU
                4iM2s4bPy9znRFHa2onRC8PEkhYL6Npvkv2pKTTqgFSmK7p7+OymIrVIiqQe
                PlPoJ6myIslJ2utoj9HeQGb59XO5hVwb+6VeNswaJ+qbkqrUKvXGXv+clNT4
                TFpNCW36zfPYTLMqk3z4rF+WpdCJzIzMaZKVflmta423sF42/eZFjAIzoccL
                RnI9yQ1CXshW4WXK3xqXE2pScO5n4iTXL6xaCg8ZGk6Xjl7NOb5DrfFdx7IM
                t2eTnom2hYrtm1uGbu+YnknzM348UzSi4QA3Fk3bmKtsrRruIzFjYrScEreW
                uGsKPTLWL/q8tDnLtyM9fxb7IXf5lkEUTyXJHNM0SFUWnYpbMqZMAXEtglg6
                R46ujESvOmi9JgaBSvKItCTtl2lvEq+7aDzpicD6BWlT5C3RrnTuI93Z9jvq
                XwUIS7Q2QEzFAGEOUtQAviTtSuhN3xrF/JAkUGncoNI/xNTEWNGe6PwN9btV
                uGRgHAxgMqFDBJMlckfB7WeD2VsD6F0lWBHQRywFp/QBpOW2fVx9WQ0Kyaar
                ZNMR2RNlUdNooXKFuW9FBczm4t//AFkwGO1s20NbCPmY1hiYQKBXLUo/TLug
                ljvAjeV93Gx6bw+3ROQeOtSOPXTvoeflmWPkIkYn28OobNkqj7AGAYM/MHi2
                DHIUz3AHdyMeX9Eu2pXv7PoFifhu19+QfkQittt1CGlWAHXT/ydhiYc9eRy0
                L5aS/0U2RfpxxfLViuUxhI8ozzLJKUHqwyD9MFIR1ZYgKRE7wOgy28f9XzH5
                KrDE8CSYOjFfn2OBijxK0hjtK0H6RaIMkhkeUF8/WUFMx5SOT3VMQycRMzo+
                Q5EcPMxibgWqh0YP8x6UYE16wpL10OSh2cOdwDjkQfOQC+Sx/wHugknKUQkA
                AA==
                """,
        """
                androidx/navigation/OuterComp＄InnerObject.class:
                H4sIAAAAAAAA/41US08TURT+7p0+plMe5SFPxQeolCpTUFcQE2w0DinFCMEo
                q0s7wsB0BmduG5as/AkuXLrQDQuJCxJNTJWdP8p47jAKQiQm7T2POef7znzn
                tj9+fv4K4C7uMeSFVwt8p7ZjeqLprAvp+J652JB2UPLr22OW59nB4tqmXZVp
                MIbcpmgK0xXeuvk7qzGkZh3PkfcZtPH8ShuSSBlIIM2QkBtOyFAo/zfLDIMu
                /SUZON46Q+94vnzMeJSlitGyH6ybm7ZcC4TjhabwPF9GoKFZ8WWl4bpUlT0B
                q6ODgDdEuFHya3Y0qKV5H4qzNLz9qiFcmvLCePn0283kXzCMncdGVGLNtYku
                6csNO2DoPotC1LNVN9LIAFfC6FZlaXmuUnrYhiEYGUoOM3SVt3xJZeaCLUVN
                SEGNvN7UaFdMHRl1gIFtUX7HUVGRvNoUw8vW7ojBB7jBc61dg+vKycZWN1Qq
                16EfvjYGWrvTvMgepHX+/V2K5/h8T04b4sXEtJ5LDiUGWJE9PnyjzWdyKcqm
                yWfk6+RnlK/Yppma4dK5G00jT3elIpol35OB77p2MLklGYafNjzp1G3Lazqh
                Q7rNHWtJt+VoN51lx7MrjfqaHSwrbZWkflW4KyJwVBwn25ekqG4tiO04HjuN
                /UQEom7TRH+RtEW3ouSKMLQpNJb8RlC1HzkKYjCGWDkzHKZoRYlI/UG1MbK3
                KEqRbSebpKfJKLpNkal2pLITB9D3yeGYjItBQCadbUcFyBCUAs1ShkfNV+Nm
                rbvzY/TouFyLy08yk8zoinmPW7v3/tHK0IPemMkiy8n2TxTeI5nYK3wDf4uk
                tldogT9L7EWDF+lMgKf1CKzvqCEGU14ffRm9FNStpt8QOToG/kjRHzUA2S/g
                zw8w+AkX96OEhmk6lY4cE+ggVe9EfAX6W1Kj0Q0jeUZWoVm4bOGKRW93jVyM
                WhjD9VWwEDdwcxVGqD7jIVIheiKnL0QucrJ0/gKE9w8Q7AQAAA==
                """,
        """
                androidx/navigation/OuterComp.class:
                H4sIAAAAAAAA/41RW2sTQRT+ZjaXzWZt03hpYk2rtmpTxW2LT7UINSgsxC20
                JSB5miRLnGQzK7uT0Mc8+UP8B8WHgoIEffNHiWe30SJCcYc935zbd+ac8+Pn
                568AnuExQ02oXhTK3qmjxET2hZahcg7H2o8a4eh9HoyhNBAT4QRC9Z3DzsDv
                6jwMhty+VFK/YDA26y0bWeQsZJBnyOh3MmZYa17J/JzB3O8GKYcFniSarnd8
                cuA1Xtm4BqtAxgWG9WYY9Z2BrzuRkCp2hFKhTrlixwu1Nw4ColpqDkNNZM4b
                X4ue0IJsfDQxqEuWiEIiwMCGZD+VibZNt94OQ302tS1e4RYvzaYWNw3z+wde
                mU13+Tbb40bmZd7k3z7meIknCbssoVlwlaI2AhHHSS8MxdRwMR2GJ1d2vvF3
                ch5r9Ij/yPg9+3u0EU9MGqHSURgEfvR0SDVXjsZKy5HvqomMZSfwDy4HRTtp
                hD2fYbEple+NRx0/OhEUw1Buhl0RtEQkE31utC9f6FOydRyOo67/Wia+6rxO
                658q2KGNZdIxV5MFEm6QliMsEXI62VR7QJqTLIMwu3UO8yx1P5wH02rwiKR9
                EYACUQEmin+Slyk6+YpfwN+ew/6ExbPUYGCTZJncd+mv0TvuE64S1tMS69gi
                3COaJSIut2G4uO7ihoubuEVXLLuooNoGi3EbK21kY1gx7sTIxajFWP0Fraxj
                CzoDAAA=
                """,
        """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRy0oDMRQ9SduxjlWnPusL1IWvhaPiThFUEApVQaUbV2kn
                aOw0gUlaXPZb/ANXggspLv0o8WZ07+ZwHjfh5Obr+/0DwCFWGFaFTjKjkudY
                i756EE4ZHd9J605b1mWi7UbAGKIn0RdxKvRDfN16kt4tMATHSit3wlDY2m5W
                UEIQoogRhqJ7VJZhvfHf5UcM1UbHuFTp+FI6kQgnyOPdfoEKMg+jHsDAOuQ/
                K6/2iCX71H04CENe4yGPiA0H5Y3acHDA99hZ6fMl4BH3cwfMn46uRP/caJeZ
                NJXZbsdRyXOTSIbJhtLyqtdtyexOtFJyphqmLdKmyJTXf2Z4a3pZW14oLxZu
                etqprmwqqyg91dq4/HG2uAZOO/ir7FdCWCMV5xoo7byh/EqEY4EwyM1NLBJW
                fgcwijDPl3Kcx3L+VwxjlFXuUahjvI6JOiYREUW1jilM34NZzGCWcovQYs4i
                +AEAejLS6AEAAA==
                """,
        """
                androidx/navigation/TestAbstractComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5DAHSlkfCuyVILRKdpGJFEVIJQoqUFolW
                2XSBnIkpTmY8yHaiLrPiQ/iDrpBYoKhLPgpxPQmwpZv7Ovfcax/756/vPwA8
                xxOGHaGHJlPDs0iLqToVTmU6OpbW7Q+sMyJ2nSz93PRGaIJKYAy1kZiKKBH6
                NHo3GMnYlRAwFF8qrdwrhmBru1/FCoohCigxFNwnZRlavcut2mNob/XGmUuU
                jkbTNFLaSaNFEr2RH8UkoXZNvEnsMnMgzFiave1+CO5Xrjfjf+CHNEfprpeb
                xrD6h3AgnRgKJ6jG02lA4jFvKt6AgY2pfqZ81qJo2GZozmdhyOs85DWK5rPy
                xZegPp/t8hZ7XSrzi69FXuO+d5f5Cc3/0aaEuwyVvwLRQxyKKZ3bmSxJpNkZ
                OxK7kw0lw/We0vJwkg6kORaDhCprvSwWSV8Y5fNlsdrVWppOIqyV9EThUTYx
                sXyrPNZ4P9FOpbKvrKLmfa0zl5/Lok0qF/zVyXP/0nSDh5RFXgvyK0+/oXye
                w4/IFvPiC2yQrS4aUEEI1BhFV5bkZ+T5klw9z3X1hFuL4oKQR1dxjbAAm5SF
                Oeke7qOBx/nCB2jmf5s0oN7aCYIuVrtY62IdNyjEzS7NvH0CZlFHg3CL0OKO
                RfE3/dAjtxgDAAA=
                """,
        """
                androidx/navigation/TestAbstractComp.class:
                H4sIAAAAAAAA/41RW2sTQRg9s5vrurFJvSXWS2trTPvQbYsgNEWoESGQpqAl
                IHmaJGOdZDMrM5PQx/4W/0HxoaAgwUd/lPjtNrYPvuTlO/Ndzvku8/vP958A
                XmKLYYOrgY7k4CxQfCpPuZWRCk6EsYc9YzXv20Y0/pIFYygO+ZQHIVenwXFv
                KPo2C5chcyCVtK8Z3Npmx0caGQ8pZBlS9rM0DNXWIg3qDLmDfjiX2l6EshEb
                riiVhc+wW2uNIksKwXA6DqSyQiseBm/FJz4JiaCIOenbSB9xPRK6fjXsbQ8F
                LDHkr8UYdhaa+KZ93UcJy3k4uMOw3or0aTAUtqe5VCbgSkU2UTBBO7LtSRjS
                rqV/sx4Jywfccoo546lLn8Jik48NGNiI4mcy9nboNdile87Ofc8pO55TnJ17
                Ts7JVcuz81V3z9lh+8x9k/71NeMUnbh6j8UaxTaf0vpWR2Eo9PbIMqy8nygr
                x6KpptLIXigOb6akj2tEA8Gw1JJKtCfjntAnnGoYlltRn4cdrmXsz4N+Uymh
                GyE3RhDZ+xBNdF+8k3GuMu/T+a9Lao3OlUp2rMTXI1wnL0N4j9AhTCfeBnlB
                fAnC9NYlchdJ+vm8GNhHlax/VYA8PMIcbl2Ty0huCf8HCh/ZJYrfcPciibh4
                QdajugIplmiQWqL9DJuEryh+nxQfdOE2UW6i0sRDrNATj5p4jCddMIOnWO0i
                ZeAZrBlkDEp/AVeTkqlcAwAA
                """,
        """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31Ry0oDMRQ9N7VTHauO7/reqgtHxZ0iaEEoVAWVblylnaCx
                0wQmaXHZb/EPXAkupLj0o8Q7o2s3h/O4CecmX9/vHwCOsEHYkCbJrE6eYyMH
                +kF6bU18p5yvp9K5CogQPcmBjFNpHuLr9pPq+ApKhOBEG+1PCaXtnVYVZQQh
                xlAhjPlH7QhbzX9vPibMNrvWp9rEl8rLRHrJnugNSlyNcpjIAQTqsv+sc7XP
                LDkgbI6GYShqIhQRs9GwNhoein06L3++BCIS+dQh5WejKzmoW+Mzm6Yq2+t6
                7le3iSLMNLVRV/1eW2V3sp2yM9e0HZm2ZKZz/WeGt7afddSFzsXKTd943VMt
                7TSnZ8ZYX+zlcADB6/8Vzl+DscYqLjRQ3n3D+CsTgRXGoDCXscpY/R3ABMIi
                XytwGevFHxEmOaveo9TAVAPTDcwgYorZBuYwfw9yWMAi5w6hw5JD8AOsqUxn
                4AEAAA==
                """,
        """
                androidx/navigation/TestClassComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5DAHSlkfCuxCkFminqdgVIUEQUqS0SKXK
                pgvkJKY4mbGR7URdZsWH8AddIbFAUZd8FOJ6UmCJurmPc++5987x/Pz1/QeA
                53jM8FTooTVqeJJoMVXHwiujk0PpfDsVzrVN9rkZjNCEl8AYaiMxFUkq9HHy
                rj+SA19CxFB8obTyLxmi9Y1eFUsoxiigxFDwn5Rj2OxeYM8uQ2u9OzY+VToZ
                TbNEaS+tFmnyRn4Uk9S3jXbeTgbe2D1hx9LubvRi8LBvtTn4V/yQ5VWGrYtN
                Y1j+Q9iTXgyFF4TxbBqRbCyYSjBgYGPCT1TItikathia81kc8zqPeY2i+ax8
                9iWqz2c7fJu9LpX52dcir/HQu8PChLX/ClPCbYbKX3XoCfbFlI721qSptFtj
                TzK3zVAyXO0qLfcnWV/aQ9FPCVnpmoFIe8KqkJ+D1Y7W0uYLJD1O/N5M7EC+
                VaHWOJhorzLZU05R8yutjc+PcmiRxIXw3eR5eGM6/z5lSRCC/NKTbyif5uUH
                ZIs5+AxrZKuLBlQQAzVG0aVz8iZ5fk6unuaiBsKNBbgg5NFlXKFahIeUxTnp
                Du6igUf5wnto5r80aUC9tSNEHSx3sNLBKq5RiOsdmnnzCMyhjgbVHWKHWw7F
                3/peHcMPAwAA
                """,
        """
                androidx/navigation/TestClassComp.class:
                H4sIAAAAAAAA/4VRXWsTQRQ9s5vPdWOT+pVYP1obta0f2xRBsEXQiBBII2gp
                SJ8myVgn2czIzCT0sb/Ff1B8KChI8NEfJd7dxvbBh7zcM/fMuWfuvfP7z/ef
                AJ5hg2GFq77Rsn8UKT6Rh9xJraI9YV0z5tY29ehLHoyhPOATHsVcHUbvugPR
                c3n4DLkdqaR7yeCvre+HyCIXIIM8Q8Z9lpZhtT3XfZuhsNOLZz6P5urrSeCK
                +DxChsZae6gdlUeDySiSygmjeBy9EZ/4OHZNrawz457TZpeboTDbZ21eDlDC
                AkPx3IzhyfxeL97eDlHBYhEeriRTanMYDYTrGi6VjbhS2qXlNupo1xnHMU1Z
                +dfornC8zx0nzhtNfPoIloRiEsDAhsQfySTbpFO/wVCfHoeBV/UCrzw9DryC
                V50eL/tb3iZ7wfzX2V9fc17ZS7RbLHEod/iEJndGx7EwT4eOYen9WDk5Ei01
                kVZ2Y/Hqokf6rabuC4aFtlSiMx51hdnjpGFYbOsej/e5kUk+I8OWUsKkSxFU
                HHzQY9MTb2VyV5u9s//fK2jQsjLphLVkd4SrlOUIrxF6hNk0q1MWJXsgzG6c
                onCSXt+fiYHHeEAxPBOgiICwgEvnxVWkm0T4A6WP7BTlb7h6kjI+HlIMSFci
                xwo1spZ638M64XPir5PjjQP4LVRbqLVwE0t0xK0WbuPOAZjFXSwfIGMRWKxY
                5CwqfwEoeIqnTgMAAA==
                """,
        """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41QTW/TQBScXSdOYhLihK805ZsKtTngtOIGqgiRkCyFIpUq
                HHLaxJa7jbOWvJuox/wWzlyQQEgcUMSRH4V461ScOCDZ897sjuf5za/f338A
                eI49hj2hojyT0WWgxEomwshMBWexNsNUaP1BmvNBnlTAGPwLsRJBKlQSvJte
                xDNTgcPgvpRKmmOG0n54MGZw9g/GdZRR8VBClbjIEwYW1uHhWg0cdZKac6kZ
                no7+Z/YLmpHEZmBtyDxkaI3mmUmlCt7GRkTCCJLwxcqhlZiFmgXQ0DmdX0rL
                +tRFhwzDzbrt8Q73uL9Ze/Rwv+rxqtPZrI94n71utF2fd3nf+fnR5X7ptPWX
                VUndLVXLvmutjpgd4J+I1TBTJs/SNM6fzQ2tNsyimKE5kio+WS6mcX4mpimd
                tEfZTKRjkUvLrw6999kyn8VvpCU7p0tl5CIeSy3pdqBUZopINA4pt1KxU9vG
                SB2nvgyX8AGxY+Kcqtf7hlpv9ysanwvNQ0KrAXbwiPD2VoXraNqIqLNulCh8
                erdegU2Oarn3BY1P/7SpbwVXNhyPC7yPJ1RfFT9Zxo0JnBA3Q9wKaewdatEJ
                6fvuBExjF3cnqGg0Ne5peAW6Gr5G6w+psmgInQIAAA==
                """,
        """
                androidx/navigation/TestClassWithArgComp＄Companion.class:
                H4sIAAAAAAAA/5VSS29SQRT+Zi6FckWlrQ/wVR+YUBO5hXRXY1IxJiS0Jtrg
                ogszwEgH7p0xcwfSJSt/iP+gKxMXhnTpjzKeuaBu7ea8vvOdM/PN/Pz1/QeA
                PTxlaAk9tEYNzyItZmoknDI6Opapa8ciTT8od3pgR22TfK55IzTBBTCG8ljM
                RBQLPYre9sdy4AoIGPIvlFbuJUNQ3+mVsIZ8iBwKDDl3qlKGve7l1+0zNOvd
                iXGx0tF4lkRKO2m1iKPX8pOYxq5tdOrsdOCMPRR2Iu3+Ti8E92u3aoN/4Mck
                Qxkal5vGsPGHcCidGAonqMaTWUAiMm+K3oCBTah+pny2S9GwyVBbzMOQV3jI
                yxQt5usXX4LKYt7iu+xVYZ1ffM3zMve9LeYn1P9XnwLuMhT/ikQPciRmdHZn
                TRxL25g4Er1thpLheldpeTRN+tIei35Mlc2uGYi4J6zy+apY6mgtbbZH0lOF
                783UDuQb5bHqu6l2KpE9lSpqPtDauOxsKZqkdM5fnzz3L0632KYs8nqQX3v2
                DevnGfyQbD4rNvCIbGnZgCJCoMwourIiPyfPV+TSeaatJ9xaFpeELLqKa4QF
                eExZmJHu4T6qeJItfIBa9s9JA+otnyDoYKODzQ62cINC3OzQzNsnYCkqqBKe
                IkxxJ0X+N9Gij4okAwAA
                """,
        """
                androidx/navigation/TestClassWithArgComp.class:
                H4sIAAAAAAAA/41S308TQRD+9vrrehY5KmIBf6CglqpcITwJIcEa4yWlJkhq
                DE/bdi3bXvfM3rbhkb/FZ1+IGhJNDPHRP8o4d634oA8kdzM7szPfzHyzP399
                /Q5gE+sMZa46OpSdY0/xkexyI0PlHYjI1AIeRW+kOdrV3Vo4eJ8DY3B7fMS9
                gKuu96rVE22TQ4ohuy2VNDsM6bK/2mRIlVebBWSQc5CGTTbXXQbmF+DgSh4W
                ChRqjmTEUKlftv4W1ekKsxtDUQGfwd5uB5PCG5dFWYkFV3SdwzWG9XK9HxpC
                8XqjgSeVEVrxwHsu3vFhYGqhiowetk2o97juC701nuu6g1nMMeQvwBg2Lz3I
                3xa2CihhPiZkgWG5Huqu1xOmpblUkceVCk2CEnmN0DSGQUAUzPzpd08Y3uGG
                k88ajFK0ThaLfCxAZPfJfyxjq0qnDm365flJ0bFKlmO55ycOfZZrO5adLp2f
                LOU2rCp7ynLPpopZ11qwqqkfH7KWm96fubBsSllI2xk3G+NtsLiK2+AjIsno
                MAiEXusbhsX9oTJyIHw1kpFsBWL37xy09VrYEQzTdalEYzhoCX3AKYahWA/b
                PGhyLWN74iz4Sgmd8Cco2XkdDnVbvJDx3fykTvOfKlgnQtM0uIX5mF/qs0JW
                lvRN0sX4EZJOkZ1JvI/I2qFoi7RTOUO+svgFU6cJwuNJJrCGJyTnxlG4iumY
                aDrFaLQXuPSPsbyYf9KZymdMffwvTGEcMIGxqancJLmEZIMofMPsW3aGG5+w
                eJp4UpQbF2T0+KxkMC/BXkWVdI38twjx9iFSPu74WPJxF/foiGUfK7h/CBbh
                AR4ewo4wHaEcwUlkNoIbYSZC6Tedt709GAQAAA==
                """,
        """
                androidx/navigation/TestGraph.class:
                H4sIAAAAAAAA/31SQWsTQRT+ZpJsNttoY6s2sdaq7UE9uG3xZhFqUFmIK9gQ
                kJ4m2SGdZDMju5PQY07+EP9B8VBQKKHe/FHimzXoQXAW3nvfN998zHuzP35+
                vQTwDLsMW0InmVHJWajFTA2FVUaHXZnbN5n4eFoFY2iMxEyEqdDD8F1/JAe2
                ihKDd6i0si8YSo8e9+qowAtQRpWhbE9VzrDd+a/zcwb/cJAWHgG4O+hH8XH3
                KG6/quMaghqR1xl2OiYbhiNp+5lQOg+F1sYWXnkYGxtP05SsbnTGxpJZ+FZa
                kQgriOOTWYm6ZC7UXAADGxN/phzaoyrZZ9hdzIOAN3nAG1Qt5v73T7y5mB/w
                Pfay6vOrzx5vcKc9YM6hEYtZ22ibmTSV2dOxZdh8P9VWTWSkZypX/VQe/b0j
                jaNtEsmw2lFaxtNJX2ZdQRqGtY4ZiLQnMuXwkgyOzTQbyNfKgdbSuPePLfZp
                OuWipZYbFuV7hDx3QcqcvkqBtgmFrnHKlScX8M+L7ftLMbCOBxTrvwWokRXg
                Y+XP4Q1Su7XyDfzDBepfsHpeEBwPi7iFneJfokcgg7UTlCKsR7gZ4RZuU4mN
                CE20TsBy3MEm7ecIctzN4f0CEKx6togCAAA=
                """,
        """
                androidx/navigation/TestInterface.class:
                H4sIAAAAAAAA/4WOz0rDQBDGv9lo08Z/qVqoR/Fu2tKbJykIgaqg4iWnbbIt
                22x3IbsNPfa5PEjPPpR0Ux/AGfjmmxn4zfz8fn0DGKNHuOW6qIwsNonmtVxw
                J41OPoR1qXaimvNchCBCvOQ1TxTXi+R1thS5CxEQutPSOCV18iwcL7jjDwS2
                qgMPp0Y6jYBApZ9vZNMNvCuGhN5u245Yn0Us9m7e321HbEDNckS4m/77lb/k
                wfELrydGu8ooJar70hGid7OucvEklSDcvK21kyvxKa2cKfGotXEHmG35UzjC
                XzBcHfQS174OPfjYZytDkCJM0U7RQeQtTlKc4iwDWZzjIgOziC26e5qGvyhR
                AQAA
                """,
        """
                androidx/navigation/TestObject.class:
                H4sIAAAAAAAA/32Sz2sTQRTHvzNJNptttLH+aGK1VtuDenDb4s0i1KCwEFew
                IVB6mmSHOMlmBnYnS485+Yf4HxQPBQUJevOPEt+sUQ+Cu/De+37nzYeZt/v9
                x6cvAJ5ij2Fb6CQzKjkPtSjUWFhldNiXuX0znMiRrYMxtCaiEGEq9Dj87VYY
                vCOllX3OUHn4aNBEDV6AKuoMVftO5Qw7vf+jnzH4R6O0hATgbqcfxSf947j7
                sokrCBpkXmXY7ZlsHE6kHWZC6TwUWhtbwvIwNjaepymhrvWmxhIsfC2tSIQV
                5PFZUaF7MhcaLoCBTck/V07tU5UcMOwtF0HA2zzgLaqWC//be95eLg75PntR
                9/nXDx5vcdd7yByhFYuia7TNTJrK7MnUMmy9nWurZjLShcrVMJXHf89I8+ia
                RDKs95SW8Xw2lFlfUA/DRs+MRDoQmXJ6ZQYnZp6N5CvlRGcFHvyDxQFNp1pe
                qeOGRXmblOcOSJnTWyvVPVKhuzjl2uNL+Bfl8s6qGbiJ+xSbvxrQIBTgY+3P
                5k3qds/aZ/DTSzQ/Yv2iNDgelPEudsu/iT4CATbOUIlwPcKNiNC3qMRmhDY6
                Z2A5bmOL1nMEOe7k8H4CjO1ti4oCAAA=
                """
    )
