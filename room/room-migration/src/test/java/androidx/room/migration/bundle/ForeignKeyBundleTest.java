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

import java.util.Arrays;

@RunWith(JUnit4.class)
public class ForeignKeyBundleTest {
    @Test
    public void schemaEquality_same_equal() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_diffTable_notEqual() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table2", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffOnDelete_notEqual() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete2",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffOnUpdate_notEqual() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table", "onDelete",
                "onUpdate2", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffSrcOrder_notEqual() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col2", "col1"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffTargetOrder_notEqual() {
        ForeignKeyBundle bundle = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target1", "target2"));
        ForeignKeyBundle other = new ForeignKeyBundle("table", "onDelete",
                "onUpdate", Arrays.asList("col1", "col2"),
                Arrays.asList("target2", "target1"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }
}
