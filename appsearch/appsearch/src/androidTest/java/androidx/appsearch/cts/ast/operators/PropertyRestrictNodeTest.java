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

import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.ast.Node;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.ast.operators.PropertyRestrictNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class PropertyRestrictNodeTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_takesPropertyPath() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);

        assertThat(propertyRestrictNode.getProperty()).isEqualTo(propertyPath);
        assertThat(propertyRestrictNode.getChild()).isEqualTo(textNode);
    }

    @Test
    public void testConstructor_throwsOnNullPointer() {
        PropertyPath propertyPath = null;
        TextNode textNode = new TextNode("foo");

        assertThrows(NullPointerException.class,
                () -> new PropertyRestrictNode(propertyPath, textNode));
    }

    @Test
    public void testGetChildren_returnsListOfSizeOne() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);
        assertThat(propertyRestrictNode.getChildren().size()).isEqualTo(1);
    }

    @Test
    public void testGetChildren_returnsViewOfList() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);

        List<Node> copyOfChildren = propertyRestrictNode.getChildren();
        // Modify original list of children
        TextNode verbatimNode = new TextNode("bar");
        verbatimNode.setVerbatim(true);
        propertyRestrictNode.setChild(verbatimNode);
        // Check that the copy is also modified.
        assertThat(copyOfChildren.size()).isEqualTo(1);
        assertThat(copyOfChildren.get(0)).isEqualTo(verbatimNode);
    }

    @Test
    public void testSetProperty_throwsOnNullPointer() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);

        assertThrows(NullPointerException.class, () -> propertyRestrictNode.setProperty(null));
    }

    @Test
    public void testSetChild_throwsOnNullPointer() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);

        assertThrows(NullPointerException.class, () -> propertyRestrictNode.setChild(null));
    }

    @Test
    public void testToString_returnsCorrectString() {
        List<PropertyPath.PathSegment> pathSegmentList =
                List.of(PropertyPath.PathSegment.create("example"),
                        PropertyPath.PathSegment.create("property"),
                        PropertyPath.PathSegment.create("segment"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        TextNode textNode = new TextNode("foo");

        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(propertyPath,
                textNode);

        assertThat(propertyRestrictNode.toString())
                .isEqualTo("(example.property.segment:(foo))");
    }
}
