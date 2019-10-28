/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] which can be used to run the same test multiple times. Useful when trying
 * to debug flaky tests.
 *
 * To use this [TestRule] do the following. <br></br><br></br>
 *
 * Add the Rule to your JUnit test. <br></br><br></br>
 * `RepeatRule mRepeatRule = new RepeatRule();
` *
 * <br></br><br></br>
 *
 * Add the [Repeat] annotation to your test case. <br></br><br></br>
 * `public void yourTestCase() {
 *
 * }
` *
 * <br></br><br></br>
 */
class RepeatRule : TestRule {
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Repeat(val times: Int = 1)

    class RepeatStatement(private val mTimes: Int, private val mStatement: Statement) :
        Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            for (i in 0 until mTimes) {
                mStatement.evaluate()
            }
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        val repeat = description.getAnnotation(Repeat::class.java)
        return if (repeat != null) {
            RepeatStatement(repeat.times, base)
        } else {
            base
        }
    }
}