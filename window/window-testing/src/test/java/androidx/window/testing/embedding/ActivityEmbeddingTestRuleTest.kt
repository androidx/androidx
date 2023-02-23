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

package androidx.window.testing.embedding

import android.app.Activity
import android.app.Application
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.EmbeddingBackend
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Test for [ActivityEmbeddingTestRule]. */
@OptIn(ExperimentalWindowApi::class)
class ActivityEmbeddingTestRuleTest {

    @get:Rule
    val testRule: ActivityEmbeddingTestRule = ActivityEmbeddingTestRule()

    private val mockActivity: Activity = mock()

    init {
        whenever(mockActivity.applicationContext).thenReturn(mock<Application>())
    }

    @Test
    fun testActivityEmbeddingController_overrideIsActivityEmbedded() {
        assertFalse(
            ActivityEmbeddingController.getInstance(mockActivity)
                .isActivityEmbedded(mockActivity)
        )
        testRule.overrideIsActivityEmbedded(
            activity = mockActivity,
            isActivityEmbedded = true
        )
        assertTrue(
            ActivityEmbeddingController.getInstance(mockActivity)
                .isActivityEmbedded(mockActivity)
        )

        testRule.overrideIsActivityEmbedded(
            activity = mockActivity,
            isActivityEmbedded = false
        )
        assertFalse(
            ActivityEmbeddingController.getInstance(mockActivity)
                .isActivityEmbedded(mockActivity)
        )
    }

    @Test
    fun testRuleResetsOnException() {
        EmbeddingBackend.reset()
        try {
            ActivityEmbeddingTestRule().apply(
                object : Statement() {
                    override fun evaluate() {
                        throw TestException
                    }
                },
                Description.EMPTY
            ).evaluate()
        } catch (e: TestException) {
            // Throw unexpected exception
        }
        assertFalse(EmbeddingBackend.getInstance(mockActivity) is StubEmbeddingBackend)
    }

    private object TestException : Exception("TEST EXCEPTION")
}