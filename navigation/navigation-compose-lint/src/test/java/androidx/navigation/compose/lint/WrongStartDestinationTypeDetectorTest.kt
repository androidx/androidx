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
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.bytecode
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WrongStartDestinationTypeDetectorTest(private val testFile: TestFile) : LintDetectorTest() {

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
                import androidx.compose.runtime.Composable

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClass())
                    NavHost(navController = controller, startDestination = TestClassComp())
                }
                """
                    )
                    .indented(),
                testFile,
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
                    NavHost(navController = controller, startDestination = TestClassWithArgComp(15)) {}
                    NavHost(navController = controller, startDestination = TestClassComp::class) {}
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp::class) {}
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp(15)) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClassComp(true)) {}
                    NavHost(navController = controller, startDestination = AbstractChildClassComp(true)) {}
                }
                """
                    )
                    .indented(),
                testFile,
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
                    NavHost(navController = controller, startDestination = OuterComp.InnerObject::class) {}
                    NavHost(navController = controller, startDestination = AbstractChildObjectComp) {}
                }
                """
                    )
                    .indented(),
                testFile,
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
                    NavHost(navController = controller, startDestination = TestInterface)
                    NavHost(navController = controller, startDestination = TestAbstract)
                    // classes with companion object to simulate marked with @Serializable
                    NavHost(navController = controller, startDestination = TestClassComp)
                    NavHost(navController = controller, startDestination = TestClassWithArgComp)
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp)
                    NavHost(navController = controller, startDestination = InterfaceChildClassComp)
                    NavHost(navController = controller, startDestination = AbstractChildClassComp)
                    NavHost(navController = controller, startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                testFile,
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
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestInterface)
                                                           ~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestAbstract)
                                                           ~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassComp)
                                                           ~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassWithArgComp)
                                                           ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = InterfaceChildClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = AbstractChildClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestAbstractComp)
                                                           ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}

// Stub
private val SOURCECODE =
    kotlin(
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

// classes with companion object to simulate classes marked with @Serializable
class TestClassComp { companion object }

class TestClassWithArgComp(val arg: Int) { companion object }

object OuterComp {
    data object InnerObject

    data class InnerClassComp (
        val innerArg: Int,
    ) { companion object }
}

class InterfaceChildClassComp(val arg: Boolean): TestInterface { companion object }

abstract class TestAbstractComp { companion object }
class AbstractChildClassComp(val arg: Boolean): TestAbstractComp() { companion object }
object AbstractChildObjectComp: TestAbstractComp()
"""
        )
        .indented()

private val BYTECODE =
    bytecode(
        "libs/StartDestinationLint.jar",
        SOURCECODE,
        0x90a33e02,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uISTsxLKcrPTKnQy0ssy0xPLMnM
                zxMS9kss88gvLnHOzyspys/JSS3yLuES5eJOzs/VS61IzC3ISRViC0ktLvEu
                UWLQYgAArkTi/VsAAAA=
                """,
        """
                androidx/navigation/AbstractChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRh9M7sssAVZsCql/qi1NRQToY03TSMlMZJgD7Xh
                AKeB3dAJy26yM5Ae+Vs8ezHRmHgwxKN/lPGbpRoPHDzMm+998/K+b97PX9++
                A3iBQ4anIvKTWPrXzUgs5ERoGUfN9kjpRIx150qGficUSmXBGPY2aS8Dpf/o
                s7AYnFcykvqUwa4PjvoMVv2oX0AGWRc2csRFMmFggwJcbOXBUSCpvpKKod77
                v21e0pRJoNvGiOwHDOXeNNahjJrvAi18oQVJ+Gxh0TeZgbwB0Ngp9a+lYS2q
                /GOGs9Wy4vIqT89q6XJvy+U5q7panvAWOytWHI/XeMv68cHhnn1R/stypK7Z
                uYznGKcThv2N6/8bEG1FS2yfi8XbWOlOHOkkDsMgeT7VFEIn9gOGUk9Gwfl8
                NgqSSzEKqVPpxWMR9kUiDb9puu/jeTIO3khDdi7mkZazoC+VpNd2FMU6na5w
                TAnb6d8rJnCqONUZOIR7xE6Jc7rdxlfkG7tfUPyUah4TGg3wDPuEd9cq3ELJ
                REmVcaPk4dFZezVNwnRnGp9R/LjRprAW3NhwPEnxEQ7ofp0umcHtIawutru4
                06Wx96hEtYsd1IZgCru4P0RWoaTwQMFVeKjgKHgK5d95zSUJ2QIAAA==
                """,
        """
                androidx/navigation/AbstractChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy27TQBQ9Y6d5mADuA0h4P4LUVqJuqopNUaUShIiUFglQ
                Nl2giT20k9gzaDyJusyKD+EPukJigaIu+SjEHSfAFtjcufece+71nPH3H1+/
                AdjFY4ZdrhKjZXIWKT6RJ9xKraKDQW4Nj23nVKZJJ+V53tHZx5YLXFFDBYwh
                HPIJj1KuTqLXg6GIbQU+Q/mZVNLuM/jrG/06llAOUEKFoWRPZc7wtPc/C/cY
                2uu9kbapVNFwkkVSWWEUT6MX4gMfp7ajFU0Yx1abQ25Gwuxt9AN4bvFqK/5D
                vs8KlmHr36YxLP8SHArLE245YV428clI5kLNBTCwEeFn0lXblCVthtZsGgRe
                wwu8kLLZtHrxyW/MpjveNnteqXoXn8te6LneHeYmbP69QxXcYqj9tolh7YhP
                Xunc3cAanabCbI0smd/RiWC42pNKHI2zgTDv+CAlZKWnY572uZGuXoD1rlLC
                FEsEPVnwVo9NLF5KxzXfjJWVmejLXFLzgVLaFl+Yo01+l5wJdHru5eku96iK
                nCt0Lm1+QfW8oO9TLBfgPh5QrM8bUEMAhIyySwvxEzq9hbh+XjjsBNfn4FxQ
                ZJdxhTgfD6kKCtFt3EETj4qFd9Eq/njygHrDY/hdLHex0sUq1ijFtS7NvHEM
                lqOBJvE5ghw3c5R/AgRzNM0uAwAA
                """,
        """
                androidx/navigation/AbstractChildClassComp.class:
                H4sIAAAAAAAA/5VSzU8TQRT/zfZ7KVIqYgE/UBBLRbYQ4kEICdaoTUoPSJoI
                p2k7lqHbWbMzbTjyt3j2QtSQaGKIR/8o45ulQqIc9LDvzXv7e7/3+ePnl28A
                1rDKUOKqHQayfeQpPpAdbmSgvK2mNiFvmcqB9NsVn2tdCXrvUmAM81fhd4U2
                FzERMsaQ3JBKmk2GeHFvscEQKy42skgg5SKONNk87DCwvSxcjGTgIEtQcyA1
                w1Lt36tap0wdYbYsGaXYY0hvtPxh6rV/55m3gisCpHCdYaVY6waGeLzDQc+T
                yohQcd97Lt7yvk9NKuLot0wQbvOwK8L1895uuJjAJEPmgozhyX80c1nEehYF
                TNmxTDPM1YKw4x0K0wy5VNrjSgUm4tFePTD1vu/TGMZ/V7wtDG9zw8nn9AYx
                WjWzImMFaORd8h9Ja5Xp1V5heHl2nHedghN9Z8eukxtxnXS8cHY8m1p1yuwp
                Sz0bzSdzzrRTjn1/n3Ry8Z3xCytNIdPxdCKXtHR0VAtXtvznlVB5VM1EnQ9e
                BdqO1ISB74twuWsYZnb6ysieqKqB1LLpi63LnulOKkFbMIzVpBL1fq8pwl1O
                GIZ8LWhxv8FDae2hM1tVSoTRkAUFu6+DftgSL6T9NzXM0/grC1Zo+HEakoMp
                uwuq9RFZSdK3SOft2ZKOkZ2IvEtkbRLaIe2WTpEpzXzG6EnE8HgYaRHLJCfP
                UbiGMbsUelk22iFy9J1zeXZXpBOlTxj9cCVN9hwwpElTUalhcAHRtpH9iok3
                7BQ3P2LmJPLEiNgmpNETo22sHHGXqGGgQv7bxHhnH7Eq7lYxW8U93Kcn5qqY
                x4N9MI0FPNxHWmNMo6jhaixqJDVyGuMahV9kX+XHYAQAAA==
                """,
        """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41SXWsTQRQ9M/nabKOttdrEqq2toBVx2+KbRYhBcSGuYENA
                +jSbHdppNjOwMwl9zJM/xH9QfCgoSNA3f5R4d40fYB/cZe+dc+fMudwz++37
                x88AHuMuwz2hk8yo5DTQYqKOhFNGB+3YukwMXOdYpcnr+EQOXA2MYeMick9a
                9+tADSWG6r7Syj1lKN3f7jdQQdVHGTWGsjtWlmG7+589nzB4+4O0UPPBcwkv
                jA567ajzvIFL8OtUvMyw1TXZUXAiXZwJpW0gtDauULVBZFw0TlOSutIdGkdi
                wSvpRCKcoBofTUrkBMtDPQ9gYEOqn6oc7dAq2aUGs6nv8yYvvtnU+/qON2fT
                Pb7DntU8/uV9lS/xnLrHsHnhcH97RG1XIjF5aazrGO0yk6YyezR0DGtvxtqp
                kQz1RFkVp7L9ZxByr2MSybDYVVpG41Ess54gDsNy1wxE2heZyvG86B+YcTaQ
                L1QOWnPh/j+y2CULy8XcrdxRyrcJVSkvUeb0Vgq0TijI3aFceXAO76zY3piT
                gYe4Q7Hxk4A6SQEeFn4fXiV2/ix8An97jsYHLJ4VBY7NIt7CVvFT0k2RwPIh
                SiGuhlgJcQ3XaYnVEE20DsEsbmCN9i18i5sW1R/BbKrC0QIAAA==
                """,
        """
                androidx/navigation/AbstractChildObjectComp.class:
                H4sIAAAAAAAA/5VSXWsTQRQ9M/nabKOttdrE+t0ifqDbFt8sSgyKC3EFGwLS
                p9ns0E6zmZGdSehjnvwh/oPiQ0FBgr75o8S7a6igfXGXvfeeO/eeyz2zP35+
                /grgMTYYHgidZEYlR4EWE7UvnDI6aMfWZWLgOgcqTd7Eh5JCM3pfA2PYOKuh
                J607bSoqSwzVHaWVe8pQunuv30AFVR9l1BjK7kBZhofd/5j9hMHbGaQFow+e
                03hhtNtrR50XDZyDX6fkeYb1rsn2g0Pp4kwobQOhtXEFsw0i46JxmhLVhe7Q
                OCILXksnEuEE5fhoUiJVWG7quQEDG1L+SOVok6JkiwbMpr7Pm7z4ZlPv+wfe
                nE23+SZ7XvP4t49VvsTz0m2GO2cu+LdWNHolEpNXxhLULjNpKrNHQ8ew9nas
                nRrJUE+UVXEq23+WIRU7JpEMi12lZTQexTLrCaphWO6agUj7IlM5nif9XTPO
                BvKlykFrTtz/hxZbJGO52L2Vq0r+OqEq+SXynN5KgW4QCnKFyFfun8A7Lo5v
                zouBZ7hFtvG7AHWiAjwsnDavUnX+LHwBf3eCxicsHhcJjtuFvYb14iel2yKC
                5T2UQlwMsRLiEi5TiNUQTbT2wCyuYI3OLXyLqxbVX5Ga3ibhAgAA
                """,
        """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41QTW9SQRQ9M3w8eKXyoFop9atWpbDw0cadprElMZJgTWrD
                AlYDb6RTHu8lbwbSJb/FtRsTjYkLQ1z6o4x3KGli0oWLufeeO2fOvXN+//nx
                E8AL1BhqIgqSWAWXfiRmaiSMiiO/HRmZfBRD2TpXYdAKhdYOGIN3IWbCD0U0
                8t8PLuTQOEgx7NwkcSa1uZZxkGHIvlKRMocM6b1evcuQ2qt3C3CQd5GGS1gk
                IwbWK6CA9Tw4bhHVnCvNUO/855YvacxImiOrRPo9hlJnHJtQRf47aUQgjCAK
                n8xS9H9mQ94G0Nwx9S+VRU2qgn2G48W87PIKX57F3OXemstzqcpifsCb7Hi9
                nPV4lTdTvz5luZc+LV2jHLGr6VzGy1qlA4bdG/f/xyJai7a4fSJmb2NtWnFk
                kjgMZfJ8bMiGVhxIhmJHRfJkOhnI5EwMQuqUO/FQhF2RKItXTfdDPE2G8o2y
                YOt0Ghk1kV2lFd0eRVFsluM19snjNA3N0ilb0+nvnGoHOYqPCR0S5pTdxnes
                Nba/ofhlydmlaF8BNTyhuHnFgoeSdZMqq0bmk+7GSsu3JlPONL6i+PlGmcIV
                YSXD8XQZd/CM8mvrDt3d6SPVxmYbd9uoYItKVNvYxr0+mMZ9POjD0ShpPNQo
                aDzSyGmUNTb+AnClX8H1AgAA
                """,
        """
                androidx/navigation/InterfaceChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy27TQBQ9Y6d5mADpA0h4P4LUgqibCsQiCAmCEJbSVgKU
                TRdoYk/bSewZNJ5EXWbFh/AHXSGxQFGXfBTijlNgXTZ37j3nnns9Z/zz1/cf
                AJ7iIcMzrhKjZXIcKj6Vh9xKrcJIWWEOeCx6RzJNeinP857OPrdd4Io6KmAM
                jRGf8jDl6jDcG45EbCvwGcovpJL2JYO/vjGoYwnlACVUGEr2SOYMz/v/tbHL
                0Fnvj7VNpQpH0yyUTqF4Gr4RB3yS2p5WuTWT2Gqzw81YmO7GIIDnNq+243/k
                p6xgGTbPN41h+Y9gR1iecMsJ87KpT1YyF2ougIGNCT+WrtqiLOkwtOezIPCa
                XuA1KJvPqqdf/OZ8tu1tsdeVqnf6tew1PNe7zdyEx+ewqIIbDLW/PjGs7fLp
                O527K1ij01SYzbEl+3s6EQyX+1KJ3Uk2FOYjH6aErPR1zNMBN9LVZ2A9UkqY
                YomgRws+6ImJxVvpuNb7ibIyEwOZS2p+pZS2xSfm6JDhJecCnZ57e7rMHapC
                ZwudS4++oXpS0Hcplguwi3sU64sG1BAADUbZhTPxEzq9M3H9pLDYCa4uwIWg
                yC7iEnE+7lMVFKKbuIUWHhQLb6Nd/PTkAfU29uFHWI6wEmEVa5TiSkQzr+2D
                5WiiRXyOIMf1HOXf1GOmWzEDAAA=
                """,
        """
                androidx/navigation/InterfaceChildClassComp.class:
                H4sIAAAAAAAA/5VSW08TQRT+Zlt6Y5G2IJbiBQS1FGELYkwsIcEatUmpCZIm
                wtO0Hcq221mzM2145Lf47AtRQ6KJIT76o4xnSoUHSQwPe858Z8/5zvXX728/
                AKxjnWGJy2bgu80jR/K+2+La9aVTlloEB7whSoeu1yx5XKmS3/0QBWNItnmf
                Ox6XLedtvS0aOooQw9xVNLtC6QuqKEYYIhuudPUmQzi3t1hjCOUWazaiiCcQ
                RoIwD1oMbM+GjbE4LNwgV33oKoblyjUqLVKqltBbho1y7DHENhreMPfTaxAt
                GMEleURxk2E1V+n4moicdr/ruCZGcs95KQ54z9MlXyod9BraD7Z50BFB8by7
                WwlMIcMQvyBjeHaddi6rKNrIYsZM5jbDfMUPWk5b6HrAXakcLqWvB0TKqfq6
                2vM8GkTqb8nbQvMm15xsVrcfogtgRsSNAE29Q/Yj16ACvZqrDK/PjtMJK2MN
                vrPjhJUcTVixcObseDa6ZhXYcxZ9MZaOJK2sVQj9/BixkuGd1AWKUUg2HBtJ
                Rgzdmqn3v1dCtVEpk1Xef+MrM1Ad+J4ngpWOZpjZ6UntdkVZ9l3l1j2xddkw
                3UnJbwqG8YorRbXXrYtgl5MPQ7riN7hX44Fr8NBol6UUwWDCgoIT7/xe0BCv
                XPNvepin9k8WrNLkw1RhhPS0WQW9l2liEdJ3SafN4ZIOEY4iRnKF0CZ5W6QT
                +VOM5me+YvyEkAVnGAkUUSA5de6FJFJmJ/QybLRC4p0YcjlmVaRH8l8w/ulK
                GvvcYUgTwyTiw+AMBsuG/R1T79kppj/jzsnAEqLWTEI2KCJLza0NuB/jCekS
                2e8R4+w+QmXMlXG/jHks0BMPyniIR/tgCjks7iOmkFLIK9gKS8rAtMKEQvYP
                Y7gHcXYEAAA=
                """,
        """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41STW/TQBB9u0kTxw00LQUSylcpoNADbituVEglAmEpGIlG
                kVBPm3hJN3F2JXsT9ZgTP4R/UHGoBBKK4MaPQsyaUA4gga2dmfd25u3O2N++
                f/wM4BHuMzSFjlOj4pNAi6kaCKuMDkJtZfpW9GXrWCXxq95Q9m0ZjKE2FFMR
                JEIPgl9sgWHzbxodmdlznTKWGEr7Siv7hKHQfNCtogzPRxEVhqI9VhnDdvt/
                7/KYwdvvJ7mcD+40vDA67BxErWdVrKBaIbLGsNU26SAYSttLhdJZILQ2NpfN
                gsjYaJIkJLXaHhlLYsFLaUUsrCCOj6cFGhFzpuIMGNiI+BPl0A5F8S4dMJ/5
                Pq/zfM1n3td3vD6f7fEd9rTs8S/vS7zGXeqeu8s/p0Tnrkdi+sJktmW0TU2S
                yPThyDJsvJ5oq8Yy1FOVqV4iD353QvNrmVgyrLSVltFk3JNpR1AOw1rb9EXS
                FalyeEH6h2aS9uVz5UBjIdz9Qxa7NMMitV2i1XBDJX+bend4jTynl74hoU1C
                gRsQ+aXtM/in+fadRTLQxBbZ6s8ELFMEKrxwXnyVst2z/An8zRkufsDqaU5w
                3M3tLdzLf1iGSySwfoRCiMshroRUWqcQjRDXsHEEluE6btB+hmqGmxm8H+eK
                JRLtAgAA
                """,
        """
                androidx/navigation/NavHostController.class:
                H4sIAAAAAAAA/41Ru0oDQRQ9d2I2cY2axFfiA7QQH4WrYqcIKoiBGEEljdUk
                u+gkmxnYnYSU+Rb/wEqwkGDpR4l3Vzsbm8N53OE+5vPr7R3AEdYIm1L7kVH+
                0NNyoB6lVUZ7DTm4MrG9MNpGJgyDKAciFDtyIL1Q6kfvptUJ2jaHDME5UVrZ
                U0Jme6dZQBaOiwnkCBP2ScWErfq/OhwTSvWusaHS3nVgpS+tZE/0BhkelRKY
                TAAE6rI/VInaZ+YfENbHI9cVFeGKIrPxKL9YGY8OxT6dZz+eHVEUSd0hJa/n
                /7Te61qe9sL4AWG2rnTQ6PdaQXQvWyE75bppy7ApI5XoX9O9M/2oHVyqRFRv
                +9qqXtBUseL0TGtj0y1jbEDwMX7HTm7DWGHlpRrI7r4i/8JEoMropGYey4yF
                nwJMwk3zlRSXsJr+HGGKs8IDMjVM1zBTwyyKTFGqoYy5B1CMeSxwHsONsRjD
                +QaW0s1z9gEAAA==
                """,
        """
                androidx/navigation/NavHostControllerKt.class:
                H4sIAAAAAAAA/41UXVPbRhQ9Kxt/KAZk82WbQAI4xEAbEUo/oWmp0xQljskk
                GToZnhZ56whkqaOVPXnkvf+iv6ApD+mUmQ7Tx/6oTu8KJw4WNHjGunfv3nPu
                0dXu/effP/8CsI4dhtvcawa+03xlerzrtHjo+J7Z4N1tX4Y13wsD33VF8ChM
                gzEYB7zLTZd7LXNn/0DYFE0wpHvpDJvV+pXoNuqDRBtLuwy3+mjbb//sS2EG
                HS902sKsRWu+74oNhoW6H7TMAxHuB9zxpMk9zw+jUtJs+GGj47qUNUwK+iUz
                yDLMHvqh63jmQbdtOl4oAo+7pkUpxOLYMo1rDBP2S2Ef9mie8IC3BSVSo6px
                0e9FnimSFr1GDsMY0ZHDKPVLhjwI7wsZOl6kL4M8MV2xSQyFeEmGsZYIay6X
                0vKI3rPFU/ETw1x16ULa51Q8yiakYcdgNz4AymEKxSw0lBjKg4V/dMKXW0Er
                Iqp+qH4vmWRM2ZeRLF6NIocZzCpRNxiKJMryPBHEW3KZpJ0OfdBKH0SSJpyL
                KS4WFCPIYQEVJegWQ2bTpiPmhPcYElV1qmf+953SWGJIbUaIHFZQ1bGMjxgq
                V+lEGncYklVLnboVrOowcfcS6KDmND7RaQJQer7euxWPRcibPOTUD63dTdCE
                YOqRVQ8wsEPlaLT5ylHeKnlNwv9yerSsnx7pWlHTtUwibg3t7bYyxulRuWjk
                ytrq8FrKGCE7StYgm99Oz+fVtrbK/v41laH0cjKjGQmKJik41A+mDJWaoWC2
                H9SNa0rSGlNqS5c2IY0ag97vBMN47O7dOaRpNv30bPhYXteRDo2erf6cobbX
                /KZgGK07nmh02vsieK7Gk7q0vs3dXR44at0LVga53k2Wc6S5vixBS/2Z3wls
                8cBRFKUexW5MDO7S0UvSJ0mgrO4rvf4PtEqRzZAtq+sSi9GJHYgNoUT/FLFs
                02qLODWyw8sF/Q8YK4UCPV+rEwCrB8phBA/JnzxLRBZj6qiQl8f4e+Rpso9U
                XOtVOnuWMBFBVbXrFFO/5O8o/xZlvC0CGOdUljBN2THUzUFUYQA1h/k4anEQ
                NX4OlcFtVMlXqBqie4CFEyy/eIOPj1E+gfnCGH2DtWPcPMF65H96jMXX70hH
                IpABneRMEnkCdVrrtLuO+/ieZD2OOvoADbKc4p/RJ/h8DwkLX1j40sJX2LCw
                ia8t3MM3e2AS32JrDwWJKYmixITEpMSMxKzEtMR1iQWJisScxLzEkMR3ElmJ
                MYk8+f8Bprsff/oHAAA=
                """,
        """
                androidx/navigation/Outer＄InnerClass.class:
                H4sIAAAAAAAA/41U31PcVBT+brI/smEXskBbfqythRV3l7YBbLUWWgUUCS5L
                BYex4stlNy6BkGCSZeqLw1P/hM7oizOO4xMPdUbBsTMOtm/+TY7juZt0ty6V
                YSa559yTc77z3XPOzV///P4HgJtYY8hzp+a5Vu2h7vB9q84Dy3X0lUZgennD
                cUxv3ua+nwRj0Lb5Ptdt7tT1lc1tsxokITMkZizHCu4xxApGcZ1BLhTX04gj
                qSIGhUGxBMqsV2dgRhoqulKQkCb/YMvyGcbK5yEwzdBVNwOjhUVpDAa16u7u
                uY7pBJMEWHX3vmYoEo/zYo6WXa+ub5vBpsctx9e547hB09vXK25Qadj2tDhM
                QiXOFxnSIkW+Zn7JG3bAsFk4XyLDKHfWbvqcHNPoQ7/IPkSlDNy1wLMcOn5/
                ofgSZGil81zqtM01LLtmeklcVnFFtKO/jV140Zm7Cq5SI/nenunUGK4XTkOf
                zhYhE8FR5AX4Gww5UfqzHN8UjgXhOH+2Y0k4jqeRw2tCu06H3+L+1rxbMxmy
                7UjDCcy6ON9EOIA0YTqmVEziLTqR+VWD2zRjFwqvqP/nNPtntZ96zzdtk6oa
                d4Mt02PoPY1CZMo7bmBbjr5sBrzGA042aXdfpvvFxJISC2j4d8j+0BI74irV
                aGB/PDm4rEoDkippJwcqPZKmqJKSINlFUibZrTx7pAycHExJE2wu05vQpCFp
                Qn72Q0LSYkspLSl2i88fyUt9mkI6OSqKFDqRmZE5Rbo6pWhdQ7EBNsEWnz+W
                KTAdejxmpGdI7xb6arYFrxCdoZgS1xKC6xQTJxj834FNYoHuYnuyaMwqfH/R
                9YN51wk817ZN78YOXZhY2MCesuWYlcbupul9KmosSutWub3OPUvsI+PwasMJ
                rF3TcPYt3yLTbLs/DJm1gFd3lvle5J3v9L7PPb5rEr3/hKXbNE3aqmtuw6ua
                C5aAGIwg1k+lo4GS6H8mytAr/mGkKaTTn4HWJdot0HeJpFo6Rqo0/CsyP9NO
                wse0dkN0/SrFjyBFsky7i6E3fesR80GaQKVxgkZviKmLsSEZL/2CzGELLtE0
                jjRh0qFDBJMlci+CRzuD2SsD6O9CsCJgklgKTqmnkB4MH+PSk1ZQSDbVIpuK
                yC5HbC4AWgoDGIxyj0XFyuZi33wLRTCYKQ0fYTiErNAqgwkEut9R+jskBbXc
                U1x5cIzXe0eOMCYij1DUike4doQbTzqOkYsYvcSDVr1Vg7GoBk0Gv+FmZxmU
                KJ7hFt6OeHxBUrQrXxr/CfHY4fifkL5DXD4cP4G0LICu0fu9sMTCnlSa7ZOT
                yt/IJmnfrli+VbE8buNdyrNCelKQeqdZg/vNULpi+AiLVL5PmoAGVkl+RvY7
                1KnpDcgGZgzcNXAP75GK9w3MYm4DzMc8PthAjy+eD32ozTXhQ/OR9dHro8/H
                rabxtg/dR470fwEaMaS//gcAAA==
                """,
        """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41US08UQRD+umcfs7M8loc8FVRAFlBmQTxBTJBoGLIsRghG
                OfXujjAwzOhM74YjJ29ePXj04ImDxAOJJgYlXvxRxOphFIRgTHa7v6quqq/m
                q575efz5K4Ap3GMYEl418J3qjumJurMupON75lJN2sGg5Xl2sFTetCsyDcaQ
                2xR1YbrCWzd/ezWG1IzjOfI+g5YfWW1AEikDCaQZEnLDCRmGi//FMM2gS39Z
                Bo63ztCeHymesp14KWKg6Afr5qYty4FwvNAUnufLqGBolnxZqrkuRWXPlNXR
                RIU3RLgx51ftqElL+zF1/IYat1/VhEsdXskXzz/Z9MhzhsF/sRGVKLs20SV9
                uWEHDK0XqxD1TMWN9DHAlSi6VVpemS3NPWxAD4wMOXsZWopbvqQwc9GWoiqk
                oES+XddoRkwtGbWAgW2Rf8dRVoFQdYLhxeFun8G7uMFzh7sG1xXIxrtuKFeu
                ST96bXQd7k7yAnuQ1vn39yme4wttOa2HFxKTei7Zk+hiBTZ/9FZbyORS5E0T
                ZoR1whmFFdskUz10XzrNNEZocCVRn/dDOed7MvBd1w7GtyRD75OaJ51t2/Lq
                TuiQbrOnWtJNOZlNc9Hx7FJtu2wHK0pbJalfEe6qCBxlx87GZSkqW4viZWwP
                nq/9WARi26aO/iJpiG7FnCvC0CbTWPZrQcV+5KgS3XGJ1QvNYYJGlIjU71YT
                o/02WSnaG2lP0mkysu6QZaoZKe/oAfR9AhzjcTDQT8dAw0kAMlRKFc2Sh0fJ
                N+JkrbX5Y3R0Gq7F4WeZ6XVES8x7mtq6d0kqQxvaYyaLdk575+jYByQTe2Pf
                wN8hqe2NHYI/TexFjRdoTYCn9ahYx0lCXEyhDvozUgfqVtM7REBH1x8pOqME
                IPsF/NkBuj/h6n7k0DBJq9KRYxRNpOrdiG+MPkeqNYZrJE/fGjQL/RauW/R0
                NwliwMIghtbAQtzC8BqMUP3yIVIh2iLQESIXgSytvwC7X1qw5AQAAA==
                """,
        """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VRXU8TQRQ9M9uP7bZKqSitlaICCpi4QHxCYoKNxk3qkggh
                MTxN20kduswku9OGxz75Q/wHxAcSTQzRN3+U8e5S5cEQd7L3zP06d+69P399
                +QbgGZ4wNITux0b1T30txmogrDLa3xtZGRfBGKrHYiz8SOiBv9c9lj1bhMNQ
                2FFa2RcMzuraYQV5FDzkUGTI2Q8qYWh2rmV9zuDu9KIs3wNPk9wg3D/YDduv
                KrgBr0TGmwxLHRMP/GNpu7FQOvGF1sZmPIkfGhuOooioZjtDY4nMfyut6Asr
                yMZPxg51x1JRSgUY2JDspyrVNujW32RYu5hUPF7nHq9eTDzuOu6Pj7x+Mdni
                G2ybO7mXRZd//1TgVZ4mbLGUxgu0lnE7Egk1Wc6Uy6kwLF/b8fJVUhGLDCv/
                ifwz5wcMc6EYvzGJbRttYxNFMn46pFrNdyNt1YkM9FglqhvJ3avh0A7api8Z
                ZjpKy3B00pXxgaAYhlrH9ER0KGKV6lNj5ep1kpK9fTOKe/K1Sn2NaZ3Df6pg
                k7aUy0bbSJdGuExagbBKyOnkM22FND9dAGF+/RzuWeZ+NA0GWnhMsnIZgBJR
                AS7Kf5PnKTr9yl/B35+j8hkzZ5nBwSrJGrnv079A73hI2CJcy0osYZ1wm2hm
                ibh2BCfArQBzAW7jDl0xH6COxhFYgrtoHiGfwEtwL0EhwUKC1m8Ngp25JgMA
                AA==
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp＄Companion.class:
                H4sIAAAAAAAA/51TTW/TQBB9a6dxYkJJ0wIJUD4DpAjqpqpQpSIkCEJESlsJ
                UC49oE2ylE3sXWSvox5z4ofwD3pC4lBFPfKjELNOoOKCVC4zb+bNm7Fn7B8/
                v58A2EKD4SlXg1jLwVGg+FgeciO1CvZTI+KWjj7X20oRCnmSZKE1XFGJB8ZQ
                HvIxD0KuDoP93lD0jQeXIf9MKmmeM7iNtW4JC8j7yMFjyJlPMmHY7vzfyB2G
                ZqMz0iaUKhiOo0AqkigeBq/ER56GpqVVYuK0b3S8y+ORiHfWuj4cO3q53j8j
                P0QZy7B+vm4MS78Fu8LwATecck40dmmZzJqiNWBgI8ofSRttEBo0GerTie87
                Vcd3yoSmk8LpF7c6nWw6G+ylV3BOv+adsmNrN5nt8Pg8O/JwnWH1nwoPqwyL
                f8sYin+Wy7Cyx8dvdGLf28Q6DEW8PjJ0tJYeCIZLHanEXhr1RPye90LKVDq6
                z8Muj6WN58nS2QRBp/bf6TTui9fScrW3qTIyEl2ZSCp+oZQ22VMmaNKVcnZ1
                5B37xdAG7lIU2F2SX3j0DYXjjL5HNp8lt1EnW5oVoAgfKDNCF+biJ+Sdubh0
                nN3FCq7MkjNBhi5ikTgX9ymqEHsDN3ELtQzdJv8gG3wHD7N/hnZBmvIB3DaW
                2qi0sYwVgrjcpt5XD8ASVFEjPoGf4FqC/C+Tnk+BcAMAAA==
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp.class:
                H4sIAAAAAAAA/5VUW08bVxD+zvq2XgysIRcCpEmLm5pLWKAJUHAaLillKZcU
                UhpC+3Cwt2ZhvUt311b6UuUpPyFS+1KpD33iIVFbqIpU0eStv6mqOmfXmFuE
                ZMk+Z2Z25pvvzMw5//z3518A7uBrhj5uF1zHLDzVbF4xi9w3HVtbKvuGO+2U
                djK6bZNkcc8TagKMQd3iFa5Z3C5qSxtbRt5PIMIQz5m26X/MEM3q3asMkWz3
                agoxJBREITPIpkCadIsMTE9BQUMSElLk72+aHkP/fD1Exhkaioav1zApnc6g
                5OmbYxu2P0jAeWfnO4ZB4lMvdte84xa1LcPfcLlpexq3bccPojxt0fEXy5Y1
                Lg4XV+gMVxhSIlWmYHzDy5bP4GbrS6jr82drOl4n5xRacUmwaadS+86K75o2
                leVStvsEdGil8109a5sqm1bBcBN4R8EN0a620/jZo+7dk/EuNZvv7Bh2geF2
                9jz8+YxVdCLZhYxI8D5Dp2jLRY4fCMescJy+2LFHOPam0InrQrpNBdjk3ua0
                UzAY0seRuu0bRXHGgXBIaQo1DCkYxId0IuPbMrdoDi9n39KLJwyZi0aC5oFv
                WAZVNub4m4bL0HIehXjl8lb1lgzX092MWLhNLgmMi4me33Z8QtK2KiXNpGO5
                Nre0B+H4TRMj3y3nfcdd4O42FSm8iPcU5ECZkzUwhtG6huyYBtV9ApPiAk9R
                iY/YLBg+L3CfE0WpVInQC8PEkhQL6Npvk/2pKTTqgFSgK7p7+OymIrVJiqQe
                PlPoJ6myIslx2htoj9DeRGb59XO5jVybh6QBNsaapxpb4qrULg1EXv8cl9To
                XFJNCG32zfPIXKsqk3z4bEiWpdCJzIzMSZKVIVltaI+2sQE2++ZFhAJToccL
                RnIjyU1CXk7X4GXK3x6VY2pccB5i4iTXL6xaAg8Zmk6Xjm7iIq/MOp5oj+86
                lmW4/dv0VHQsl23fLBm6XTE9k2Zo8niuaEzDIW6eN21jsVzaMNxHYs7EeDl5
                bq1y1xR61di44vP89gLfqeqZs9gPuctLBtE8lSR1TNUgVVlxym7emDEFxLUq
                xOo5cnRtJHrZQes1MQxUlkekxWm/THuLeOFF80mPBdYvSJshb4l2pWcfyZ6O
                39H4KkBYpbUJYjKGCXOEoobxJWlXQm/61ixmiCSBSiMHlf4hpiZGi/ZYz29o
                3K3BxQPjSACTCh2qMGkidxTcdTaYvTWA3laCFQGDxFJwSh5AWuvYx9WXtaCQ
                bLJGNlkle6IsahJtVK4w961qAdOd0e9/gCwY5Ho69tARQj6mNQImEOhlq6Yf
                o11Q6zzAjbV93Gx5bw+3ROQeutXuPfTtof/lmWN0VhmdbA+jsqVrPMIaBAz+
                wJ2zZZCr8Qx3qS0hj69oF+3K9PT+glh0t/dvSD8iFtntPYS0IID66P+TsETD
                njwO2hdJyP8inSD9uGKZWsUyGMVHlGeN5IQgNRKkH0OiSrUtSErEDpBbY/u4
                /yumXwWWCJ4EUyfm63MsU5FzJE3Qvh6kXyHKIJnhAfX1k3VEdMzo+FTHLHQS
                MafjM8yTg4cFLK5D9dDsYcmDEqxxT1jSHlo8tHq4GxhHPWgeOgN54n/YBk2n
                VQkAAA==
                """,
        """
                androidx/navigation/OuterComp＄InnerObject.class:
                H4sIAAAAAAAA/41TS2/TQBCeXTuJ46St+6BPKI8GSBqo0wCnVkglAtVVmiJS
                FUFPm8Qkbhy72Juox574CRw4coBLDlQcKoGEAr3xoxCzjiHQigop2flmPDPf
                +Jv19x+fvgDAXbhHIMOcmudatQPdYR2rzrjlOvpWm5tewW3tpwzHMb2typ5Z
                5TEgBLQ91mG6zZy6/isqEYiuWo7F7xOQ0pmdJEQgqoIMMQIyb1g+gWzxv1lW
                CCjcLXPPcuoEJtKZ4oCxH8WMhaLr1fU9k1c8Zjm+zhzH5UFTXy+5vNS2bcxK
                /NFWgWFs3GB+o+DWzGBQQ3Le51ZxePNlm9k45YV08fTbrWSeE0idx4ZUrGKb
                SBdxecP0CIyd7YLUq1U70EgFKoRRjFJ5e61UeJiEWVDjGJwjMFpsuhzT9E2T
                sxrjDAtpqyPhrog44uIAAqSJ8QNLeDlEtWUCL3qH8yqdpirVeocqVQRIhFZR
                RUgbVk5eqdO9wzzNkQcxhX57G6Ua3RjXpFmak/OKFpmVp0mOrJ+8ljbiWhSj
                McQEsYI4LrBgyxMxw6VzNxqDDC6vxDrrrs8LrsM917ZNb6nJCcw9aTvcapmG
                07F8C7VbG+iJN6a/n5Gi5ZildqtiettCXyGrW2X2DvMs4YfBoTJn1eYm2w/9
                1Onej5nHWiZO9RdJMrgZBZv5vomuWnbbXtV8ZIkWM2GLnTPDwTKuSQ42MCO2
                hvYWelG0Q2gj+DQSeLfR08WeRHTxGJQjBBSWwmTx5el4JvsJEMdWomkCIzQo
                vhoWS2MjH4JHg3QpTP+TGT9LGA15B6Vj3X+UEhiHiZDJQEvRTi1m30FE7ma/
                An0DEamb7QF9KneDwXN4ykBjStBssl8QNhNoEv8E1QFxs/E7QqDA9G8ppoIC
                gMRnoM+OYeYjXDwKAhLk8RQ6UliEYVT1TsCXRYHEaHjLUJ75XZAMuGzAFQPf
                7hpCWDAgBdd3gfhwA27uguqLX9qHqA/jAZj0QQtAAs+fAKoQrfAEAAA=
                """,
        """
                androidx/navigation/OuterComp.class:
                H4sIAAAAAAAA/41RXU8TQRQ9M9uPZVmhVBQqAiqoFI0L6BMSE2w0blKXREgT
                06dpu8Gh2xmzM2145Mkf4j8gPpBoYoi++aOMdxeUGBPiTvaeuV/nzr33x8/P
                XwE8wQOGeaF6qZa9w0CJkdwXVmoV7AxtnDb04H0ZjKFyIEYiSITaD3Y6B3HX
                luEwlLakkvYZg7NSb/koouShgDJDwb6ThmGxeSnzUwZ3q5vkHB54luiG0e7e
                dtR44eMKvDEyTjAsNXW6HxzEtpMKqUwglNI25zJBpG00TBKimmr2tSWy4HVs
                RU9YQTY+GDnUJcvEWCbAwPpkP5SZtka33jpD/fTI9/gs93jl9MjjruN+/8Bn
                T482+Brb5E7hednl3z6WeIVnCRsso5kIlaI2EmFM1gvDeG44mw7Dw0s7X/47
                uYxFesR/ZPye/W2G6UiMXmljG1rZVCdJnD7qU925N0Nl5SAO1Uga2Uni7Yth
                0V4auhczTDaliqPhoBOne4JiGKpN3RVJS6Qy08+N/sUrY0r2dvUw7cYvZear
                nddp/VMF67S1Qj7qWrZEwmXSSoQVQk6nmGt3SQuyhRAWV0/gHufue+fBwGPc
                J+mfBWCMqAAX43+SZyg6+8a/gL89gf8Jk8e5wcEKySq5b9E/T++4Q7hAWM9L
                LGGVcJNopoi42oYT4mqI6RDXcJ2umAkxi1obzOAG5tooGngGNw1KBvMGC78A
                XHUOHD4DAAA=
                """,
        """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRzS5DQRg9M21vuYqiqL8EC8HCReyIpCSiSZEg3VhNeydM
                ezuT3Jk2ln0Wb2AlsZDG0kOJby57m5Pz883kzDdf3+8fAI6wxrAudJwaFT9H
                WgzUo3DK6OheWldrWZeKtiuCMZQ7YiCiROjH6KbVkd7NMQQnSit3ypDb3mmW
                UEAQIo8iQ949Kcuw2fjv8mOGmUbXuETp6Eo6EQsnyOO9QY4KMg/jHsDAuuQ/
                K6/2icUH1H00DENe5SEvExsNx7aqo+Eh32dnhc+XgJe5nztk/nTlWgwujXXn
                RrvUJIlM97qOip6bWDJMN5SW1/1eS6b3opWQM9swbZE0Raq8/jPDO9NP2/JC
                ebF029dO9WRTWUVpTWvjsgfa/AY47eGvtl8LYZVUlGmgsPuGsVciHEuEQWbu
                Ypmw9DuAcYRZvpLhIlaz/2KYoKz0gFwdk3VM1TGNMlHM1DGLuQcwiwrmKbcI
                LRYsgh8jsPDA7AEAAA==
                """,
        """
                androidx/navigation/TestAbstractComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5DAHSB5DwfgSpRaKTVOwKSCUIESktUqmy
                6QI5E1OczHiQ7URdZsWH8AddIbFAUZd8FOJ6EmBLN/d17rnXPvbPX99/AHiG
                xwzbQg9NpoankRZTdSKcynR0JK3bG1hnROw6Wfq56Y3QBJXAGGojMRVRIvRJ
                9G4wkrErIWAoPldauZcMweZWv4oVFEMUUGIouE/KMrR6F1u1y9De7I0zlygd
                jaZppLSTRoskei0/iklC7Zp4k9hlZl+YsTS7W/0Q3K9cb8b/wA9pjtJdLzaN
                YfUPYV86MRROUI2n04DEY95UvAEDG1P9VPmsRdGwzdCcz8KQ13nIaxTNZ+Xz
                L0F9PtvhLfaqVObnX4u8xn3vDvMTmv+jTQm3GCp/BWLYOBDTt5n1Z3cmSxJp
                tseOBO9kQ8lwtae0PJikA2mOxCChylovi0XSF0b5fFmsdrWWppMIayU9U/g+
                m5hYvlEeaxxOtFOp7CurqHlP68zlZ7Nok9IFf33y3L823eIeZZHXg/zKk28o
                n+XwfbLFvPgCD8hWFw2oIARqjKJLS/JT8nxJrp7l2nrC9UVxQcijy7hCWICH
                lIU56TbuoIFH+cK7aOb/mzSg3toxgi5Wu1jrYh0bFOJal2beOAazqKNBuEVo
                cdOi+Bu0qQF5HAMAAA==
                """,
        """
                androidx/navigation/TestAbstractComp.class:
                H4sIAAAAAAAA/41RXU8TQRQ9s9vPdSstohZRAakIPLBATEwETbDG2KTUREkT
                06dpO+K02xkzM2145Lf4D4gPJJoY4qM/ynh3qfDgCy/3zP06994zv/98/wng
                KTYYalz1jZb940jxiTziTmoVHQrr9rvWGd5zdT36kgdjKA/4hEcxV0fRu+5A
                9FwePkNuTyrpXjL4a+vtEFnkAmSQZ8i4z9IyrDavM2CXobDXi6dUm9dpqSWG
                K0rlETJsrzWH2hFDNJiMIqmcMIrH0WvxiY9jalDUOe45bQ64GQqze7HszQAl
                zDAUL8kYtq618dX43RAVzBbh4RbDSlObo2ggXNdwqWzEldIuZbBRS7vWOI7p
                1sq/XQ+E433uOMW80cSnT2GJKSYGDGxI8WOZeFv06m+TnucnYeBVvcArn58E
                XsErrFbPT5b8HW+LPWf+q+yvrzmv7CXVOyzhmGvxyVttEwmc0XEszObQMSy8
                HysnR6KhJtLKbiz2rzalz6vrvmCYaUolWuNRV5hDTjUMs03d43GbG5n402DY
                UEqYesytFdQcfNBj0xNvZJKbn85p/zcls0ySZdI75xMFCVfIyxHeJvQIs6lX
                Iy9K1CDMbpyhcJqmH0+LgRdYJRteFKCIgLCAG5fNVaR6IvyB0kd2hvI3zJ2m
                ER9PyAZUVyLGCi2ylnI/wjrhM4rfIca7HfgNVBuYb+AeFuiJ+w08wMMOmMUi
                ljrIWAQWyxY5i8pfEw28C2ADAAA=
                """,
        """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31Ry0oDMRQ9N7VTHau2Put7qy4cFXeKoIJYqAoq3bhKO0HT
                ThOYpMVlv8U/cCW4kOLSjxLvjK4lcDiPm+Tm5uv7/QPAIdYJ69LEqdXxc2Tk
                QD9Kr62J7pXz54l0rgQiVDpyIKNEmsfoptVRbV9CgRAca6P9CaGwtd0so4gg
                xBhKhDH/pB1hs/HvyUeEaqNrfaJNdKW8jKWX7IneoMCtUQYTGYBAXfafdab2
                mMX7hI3RMAxFTYSiwmw0rI2GB2KPzoqfL4GoiKzqgLK989dycGn5Tmt8apNE
                pbtdzz2e21gRZhraqOt+r6XSe9lK2Jlt2LZMmjLVmf4zwzvbT9vqQmdi+bZv
                vO6ppnaa01NjrM/f5rAPwSP4azqbCGONVZRroLjzhvFXJgLLjEFurvACyr8F
                mECY56s5LmEt/yfCJGflBxTqmKpjuo4ZVJiiWscs5h5ADvNY4NwhdFh0CH4A
                wpjRa+QBAAA=
                """,
        """
                androidx/navigation/TestClassComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5DAHSB5DwLgSpBbXTVOyKkCAIESktElTZ
                dIGcxBQnMzaynajLrPgQ/qArJBYo6pKPQlxPCixRN/dx7j332sf++ev7DwBP
                8YjhidBDa9TwJNFiqo6FV0Ynh9L5diqca5vsczMYoQkvgTHURmIqklTo4+Rt
                fyQHvoSIofhMaeWfM0Qbm70qllCMUUCJoeA/Kcew1b3Anj2G1kZ3bHyqdDKa
                ZonSXlot0uSV/CgmqW8b7bydDLyx+8KOpd3b7MXgYd9qc/Cv+CHLqwzbF5vG
                sPyHsC+9GAovCOPZNCLZWDCVYMDAxoSfqJDtUDRsMTTnszjmdR7zGkXzWfns
                S1Sfz3b5DntZKvOzr0Ve46F3l4UJ6/8VpoRbDJW/6jCsHYjpG+PCwb01aSrt
                9tiT1G0zlAxXu0rLg0nWl/ZQ9FNCVrpmINKesCrk52C1o7W0+RJJDxS/NxM7
                kK9VqDXeTbRXmewpp6j5hdbG5wdzaJHMhXB38jy8M13hHmVJEIP80uNvKJ/m
                5ftkizmYYJ1sddGACmKgxii6dE7eIs/PydXTXNhAuL4AF4Q8uowrVIvwgLI4
                J93GHTTwMF94F838W5MG1Fs7QtTBcgcrHaxijUJc69DMG0dgDnU0qO4QO9x0
                KP4Ga0TtSRMDAAA=
                """,
        """
                androidx/navigation/TestClassComp.class:
                H4sIAAAAAAAA/4VRXU8TQRQ9s9vPdZEWUYuogKACRheIiYkQE60xNik1UUJi
                eJq2I067nTEz04ZHfov/gPhAoolpfPRHGe8sFR584OWeuefee+7H/P7z/SeA
                p1hnWOKqa7TsHiWKj+Qhd1KrZE9YV0+5tXU9+FIEY6j0+IgnKVeHybt2T3Rc
                ESFDYUcq6V4whKtr+zHyKETIociQc5+lZVhuXqq+zVDa6aQTnUeX5q94wxXx
                RcQMm6vNvnZUnvRGg0QqJ4ziafJafOLD1NW1ss4MO06bXW76wmyfjXk1whSm
                GcrnYgyPL5/1ovd2jCpmyghwzW+pzWHSE65tuFQ24Uppl5XbpKVda5imtGX1
                36C7wvEud5y4YDAK6SOYN2VvwMD6xB9J723Qq7vJsDI+jqOgFkRBZXwcBaWg
                Nj5eDLeCDfacha/yv74Wgkrgc7eYV5ht8dFbbf32zug0FeZJ3zHMvx8qJwei
                oUbSynYqXl7MST9W113BMN2USrSGg7Ywe5xyGGaausPTfW6k9ydk3FBKmOww
                goqjD3poOuKN9LG5SZ/9/7pgkw6Wy7ac8/cjXCavQHidMCDMZ94KeYm/BWF+
                /RSlkyx8f5Lsgw/IxmcJKCMiLOHKeXEN2TUR/8DUR3aKyjfMnmRMiIdkI8qb
                IsUqDbKaad/DGuEz4m+Q4s0DhA3UGphr4Bbm6YnbDdzB3QMwiwUsHiBnEVks
                WRQsqn8BnQupylIDAAA=
                """,
        """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41Qz2sTQRh9M5tskjUxm1g1Tav1R5E2Bzct3pRiDIgLsUIt
                8ZDTJLuk02xmYWcSeszf4tmLoAgeJHj0jxK/2RRPHoTd931v5vG++d6v399/
                AHiGfYZ9oaIsldFVoMRSToWRqQrOY236idD6gzQXvWxaAmPwL8VSBIlQ0+Dd
                +DKemBIcBveFVNKcMBQOwsMhg3NwOKyiiJKHAsrERTZlYGEVHm5UwFElqbmQ
                muHJ4H9mP6cZ09j0rA2ZhwyNwSw1iVTB29iISBhBEj5fOrQSs1CxABo6o/Mr
                aVmXuuiIob9eNT3e4h731yuPPu6XPV52WuvVMe+yV7Wm6/M27zo/P7rcL5w1
                /rIyqduFctF3rdUxswO2TsXyTUrPTZXJ0iSJs6czQ+v10yhmqA+kik8X83Gc
                nYtxQifNQToRyVBk0vLrQ+99usgm8WtpyfbZQhk5j4dSS7rtKZWaPBaNI8qu
                kO/VtFFSx6kvwiXcI3ZCnFP1Ot9Q6ex8Re1zrnlAaDXALh4S3tmocBN1GxN1
                1o1ShU//xiuw6VEtdr6g9umfNtWN4NqG41GO9/GY6sv8kUXcGsEJsRXidkhj
                71KLVohttEdgGjvYHaGkUde4p+Hl6Gr4Go0/q1d22KECAAA=
                """,
        """
                androidx/navigation/TestClassWithArgComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ9nknzGAKkDyDh/QhSikSniborQipBiEhpkWgV
                Fl0gJzGtkxkb2U7UZVZ8CH/QFRILFHXJRyGuJwG2dHN97zn33Os5np+/vv8A
                sIOnDC2uhkbL4Vms+FSecCe1io+Ede2EW/tButM9c9LW6ee6D1wRXQBjqIz4
                lMcJVyfxu/5IDFwBIUP+hVTSvWQIG5u9MlaQj5BDgSHnTqVl2Oleft0uQ7PR
                HWuXSBWPpmkslRNG8SR+LT7xSeLaWllnJgOnzT43Y2F2N3sRAr92vT74R35M
                M5Zh63LTGFb/CPaF40PuOGFBOg3JROZDyQcwsDHhZ9JX25QNmwz1+SyKgmoQ
                BRXK5rPixZewOp+1gm32qlAMLr7mg0rge1vMT2j8rz8F3GEo/TWJYeOAT99q
                6+/vjE4SYbbGjoxv66FguN6VShxM0r4wR7yfELLW1QOe9LiRvl6C5Y5SwmS7
                BD1XdKgnZiDeSM/V3k+Uk6noSSupeU8p7bL7WTTJ7Zy3gM7Avzp9yQOqYu8J
                nSvPvqF4ntEPKeYzsIlHFMuLBpQQARVG2ZWl+DmdwVJcPs/89YKbC3AhyLKr
                uEZciMdURZnoLu6hhifZwvuoZ/86eUC9lWOEHax2sNbBOjYoxY0Ozbx1DGZR
                RY14i8jitkX+N9lemKQoAwAA
                """,
        """
                androidx/navigation/TestClassWithArgComp.class:
                H4sIAAAAAAAA/41S308TQRD+9vrrehY5KmIBf6CglqpcITwJIcEawyWlJkhq
                DE/bdi3bXvfM3bbhkb/FZ1+IGhJNDPHRP8o4e1R40AeSu5mduZnv2/nmfv3+
                9gPAOlYZylx1olB2jjzFR7LLtQyVty9iXQt4HL+V+nA76tbCwYccGIPb4yPu
                BVx1vdetnmjrHFIM2U2ppN5iSJf95SZDqrzcLCCDnIM0bIp51GVgfgEOruVh
                oUCl+lDGDJX6Vfk3iKcr9LaBIgKfwd5sB2PitauiLBnDFX3O4QbDarneDzWh
                eL3RwJNKi0jxwHsp3vNhoGuhinU0bOsw2uVRX0Qb53PddDCNGYb8BRjD+pUH
                ubzCRgElzBpB5hgW62HU9XpCtyIuVexxpUKdoMReI9SNYRCQBFN/77srNO9w
                zSlnDUYpWiczJm8MSOw+5Y+kiap06tCmd86Oi45VshzLPTt26LFc27HsdOns
                eCG3ZlXZc5Z7MVHMutacVU39/Ji13PTe1EVkU8tc2s64WYO3xgzLdIOPdsLY
                CKWjMAhEtNLXDPN7Q6XlQPhqJGPZCsT25Sy0+VrYEQyTdalEYzhoiWifUw1D
                sR62edDkkTTxOFnwlRJRoqGgZudNOIza4pU032bHPM1/WLBKoqZpeAuzRmO6
                a4WiLPnb5IvmRySfojiTZJ9QtEXVFnmncop8Zf4rJk4ShKfjThDqM7Iz51W4
                jkkjNp0MGu0GLr3nWJ7ZAflM5QsmPv0XpnBeMIax6VK5cXMJyRZR+I7pd+wU
                tz5j/iTJpLCSEJL0hGgG8xLsZVTJ1yh/hxDvHiDl456PBR/38YCOWPSxhIcH
                YDEe4fEB7BiTMcoxnMRmY7gxpmKU/gBa3QlZHAQAAA==
                """,
        """
                androidx/navigation/TestGraph.class:
                H4sIAAAAAAAA/31SXWsTQRQ9M/nabKONtbaJtVZtH9QHty2+WYQa/FhYV7Ah
                IH2aZId0ks2M7E5CH/PkD/EfFB8KChL0zR8l3lmDPgjuwL33nDlzmHt3fvz8
                /BXAY+wxbAudZEYl54EWMzUUVhkddGVuX2bi/VkNjKE5EjMRpEIPgzf9kRzY
                GkoM1SOllX3KULr/oNdABVUfZdQYyvZM5Qw70X+dnzB4R4O08PDB3UEvjE+6
                x3HneQNX4NeJvMqwG5lsGIyk7WdC6TwQWhtbeOVBbGw8TVOyuhaNjSWz4LW0
                IhFWEMcnsxJ1yVyouwAGNib+XDm0T1VywLC3mPs+b3GfN6lazL3vH3hrMT/k
                ++xZzePfPlZ5kzvtIXMO67GYvTK57RhtM5OmMns0tgxbb6faqokM9Uzlqp/K
                47/3pJF0TCIZViOlZTyd9GXWFaRhWIvMQKQ9kSmHl6R/YqbZQL5QDrSXxr1/
                bHFAEyoXbbXdwCjfJlSl3KTMaVUKtEMocM1Trjy8hHdRbN9ZioEN3KXY+C1A
                nawADyt/Dm+S2n0rX8DfXaLxCasXBcFxr4jb2C3eE/0IMlg7RSnE9RDrIW5g
                g0pshmihfQqW4ya2aD+Hn+NWjuovG8mH6YwCAAA=
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
                oPQ0yQ5xks0M7E5Cjzn5Q/wHxUNBQYLe/FHimzXqQXAH3nvfN998s+/tfv/x
                6QuAJ9hj2BY6yYxKLkItZmoorDI67MrcvumP5MBWwRgaIzETYSr0MPzNlhi8
                Y6WVfcZQ2n/Yq6MCL0AZVYayfadyhp3O/62fMvjHg7QwCcDdST+KT7sncftF
                HdcQ1Ii8zrDbMdkwHEnbz4TSeSi0NrYwy8PY2HiapmS11hkbS2bha2lFIqwg
                jk9mJeqTuVBzAQxsTPyFcuiAquSQYW8xDwLe5AFvULWY+9/e8+ZifsQP2POq
                z79+8HiDO+0Rcw4bsZi9MrltG20zk6Yyezy2DFtvp9qqiYz0TOWqn8qTv+9J
                M2mbRDKsdpSW8XTSl1lXkIZhvWMGIu2JTDm8JINTM80G8qVyoLU07v1ji0Oa
                ULloq+UGRnmbkEe5QZnTqhToHqHQNU+58ugK/mWxvbMUA03cp1j/JUCNrAAf
                K38Ob5LaPSufwc+uUP+I1cuC4HhQxLvYLf4o+hBksH6OUoQbETYi3MQtKrEZ
                0SWtc7Act7FF+zmCHHdyeD8ByEcQb44CAAA=
                """
    )
