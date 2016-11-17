/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle

import com.android.support.lifecycle.utils.load
import com.android.support.lifecycle.utils.processClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValidCasesTest {
    @Test
    fun testTest() {
        processClass("Bar").compilesWithoutError()
    }

    @Test
    fun testInheritance() {
        processClass("InheritanceOk1").compilesWithoutError()
    }

    @Test
    fun testInheritance2() {
        processClass("InheritanceOk2").compilesWithoutError().and().generatesSources(
                load("foo", "InheritanceOk2Base_LifecycleAdapter", "expected"),
                load("foo", "InheritanceOk2Derived_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testInheritance3() {
        processClass("InheritanceOk3").compilesWithoutError().and().generatesSources(
                load("foo", "InheritanceOk3Base_LifecycleAdapter", "expected"),
                load("foo", "InheritanceOk3Derived_LifecycleAdapter", "expected")
        )
    }

    @Test
    fun testNoPackageClass() {
        processClass("NoPackageOk", "").compilesWithoutError()
    }

    @Test
    fun testInterface1(){
        processClass("InterfaceOk1").compilesWithoutError()
    }

    @Test
    fun testInterface2() {
        processClass("InterfaceOk2").compilesWithoutError().and().generatesSources(
                load("foo", "InterfaceOk2Base_LifecycleAdapter", "expected"),
                load("foo", "InterfaceOk2Derived_LifecycleAdapter", "expected"),
                load("foo", "InterfaceOk2Interface_LifecycleAdapter", "expected")
        )
    }


}