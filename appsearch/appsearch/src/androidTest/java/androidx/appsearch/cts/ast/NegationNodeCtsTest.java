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

import androidx.appsearch.ast.NegationNode;
import androidx.appsearch.ast.Node;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class NegationNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testSetChildren_throwsOnNullNode() {
        TextNode textNode = new TextNode("foo");

        NegationNode negationNode = new NegationNode(textNode);

        Node nullNode = null;
        assertThrows(NullPointerException.class, () -> negationNode.setChild(nullNode));
    }

    @Test
    public void testGetChildren_returnsListOfSizeOne() {
        TextNode textNode = new TextNode("foo");
        NegationNode negationNode = new NegationNode(textNode);

        assertThat(negationNode.getChildren().size()).isEqualTo(1);
        assertThat(negationNode.getChildren().get(0)).isEqualTo(textNode);
    }

    @Test
    public void testGetChildren_returnsViewOfList() {
        TextNode textNode = new TextNode("foo");
        NegationNode negationNode = new NegationNode(textNode);

        List<Node> copyOfChildren = negationNode.getChildren();

        // Make changes to the original list
        TextNode verbatimNode = new TextNode("bar");
        negationNode.setChild(verbatimNode);

        // Check that the copy also changes.
        assertThat(copyOfChildren.size()).isEqualTo(1);
        assertThat(copyOfChildren.get(0)).isEqualTo(verbatimNode);
    }

    @Test
    public void testConstructor_throwsOnNullNode() {
        Node nullNode = null;

        assertThrows(NullPointerException.class, () -> new NegationNode(nullNode));
    }

    @Test
    public void testConstructor_mChildRetainsType() {
        TextNode textNode = new TextNode("foo");

        NegationNode negationText = new NegationNode(textNode);

        assertThat(negationText.getChild()).isEqualTo(textNode);
    }
}
