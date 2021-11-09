/*
 * Copyright 2018 The Android Open Source Project
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

@RunWith(JUnit4.class)
public class DatabaseBundleTest {

    @Test
    public void buildCreateQueries_noFts() {
        EntityBundle entity1 = new EntityBundle("e1", "sq1",
                asList(createFieldBundle("foo1"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo1")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        EntityBundle entity2 = new EntityBundle("e2", "sq2",
                asList(createFieldBundle("foo2"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo2")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        DatabaseBundle bundle = new DatabaseBundle(1, "hash",
                asList(entity1, entity2), Collections.<DatabaseViewBundle>emptyList(),
                Collections.<String>emptyList());

        assertThat(bundle.buildCreateQueries(), is(asList("sq1", "sq2")));
    }

    @Test
    public void buildCreateQueries_withFts() {
        EntityBundle entity1 = new EntityBundle("e1", "sq1",
                asList(createFieldBundle("foo1"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo1")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        FtsEntityBundle entity2 = new FtsEntityBundle("e2", "sq2",
                asList(createFieldBundle("foo2"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo2")),
                "FTS4",
                createFtsOptionsBundle(""),
                Collections.<String>emptyList());
        EntityBundle entity3 = new EntityBundle("e3", "sq3",
                asList(createFieldBundle("foo3"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo3")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        DatabaseBundle bundle = new DatabaseBundle(1, "hash",
                asList(entity1, entity2, entity3), Collections.<DatabaseViewBundle>emptyList(),
                Collections.<String>emptyList());

        assertThat(bundle.buildCreateQueries(), is(asList("sq1", "sq2", "sq3")));
    }

    @Test
    public void buildCreateQueries_withExternalContentFts() {
        EntityBundle entity1 = new EntityBundle("e1", "sq1",
                asList(createFieldBundle("foo1"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo1")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        FtsEntityBundle entity2 = new FtsEntityBundle("e2", "sq2",
                asList(createFieldBundle("foo2"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo2")),
                "FTS4",
                createFtsOptionsBundle("e3"),
                asList("e2_trig"));
        EntityBundle entity3 = new EntityBundle("e3", "sq3",
                asList(createFieldBundle("foo3"), createFieldBundle("bar")),
                new PrimaryKeyBundle(false, asList("foo3")),
                Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        DatabaseBundle bundle = new DatabaseBundle(1, "hash",
                asList(entity1, entity2, entity3), Collections.<DatabaseViewBundle>emptyList(),
                Collections.<String>emptyList());

        assertThat(bundle.buildCreateQueries(), is(asList("sq1", "sq3", "sq2", "e2_trig")));
    }

    private FieldBundle createFieldBundle(String name) {
        return new FieldBundle("foo", name, "text", false, null);
    }

    private FtsOptionsBundle createFtsOptionsBundle(String contentTableName) {
        return new FtsOptionsBundle("", Collections.<String>emptyList(), contentTableName,
                "", "", Collections.<String>emptyList(), Collections.<Integer>emptyList(), "");
    }
}
