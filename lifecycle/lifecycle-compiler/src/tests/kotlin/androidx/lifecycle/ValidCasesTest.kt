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

package androidx.lifecycle

import androidx.lifecycle.utils.load
import androidx.lifecycle.utils.processClass
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.lang.Exception
import java.net.URLClassLoader
import javax.tools.StandardLocation

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
        ).and().generatesProGuardRule("foo.OnAnyMethod.pro")
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
                .and().generatesProGuardRule("foo.InheritanceOk2Base.pro")
                .and().generatesProGuardRule("foo.InheritanceOk2Derived.pro")
    }

    @Test
    fun testInheritance3() {
        processClass("foo.InheritanceOk3").compilesWithoutError().and().generatesSources(
                load("foo.InheritanceOk3Base_LifecycleAdapter", "expected"),
                load("foo.InheritanceOk3Derived_LifecycleAdapter", "expected")
        )
                .and().generatesProGuardRule("foo.InheritanceOk3Base.pro")
                .and().generatesProGuardRule("foo.InheritanceOk3Derived.pro")
    }

    @Test
    fun testNoPackageClass() {
        processClass("NoPackageOk").compilesWithoutError()
    }

    @Test
    fun testInterface1() {
        processClass("foo.InterfaceOk1").compilesWithoutError()
    }

    @Test
    fun testInterface2() {
        processClass("foo.InterfaceOk2").compilesWithoutError().and().generatesSources(
                load("foo.InterfaceOk2Base_LifecycleAdapter", "expected"),
                load("foo.InterfaceOk2Derived_LifecycleAdapter", "expected"),
                load("foo.InterfaceOk2Interface_LifecycleAdapter", "expected")
        )
                .and().generatesProGuardRule("foo.InterfaceOk2Base.pro")
                .and().generatesProGuardRule("foo.InterfaceOk2Derived.pro")
                .and().generatesProGuardRule("foo.InterfaceOk2Interface.pro")
    }

    @Test
    fun testInheritanceDifferentPackages1() {
        processClass("foo.DifferentPackagesBase1",
                "bar.DifferentPackagesDerived1").compilesWithoutError().and().generatesSources(
                load("foo.DifferentPackagesBase1_LifecycleAdapter", "expected"),
                load("bar.DifferentPackagesDerived1_LifecycleAdapter", "expected")
        )
                .and().generatesProGuardRule("foo.DifferentPackagesBase1.pro")
                .and().generatesProGuardRule("bar.DifferentPackagesDerived1.pro")
    }

    @Test
    fun testInheritanceDifferentPackages2() {
        processClass("foo.DifferentPackagesBase2",
                "bar.DifferentPackagesDerived2").compilesWithoutError().and().generatesSources(
                load("foo.DifferentPackagesBase2_LifecycleAdapter", "expected"),
                load("bar.DifferentPackagesDerived2_LifecycleAdapter", "expected")
        )
                .and().generatesProGuardRule("foo.DifferentPackagesPreBase2.pro")
                .and().generatesProGuardRule("foo.DifferentPackagesBase2.pro")
                .and().generatesProGuardRule("bar.DifferentPackagesDerived2.pro")
    }

    private fun <T> CompileTester.GeneratedPredicateClause<T>.generatesProGuardRule(name: String):
            CompileTester.SuccessfulFileClause<T> {
        return generatesFileNamed(StandardLocation.CLASS_OUTPUT, "", "META-INF/proguard/$name")
    }

    @Test
    fun testJar() {
        JavaSourcesSubject.assertThat(load("foo.DerivedFromJar", ""))
                .withClasspathFrom(libraryClassLoader())
                .processedWith(LifecycleProcessor())
                .compilesWithoutError().and()
                .generatesSources(load("foo.DerivedFromJar_LifecycleAdapter", "expected"))
    }

    @Test
    fun testExtendFromJarFailToGenerateAdapter() {
        val compileTester = JavaSourcesSubject.assertThat(load("foo.DerivedFromJar1", ""))
                .withClasspathFrom(libraryClassLoader())
                .processedWith(LifecycleProcessor())
                .compilesWithoutError()
        compileTester.withWarningContaining("Failed to generate an Adapter for")
        doesntGenerateClass(compileTester, "test.library", "ObserverNoAdapter_LifecycleAdapter")
        doesntGenerateClass(compileTester, "foo", "DerivedFromJar1_LifecycleAdapter")
    }

    // compile-testing has fancy, but not always convenient API
    private fun doesntGenerateClass(compile: CompileTester.SuccessfulCompilationClause,
                                    packageName: String, className: String) {
        try {
            compile.and().generatesFileNamed(StandardLocation.CLASS_OUTPUT,
                    packageName, "$className.class")
            throw Exception("$packageName.$className shouldn't be generated")
        } catch (e: AssertionError) {
        }
    }

    private fun libraryClassLoader(): URLClassLoader {
        val jarUrl = File("src/tests/test-data/lib/test-library.jar").toURI().toURL()
        return URLClassLoader(arrayOf(jarUrl), this.javaClass.classLoader)
    }
}
