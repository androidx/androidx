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
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class WrongNavigateRouteDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WrongNavigateRouteDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongNavigateRouteDetector.WrongNavigateRouteType)

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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
15 errors, 0 warnings
                """
            )
    }

    private val sourceCode =
        """

package androidx.navigation

public open class NavController {

    public fun navigate(resId: Int) {}

    public fun navigate(route: String) {}

    public fun <T : Any> navigate(route: T) {}
}

object TestObject

class TestClass

class TestClassWithArg(val arg: Int)

val classInstanceRef = TestClass()

val classInstanceWithArgRef = TestClassWithArg(15)

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
        compiled(
            "libs/StartDestinationLint.jar",
            kotlin(sourceCode).indented(),
            0xa3960069,
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
                /66asKniVDtwCQ+InRLndHvNryg0976g9CnTPCY0GuApDgnvbVS4hbKJkSrj
                RqnDp7Pxapl06Xaan1H6uNWmuBHc2HA8yXCfXoE32ZIObo9g9XCnh7s9Gnuf
                StR62EV9BKawhwcj5BTKCg8VPIVHCq6Cr1D5DV3dCrfVAgAA
                """,
            """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41STW/TQBB9u0kcxw00lI8mFGhpQVAOuK24USGFCCRLwUg0
                ioR6WturdhtnV7I3UY858UP4BxWHSiChCG78KMTYhA+JHrDlmX2zb99o3vrb
                94+fATzBPYYHQieZUcmpr8VUHQmrjPa7UW4zEdvesUqT19GJjG0djGHjIvJA
                5vbXgToqDM6+0so+Y6g83B42UYPjoYo6Q9Ueq5xhu/+fPZ8yuPtxWqp54IWE
                G4QHg27Ye9HEJXgNKl5m2Oqb7Mg/kTbKhNK5L7Q2tlTN/dDYcJKmJHWlPzKW
                xPxX0opEWEE1Pp5WyAlWhEYRwMBGVD9VBdqhVbJLDeYzz+NtXn7zmfv1HW/P
                Z3t8hz2vu/zLe4e3eEHdY9i8cLi/PaK2rVBMe0bbzKSpzB6PLMPam4m2aiwD
                PVW5ilLZ/TMEOdcziWRY7istw8k4ktlAEIdhpW9ikQ5Fpgq8KHoHZpLF8qUq
                QGchPPxHFrtkX7WcuVO4SfkOIYdyizKnt1aidUJ+4Qzl2qNzuGfl9saCDNzH
                XYrNnwQ0SApwsfT78Cqxi2fpE/jbczQ/YPmsLHBslvE2tsofkm6JBFYOUQlw
                NcC1ANdxg5ZYDdBG5xAsx02s0X4OL8etHM4PGz/wlc0CAAA=
                """,
            """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRT+ZhZY2FJZqFZK/dFatYWD0MabprElMSHBmtSG
                A5wGdqVTltlkZyA98rd49mKiMfFgiEf/KOMbSpqYcPAw771v3jffe/P9/vPj
                J4CX2GfYFypIYhlc15WYyqEwMlb1ljJh8lEMwualjIJmJLR2wRj8KzEV9Uio
                Yf19/yocGBcOw+4qiYtQm1sZF2mGzGuppDlmSB10qx0G56DaycNFzkMKHmGR
                DBlYN4881nPguENUcyk1Q7X9n1u+ojHD0JxYJdLvMhTbo9hEUtXfhUYEwgii
                8PHUof8zG3I2gOaO6P5aWtSgKjhkOJ3PSh4v88WZzzzur3k865TnsyPeYKfr
                pYzPK7zh/PqU4X7qvHiLssSupLJpP2OVjhj2Vu7/j0W0Fm3hn4lpM1YmiaMo
                TF6MDFnQjIOQodCWKjybjPthciH6Ed2U2vFARB2RSIuXl96HeJIMwrfSgq3z
                iTJyHHakltQ9USo2i9Eah+RvigZm6JSs4fRvTrWLLMUnhI4Jc8pe7TvWatvf
                UPiy4OxRtK+AHTyluHnDgo+idZIqq0bGk+7GUqtuDaacrn1F4fNKmfwNYSnD
                8WwRd/Gc8hvq3aXevR6cFjZbuN9CGVtUotLCNh70wDQe4lEPrkZR47FGXmNH
                I6tR0tj4C6G2ycbxAgAA
                """,
            """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41SXWsTQRQ9M0mTzTbatH4l1q9aldoHty2+WYQaFBbiCjYE
                pE+T3TGdZDMDu5PQxzz5Q/wHxYeCggR980eJd9ZYHxR0l7kfZ849M/fufvv+
                8TOAx3jAsCV0khmVnARaTNVAWGV0EGors7cilu1jlSav+kMZ2yoYQ2MopiJI
                hR4Ev9ASw8bfNLoyt+c6VSwxVPaVVvYpQ2nrYa+OKjwfZdQYyvZY5Qzbnf+9
                yxMGbz9OCzkf3Gl4YXTYPYjaz+tYQb1GYINhs2OyQTCUtp8JpfNAaG1sIZsH
                kbHRJE1JarUzMpbEgpfSikRYQRgfT0s0IuZMzRkwsBHhJ8plOxQlu3TAfOb7
                vMmLNZ95X9/x5ny2x3fYs6rHv7yv8AZ31D13l39Oic5tRGLaNtpmJk1l9mhk
                GdZfT7RVYxnqqcpVP5UHv7ug2bVNIhlWOkrLaDLuy6wriMOw1jGxSHsiUy5f
                gP6hmWSxfKFc0loI9/6QxS7Nr0wtV2i13EDJ36G+Xb5GntNL34+yDcoCNxzy
                S9tn8E+L7bsLMoiwSbb+k4BlikCFF86LrxHbPcufwN+c4eIHrJ4WAMe9wt7G
                /eJnZbhEApePUApxJcTVkEqbFKIV4jrWj8By3MBN2s9Rz3Erh/cDKimwMukC
                AAA=
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
                H4sIAAAAAAAA/41SXU8TQRQ9s1va7VLoFkRoAZEPsfWDBfVNYkKamGysNUGC
                ITxN27EuLLPJzrThkd/iL1B5IJHEEB/9UcY7ayMBVNhs5s69c865987cHz+/
                fgPwDKsMi1x2kjjsHPqS98Mu12Es/Sbv12OpkziKRPJK58AYvD3e537EZdd/
                09oTbYraDONdoesRVyqQSnPZFpviPcN8tdb4m+6WUL/RzylxI066/p7QrYSH
                UvlcylinMOU3Y93sRRGhvPYV8blrpAtwkM/DgstQuVzeu1B/2Ei6qVD1uioH
                YCpjsv0vkeWbSRQwiqIpymNw1ttRKEP9gsGu1rYZZv8rkcMthux6yijgNsZd
                TGCSYekmiXMoM2SqQW3bUKddVDDDUGrsx5pq8F8LzTtcc2rROujbNBTMLHmz
                gIHtm41Fh4eh2dG8WJ01ho2zozH37Mi1pizXcuyBtRZK3tlRxVpl3z9mHTqv
                ZBzLsymaoeDQeTDr5YzQE3reC5O2sq8Zpjd7UocHIpD9UIWtSGycDwa1Uo87
                gqHYCKVo9g5aItnihGFw38a9pC1ehsYpDzS2ryhgjd4gY5qim6A5oSYfkpcl
                myNbMc90KWajjCHyLDwib4ai5st8wfCn9JIeD7DA+AVeGQWMXGWVLrMmLrAc
                jJEOS1lPkb4DJk8xsXOCqWMMn6Ky4xVPMHuM0uc/Qi6lMenNZFhYSdt7AJ9s
                nRB3qPy5XdgB7gaYD7CAxQBLuBdgGfd3wRSqqO3CUebPKwwpZBVGFYoKBYWR
                X85LYc0vBAAA
                """,
            """
                androidx/navigation/Outer＄InnerClass.class:
                H4sIAAAAAAAA/41U31MbVRT+7m5+bJYAG6AtkNiqRExC2wVstRZaBRRZDKGC
                w1jx5ZKsYWHZxd0NU18cnvondEZfnHEcn3ioMxocO+Ng++bf5Diem90mNVSG
                meSec8+e853vnnPu/euf3/8AcAPrDHnu1DzXqj3QHX5g1XlguY6+2ghML284
                jukt2Nz3k2AM2g4/4LrNnbq+urVjVoMkZIbErOVYwV2GWMEobjDIheJGGnEk
                VcSgMCiWQJnz6gzMSENFTwoS0uQfbFs+w3j5PARmGHrqZmC0sSiNwaBW3b19
                1zGdYIoAq+7+1wxF4nFezLGy69X1HTPY8rjl+Dp3HDdoeft6xQ0qDdueEYdJ
                qMT5IkNapMjXzC95ww4YtgrnS2QY5e7azZyTYxqDGBLZR6mUgbseeJZDxx8q
                FF+ADK10nkvdtvmGZddML4nLKq6Idgx1sAvPO3NHwWvUSL6/bzo1hmuF09Cn
                s0XIRHAMeQH+BkNOlP4sxzeFY0E4LpztWBKOE2nk8IrQrtHht7m/veDWTIZM
                J9JwArMuzjcZDiBNmI5pFVN4i05kftXgNs3YhcJL6v85zf5Z7afe8y3bpKrG
                3WDb9BgGTqMQmfKuG9iWo6+YAa/xgJNN2juQ6X4xsaTEAhr+XbI/sMSOuEo1
                GtgfTw4vq9KwpErayaFKP0lTVElJkOwhKZPsU54+VIZPDqelSTbfO5DQpFFp
                Un76Q0LSYsspLSl2S88eysuDmkI6OSqKFDqRmZE5Rbo6rWg9o7FhNsmWnj2S
                KTAdejxipPeS3if0tUwbXiE6ozElriUE12kmTjDyvwObxCLdxc5k0VtR4QcL
                rhN4rm2b3vVduiyxsHn9ZcsxK429LdP7VNRXlNWtcnuDe5bYR8bsWsMJrD3T
                cA4s3yLTXKc3DL3rAa/urvD9yDvf7X2Pe3zPJGr/CUt3KJq0Vdfdhlc1Fy0B
                MRJBbJxKR8Mk0VsmSjAg3i/SFNLpVaB1mXaL9F0iqZaOkSplf0Xvz7ST8DGt
                fRAdH6X4LFIky7S7GHrTt34xG6QJVBolaPQPMXUxMiTjpV/Qe9SGS7SM2RZM
                OnSIYDJE7nnwWHcwe2kAvSwEKwKmiKXglHoC6X72GJcet4NCsqk22VREdiVi
                cwHQUhjGSJR7PCpWJhf75lsogsFsKdtENoSs0CqDCQS621H62yQFtdwTXLl/
                jFcHXm9iXEQ2UdSKTVxt4vrjrmPkIkYv8KBVb9dgPKpBi8FvuNFdBiWKZ7iJ
                tyMeX5AU7cqXJn5CPHY08Sek7xCXjyZOIK0IoKv0/15YYmFPKq32yUnlb2SS
                tO9ULN+uWB638C7lWSU9KUi906rBvVYoXS98hCUq3yctQANrJD8j+23q1Mwm
                ZAOzBu4YuIv3SMX7BuYwvwnmYwEfbKLfF78PfaitNeFD85HxMeBj0MfNlvGW
                D91HjvR/AXBODlL6BwAA
                """,
            """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41US08UQRD+umcfs7M8loc8FR8s8lJmQTxBTJBoHLIsxiUY
                5dTsjjAwzOhM74YjJ29ePXj04ImDxAOJJgYlXvxRxOrZISAEY7Lb/VV1VX3V
                X/Xu7+Ov3wFM4z7DkPCqge9Ud0xP1J11IR3fM5dq0g7ylufZwdLapl2RaTCG
                3KaoC9MV3rp54tUYUrOO58gHDNrI6EoTkkgZSCDNkJAbTsgwXPwvhhkGXfpl
                GTjeOkPnyGjxlK3hpYjBoh+sm5u2XAuE44Wm8DxfRgVDs+TLUs11KSp7pqyO
                Fiq8IcKNeb9qR01a2q/p43fUuP2mJlzq8MpI8fzNZkZfMuT/xUZUYs21iS7p
                yw07YGi/WIWoZytupI8BrkTRrVJ5ea40/6gJfTAy5OxnaCtu+ZLCzEVbiqqQ
                ghL5dl2jGTG1ZNQCBrZF/h1HWQVC1UmGV4e7Awbv4QbPHe4aXFcgG++6oVy5
                Fv3ordFzuDvFC+xhWuc/P6Z4ji905LQ+XkhM6blkX6KHFdiTo/faQiaXIm+a
                MCOsE84orNimmOqh99JppjFKb6Qk6vO+JwPfde1gYksy9D+redLZti2v7oQO
                aTZ3qiO9ksZcWouOZ5dq22t2sKx0VXL6FeGuiMBRduxsLktR2VoUr2M7f772
                UxGIbZu6+YukKXoR864IQ5tMo+zXgor92FEleuMSKxeawySNJxEp36umRfsd
                slK0N9OepNNkZN0ly1TzUd6xA+j7BDgm4mCgh46BpkYAMlRKFc2Sh0fJN+Nk
                rb31c3R0Gq7F4WeZSWa0xbynqe17l6QydKAzZrJo57R3j41/QjKxN/4D/AOS
                2t74IfjzxF7UeIHWBHhaj4p1NRLiYgp10ZeROlAvmn4/BHS644kU3VECkP0G
                /uIAvV9wdT9yaJiiVenIMYYWUvVexDdOf0WqNYZrJM/AKjQL1y3csOh2twhi
                0EIeQ6tgIW5jeBVGqD4jIVIhOiLQFSIXgSytfwCP7WED4AQAAA==
                """,
            """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VRW2sTQRg9M5vLZhNtGi9JrK2XptpUcNviUy1CDQoLMYW2
                BCRPk2SIk2xmYXcS+pgnf4j/oPhQUJCgb/4o8dttNA9S3GG/M9/tfDNnfv76
                8g3ACzxjqArdDwPVP3e1mKqBMCrQ7vHEyDALxlAciqlwfaEH7nF3KHsmC4sh
                c6i0Mq8YrO16u4A0Mg5SyDKkzAcVMaw1r2V9yWAf9vyk3wGPm2yvdXp21Gq8
                KeAGnBwFbzJsNoNw4A6l6YZC6cgVWgcm4YncVmBaE98nqtXmKDBE5r6TRvSF
                ERTj46lFt2OxycUGDGxE8XMVe7u06+8x1OezgsMr3OHF+czhtmX/+Mgr89k+
                32UH3Eq9ztr8+6cML/K4YZ/FNI6ntQwbvojokvnEuVKFoXbtjWvLpiweMGz9
                p/KPzo9I/ZaYNgJtwsD3Zfh8RHPWTibaqLH09FRFquvLo6UwpH8j6EuGlabS
                sjUZd2V4JqiGodQMesJvi1DF/iJYWJ5MUrNzGkzCnnyr4lx1Maf9zxTs0Qul
                Elmr8YMR1sjLEBYJOa104m2R58biE6Z3LmFfJOkni2KgjKdkC1cFyBEVYCP/
                t7lM1fGX/wr+/hKFz1i5SAIWtsmWKP2Q/nU6x2PCDcJ6MmITO4QHRLNKxKUO
                LA+3PNz2cAd3aYuyhwqqHbAI97DWQTqCE+F+hEyE9QgbvwFBj+0jIgMAAA==
                """,
            """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRy0oDMRQ9SduxjlWnPusLfICoC0fFnSKoIBSqgko3rtJO
                0NhpApO0uOy3+AeuBBdSXPpR4s3o3s3hPG7Cyc3X9/sHgEOsMKwKnWRGJc+x
                Fn31IJwyOr6T1p22rMtE242AMURPoi/iVOiH+Lr1JL1bYAiOlVbuhKGwtd2s
                oIQgRBEjDEX3qCzDeuO/y48Yqo2OcanS8aV0IhFOkMe7/QIVZB5GPYCBdch/
                Vl7tEUv2qftwEIa8xkMeERsOypu14eCA77Gz0udLwCPu5w6YPx1dif650S4z
                aSqz3Y6jkucmkQyTDaXlVa/bktmdaKXkTDVMW6RNkSmv/8zw1vSytrxQXizc
                9LRTXdlUVlF6qrVx+eNscQ2cdvBX2a+EsEYqzjVQ2nlD+ZUIxwJhkJsbWCSs
                /A5gFGGeL+U4j+X8rxjGKKvco1DHeB0TdUwiIopqHVOYvgezmMEs5RahxZxF
                8AMTgVUI6AEAAA==
                """,
            """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31RO0sDQRD+ZmMuekY93/HdqoWnYqcIGhACUUEljdUmt+ia
                yy7cboJlfov/wEqwkGDpjxLnTmubj+8xM8zsfn2/fwA4wgZhQ5okszp5jo0c
                6AfptTXxnXK+nkrnKiBC9CQHMk6leYiv20+q4ysoEYITbbQ/JZS2d1pVlBGE
                GEOFMOYftSNsNf+dfEyYbXatT7WJL5WXifSSPdEblHg1ymEiBxCoy/6zztU+
                s+SAsDkahqGoiVBEzEbD2mh4KPbpvPz5EohI5FWHlPdGV3JQt8ZnNk1Vttf1
                vF/dJoow09RGXfV7bZXdyXbKzlzTdmTakpnO9Z8Z3tp+1lEXOhcrN33jdU+1
                tNOcnhljfXGXwwEEn/+3cP4ajDVWcaGB8u4bxl+ZCKwwBoU5g1XG6m8BJhAW
                +VqBy1gv/ogwyVn1HqUGphqYbnBXxBSzDcxh/h7ksIBFzh1ChyWH4AcFuY/X
                4AEAAA==
                """,
            """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41QTYvTUBQ97yVN09jatH51On47yEwXpjO4UwZrQQjUEcah
                Lrp6bUPnTdMXyHsts+xvce1GUAQXUlz6o8T70sGVCyE5956bw7m559fv7z8A
                PMcew55Q0zyT08tIiZWcCSMzFZ0l2vRTofUHac57+awMxhBeiJWIUqFm0bvx
                RTIxZTgM3kuppDlmcPfjgyGDs38wrKKEcgAXPnGRzxhYXEWAaxVwVElqzqVm
                eDr4n90vaMcsMT1rQ+YxQ2Mwz0wqVfQ2MWIqjCAJX6wcOolZqFgALZ3T/FJa
                1qVuesjQ36ybAW/xgIebdUAPD/2A+05rsz7iXfa61vRC3uZd5+dHj4fuaeMv
                80nddv1S6FmrI2YXhCdi1c+UybM0TfJnc0On9bNpwlAfSJWcLBfjJD8T45Qm
                zUE2EelQ5NLyq2HwPlvmk+SNtGTndKmMXCRDqSV97SmVmSISjUPKzS1uatoY
                qePUl+ARPiB2TJxTDTrfUOnsfkXtc6F5SGg1QAOPCG9vVbiOuo2IOutGiSKk
                d+sV2eSoljpfUPv0T5vqVnBlw/G4wPt4QvVV8ZMl3BjBiXEzxq2Y1t6hFq0Y
                O2iPwDR2cXeEskZd455GUKCnEWo0/gB/P3a9nQIAAA==
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
                H4sIAAAAAAAA/31STW/TQBB9u0kcxw00lI8mFEqhPQAH3FbcqJBKBJKlYCQa
                Rap62sSrsImzK9kbq8ec+CH8g4pDJZBQBDd+FGLWBDggYUsz896+edoZ+/uP
                T18APMUew7bQSWZUch5qUaixsMrosC9z+2Y4kSNbB2NoTUQhwlTocfibrTB4
                R0or+5yh8vDRoIkavABV1Bmq9p3KGXZ6/7d+xuAfjdLSJAB3nX4Un/SP4+7L
                Jq4gaBB5lWG3Z7JxOJF2mAml81BobWxploexsfE8TcnqWm9qLJmFr6UVibCC
                OD4rKjQnc6HhAhjYlPhz5dA+VckBw95yEQS8zQPeomq58L+95+3l4pDvsxd1
                n3/94PEWd9pD5hxasSi6RtvMpKnMnkwtw9bbubZqJiNdqFwNU3n89460j65J
                JMN6T2kZz2dDmfUFaRg2emYk0oHIlMMrMjgx82wkXykHOivjwT+2OKDtVMuR
                Om5ZlLcJee6ClDm9tRLdIxS6wSnXHl/CvyiPd1ZiUPN9is1fAjTICvCx9qd5
                k9TuWfsMfnqJ5kesX5QEx4My3sVu+TfRRyCDjTNUIlyPcCPCTdyiEpsR2uic
                geW4jS06zxHkuJPD+wnyhROwigIAAA==
                """
        )
}
