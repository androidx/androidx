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

package androidx.kruth

/*
 * Copyright (c) 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.kruth.ChainingTest.MyObjectSubject.Companion.myObjects
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.Subject.Factory
import org.junit.Rule
import org.junit.Test

/** Tests for chained subjects (produced with [Subject.check], etc.). */
class ChainingTest {
    @get:Rule
    val expectFailure = ExpectFailure()

    private val throwable = Throwable("root")

    @Test
    fun noChaining() {
        expectFailureWhenTestingThat("root").isThePresentKingOfFrance()
        assertNoCause("message")
    }

    @Test
    fun oneLevel() {
        expectFailureWhenTestingThat("root").delegatingTo("child").isThePresentKingOfFrance()
        assertNoCause("message")
    }

    @Test
    fun twoLevels() {
        expectFailureWhenTestingThat("root")
            .delegatingTo("child")
            .delegatingTo("grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("message")
    }

    @Test
    fun noChainingRootThrowable() {
        expectFailureWhenTestingThat(throwable).isThePresentKingOfFrance()
        assertHasCause("message")
    }

    @Test
    fun oneLevelRootThrowable() {
        expectFailureWhenTestingThat(throwable).delegatingTo("child").isThePresentKingOfFrance()
        assertHasCause("message")
    }

    @Test
    fun twoLevelsRootThrowable() {
        expectFailureWhenTestingThat(throwable)
            .delegatingTo("child")
            .delegatingTo("grandchild")
            .isThePresentKingOfFrance()
        assertHasCause("message")
    }

    // e.g., future.failureCause()
    @Test
    fun oneLevelDerivedThrowable() {
        expectFailureWhenTestingThat("root").delegatingTo(throwable).isThePresentKingOfFrance()
        assertHasCause("message")
    }

    @Test
    fun twoLevelsDerivedThrowableMiddle() {
        expectFailureWhenTestingThat("root")
            .delegatingTo(throwable)
            .delegatingTo("grandchild")
            .isThePresentKingOfFrance()
        assertHasCause("message")
    }

    @Test
    fun twoLevelsDerivedThrowableLast() {
        expectFailureWhenTestingThat("root")
            .delegatingTo("child")
            .delegatingTo(throwable)
            .isThePresentKingOfFrance()
        assertHasCause("message")
    }

    @Test
    fun oneLevelNamed() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamed("child", "child")
            .isThePresentKingOfFrance()
        assertNoCause("value of    : myObject.child\nmessage\nmyObject was: root")
    }

    @Test
    fun twoLevelsNamed() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamed("child", "child")
            .delegatingToNamed("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of    : myObject.child.grandchild\nmessage\nmyObject was: root")
    }

    @Test
    fun twoLevelsOnlyFirstNamed() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamed("child", "child")
            .delegatingTo("grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("message\nmyObject was: root")
    }

    @Test
    fun twoLevelsOnlySecondNamed() {
        expectFailureWhenTestingThat("root")
            .delegatingTo("child")
            .delegatingToNamed("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of    : myObject.grandchild\nmessage\nmyObject was: root")
    }

    @Test
    fun oneLevelNamedNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamedNoNeedToDisplayBoth("child", "child")
            .isThePresentKingOfFrance()
        assertNoCause("value of: myObject.child\nmessage")
    }

    @Test
    fun twoLevelsNamedNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamedNoNeedToDisplayBoth("child", "child")
            .delegatingToNamedNoNeedToDisplayBoth("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of: myObject.child.grandchild\nmessage")
    }

    @Test
    fun twoLevelsOnlyFirstNamedNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamedNoNeedToDisplayBoth("child", "child")
            .delegatingTo("grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("message")
    }

    @Test
    fun twoLevelsOnlySecondNamedNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingTo("child")
            .delegatingToNamedNoNeedToDisplayBoth("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of: myObject.grandchild\nmessage")
    }

    @Test
    fun twoLevelsNamedOnlyFirstNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamedNoNeedToDisplayBoth("child", "child")
            .delegatingToNamed("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of    : myObject.child.grandchild\nmessage\nmyObject was: root")
    }

    @Test
    fun twoLevelsNamedOnlySecondNoNeedToDisplayBoth() {
        expectFailureWhenTestingThat("root")
            .delegatingToNamed("child", "child")
            .delegatingToNamedNoNeedToDisplayBoth("grandchild", "grandchild")
            .isThePresentKingOfFrance()
        assertNoCause("value of    : myObject.child.grandchild\nmessage\nmyObject was: root")
    }

    @Test
    fun namedAndMessage() {
        expectFailure
            .whenTesting()
            .withMessage("prefix")
            .about(myObjects())
            .that("root")
            .delegatingToNamed("child", "child")
            .isThePresentKingOfFrance()
        assertNoCause("prefix\nvalue of    : myObject.child\nmessage\nmyObject was: root")
    }

    @Test
    fun checkFail() {
        expectFailureWhenTestingThat("root").doCheckFail()
        assertNoCause("message")
    }

    @Test
    fun checkFailWithName() {
        expectFailureWhenTestingThat("root").doCheckFail("child")
        assertNoCause("message\nvalue of    : myObject.child\nmyObject was: root")
    }

    @Test
    fun badFormat() {
        val stringSubjectWithBadFormat = object : StringSubject("root", FailureMetadata()) {
            fun checkWithBadFormat() {
                check("%s %s", 1, 2, 3)
            }
        }

        try {
            @Suppress("LenientFormatStringValidation") // Intentional for testing.
            stringSubjectWithBadFormat.checkWithBadFormat()
            assert_().fail()
        } catch (expected: IllegalArgumentException) {
        }
    }

    /*
     * TODO(cpovirk): It would be nice to have multiple Subject subclasses so that we know we're
     *  pulling the type from the right link in the chain. But we get some coverage of that from
     *  other tests like MultimapSubjectTest.
     */
    private class MyObjectSubject(
        metadata: FailureMetadata,
        actual: Any?
    ) : Subject<Any>(metadata, actual) {
        companion object {
            private val FACTORY: Factory<MyObjectSubject, Any> = Factory { metadata, actual ->
                MyObjectSubject(metadata, actual)
            }

            fun myObjects(): Factory<MyObjectSubject, Any> {
                return FACTORY
            }
        }

        /** Runs a check that always fails with the generic message "message." */
        fun isThePresentKingOfFrance() {
            failWithoutActual(simpleFact("message"))
        }

        fun doCheckFail() {
            check().withMessage("message").fail()
        }

        fun doCheckFail(name: String) {
            check(name).withMessage("message").fail()
        }

        /**
         * Returns a new [MyObjectSubject] for the given actual value, chaining it to the current
         * subject with [Subject.check].
         */
        fun delegatingTo(actual: Any): MyObjectSubject {
            return check().about(myObjects()).that(actual)
        }

        /**
         * Returns a new [MyObjectSubject] for the given actual value, chaining it to the current
         * subject with [Subject.check].
         */
        fun delegatingToNamed(actual: Any, name: String): MyObjectSubject {
            return check(name).about(myObjects()).that(actual)
        }

        fun delegatingToNamedNoNeedToDisplayBoth(
            actual: Any,
            name: String
        ): MyObjectSubject {
            return checkNoNeedToDisplayBothValues(name).about(myObjects()).that(actual)
        }
    }

    private fun expectFailureWhenTestingThat(actual: Any): MyObjectSubject {
        return expectFailure.whenTesting().about(myObjects()).that(actual)
    }

    private fun assertNoCause(message: String) {
        ExpectFailure.assertThat(expectFailure.getFailure()).hasMessageThat().isEqualTo(message)
        ExpectFailure.assertThat(expectFailure.getFailure()).hasCauseThat().isNull()
    }

    private fun assertHasCause(message: String) {
        ExpectFailure.assertThat(expectFailure.getFailure()).hasMessageThat().isEqualTo(message)
        ExpectFailure.assertThat(expectFailure.getFailure()).hasCauseThat().isEqualTo(throwable)
    }
}
