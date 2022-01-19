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
package androidx.test.junitgtest

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.Filterable
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.notification.RunNotifier

/**
 * Custom Runner that implements a bridge between JUnit and GTest.
 *
 * Use this Runner in a `@RunWith` annotation together with a `@TargetLibrary`
 * annotation on an empty class to create a connected test that consists of native
 * tests written against the Google Test Framework.
 */
class GtestRunner(testClass: Class<*>) : Runner(),
    Filterable {
    private val targetClass: Class<*>
    private var description: Description
    override fun getDescription(): Description {
        return description
    }

    @Throws(NoTestsRemainException::class)
    override fun filter(filter: Filter) {
        val children: List<Description> = description.children
        description = Description.createSuiteDescription(targetClass)
        val iter = children.iterator()
        while (iter.hasNext()) {
            val testDescription = iter.next()
            if (filter.shouldRun(testDescription)) {
                description.addChild(testDescription)
            }
        }
        if (description.children.isEmpty()) {
            throw NoTestsRemainException()
        }
    }

    override fun run(notifier: RunNotifier) {
        for (description in description.children) {
            addTest(description.methodName)
        }
        run(targetClass.name, notifier)
    }

    private companion object {
        private var onceFlag = false
    }

    init {
        synchronized(GtestRunner::class.java) {
            check(!onceFlag) { "Error multiple GtestRunners defined" }
            onceFlag = true
        }
        targetClass = testClass
        val library = testClass.getAnnotation(TargetLibrary::class.java)
            ?: throw IllegalStateException("Missing required @TargetLibrary annotation")
        System.loadLibrary(library.libraryName)
        description = Description.createSuiteDescription(testClass)
        // The nInitialize native method will populate the description based on
        // GTest test data.
        initialize(testClass.name, description)
    }

    private external fun initialize(className: String, description: Description)
    private external fun addTest(testName: String)
    private external fun run(className: String, notifier: RunNotifier): Boolean
}