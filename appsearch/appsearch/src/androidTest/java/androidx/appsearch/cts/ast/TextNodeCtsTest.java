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

package androidx.appsearch.cts.ast;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.ast.TextNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class TextNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_prefixVerbatimFalseByDefault() {
        TextNode defaultTextNode = new TextNode("foo");

        assertThat(defaultTextNode.isPrefix()).isFalse();
        assertThat(defaultTextNode.isVerbatim()).isFalse();
    }

    @Test
    public void testCopyConstructor_fieldsCorrectlyCopied() {
        TextNode fooNode = new TextNode("foo");
        fooNode.setPrefix(false);
        fooNode.setVerbatim(true);

        TextNode copyConstructedFooNode =  new TextNode(fooNode);

        assertThat(fooNode.getValue()).isEqualTo(copyConstructedFooNode.getValue());
        assertThat(fooNode.isPrefix()).isEqualTo(copyConstructedFooNode.isPrefix());
        assertThat(fooNode.isVerbatim()).isEqualTo(copyConstructedFooNode.isVerbatim());
    }

    @Test
    public void testCopyConstructor_originalUnchanged() {
        TextNode fooNode = new TextNode("foo");
        fooNode.setPrefix(true);
        fooNode.setVerbatim(true);
        TextNode barNode = new TextNode(fooNode);
        barNode.setValue("bar");
        barNode.setPrefix(false);

        // Check original is unchanged.
        assertThat(fooNode.getValue()).isEqualTo("foo");
        assertThat(fooNode.isPrefix()).isTrue();
        assertThat(fooNode.isVerbatim()).isTrue();
        // Check that the fields were modified.
        assertThat(barNode.getValue()).isEqualTo("bar");
        assertThat(barNode.isPrefix()).isFalse();
        // Check that fields that weren't set are unmodified.
        assertThat(barNode.isVerbatim()).isTrue();
    }

    @Test
    public void testGetChildren_alwaysReturnEmptyList() {
        TextNode fooNode = new TextNode("foo");
        assertThat(fooNode.getChildren().isEmpty()).isTrue();
    }

    @Test
    public void testConstructor_throwsIfStringNull() {
        String nullString = null;
        assertThrows(NullPointerException.class, () -> new TextNode(nullString));
    }

    @Test
    public void testCopyConstructor_throwsIfStringNodeNull() {
        TextNode nullTextNode = null;
        assertThrows(NullPointerException.class, () -> new TextNode(nullTextNode));
    }

    @Test
    public void testToString_noFlagsSetReturnPlainValue() {
        TextNode node = new TextNode("foo");
        assertThat(node.getValue()).isEqualTo("foo");
        assertThat(node.toString()).isEqualTo("(foo)");
    }

    @Test
    public void testToString_prefixFlagSetReturnPrefixedString() {
        TextNode node = new TextNode("foo");
        node.setPrefix(true);

        assertThat(node.toString()).isEqualTo("(foo*)");
    }

    @Test
    public void testToString_verbatimFlagSetReturnQuotedString() {
        TextNode node = new TextNode("foo");
        node.setVerbatim(true);

        assertThat(node.toString()).isEqualTo("(\"foo\")");
    }

    @Test
    public void testToString_prefixVerbatimFlagsSetReturnPrefixedQuotedString() {
        TextNode node = new TextNode("foo");
        node.setPrefix(true);
        node.setVerbatim(true);

        assertThat(node.toString()).isEqualTo("(\"foo\"*)");
    }

    @Test
    public void testToString_handlesEscaping() {
        TextNode node = new TextNode("(NOT \"foo\" OR bar:-baz) AND (property.path > 0)");

        assertThat(node.toString()).isEqualTo("(\\(not \\\"foo\\\" or bar\\:\\-baz\\) "
                        + "and \\(property\\.path \\> 0\\))");
    }

    @Test
    public void testToString_verbatimEscapesOnlyQuotes() {
        TextNode node = new TextNode("(NOT \"foo\" OR bar:-baz) AND (property.path > 0)");
        node.setVerbatim(true);

        assertThat(node.toString()).isEqualTo("(\"(NOT \\\"foo\\\" OR bar:-baz) AND "
                + "(property.path > 0)\")");
    }


    @Test
    public void testToString_handlesEscaping_specialCharacters() {
        TextNode germanNode = new TextNode("Straße");
        assertThat(germanNode.toString()).isEqualTo("(straße)");
        // Ideographs like CJKT characters should remain unchanged.
        TextNode chineseNode = new TextNode("我每天走路去上班");
        assertThat(chineseNode.toString()).isEqualTo("(我每天走路去上班)");
    }
}
