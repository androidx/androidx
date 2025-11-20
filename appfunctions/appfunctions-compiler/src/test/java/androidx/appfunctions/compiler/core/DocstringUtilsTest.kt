/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocstringUtilsTest {
    @Test
    fun getParamDescriptionsFromKDoc_multiLineDescription() {
        assertThat(getParamDescriptionsFromKDoc(MULTI_LINE_PARAM_DESCRIPTION_DOCSTRING))
            .containsExactly(
                "input",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
            )
    }

    @Test
    fun getParamDescriptionsFromKDoc_multipleParameters() {
        assertThat(getParamDescriptionsFromKDoc(MULTI_PARAM_DESCRIPTIONS_DOCSTRING))
            .containsExactly("input1", "First parameter.", "input2", "Second parameter.")
    }

    @Test
    fun getParamDescriptionsFromKDoc_multipleTags() {
        assertThat(getParamDescriptionsFromKDoc(MULTI_TAG_DOCSTRING))
            .containsExactly("input1", "First parameter.", "input2", "Second parameter.")
    }

    @Test
    fun getParamDescriptionsFromKDoc_noParams() {
        assertThat(getParamDescriptionsFromKDoc(NO_PARAMS_DOCSTRING)).isEmpty()
    }

    @Test
    fun getParamDescriptionsFromKDoc_descriptionStartsOnNextLine() {
        assertThat(getParamDescriptionsFromKDoc(PARAM_DESCRIPTION_ON_NEXT_LINE_DOCSTRING))
            .containsExactly("input1", "First parameter.", "input2", "Second parameter.")
    }

    @Test
    fun sanitizeKDoc_multiLineDescription() {
        assertThat(sanitizeKDoc(MULTI_LINE_PARAM_DESCRIPTION_DOCSTRING))
            .isEqualTo("Fake docstring to test param descriptions.")
    }

    @Test
    fun sanitizeKDoc_multipleParameters() {
        assertThat(sanitizeKDoc(MULTI_PARAM_DESCRIPTIONS_DOCSTRING))
            .isEqualTo("Fake docstring to test param descriptions.")
    }

    @Test
    fun sanitizeKDoc_multipleTags() {
        assertThat(sanitizeKDoc(MULTI_TAG_DOCSTRING))
            .isEqualTo("Fake docstring to test param descriptions.")
    }

    @Test
    fun sanitizeKDoc_customTag() {
        assertThat(sanitizeKDoc(CUSTOM_TAG_DOCSTRING))
            .isEqualTo(
                """
                    Fake docstring to test param descriptions.
                    
                    @customTag Custom tag content."""
                    .trimIndent()
            )
    }

    @Test
    fun sanitizeKDoc_noParams() {
        assertThat(sanitizeKDoc(NO_PARAMS_DOCSTRING))
            .isEqualTo("Fake docstring to test param descriptions.")
    }

    @Test
    fun sanitizeKDoc_nonTagAtUsage() {
        assertThat(sanitizeKDoc(NON_TAG_AT_USAGE_DOCSTRING))
            .isEqualTo(NON_TAG_AT_USAGE_DOCSTRING.trim())
    }

    @Test
    fun getResponseDescriptionFromKDoc_multipleTags() {
        assertThat(getResponseDescriptionFromKDoc(MULTI_TAG_DOCSTRING))
            .isEqualTo("The response description.")
    }

    @Test
    fun getResponseDescriptionFromKDoc_multiLineDescription() {
        assertThat(getResponseDescriptionFromKDoc(MULTI_LINE_RESPONSE_DESCRIPTION_DOCSTRING))
            .isEqualTo(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
            )
    }

    companion object {

        private const val MULTI_LINE_PARAM_DESCRIPTION_DOCSTRING =
            """Fake docstring to test param descriptions.

@param input Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
ullamco laboris nisi ut aliquip ex ea commodo consequat."""

        private const val MULTI_PARAM_DESCRIPTIONS_DOCSTRING =
            """Fake docstring to test param descriptions.

@param input1 First parameter.
@param input2 Second parameter."""

        private const val MULTI_TAG_DOCSTRING =
            """Fake docstring to test param descriptions.

@constructor Constructor description.
@receiver Receiver description.
@param input1 First parameter.
@param input2 Second parameter.
@property property description.
@return The response description.
@throws IllegalArgumentException when some condition happens.
@exception IllegalArgumentException
@sample sample content
@author Author Name
@since 1990
@suppress ...
@see anotherFunction"""

        private const val CUSTOM_TAG_DOCSTRING =
            """Fake docstring to test param descriptions.

@param input A parameter.
@customTag Custom tag content.
@throws IllegalArgumentException"""

        private const val NO_PARAMS_DOCSTRING =
            """Fake docstring to test param descriptions.

@throws IllegalArgumentException
@see anotherFunction"""

        private const val PARAM_DESCRIPTION_ON_NEXT_LINE_DOCSTRING =
            """Fake docstring to test param descriptions.

@param input1
First parameter.
@param input2

Second parameter.

@see Something else."""

        private const val NON_TAG_AT_USAGE_DOCSTRING =
            """Fake SendEmail app function description.

Sends an email to email address in format xx@gmail.com
    """

        private const val MULTI_LINE_RESPONSE_DESCRIPTION_DOCSTRING =
            """Fake docstring to test response description.

@return Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
ullamco laboris nisi ut aliquip ex ea commodo consequat."""
    }
}
