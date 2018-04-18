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

import static java.util.Arrays.asList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(JUnit4.class)
public class EntityBundleTest {
    @Test
    public void schemaEquality_same_equal() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                asList(createFieldBundle("foo"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("foo")),
                asList(createForeignKeyBundle("bar", "foo")));

        EntityBundle other = new EntityBundle("foo", "sq",
                asList(createFieldBundle("foo"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("foo")),
                asList(createForeignKeyBundle("bar", "foo")));

        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_reorderedFields_equal() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                asList(createFieldBundle("foo"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());

        EntityBundle other = new EntityBundle("foo", "sq",
                asList(createFieldBundle("bar"), createFieldBundle("foo")),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());

        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_diffFields_notEqual() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                asList(createFieldBundle("foo"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());

        EntityBundle other = new EntityBundle("foo", "sq",
                asList(createFieldBundle("foo2"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());

        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_reorderedForeignKeys_equal() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                asList(createForeignKeyBundle("x", "y"),
                        createForeignKeyBundle("bar", "foo")));

        EntityBundle other = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                asList(createForeignKeyBundle("bar", "foo"),
                        createForeignKeyBundle("x", "y")));


        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_diffForeignKeys_notEqual() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                asList(createForeignKeyBundle("bar", "foo")));

        EntityBundle other = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                Collections.<IndexBundle>emptyList(),
                asList(createForeignKeyBundle("bar2", "foo")));

        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_reorderedIndices_equal() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("foo"), createIndexBundle("baz")),
                Collections.<ForeignKeyBundle>emptyList());

        EntityBundle other = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("baz"), createIndexBundle("foo")),
                Collections.<ForeignKeyBundle>emptyList());

        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_diffIndices_notEqual() {
        EntityBundle bundle = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("foo")),
                Collections.<ForeignKeyBundle>emptyList());

        EntityBundle other = new EntityBundle("foo", "sq",
                Collections.<FieldBundle>emptyList(),
                new PrimaryKeyBundle(false, asList("foo")),
                asList(createIndexBundle("foo2")),
                Collections.<ForeignKeyBundle>emptyList());

        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    private FieldBundle createFieldBundle(String name) {
        return new FieldBundle("foo", name, "text", false);
    }

    private IndexBundle createIndexBundle(String colName) {
        return new IndexBundle("ind_" + colName, false,
                asList(colName), "create");
    }

    private ForeignKeyBundle createForeignKeyBundle(String targetTable, String column) {
        return new ForeignKeyBundle(targetTable, "CASCADE", "CASCADE",
                asList(column), asList(column));
    }
}
