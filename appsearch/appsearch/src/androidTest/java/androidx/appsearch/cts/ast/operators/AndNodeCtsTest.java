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

package androidx.appsearch.cts.ast.operators;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.ast.NegationNode;
import androidx.appsearch.ast.Node;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.ast.operators.AndNode;
import androidx.appsearch.ast.operators.OrNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class AndNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_buildsAndNode() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");

        List<Node> childNodes = List.of(foo, bar, baz);

        AndNode andNode = new AndNode(childNodes);
        assertThat(andNode.getChildren()).containsExactly(foo, bar, baz).inOrder();
    }

    @Test
    public void testConstructor_buildsAndNodeVarArgs() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");

        AndNode andNode = new AndNode(foo, bar, baz);
        assertThat(andNode.getChildren()).containsExactly(foo, bar, baz).inOrder();
    }

    @Test
    public void testConstructor_throwsWhenNullListPassed() {
        assertThrows(NullPointerException.class, () -> new AndNode(null));
    }

    @Test
    public void testConstructor_throwsWhenNotEnoughNodes() {
        TextNode foo = new TextNode("foo");
        assertThrows(IllegalArgumentException.class,
                () -> new AndNode(Collections.singletonList(foo)));
    }

    @Test
    public void testConstructor_throwsWhenNullArgPassed() {
        TextNode foo = new TextNode("foo");
        TextNode baz = new TextNode("baz");
        assertThrows(NullPointerException.class, () -> new AndNode(foo, null, baz));
    }

    @Test
    public void testSetChildren_throwsWhenNullListPassed() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        assertThrows(NullPointerException.class, () -> andNode.setChildren(null));
    }

    @Test
    public void testSetChildren_throwsWhenNotEnoughNodes() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        assertThrows(IllegalArgumentException.class,
                () -> andNode.setChildren(Collections.singletonList(foo)));
    }

    @Test
    public void testAddChild_addsToBackOfList() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        TextNode baz = new TextNode("baz");
        andNode.addChild(baz);

        assertThat(andNode.getChildren()).containsExactly(foo, bar, baz).inOrder();
    }

    @Test
    public void testSetChild_throwsOnOutOfRangeIndex() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        TextNode baz = new TextNode("baz");
        assertThrows(IllegalArgumentException.class, () -> andNode.setChild(3, baz));
    }

    @Test
    public void testSetChild_throwsOnNullNode() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        assertThrows(NullPointerException.class, () -> andNode.setChild(0, null));
    }

    @Test
    public void testSetChild_setCorrectChild() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        TextNode baz = new TextNode("baz");
        andNode.setChild(0, baz);

        assertThat(andNode.getChildren()).containsExactly(baz, bar).inOrder();
    }

    @Test
    public void testGetChildren_returnsViewOf() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        List<Node> copyChildNodes = andNode.getChildren();

        // Check that initially copy is equivalent to original list
        assertThat(copyChildNodes).containsExactly(foo, bar).inOrder();
        // Now make changes to the original list
        TextNode baz = new TextNode("baz");
        andNode.setChild(1, baz);

        TextNode bat = new TextNode("bat");
        andNode.addChild(bat);
        // Check that the copied list is the same as the original list.
        assertThat(copyChildNodes).containsExactly(foo, baz, bat).inOrder();
    }

    @Test
    public void testRemoveChild_throwsIfListIsTooSmall() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        assertThrows(IllegalStateException.class, () -> andNode.removeChild(0));
    }

    @Test
    public void testRemoveChild_throwsIfIndexOutOfRange() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");
        AndNode andNode = new AndNode(foo, bar, baz);

        assertThrows(IllegalArgumentException.class, () -> andNode.removeChild(-1));
        assertThrows(IllegalArgumentException.class, () -> andNode.removeChild(3));
    }

    @Test
    public void testToString_joinsChildNodesWithAnd() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");
        NegationNode notBaz = new NegationNode(baz);

        AndNode andNode = new AndNode(foo, bar, notBaz);
        assertThat(andNode.toString()).isEqualTo("((foo) AND (bar) AND NOT (baz))");
    }

    @Test
    public void testToString_respectsOperatorPrecedence() {
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");
        OrNode orNode = new OrNode(bar, baz);

        AndNode andNode = new AndNode(foo, orNode);
        assertThat(andNode.toString()).isEqualTo("((foo) AND ((bar) OR (baz)))");
    }
}
