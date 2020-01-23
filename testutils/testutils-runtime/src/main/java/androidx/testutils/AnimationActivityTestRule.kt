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

package androidx.testutils

import android.app.Activity
import android.animation.ValueAnimator
import android.os.Build
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.intercepting.SingleActivityFactory

import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.RuntimeException
import java.lang.reflect.Field

/**
 * To solve the issue that androidx changes system settings to make animation duration to 0:
 * This ActivityTestRule subclass reads @AnimationTest annotation and change animation scale to 1
 * after activity is launched. The animation scale value is restored after test.
 * This applies both to statically created Activity (launchActivity = true) and dynamically created
 * Activity (launchActivity = false).
 */
open class AnimationActivityTestRule<T : Activity> : ActivityTestRule<T> {

    private enum class TestType {
        NORMAL, // Test without the @AnimationTest
        ANIMATION, // Test with @AnimationTest tag
    }

    /**
     * Reflect into the duration field and make it accessible.
     */
    val durationField: Field by lazy(LazyThreadSafetyMode.NONE) {
        ValueAnimator::class.java.getDeclaredField("sDurationScale").also {
            it.isAccessible = true
        }
    }

    private lateinit var testType: TestType

    constructor(activity: Class<T>) : super(activity)

    constructor(
        activity: Class<T>,
        initialTouchMode: Boolean
    ) : super(activity, initialTouchMode)

    constructor(
        activity: Class<T>,
        initialTouchMode: Boolean,
        launchActivity: Boolean
    ) : super(activity, initialTouchMode, launchActivity)

    constructor(
        singleActivityFactory: SingleActivityFactory<T>,
        initialTouchMode: Boolean,
        launchActivity: Boolean
    ) : super(singleActivityFactory, initialTouchMode, launchActivity)

    override fun afterActivityLaunched() {
        // make sure "apply()" is invoked
        if (!::testType.isInitialized) {
            throw RuntimeException("Please use @Rule for AnimationActivityTestRule")
        }
        if (testType == TestType.ANIMATION) {
            durationField.setFloat(null, 1.0f)
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        testType = TestType.NORMAL
        if (Build.VERSION.SDK_INT >= 16 &&
            (description.annotations.any { it.annotationClass == AnimationTest::class } ||
                description.testClass.annotations.any
                    { it.annotationClass == AnimationTest::class })) {
            testType = TestType.ANIMATION
            val wrappedStatement = super.apply(base, description)
            return object : Statement() {
                override fun evaluate() {
                    val savedScale = durationField.getFloat(null)
                    try {
                        wrappedStatement.evaluate()
                    } finally {
                        durationField.setFloat(null, savedScale)
                    }
                }
            }
        }
        return super.apply(base, description)
    }
}
