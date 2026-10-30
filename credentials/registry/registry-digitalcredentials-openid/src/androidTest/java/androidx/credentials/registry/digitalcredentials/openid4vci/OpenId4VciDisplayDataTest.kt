/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.openid4vci

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OpenId4VciDisplayDataTest {
    @Test
    fun displayData_emptyEntries_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData(entries = emptyList())
        }
    }

    @Test
    fun entry_blankId_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Entry(id = "  ")
        }
    }

    @Test
    fun entry_emptyId_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { OpenId4VciDisplayData.Entry(id = "") }
    }

    @Test
    fun explainer_empty_throwsException() {
        assertThrows(IllegalArgumentException::class.java) { OpenId4VciDisplayData.Explainer() }
    }

    @Test
    fun explainer_blankIssuer_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(perIssuer = mapOf(" " to "explainer"))
        }
    }

    @Test
    fun explainer_blankText_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(perIssuer = mapOf("https://issuer.com" to ""))
        }
    }

    @Test
    fun explainer_blankDefault_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = "  ")
        }
    }

    @Test
    fun explainer_validMarkdownLink_renderedUnderLimit_success() {
        // Raw length is > 150 characters, but rendered text "Terms of Service" is 16 chars <= 150
        val longUrl = "https://example.com/terms?query=" + "a".repeat(150)
        val rawText = "Please read our [Terms of Service]($longUrl) carefully."
        val explainer = OpenId4VciDisplayData.Explainer(default = rawText)
        assertThat(explainer.default).isEqualTo(rawText)
    }

    @Test
    fun explainer_plainText_underLimit_success() {
        val exactLimitText = "a".repeat(OpenId4VciDisplayData.Explainer.MAX_EXPLAINER_LENGTH)
        val explainer = OpenId4VciDisplayData.Explainer(default = exactLimitText)
        assertThat(explainer.default).isEqualTo(exactLimitText)
    }

    @Test
    fun explainer_plainText_exceeds150_throwsException() {
        val tooLongText = "a".repeat(OpenId4VciDisplayData.Explainer.MAX_EXPLAINER_LENGTH + 1)
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = tooLongText)
        }
    }

    @Test
    fun explainer_renderedText_exceeds150_throwsException() {
        // The link text itself exceeds 150 characters
        val tooLongLinkText = "a".repeat(OpenId4VciDisplayData.Explainer.MAX_EXPLAINER_LENGTH + 1)
        val markdown = "[$tooLongLinkText](https://example.com)"
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = markdown)
        }
    }

    @Test
    fun explainer_perIssuer_exceeds150_throwsException() {
        val tooLongText = "a".repeat(OpenId4VciDisplayData.Explainer.MAX_EXPLAINER_LENGTH + 1)
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(perIssuer = mapOf("https://issuer.com" to tooLongText))
        }
    }

    @Test
    fun explainer_escapedBrackets_countedAsLiteral() {
        // Escaped brackets are not parsed as markdown links; raw text is counted as literal
        val escapedText = "\\[not a link\\](https://example.com)"
        val explainer = OpenId4VciDisplayData.Explainer(default = escapedText)
        assertThat(explainer.default).isEqualTo(escapedText)

        // When the full escaped string exceeds 150 characters, it fails
        val longEscapedText = "\\[" + "a".repeat(150) + "\\](https://example.com)"
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = longEscapedText)
        }
    }

    @Test
    fun explainer_invalidUrlScheme_countedAsLiteral() {
        // Non-http/https URL is not a valid web link; counted as raw literal string
        val longCustomScheme = "[Link](" + "custom://".repeat(20) + ")"
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = longCustomScheme)
        }
    }

    @Test
    fun explainer_nestedBrackets_nonLink_countsLinkText() {
        // [text with [nested] brackets](https://example.com) has non-link nested brackets
        // Renders as "text with [nested] brackets"
        val rawText = "[text with [nested] brackets](https://example.com)"
        val renderedLen = ExplainerTextParser.computeRenderedLength(rawText)
        assertThat(renderedLen).isEqualTo("text with [nested] brackets".length)

        val explainer = OpenId4VciDisplayData.Explainer(default = rawText)
        assertThat(explainer.default).isEqualTo(rawText)
    }

    @Test
    fun explainer_nestedLinks_innerLinkRenderedAndOuterTreatedAsLiteral() {
        // When an outer link contains an inner markdown link, the inner link is rendered
        // and the outer structure is kept as plain literal text:
        // [[inner](https://inner.com)](https://outer.com) -> "[inner](https://outer.com)"
        val rawText = "[[inner](https://inner.com)](https://outer.com)"
        val renderedLen = ExplainerTextParser.computeRenderedLength(rawText)
        assertThat(renderedLen).isEqualTo("[inner](https://outer.com)".length)

        val explainer = OpenId4VciDisplayData.Explainer(default = rawText)
        assertThat(explainer.default).isEqualTo(rawText)

        // When the outer literal URL makes the rendered text exceed 150 characters, it fails
        val longOuterUrl = "https://outer.com/search?q=" + "a".repeat(150)
        val longNested = "[[inner](https://inner.com)]($longOuterUrl)"
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciDisplayData.Explainer(default = longNested)
        }
    }

    @Test
    fun explainer_multipleNestedLinks_countsRenderedText() {
        // Outer link containing multiple inner links
        // "Prefix [see [link1](https://a.com) and [link2](https://b.com)](https://outer.com) End"
        // Renders to: "Prefix [see link1 and link2](https://outer.com) End"
        val rawText =
            "Prefix [see [link1](https://a.com) and [link2](https://b.com)](https://outer.com) End"
        val expectedRendered = "Prefix [see link1 and link2](https://outer.com) End"
        val renderedLen = ExplainerTextParser.computeRenderedLength(rawText)
        assertThat(renderedLen).isEqualTo(expectedRendered.length)

        val explainer = OpenId4VciDisplayData.Explainer(default = rawText)
        assertThat(explainer.default).isEqualTo(rawText)
    }

    @Test
    fun explainer_unclosedBrackets_treatedAsLiteral() {
        // Unclosed '[' or '(' are treated as literal text
        val rawText = "Check [unclosed bracket and [link](https://example.com)"
        val expectedRendered = "Check [unclosed bracket and link"
        val renderedLen = ExplainerTextParser.computeRenderedLength(rawText)
        assertThat(renderedLen).isEqualTo(expectedRendered.length)

        val explainer = OpenId4VciDisplayData.Explainer(default = rawText)
        assertThat(explainer.default).isEqualTo(rawText)
    }

    @Test
    fun explainer_emptyLinkTextOrUrl_treatedAsLiteral() {
        // Empty link text []() or [](https://example.com) is treated as literal
        val rawText1 = "Empty []() link"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText1)).isEqualTo(rawText1.length)

        val rawText2 = "Empty [](https://example.com) link"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText2)).isEqualTo(rawText2.length)

        val rawText3 = "Empty [text]() link"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText3)).isEqualTo(rawText3.length)
    }

    @Test
    fun explainer_unclosedSquareBrackets_treatedAsLiteral() {
        // Single unclosed bracket
        val rawText1 = "Start [unclosed bracket text"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText1)).isEqualTo(rawText1.length)

        // Trailing unclosed bracket
        val rawText2 = "Text with trailing ["
        assertThat(ExplainerTextParser.computeRenderedLength(rawText2)).isEqualTo(rawText2.length)

        // Multiple unclosed brackets
        val rawText3 = "[[[multiple unclosed"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText3)).isEqualTo(rawText3.length)

        // Unclosed nested brackets
        val rawText4 = "[outer [nested [deep unclosed brackets"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText4)).isEqualTo(rawText4.length)

        // Unclosed outer bracket containing a closed inner link
        // Renders as: "[outer inner text"
        val rawText5 = "[outer [inner](https://example.com) unclosed outer"
        val expected5 = "[outer inner unclosed outer"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText5)).isEqualTo(expected5.length)
    }

    @Test
    fun explainer_unclosedParentheses_treatedAsLiteral() {
        // Unclosed '(' in URL portion
        val rawText1 = "[link](https://example.com/unclosed"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText1)).isEqualTo(rawText1.length)

        // Nested unclosed '(' inside URL portion
        val rawText2 = "[link](https://example.com(unclosed_paren"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText2)).isEqualTo(rawText2.length)

        // Double unclosed '('
        val rawText3 = "[link]((https://example.com"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText3)).isEqualTo(rawText3.length)
    }

    @Test
    fun explainer_unclosedAndNestedBraces_treatedAsLiteral() {
        // Single unclosed brace in plain text
        val rawText1 = "Text with {unclosed brace"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText1)).isEqualTo(rawText1.length)

        // Nested unclosed braces in plain text
        val rawText2 = "Text with {outer {nested unclosed brace"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText2)).isEqualTo(rawText2.length)

        // Matched nested braces in plain text
        val rawText3 = "Text with {nested {inner} braces}"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText3)).isEqualTo(rawText3.length)

        // Nested braces within markdown link text -> renders link text with braces intact
        val rawText4 = "[link {with} {nested {braces}}](https://example.com)"
        val expected4 = "link {with} {nested {braces}}"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText4)).isEqualTo(expected4.length)

        // Braces in URL parameter (valid link) -> renders link text
        val rawText5 = "[link](https://example.com/{user_id}?token={token})"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText5)).isEqualTo("link".length)

        // Unclosed brace in URL parameter (valid link structure) -> renders link text
        val rawText6 = "[link](https://example.com/{unclosed_param)"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText6)).isEqualTo("link".length)
    }

    @Test
    fun explainer_mismatchedBrackets_treatedAsLiteral() {
        // Inverted brackets
        val rawText1 = "]reversed brackets["
        assertThat(ExplainerTextParser.computeRenderedLength(rawText1)).isEqualTo(rawText1.length)

        // Bracket closed with paren instead of square bracket
        val rawText2 = "[link)(https://example.com)"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText2)).isEqualTo(rawText2.length)

        // Bracket followed by bracket instead of paren
        val rawText3 = "[link][https://example.com]"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText3)).isEqualTo(rawText3.length)

        // Bracket closed with brace
        val rawText4 = "[link}(https://example.com)"
        assertThat(ExplainerTextParser.computeRenderedLength(rawText4)).isEqualTo(rawText4.length)
    }
}
