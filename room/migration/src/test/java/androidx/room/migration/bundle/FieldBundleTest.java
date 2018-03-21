/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.migration.bundle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldBundleTest {
    @Test
    public void schemaEquality_same_equal() {
        FieldBundle bundle = new FieldBundle("foo", "foo", "text", false);
        FieldBundle copy = new FieldBundle("foo", "foo", "text", false);
        assertThat(bundle.isSchemaEqual(copy), is(true));
    }

    @Test
    public void schemaEquality_diffNonNull_notEqual() {
        FieldBundle bundle = new FieldBundle("foo", "foo", "text", false);
        FieldBundle copy = new FieldBundle("foo", "foo", "text", true);
        assertThat(bundle.isSchemaEqual(copy), is(false));
    }

    @Test
    public void schemaEquality_diffColumnName_notEqual() {
        FieldBundle bundle = new FieldBundle("foo", "foo", "text", false);
        FieldBundle copy = new FieldBundle("foo", "foo2", "text", true);
        assertThat(bundle.isSchemaEqual(copy), is(false));
    }

    @Test
    public void schemaEquality_diffAffinity_notEqual() {
        FieldBundle bundle = new FieldBundle("foo", "foo", "text", false);
        FieldBundle copy = new FieldBundle("foo", "foo2", "int", false);
        assertThat(bundle.isSchemaEqual(copy), is(false));
    }

    @Test
    public void schemaEquality_diffPath_equal() {
        FieldBundle bundle = new FieldBundle("foo", "foo", "text", false);
        FieldBundle copy = new FieldBundle("foo>bar", "foo", "text", false);
        assertThat(bundle.isSchemaEqual(copy), is(true));
    }
}
