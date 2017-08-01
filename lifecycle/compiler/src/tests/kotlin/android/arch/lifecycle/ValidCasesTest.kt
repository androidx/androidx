/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.lifecycle

import android.arch.lifecycle.utils.load
import android.arch.lifecycle.utils.processClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValidCasesTest {
    @Test
    fun testTest() {
        processClass("foo.Bar").compilesWithoutError()
    }

    @Test
    fun testOnAny() {
        processClass("foo.OnAnyMethod").compilesWithoutError().and().generatesSources(
                load("foo.OnAnyMethod_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testInheritance() {
        processClass("foo.InheritanceOk1").compilesWithoutError()
    }

    @Test
    fun testInheritance2() {
        processClass("foo.InheritanceOk2").compilesWithoutError().and().generatesSources(
                load("foo.InheritanceOk2Base_LifecycleAdapter", "expected"),
                load("foo.InheritanceOk2Derived_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testInheritance3() {
        processClass("foo.InheritanceOk3").compilesWithoutError().and().generatesSources(
                load("foo.InheritanceOk3Base_LifecycleAdapter", "expected"),
                load("foo.InheritanceOk3Derived_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testNoPackageClass() {
        processClass("NoPackageOk").compilesWithoutError()
    }

    @Test
    fun testInterface1(){
        processClass("foo.InterfaceOk1").compilesWithoutError()
    }

    @Test
    fun testInterface2() {
        processClass("foo.InterfaceOk2").compilesWithoutError().and().generatesSources(
                load("foo.InterfaceOk2Base_LifecycleAdapter", "expected"),
                load("foo.InterfaceOk2Derived_LifecycleAdapter", "expected"),
                load("foo.InterfaceOk2Interface_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testInheritanceDifferentPackages1() {
        processClass("foo.DifferentPackagesBase1",
                "bar.DifferentPackagesDerived1").compilesWithoutError().and().generatesSources(
                load("foo.DifferentPackagesBase1_LifecycleAdapter", "expected"),
                load("bar.DifferentPackagesDerived1_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testInheritanceDifferentPackages2() {
        processClass("foo.DifferentPackagesBase2",
                "bar.DifferentPackagesDerived2").compilesWithoutError().and().generatesSources(
                load("foo.DifferentPackagesBase2_LifecycleAdapter", "expected"),
                load("bar.DifferentPackagesDerived2_LifecycleAdapter", "expected")
        )
    }
}
