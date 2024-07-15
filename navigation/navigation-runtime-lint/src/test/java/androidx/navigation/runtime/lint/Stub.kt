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

val TEST_CLASS =
    """
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
