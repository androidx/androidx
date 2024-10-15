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

package androidx.appsearch.cts.ast.query;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.ast.Node;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.ast.query.SearchNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class SearchNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_defaultValues() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);

        assertThat(searchNode.getProperties()).isEmpty();
    }

    @Test
    public void testConstructor_throwsOnNullQuery() {
        assertThrows(NullPointerException.class, () -> new SearchNode(null));
    }

    @Test
    public void testConstructor_throwsOnNullPropertyList() {
        TextNode node = new TextNode("foo");
        assertThrows(NullPointerException.class, () -> new SearchNode(node, null));
    }

    @Test
    public void testConstructor_throwOnNullPropertyInList() {
        TextNode node = new TextNode("foo");

        PropertyPath examplePath = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("path")
                )
        );

        ArrayList<PropertyPath> listWithNull = new ArrayList<>(2);
        listWithNull.add(examplePath);
        listWithNull.add(null);

        assertThrows(NullPointerException.class, () -> new SearchNode(node, listWithNull));
    }

    @Test
    public void testGetFunctionName_functionNameCorrect() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThat(searchNode.getFunctionName()).isEqualTo("search");
    }

    @Test
    public void testGetChildren_returnsListOfSizeOne() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThat(searchNode.getChildren()).containsExactly(node);
    }

    @Test
    public void testGetChildren_returnsListView() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);

        List<Node> copyOfChildren = searchNode.getChildren();

        // Modify the original list.
        TextNode newNode = new TextNode("bar");
        searchNode.setChild(newNode);
        // Check that the copy was also modified.
        assertThat(copyOfChildren).containsExactly(newNode);
    }

    @Test
    public void testGetProperties_returnsListView() {
        PropertyPath examplePath = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("path")
                    )
        );
        PropertyPath anotherPath = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("another"),
                        PropertyPath.PathSegment.create("path")
                )
        );
        List<PropertyPath> properties = List.of(examplePath, anotherPath);

        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node, properties);

        List<PropertyPath> copyOfProperties = searchNode.getProperties();

        // Modify the original list
        searchNode.setProperties(Collections.emptyList());
        searchNode.addProperty(examplePath);
        PropertyPath yetAnother = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("yet"),
                        PropertyPath.PathSegment.create("another")
                )
        );
        searchNode.addProperty(yetAnother);
        PropertyPath oneMore = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("one"),
                        PropertyPath.PathSegment.create("more"))
        );
        searchNode.addProperty(oneMore);
        // Check that the copy was also modified.
        assertThat(copyOfProperties).containsExactly(examplePath, yetAnother, oneMore).inOrder();
    }

    @Test
    public void testSetChild_throwsOnNull() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThrows(NullPointerException.class, () -> searchNode.setChild(null));
    }

    @Test
    public void testSetProperties_throwsOnNull() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThrows(NullPointerException.class, () -> searchNode.setProperties(null));
    }

    @Test
    public void testSetProperties_throwsOnNullPropertyInList() {
        PropertyPath examplePath = new PropertyPath(
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("path")
                )
        );

        ArrayList<PropertyPath> properties = new ArrayList<>(2);
        properties.add(examplePath);
        properties.add(null);

        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThrows(NullPointerException.class, () -> searchNode.setProperties(properties));
    }

    @Test
    public void testAddProperty_throwsOnNull() {
        TextNode node = new TextNode("foo");
        SearchNode searchNode = new SearchNode(node);
        assertThrows(NullPointerException.class, () -> searchNode.addProperty(null));
    }
}
