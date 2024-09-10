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

import org.junit.Test;

public class TextNodeCtsTest {
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
}
