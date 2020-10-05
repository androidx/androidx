/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications.data

import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import kotlin.reflect.KClass

// TODO: Promote this to a shared location and use across test targets.

/** Defined a package that should not be instrumented. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DoNotInstrumentPackage(val packageName: String)

/** Defines a class that should not be instrumented. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DoNotInstrumentClass(val classType: KClass<*>)

/** A test runner that allows configuring packages and classes that should not be instrumented. */
class RestrictedInstrumentationRobolectricTestRunner(private val testClass: Class<*>) :
    RobolectricTestRunner(testClass) {

    override fun createClassLoaderConfig(method: FrameworkMethod?) =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method)).apply {
            testClass.getAnnotationsByType(DoNotInstrumentPackage::class.java).forEach {
                doNotInstrumentPackage(it.packageName)
            }
            testClass.getAnnotationsByType(DoNotInstrumentClass::class.java).forEach {
                doNotInstrumentClass(it.classType.qualifiedName)
            }
        }.build()
}
