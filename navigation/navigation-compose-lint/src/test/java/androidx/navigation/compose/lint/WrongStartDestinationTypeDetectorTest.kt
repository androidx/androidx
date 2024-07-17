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

package androidx.navigation.compose.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class WrongStartDestinationTypeDetectorTest : LintDetectorTest() {

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClass())
                }
                """
                    )
                    .indented(),
                byteCode,
                Stubs.Composable
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClassWithArg(15)) {}
                    NavHost(navController = controller, startDestination = classInstanceRef) {}
                    NavHost(navController = controller, startDestination = classInstanceWithArgRef) {}
                    NavHost(navController = controller, startDestination = TestClass::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass(15)) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClass(true)) {}
                    NavHost(navController = controller, startDestination = AbstractChildClass(true)) {}
                }
                """
                    )
                    .indented(),
                byteCode,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = Outer) {}
                    NavHost(navController = controller, startDestination = Outer.InnerObject) {}
                    NavHost(navController = controller, startDestination = TestObject) {}
                    NavHost(navController = controller, startDestination = TestObject::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerObject::class) {}
                    NavHost(navController = controller, startDestination = InterfaceChildObject) {}
                    NavHost(navController = controller, startDestination = AbstractChildObject) {}
                }
                """
                    )
                    .indented(),
                byteCode,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClass) {}
                    NavHost(navController = controller, startDestination = TestClassWithArg) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClass) {}
                    NavHost(navController = controller, startDestination = AbstractChildClass) {}
                }
                """
                    )
                    .indented(),
                byteCode,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClass) {}
                                                           ~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassWithArg) {}
                                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = Outer.InnerClass) {}
                                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = InterfaceChildClass) {}
                                                           ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = AbstractChildClass) {}
                                                           ~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
            """
            )
    }

    // Stub
    private val sourceCode =
        """
package androidx.navigation

import kotlin.reflect.KClass
import kotlin.reflect.KType
import androidx.compose.runtime.Composable

// NavHost
public open class NavHostController

@Composable
public fun NavHost(
    navController: NavHostController,
    startDestination: Any,
) {}

val classInstanceRef = TestClass()

val classInstanceWithArgRef = TestClassWithArg(15)

val innerClassInstanceRef = Outer.InnerClass(15)

object TestGraph

object TestObject

class TestClass

class TestClassWithArg(val arg: Int)

object Outer {
    data object InnerObject

    data class InnerClass (
        val innerArg: Int,
    )
}

interface TestInterface
class InterfaceChildClass(val arg: Boolean): TestInterface
object InterfaceChildObject: TestInterface

abstract class TestAbstract
class AbstractChildClass(val arg: Boolean): TestAbstract()
object AbstractChildObject: TestAbstract()

"""

    private val byteCode =
        bytecode(
            "libs/StartDestinationLint.jar",
            kotlin(sourceCode),
            0x5341e714,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uISTsxLKcrPTKnQy0ssy0xPLMnM
                zxMS9kss88gvLnHOzyspys/JSS3yLuES5eJOzs/VS61IzC3ISRViC0ktLvEu
                UWLQYgAArkTi/VsAAAA=
                """,
            """
                androidx/navigation/AbstractChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRh9M7sssAVZsCql/qi1NZQYoY03TSMlMZJgD7Xh
                AKeB3dAJy26yM5Ae+Vs8ezHRmHgwxKN/lPGbpRoPHDzMm+998/K+b97PX9++
                A3iBQ4anIvKTWPrXzUgs5ERoGUfN9kjpRIx150qGficUSmXBGPY2aS8Dpf/o
                s7AYnFcykvqUwa4PjvoMVv2oX0AGWRc2csRFMmFggwJcbOXBUSCpvpKKod77
                v21e0pRJoNvGiOwHDOXeNNahjJrvAi18oQVJ+Gxh0TeZgbwB0Ngp9a+lYS2q
                /GOGs9Wy4vIqT89q6XJvy+U5q7panvAWOytWHI/XeMv68cHhnn1R/stypK7Z
                uYznGKcThv2N6/8bEG1FS2yfi8XbWOlOHOkkDsMgeT7VFEIn9gOGUk9Gwfl8
                NgqSSzEKqVPpxWMR9kUiDb9puu/jeTIO3khDdi7mkZazoC+VpNd2FMU6na5w
                TAnb6d8rJnCqONUZOIR7xE6Jc7rdxlfkG7tfUPyUah4TGg3wDPuEd9cq3ELJ
                REmVcaPk4dFZezVNwnRnGp9R/LjRprAW3NhwPEnxEQ7ofp0umcHtIawutru4
                06Wx96hEtYsd1IZgCru4P0RWoaTwQMFVeKjgKHgK5d/NuJIe2QIAAA==
                """,
            """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41SXWsTQRQ9M/nabKOttdrEqq2toBV02+KbRYhBcSGuYENA
                +jSbHdppNjOwMwl9zJM/xH9QfCgoSNA3f5R4d40fYB/cZe+dc+fMudwz++37
                x88AHuMuwz2hk8yo5DTQYqKOhFNGB+3YukwMXOdYpcnr+EQOXA2MYeMick9a
                9+tADSWG6r7Syj1lKN3f7jdQQdVHGTWGsjtWlmG7+589nzB4+4O0UPPBcwkv
                jA567ajzvIFL8OtUvMyw1TXZUXAiXZwJpW0gtDauULVBZFw0TlOSutIdGkdi
                wSvpRCKcoBofTUrkBMtDPQ9gYEOqn6oc7dAq2aUGs6nv8yYvvtnU+/qON2fT
                Pb7DntU8/uV9lS/xnLrHsHnhcH97RG1XIjF5aazrGO0yk6YyezR0DGtvxtqp
                kQz1RFkVp7L9ZxByr2MSybDYVVpG41Ess54gDsNy1wxE2heZyvG86B+YcTaQ
                L1QOWnPh/j+y2CULy8XcrdxRyrcJVSkvUeb0Vgq0TijI3aFceXAO76zY3piT
                gYe4Q7Hxk4A6SQEeFn4fXiV2/ix8An97jsYHLJ4VBY7NIt7CVvFT0k2RwPIh
                SiGuhlgJcQ3XaYnVEE20DsEsbmCN9i18i5sW1R9Ulqy20QIAAA==
                """,
            """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41QTW9SQRQ9M3w8eKXyoFop9atWLbDw0cadprElMZJgTWrD
                AlYDb6RTHu8lbwbSJb/FtRsTjYkLQ1z6o4x3KGli0oWLufeeO2fOvXN+//nx
                E8AL7DHsiShIYhVc+pGYqZEwKo78dmRk8lEMZetchUErFFo7YAzehZgJPxTR
                yH8/uJBD4yDFsHOTxJnU5lrGQYYh+0pFyhwypGu9epchVat3C3CQd5GGS1gk
                IwbWK6CA9Tw4bhHVnCvNUO/855YvacxImiOrRPo9hlJnHJtQRf47aUQgjCAK
                n8xS9H9mQ94G0Nwx9S+VRU2qgn2G48W87PIKX57F3OXemstzqcpifsCb7Hi9
                nPV4lTdTvz5luZc+LV2jHLGr6VzGy1qlA4bdG/f/xyJai7a4fSJmb2NtWnFk
                kjgMZfJ8bMiGVhxIhmJHRfJkOhnI5EwMQuqUO/FQhF2RKItXTfdDPE2G8o2y
                YOt0Ghk1kV2lFd0eRVFsluM19snjNA3N0ilb0+nvnGoHOYqPCR0S5pTdxnes
                Nba/ofhlydmlaF8BNTyhuHnFgoeSdZMqq0bmk+7GSsu3JlPONL6i+PlGmcIV
                YSXD8XQZd/CM8mvrDt3d6SPVxmYbd9uoYItKVNvYxr0+mMZ9POjD0ShpPNQo
                aDzSyGmUNTb+AhJd9mP1AgAA
                """,
            """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41SwW7TQBB9u0kTxw00LQUSCpRSQG0PuK24USGVCISlYCQa
                RUI9beIl3cTZlexN1GNOfAh/UHGoBBKK4MZHIWZNKAeQwNbOzHs783Zn7G/f
                P34G8AgPGLaEjlOj4tNAi4nqC6uMDkJtZfpW9GTzRCXxq+5A9mwZjKE2EBMR
                JEL3g19sgWHjbxptmdkLnTIWGEoHSiv7hKGwtd2pogzPRxEVhqI9URnDTut/
                7/KYwTvoJbmcD+40vDA6ah9GzWdVLKFaIbLGsNkyaT8YSNtNhdJZILQ2NpfN
                gsjYaJwkJLXcGhpLYsFLaUUsrCCOjyYFGhFzpuIMGNiQ+FPl0C5F8R4dMJv6
                Pq/zfM2m3td3vD6b7vNd9rTs8S/vS7zGXeq+u8s/p0TnrkZi8sJktmm0TU2S
                yPTh0DKsvR5rq0Yy1BOVqW4iD393QvNrmlgyLLWUltF41JVpW1AOw0rL9ETS
                EalyeE76R2ac9uRz5UBjLtz5QxZ7NMMitV2i1XBDJX+Hend4hTynl74hoQ1C
                gRsQ+YWdc/hn+fbdeTKwjU2y1Z8JWKQIVHjpovg6Zbtn8RP4m3Nc/oDls5zg
                uJfbddzPf1iGKySweoxCiKshroVUWqcQjRA3sHYMluEmbtF+hmqG2xm8H3Jw
                I2btAgAA
                """,
            """
                androidx/navigation/NavHostController.class:
                H4sIAAAAAAAA/41Ru0oDQRQ9d2I2yRpN1KjxBVqIj8JVsVMEFcRAVFBJYzXJ
                LnGSzQzsTkLKfIt/YCVYSLD0o8S7q52NzeE87nAf8/n19g7gCGuETan9yCh/
                6Gk5UG1pldHejRxcmdheGG0jE4ZBlAMRyh05kF4oddu7bXaCls0hQ3BOlFb2
                lJDZ3mkUkYXjYgI5woR9UjFhq/6vDseEmXrX2FBp7zqw0pdWsid6gwyPSgkU
                EgCBuuwPVaL2mfkHhPXxyHVFVbiizGw8yi9Ux6NDsU/n2Y9nR5RFUndIyevK
                n9Z7XcvTXhg/IJTqSgc3/V4ziB5kM2Rntm5aMmzISCX613TvTT9qBZcqEUt3
                fW1VL2ioWHF6prWx6ZYxNiD4GL9jJ7dhrLLyUg1kd1+Rf2EisMTopGYBy4zF
                nwJWbpqvpLiI1fTnCJOcFR+RqWGqhukaSigzxUwNs5h7BMWoYJ7zGG6MhRjO
                N+LuMN72AQAA
                """,
            """
                androidx/navigation/NavHostControllerKt.class:
                H4sIAAAAAAAA/41UXVPbRhQ9Kxt/KAZkE4NtAgngEANtRCj9hKalTlPUOKaT
                ZOhkeFrkrSOQpY5W9uSR9/6L/oKmPKRTZjpMH/ujOr0rnJhY0OAZ6969e8+5
                R1e7959///wLwDp2GO5wrxX4Tuul6fGe0+ah43tmk/e2fRnWfS8MfNcVwaMw
                DcZgHPAeN13utc2d/QNhUzTBkO6nM2zWGlei22gME20s7TLcHqBtv/OzL4UZ
                dL3Q6QizHq35vis2GBYaftA2D0S4H3DHkyb3PD+MSkmz6YfNrutS1igpGJTM
                IMswe+iHruOZB72O6XihCDzumhalEItjyzSuMRTtF8I+7NP8wAPeEZRIjarF
                RZ+LPFUkbXqNHEYxpiOHceqXDHkQPhAydLxIXwZ5YrpikxgK8ZIME20R1l0u
                peURvWeLJ+Inhrna0oW0z6h4lE1Iw47Bbr4HlMMUSlloKDNUhgv/6IQvtoJ2
                RFR7X/1+MsmYsi8jWbwaRQ4zmFWibjKUSJTleSKIt+QySTtd+qDVAYgkFZ2L
                KS4WFCPIYQFVJeg2Q2bTpiPmhPcZEjV1qmf+953SWGJIbUaIHFZQ07GMDxiq
                V+lEGncZkjVLnboVrOowce8S6LDmND7SaQJQer7RvxWPRchbPOTUD63TS9CE
                YOqRVQ8wsEPlaLT50lHeKnktwv9yerSsnx7pWknTtUwibg3tzbYyxulRpWTk
                Ktrq6FrKGCM7TtYgm99Oz+fVtrbK/v41laH0SjKjGQmKJik4MgimDJWaoWB2
                ENSNa0rSGlNqy5c2IY06gz7oBMP12N27e0jTbPrJ2fCxvJ4jHRo9W4M5Q22v
                +y3BMN5wPNHsdvZF8EyNJ3VpfZu7uzxw1LofrA5zvZ0s75DmBrIELfWnfjew
                xUNHUZT7FLsxMbhHRy9JnySBirqv9Prf0SpFNkO2oq5LLEYndig2gjL9U8Sy
                Tast4tTIji4X9D9grBQK9HylTgCsPohmHL4nf/IsEVlMqKNCXh7Xz5GnyT5S
                ca1f6exZRjGCqmo3KKZ+yd9R+S3KeFMERHZeZRnTlB1D3RpGTQyh5jAfRy0O
                o4rvoDK4gxr5ClVHdA+wcILl56/x4TEqJzCfG+OvsXaMWydYj/yPj7H46i3p
                WATKQyc5k0SeQIPWOu2u4wG+JVmPo44+RJMsp/gn9Ak+3UPCwmcWPrfwBTYs
                bOJLC/fx1R6YxNfY2kNBYkqiJFGUmJSYkZiVmJa4IbEgUZWYk5iXGJH4RiIr
                MSGRJ/8/37G4ovoHAAA=
                """,
            """
                androidx/navigation/Outer＄InnerClass.class:
                H4sIAAAAAAAA/41U308cVRT+7uyv2WGBWaAtP9ZWy4q7S9sBbLUWWgUUGVyW
                CoZY8eWyOy4DwwzO3CX1xfDUP6GJvpgY41Mf2kTB2MRg++bfZIzn7kx361IJ
                ycw955455zvfPefc+euf3/8AcB1rDHnu1nzPrt03XL5v17mwPddYaQjLz5uu
                a/nzDg+CFBiDvs33ueFwt26sbG5bVZFCjCE5Y7u2uMMQL5jFdYZYobieQQIp
                DXGoDKotUWb9OgMzM9DQlYaCDPmLLTtgGCufhcA0Q1fdEmYLi9KYDFrV293z
                XMsVkwRY9fa+YSgSj7NijpY9v25sW2LT57YbGNx1PdH0DoyKJyoNx5mWh0lq
                xPk8Q0amyNesr3jDEQybhbMlMs1yZ+2mz8gxg34MyOzDVErhrQnfdun4A4Xi
                S5Chlc5zodM217CdmuWncFHDJdmOgTZ24UVnbqt4gxrJ9/Yst8ZwtXAS+mS2
                CJkIjiIvwd9kyMnSn+b4lnQsSMf50x1L0nE8gxxek9pVOvwWD7bmvZrFkG1H
                mq6w6vJ8E+EA0oQZmNIwibfpRNbXDe7QjJ0rvKL+X9Dsn9Z+6j3fdCyqasIT
                W5bP0HcShciUdzzh2K6xbAle44KTTdndj9H9YnJJywU0/Dtkv2/LHXFVajSw
                Px0fXNSUQUVT9OMDjR5FVzVFTZLsIhkj2aM+e6AOHh9MKRNsrrsvqSvDykTs
                2Y9JRY8vpfWU3C0+fxBb6tdV0slRVZXQicyMzGnStSlV7xqOD7IJtvj8YYwC
                M6HHQ0Z6N+k9Ul/NtuBVojMcVxN6UnKdYvIEQ/87sCks0F1sTxaNWYXvL3qB
                mPdc4XuOY/nXdujCxMMG9pZt16o0djct/zNZY1lar8qdde7bch8ZR1YbrrB3
                LdPdtwObTLPt/jB0rwle3Vnme5F3vtP7Lvf5rkX0/hOWadO0aKuteQ2/ai3Y
                EmIoglg/kY4GSqH/mSxDn/yHkaaSTn8GWpdot0DfFZJa6Qjp0siv6H5COwWf
                0NoD2fXLFD+KNMky7c6H3vStV84HaRKVxgk6vSGmIceGZKL0C7ofteCSTeNo
                EyYTOkQwWSL3Ini0M5i9MoD+LgQrAyaJpeSUfgrl3sgRLjxuBYVk0y2y6Yjs
                csTmHKCnMYihKPdYVKxsLv7td1Alg5nSyCFGQsgKrTEwiUD3O0p/i6SklnuK
                S/eO8Hrf5UOMychDFPXiIa4c4trjjmPkIkYv8aDVaNVgLKpBk8FvuN5ZBjWK
                Z7iBdyIeX5KU7cqXxn9GIv5o/E8o3yMRezR+DGVZAl2h9wdpiYc9qTTbF0up
                fyObon27YvlWxfK4ifcozwrpKUnq3WYN7jZD6YrhYyxS+T5tAppYJfk52W9R
                p6Y3EDMxY+K2iTt4n1R8YGIWcxtgAebx4QZ6A/l8FEBrrskAeoBsgL4A/QFu
                NI03AxgBcqT/CyhrMdv+BwAA
                """,
            """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41US08UQRD+umcfs7M8loc8FVEWeSmzIJ4gJkg0DFkWIwSj
                nHp3RxgYZnSmd8ORkzevHjx68MRB4oFEE4MSL/4oYvUwCkIwJrvdX1VX1Vfz
                Vc/8PP78FcAU7jEMCq8a+E51x/RE3VkX0vE9c6km7SBveZ4dLJU37YpMgzHk
                NkVdmK7w1s3fXo0hNeN4jrzPoA2PrDYgiZSBBNIMCbnhhAxDxf9imGbQpb8s
                A8dbZ2gfHimesp14KWKg6Afr5qYty4FwvNAUnufLqGBolnxZqrkuRWXPlNXR
                RIU3RLgx51ftqElL+zF1/IYat1/VhEsdXhkunn+y6ZHnDPl/sRGVKLs20SV9
                uWEHDK0XqxD1TMWN9DHAlSi6VVpemS3NPWxAD4wMOXsZWopbvqQwc9GWoiqk
                oES+XddoRkwtGbWAgW2Rf8dRVoFQdYLhxeFun8G7uMFzh7sG1xXIxrtuKFeu
                ST96bXQd7k7yAnuQ1vn39yme4wttOa2HFxKTei7Zk+hiBTZ/9FZbyORS5E0T
                ZoR1whmFFdskUz10XzrNNEZocCVRn/dDOed7MvBd1w7GtyRD75OaJ51t2/Lq
                TuiQbrOnWtJNOZlNc9Hx7FJtu2wHK0pbJalfEe6qCBxlx87GZSkqW4viZWzn
                z9d+LAKxbVNHf5E0RLdizhVhaJNpLPu1oGI/clSJ7rjE6oXmMEEjSkTqd6uJ
                0X6brBTtjbQn6TQZWXfIMtWMlHf0APo+AY7xOBjop2Og4SQAGSqlimbJw6Pk
                G3Gy1tr8MTo6Ddfi8LPM9DqiJeY9TW3duySVoQ3tMZNFO6e9c3TsA5KJvbFv
                4O+Q1PbGDsGfJvaixgu0JsDTelSs4yQhLqZQB/0ZqQN1q+kdIqCj648UnVEC
                kP0C/uwA3Z9wdT9yaJikVenIMYomUvVuxDdGnyPVGsM1kqdvDZqF6xb6LXq6
                mwQxYCGPwTWwELcwtAYjVL/hEKkQbRHoCJGLQJbWX0Nhj3HkBAAA
                """,
            """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VRXWsTQRQ9M5uPzSbaNFabGNuqTbWp4LbFp1qEGhQXYgq2
                BCRPk2SIk2xnYXcS+pgnf4j/oPhQUJCgb/4o8e42mgcp7rD3zP06d+69P399
                +QbgGZ4wVITuh4Hqn7taTNRAGBVo93hsZJgFYygOxUS4vtAD97g7lD2ThcWQ
                OVRamRcM1na9XUAaGQcpZBlS5oOKGKrNa1mfM9iHPT/Jd8DjJNtrnZwetRqv
                CrgBJ0fGmwybzSAcuENpuqFQOnKF1oFJeCK3FZjW2PeJark5CgyRuW+lEX1h
                BNn42cSi7lgscrEAAxuR/VzF2i7d+nsM9dm04PAyd3hxNnW4bdk/PvLybLrP
                d9kBt1Ivszb//inDizxO2GcxjeNpLcOGLyJqMp8oV1NhqF3bcW2RlMUGw9Z/
                Iv/M+QHDSktM3gSRaQTahIHvy/DpiGpV3421UWfS0xMVqa4vjxbDoR00gr5k
                WGoqLVvjs64MTwXFMJSaQU/4bRGqWJ8bC4vXSUp2ToJx2JOvVeyrzOu0/6mC
                PdpSKhltJV4aYY20DGGRkNNJJ9oWaW68AML0ziXsi8T9aB4MbOAxycJVAHJE
                BdjI/01epej4y38Ff3+JwmcsXSQGC9skS+S+T/8aveMh4TphPSmxiR3CA6JZ
                JuJSB5aHWx5WPNzGHbpi1UMZlQ5YhLuodpCO4ES4FyETYS3C+m+Jq/qyJgMA
                AA==
                """,
            """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRy0oDMRQ9SduxjtXW+qovUBfiAxwVd4qgglioCirduEo7
                QWOnCUzS4rLf4h+4ElxIcelHiTejezeH87gJJzdf3+8fAA6wzLAidJwaFT9H
                WvTVg3DK6OhOWnfSsi4VbTcCxlB5En0RJUI/RNetJ+ndHENwpLRyxwy5jc1m
                CQUEIfIYYci7R2UZ1hr/XX7IMNnoGJcoHV1KJ2LhBHm8289RQeZh1AMYWIf8
                Z+XVLrF4j7oPB2HIazzkFWLDQXG9Nhzs8112Wvh8CXiF+7l95k9PX4n+hbHu
                zGiXmiSR6U7HUdEzE0uGckNpedXrtmR6J1oJOdWGaYukKVLl9Z8Z3ppe2pbn
                yov5m552qiubyipKT7Q2Lnugza+C0x7+avu1ENZIRZkGCltvKL4S4ZgnDDJz
                GwuEpd8BjCLM8sUM57CU/RfDGGWle+TqGK9joo4yKkQxWUcVU/dgFtOYodwi
                tJi1CH4AV4wNbewBAAA=
                """,
            """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31Ry0oDMRQ9N7VTHavW+qrvrbpwrLhTBBXEQlVQ6cZV2gka
                O01gkhaX/Rb/wJXgQopLP0q8M7p2cziPm5ubm6/v9w8AB1gnrEsTp1bHz5GR
                A/0gvbYmulPOnyXSuRKIUHmSAxkl0jxE1+0n1fElFAjBkTbaHxMKW9utMooI
                QoyhRBjzj9oRNpv/dj4kzDa71ifaRJfKy1h6yZ7oDQo8GmUwkQEI1GX/WWdq
                j1lcJ2yMhmEoaiIUFWajYW003Bd7dFr8fAlERWRV+5Sdnb+SgwvLd1rjU5sk
                Kt3tep7xzMaKMNPURl31e22V3sl2wk61aTsyaclUZ/rPDG9tP+2oc52J5Zu+
                8bqnWtppTk+MsT5/m0MdglfwN3S2EcYaqyjXQHHnDeOvTASWGYPcXMUKY/m3
                ABMI83w1xyWs5f9EmOSsfI9CA1MNTDcwgwpTzDZQxdw9yGEeC5w7hA6LDsEP
                tqQsxuQBAAA=
                """,
            """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41Qz2sTQRh9M5tskjUxm1g1Ta31R5E2Bzct3pRiDIgLsUIt
                8ZDTJLuk02xmYWcSeszf4tmLoAgeJHj0jxK/2RRPHoTd931v5vG++d6v399/
                AHiGfYZ9oaIsldFVoMRSToWRqQrOY236idD6gzQXvWxaAmPwL8VSBIlQ0+Dd
                +DKemBIcBveFVNKcMBQOwsMhg3NwOKyiiJKHAsrERTZlYGEVHm5UwFElqbmQ
                muHJ4H9mP6cZ09j0rA2ZhwyNwSw1iVTB29iISBhBEj5fOrQSs1CxABo6o/Mr
                aVmXuuiIob9eNT3e4h731yuPPu6XPV52WuvVMe+yV7Wm6/M27zo/P7rcL5w1
                /rIyqduFctF3rdUxswO2TsXyTUrPTZXJ0iSJs6czQ+v10yhmqA+kik8X83Gc
                nYtxQifNQToRyVBk0vLrQ+99usgm8WtpyfbZQhk5j4dSS7rtKZWaPBaNI8qu
                kO/VtFFSx6kvwiXcI3ZCnFP1Ot9Q6ex8Re1zrnlAaDXALh4S3tmocBN1GxN1
                1o1ShU//xiuw6VEtdr6g9umfNtWN4NqG41GO9/GY6sv8kUXcGsEJsRXidkhj
                71KLVohttEdgGju4N0JJo66xq+Hl6Gr4Go0/oUSxVaECAAA=
                """,
            """
                androidx/navigation/TestGraph.class:
                H4sIAAAAAAAA/31SXWsTQRQ9M/nabKONtZrEWqu2D60Pblt8swg1+LEQV7Ah
                UPo0yQ7pJJsZ2Z2EPubJH+I/KD4UFCTomz9KvLMGfRDcgXvvOXPmMPfu/Pj5
                +SuAJ9hh2BQ6To2KLwItZmoorDI66MrMvkrF+/MKGEN9JGYiSIQeBm/7Izmw
                FRQYykdKK/uMobC716uhhLKPIioMRXuuMoatzn+dnzJ4R4Mk9/DB3UEvjE66
                x1H7RQ3X4FeJvM6w3THpMBhJ20+F0lkgtDY298qCyNhomiRkdaMzNpbMgjfS
                ilhYQRyfzArUJXOh6gIY2Jj4C+XQPlXxAcPOYu77vMl9XqdqMfe+f+DNxfyQ
                77PnFY9/+1jmde60h8w5rEdi9tpktm20TU2SyPTx2DJsvJtqqyYy1DOVqX4i
                j//ek0bSNrFkWO0oLaPppC/TriANw1rHDETSE6lyeEn6J2aaDuRL5UBradz7
                xxYHNKFi3lbLDYzyPUJlynXKnFYpR1uEAtc85dKjK3iX+fb9pRho4AHF2m8B
                qmQFeFj5c7hBavetfAE/vULtE1Yvc4LjYR43sZ2/J/oRZLB2hkKImyHWQ9zC
                bSrRCNFE6wwswx1s0H4GP8PdDOVfjjOBnYwCAAA=
                """,
            """
                androidx/navigation/TestInterface.class:
                H4sIAAAAAAAA/4WOz0rDQBDGv9lo08Z/qbZQj+LdtKU3TyKIgaqg4iWnbbIt
                22x3IbsNPfa5PEjPPpS4qQ/gDHzzzQz8Zr5/Pr8ATNAnXHFdVEYWm0TzWi64
                k0Yn78K6VDtRzXkuQhAhXvKaJ4rrRfIyW4rchQgI3WlpnJI6eRKOF9zxWwJb
                1YGHUyOdRkCg0s83sumG3hUjQn+3bUdswCIWezcf7LZjNqRmOSZcT//9yl/y
                4N4zrx+NdfdGu8ooJaqb0hGiN7OucvEglSBcvq61kyvxIa2cKXGntXF7oG35
                czjAXzBc7PUcPV9HHn7os5UhSBGmaKfoIPIWRymOcZKBLE5xloFZxBbdX1Yg
                D5FVAQAA
                """,
            """
                androidx/navigation/TestObject.class:
                H4sIAAAAAAAA/31SQWsTQRT+ZpJsNtto01ptYrVW20P14LbFm0WoQXEhrmBD
                oPQ0yQ5xks0M7E5Cjzn5Q/wHxUNBQYLe/FHimzXqQXAX3nvfN9/72Pd2vv/4
                9AXAE+wxbAudZEYlF6EWMzUUVhkddmVu3/RHcmCrYAyNkZiJMBV6GP5mSwze
                sdLKPmMo7T/s1VGBF6CMKkPZvlM5w07n/9ZPGfzjQVqYBOCu04/i0+5J3H5R
                xzUENSKvM+x2TDYMR9L2M6F0HgqtjS3M8jA2Np6mKVmtdcbGkln4WlqRCCuI
                45NZieZkLtRcAAMbE3+hHDqgKjlk2FvMg4A3ecAbVC3m/rf3vLmYH/ED9rzq
                868fPN7gTnvEnMNGLGavTG7bRtvMpKnMHo8tw9bbqbZqIiM9U7nqp/Lk73fS
                TtomkQyrHaVlPJ30ZdYVpGFY75iBSHsiUw4vyeDUTLOBfKkcaC2Ne//Y4pA2
                VC7GarmFUd4m5FFuUOb0Vgp0j1DohqdceXQF/7I43lmKQe33KdZ/CVAjK8DH
                yp/mTVK7Z+Uz+NkV6h+xelkQHA+KeBe7xY2iH0EG6+coRbgRYSPCTdyiEpsR
                mmidg+W4jS06zxHkuJPD+wldvRYbjgIAAA==
                """
        )

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}
