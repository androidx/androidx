/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.PropertyPath;

import com.google.common.collect.ImmutableList;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class PropertyPathCtsTest {
    @Test
    public void testPropertyPathInvalid() {
        // These paths are invalid because they are malformed. These throw an exception --- the
        // querier shouldn't provide such paths.
        Throwable e;
        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath(""));
        assertThat(e.getMessage()).startsWith("Malformed path (blank property name)");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("]"));
        assertThat(e.getMessage()).startsWith("Malformed path (no starting '[')");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a["));
        assertThat(e.getMessage()).startsWith("Malformed path (no ending ']')");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[]"));
        assertThat(e.getMessage()).startsWith("Malformed path (\"\" as path index)");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[b]"));
        assertThat(e.getMessage()).startsWith("Malformed path (\"b\" as path index)");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[-1]"));
        assertThat(e.getMessage()).startsWith("Malformed path (path index less than 0)");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[0.]"));
        assertThat(e.getMessage()).startsWith("Malformed path (\"0.\" as path index)");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[0][0]"));
        assertThat(e.getMessage()).startsWith("Malformed path (']' not followed by '.')");

        e = Assert.assertThrows(IllegalArgumentException.class, () -> new PropertyPath("a[0]b"));
        assertThat(e.getMessage()).startsWith("Malformed path (']' not followed by '.')");
    }

    @Test
    public void testPropertyPathValid() {
        PropertyPath path = new PropertyPath("foo.bar[1]");
        assertThat(path.size()).isEqualTo(2);
        assertThat(path.get(0).getPropertyName()).isEqualTo("foo");
        assertThat(path.get(0).getPropertyIndex())
                .isEqualTo(PropertyPath.PathSegment.NON_REPEATED_CARDINALITY);
        assertThat(path.get(1).getPropertyName()).isEqualTo("bar");
        assertThat(path.get(1).getPropertyIndex()).isEqualTo(1);

        path = new PropertyPath("a.b.c.d.e.f.g.h.i.j");
        assertThat(path.size()).isEqualTo(10);
        assertThat(path.get(4).getPropertyName()).isEqualTo("e");
        assertThat(path.get(9).getPropertyName()).isEqualTo("j");
    }

    @Test
    public void testPathSegmentInvalid() {
        Assert.assertThrows(NullPointerException.class,
                () -> PropertyPath.PathSegment.create(null));

        Throwable e;
        e = Assert.assertThrows(IllegalArgumentException.class,
                () -> PropertyPath.PathSegment.create(""));
        assertThat(e.getMessage()).startsWith("Invalid propertyName value");
        e = Assert.assertThrows(IllegalArgumentException.class,
                () -> PropertyPath.PathSegment.create("["));
        assertThat(e.getMessage()).startsWith("Invalid propertyName value");
        e = Assert.assertThrows(IllegalArgumentException.class,
                () -> PropertyPath.PathSegment.create("."));
        assertThat(e.getMessage()).startsWith("Invalid propertyName value");
        e = Assert.assertThrows(IllegalArgumentException.class,
                () -> PropertyPath.PathSegment.create("]"));
        assertThat(e.getMessage()).startsWith("Invalid propertyName value");
        e = Assert.assertThrows(IllegalArgumentException.class,
                () -> PropertyPath.PathSegment.create("foo", -2));
        assertThat(e.getMessage()).startsWith("Invalid propertyIndex value");
    }

    @Test
    public void testPathSegmentValid() {
        PropertyPath.PathSegment segment = PropertyPath.PathSegment.create("foo", 2);
        assertThat(segment.toString()).isEqualTo("foo[2]");
        segment = PropertyPath.PathSegment.create("foo");
        assertThat(segment.toString()).isEqualTo("foo");
        assertThat(segment).isEqualTo(PropertyPath.PathSegment.create("foo"));
    }

    @Test
    public void testBuildPropertyPathFromSegmentList() {
        List<PropertyPath.PathSegment> list = ImmutableList.of(
                PropertyPath.PathSegment.create("foo", 1),
                PropertyPath.PathSegment.create("bar", 2),
                PropertyPath.PathSegment.create("name"));
        PropertyPath path = new PropertyPath(list);
        assertThat(path.toString()).isEqualTo("foo[1].bar[2].name");
        assertThat(path).isEqualTo(new PropertyPath("foo[1].bar[2].name"));
    }

    @Test
    public void testPropertyPathEquality() {
        PropertyPath.PathSegment segment = PropertyPath.PathSegment.create("bar");
        PropertyPath path = new PropertyPath("foo[1].bar");
        assertThat(path.get(1)).isEqualTo(segment);
    }
}
