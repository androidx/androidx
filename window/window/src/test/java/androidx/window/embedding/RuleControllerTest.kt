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

package androidx.window.embedding

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [RuleController] */
class RuleControllerTest {

    private lateinit var mockEmbeddingBackend: EmbeddingBackend
    private lateinit var mockContext: Context
    private lateinit var ruleController: RuleController

    @Before
    fun setUp() {
        mockEmbeddingBackend = mock()
        ruleController = RuleController(mockEmbeddingBackend)

        mockContext = mock()
        doReturn(mockContext).whenever(mockContext).applicationContext
    }

    @Test
    fun testAddRule() {
        ruleController.addRule(mock())

        verify(mockEmbeddingBackend).addRule(any())
    }

    @Test
    fun testRemoveRule() {
        ruleController.removeRule(mock())

        verify(mockEmbeddingBackend).removeRule(any())
    }

    @Test
    fun testSetRules() {
        ruleController.setRules(mock())

        verify(mockEmbeddingBackend).setRules(any())
    }

    @Test
    fun testClearRules() {
        ruleController.clearRules()

        verify(mockEmbeddingBackend).setRules(eq(emptySet()))
    }

    @Test
    fun testGetRules() {
        val rules = mock<Set<EmbeddingRule>>()

        doReturn(rules).whenever(mockEmbeddingBackend).getRules()

        assertEquals(rules, ruleController.getRules())
    }

    @Test
    fun testGetInstance() {
        EmbeddingBackend.overrideDecorator(object : EmbeddingBackendDecorator {
            override fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend =
                mockEmbeddingBackend
        })

        val controller = RuleController.getInstance(mockContext)
        controller.clearRules()

        verify(mockEmbeddingBackend).setRules(eq(emptySet()))

        EmbeddingBackend.reset()
    }
}
