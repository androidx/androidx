/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.testing.unit

import androidx.glance.appwidget.EmittableCircularProgressIndicator
import androidx.glance.appwidget.EmittableLinearProgressIndicator
import androidx.glance.layout.EmittableColumn
import androidx.glance.testing.unit.getGlanceNodeAssertionFor
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert
import org.junit.Test

class UnitTestFiltersTest {
    @Test
    fun isCircularProgressIndicator_match() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableCircularProgressIndicator()
            },
            onNodeMatcher = isIndeterminateCircularProgressIndicator()
        )

        nodeAssertion.assertExists()
        // no error
    }

    @Test
    fun isCircularProgressIndicator_noMatch_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableLinearProgressIndicator()
            },
            onNodeMatcher = isIndeterminateCircularProgressIndicator()
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertExists()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed assertExists" +
                    "\nReason: Expected '1' node(s) matching condition: " +
                    "is an indeterminate circular progress indicator, but found '0'"
            )
    }

    @Test
    fun isIndeterminateLinearProgressIndicator_match() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableLinearProgressIndicator().apply {
                    indeterminate = true
                }
            },
            onNodeMatcher = isIndeterminateLinearProgressIndicator()
        )

        nodeAssertion.assertExists()
        // no error
    }

    @Test
    fun isIndeterminateLinearProgressIndicator_noMatch_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableCircularProgressIndicator()
            },
            onNodeMatcher = isIndeterminateLinearProgressIndicator()
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertExists()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed assertExists" +
                    "\nReason: Expected '1' node(s) matching condition: " +
                    "is an indeterminate linear progress indicator, but found '0'"
            )
    }

    @Test
    fun isLinearProgressIndicator_match() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableLinearProgressIndicator().apply {
                    progress = 10.0f
                }
            },
            onNodeMatcher = isLinearProgressIndicator(10.0f)
        )

        nodeAssertion.assertExists()
        // no error
    }

    @Test
    fun isLinearProgressIndicator_progressValueNotMatch_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children += EmittableLinearProgressIndicator().apply {
                    indeterminate = false
                    progress = 10.0f
                }
            },
            onNodeMatcher = isLinearProgressIndicator(11.0f)
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertExists()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed assertExists" +
                    "\nReason: Expected '1' node(s) matching condition: " +
                    "is a linear progress indicator with progress value: 11.0, but found '0'"
            )
    }
}
