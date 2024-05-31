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

package androidx.compose.testutils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule

/**
 * A Rule that allows simulation of parameterized tests that change a Composable input. Make sure to
 * set the content using [setContent] and react to parameter changes proposed by the content block
 * parameter. Later, use [forEachParameter] to execute a list of changes in the parameters of the
 * used composable.
 */
interface ParameterizedComposeTestRule<T> : ComposeTestRule {
    /**
     * Sets the content to be tested, make sure to use config to set the inputs of the tested
     * Composable.
     */
    fun setContent(content: @Composable (parameter: T) -> Unit)

    /**
     * Runs [block] for each config in [parameters] making sure that composition is reset between
     * the runs effectively simulating a parameterized test run.
     */
    fun forEachParameter(parameters: List<T>, block: (T) -> Unit)
}

/**
 * A helper class to run parameterized tests with composition. This is useful for tests that change
 * Composables parameters in parameterized fashion.
 */
private class ParameterizedComposeTestRuleImpl<T>(private val rule: ComposeContentTestRule) :
    ParameterizedComposeTestRule<T>, ComposeTestRule by rule {
    private var content: @Composable (config: T) -> Unit = {}
    private var contentInitialized = false

    override fun setContent(content: @Composable (config: T) -> Unit) {
        check(!contentInitialized) { "SetContent should be called only once per test case." }
        this.content = content
        contentInitialized = true
    }

    override fun forEachParameter(parameters: List<T>, block: T.() -> Unit) {
        check(parameters.isNotEmpty()) { "Config List Cannot Be Empty" }
        var configState by mutableStateOf(parameters.first())

        // setting content on the first config
        rule.setContent { key(configState) { content(configState) } }
        runBlockCheck(block, configState)
        rule.mainClock.advanceTimeByFrame() // push time forward

        // changing contents on remaining configs
        for (index in 1..parameters.lastIndex) {
            configState = parameters[index] // change params
            rule.mainClock.advanceTimeByFrame() // push time forward
            runBlockCheck(block, configState)
        }
    }

    private fun runBlockCheck(onParamConfigBlock: T.() -> Unit, currentConfig: T) {
        try {
            onParamConfigBlock.invoke(currentConfig)
        } catch (error: AssertionError) {
            val newErrorMessage = "Error on Config=$currentConfig"
            throw AssertionError(newErrorMessage, error)
        }
    }
}

/** Creates a [ParameterizedComposeTestRule] to simulate input parameterization in tests. */
fun <T> createParameterizedComposeTestRule(): ParameterizedComposeTestRule<T> {
    val contentRule = createComposeRule()
    return ParameterizedComposeTestRuleImpl(contentRule)
}
