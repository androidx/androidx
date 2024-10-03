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
import androidx.appsearch.ast.query.PropertyDefinedNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class PropertyDefinedNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_throwsOnNullPointer() {
        assertThrows(NullPointerException.class, () -> new PropertyDefinedNode(null));
    }

    @Test
    public void testGetFunctionName_functionNameCorrect() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        PropertyDefinedNode propertyDefinedNode =
                new PropertyDefinedNode(propertyPath);
        assertThat(propertyDefinedNode.getFunctionName()).isEqualTo("propertyDefined");
    }

    @Test
    public void testGetChildren_returnsEmptyList() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        PropertyDefinedNode propertyDefinedNode =
                new PropertyDefinedNode(propertyPath);

        assertThat(propertyDefinedNode.getChildren().isEmpty()).isTrue();
    }

    @Test
    public void testGetProperty_returnsCorrectProperty() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        PropertyDefinedNode propertyDefinedNode =
                new PropertyDefinedNode(propertyPath);

        assertThat(propertyDefinedNode.getProperty()).isEqualTo(propertyPath);
    }

    @Test
    public void testSetProperty_throwsOnNullPointer() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        PropertyDefinedNode propertyDefinedNode =
                new PropertyDefinedNode(propertyPath);

        assertThrows(NullPointerException.class, () -> propertyDefinedNode.setProperty(null));
    }

    @Test
    public void testSetProperty_setsCorrectProperty() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);
        PropertyDefinedNode propertyDefinedNode =
                new PropertyDefinedNode(propertyPath);

        List<PropertyPath.PathSegment> anotherPathSegmentList = List.of(
                PropertyPath.PathSegment.create("another"),
                PropertyPath.PathSegment.create("property"));
        PropertyPath anotherProperty = new PropertyPath(anotherPathSegmentList);
        propertyDefinedNode.setProperty(anotherProperty);

        assertThat(propertyDefinedNode.getProperty()).isEqualTo(anotherProperty);
    }
}
