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
import androidx.appsearch.ast.operators.ComparatorNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class ComparatorNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_correctConstruction() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("example"),
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        int value = 5;

        ComparatorNode equalsNode = new ComparatorNode(ComparatorNode.EQUALS, propertyPath, value);

        assertThat(equalsNode.getComparator()).isEqualTo(ComparatorNode.EQUALS);
        assertThat(equalsNode.getPropertyPath()).isEqualTo(propertyPath);
        assertThat(equalsNode.getValue()).isEqualTo(value);
    }

    @Test
    public void testConstructor_throwsOnInvalidComparator() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("example"),
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        int value = 5;

        IllegalArgumentException thrownLow = assertThrows(IllegalArgumentException.class,
                () -> new ComparatorNode(-1, propertyPath, value));
        assertThat(thrownLow).hasMessageThat().contains("Comparator intDef");
        assertThat(thrownLow).hasMessageThat().contains("too low");

        IllegalArgumentException thrownHigh =  assertThrows(IllegalArgumentException.class,
                () -> new ComparatorNode(1000, propertyPath, value));
        assertThat(thrownHigh).hasMessageThat().contains("Comparator intDef");
        assertThat(thrownHigh).hasMessageThat().contains("too high");
    }

    @Test
    public void testConstructor_throwsOnNullPointer() {
        PropertyPath nullPropertyPath = null;
        int value = 5;

        assertThrows(NullPointerException.class,
                () -> new ComparatorNode(ComparatorNode.GREATER_EQUALS, nullPropertyPath, value));
    }

    @Test
    public void testGetChildren_returnsEmptyList() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("example"),
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        int value = 5;

        ComparatorNode lessThanNode = new ComparatorNode(ComparatorNode.LESS_THAN, propertyPath,
                value);

        assertThat(lessThanNode.getChildren().isEmpty()).isTrue();
    }

    @Test
    public void testSetComparator_throwsOnInvalidComparator() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("example"),
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        int value = 5;

        IllegalArgumentException thrownLow =  assertThrows(IllegalArgumentException.class,
                () -> new ComparatorNode(ComparatorNode.GREATER_THAN, propertyPath, value)
                        .setComparator(-1));
        assertThat(thrownLow).hasMessageThat().contains("Comparator intDef");
        assertThat(thrownLow).hasMessageThat().contains("too low");

        IllegalArgumentException thrownHigh =
                assertThrows(IllegalArgumentException.class, () -> new ComparatorNode(
                ComparatorNode.GREATER_THAN, propertyPath, value)
                        .setComparator(1000));
        assertThat(thrownHigh).hasMessageThat().contains("Comparator intDef");
        assertThat(thrownHigh).hasMessageThat().contains("too high");
    }

    @Test
    public void testSetPropertyPath_throwsOnNullPropertyPath() {
        List<PropertyPath.PathSegment> pathSegmentList = List.of(
                PropertyPath.PathSegment.create("example"),
                PropertyPath.PathSegment.create("property"),
                PropertyPath.PathSegment.create("path"));
        PropertyPath propertyPath = new PropertyPath(pathSegmentList);

        int value = 5;

        assertThrows(NullPointerException.class,
                () -> new ComparatorNode(ComparatorNode.LESS_EQUALS, propertyPath, value)
                        .setPropertyPath(null));
    }
}
