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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PojoWithNullRelationKeyTest {

    NullRelationDatabase mDatabase;
    NullRelationDao mDao;

    @Before
    public void setup() {
        mDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                NullRelationDatabase.class).build();
        mDao = mDatabase.getDao();
    }

    @Test
    public void testNullParentKeyRelationship() {
        ParentEntity parent0 = createParent(0, null);
        ParentEntity parent1 = createParent(1, 0);

        mDao.insert(parent0);
        mDao.insert(parent1);

        ChildEntity child0 = createChild(0);
        ChildEntity child1 = createChild(1);

        mDao.insert(child0);
        mDao.insert(child1);

        List<ParentWithChildren> loaded = mDao.getAllParentWithChildren();
        assertThat(loaded.size(), is(2)); // 2 parent items inserted

        // Pojo with null relationship key doesn't have any children.
        assertThat(loaded.get(0).parentEntity.parentId, is(0));
        assertThat(loaded.get(0).childrenEntities.size(), is(0));
        assertThat(loaded.get(0).parentEntity.name, is("parent 0"));

        // Pojo with non-null relationship has 1 child.
        assertThat(loaded.get(1).parentEntity.parentId, is(1));
        assertThat(loaded.get(1).childrenEntities.size(), is(1));
        assertThat(loaded.get(1).parentEntity.name, is("parent 1"));
        assertThat(loaded.get(1).childrenEntities.get(0).name, is("child 0"));

        List<ParentWithChildren> withOneChild = mDao.getAllParentWithChildren();
        assertThat(withOneChild.size(), is(2));
        assertThat(withOneChild.get(0).parentEntity.parentId, is(0));
    }

    @Test
    public void parentWithOneOptionalChild() {
        ParentEntity hasNoChild = createParent(0, null);
        ParentEntity hasChild = createParent(1, 0);

        mDao.insert(hasNoChild);
        mDao.insert(hasChild);

        ChildEntity child = createChild(0);

        mDao.insert(child);

        ParentWithChild resultWithoutChild = mDao.findParentsWithChild(0);
        assertThat(resultWithoutChild.parentEntity.parentId, is(0));
        assertThat(resultWithoutChild.parentEntity.name, is("parent 0"));
        assertThat(resultWithoutChild.parentEntity.relationId, is(nullValue()));
        assertThat(resultWithoutChild.childEntity, is(nullValue()));

        ParentWithChild resultWithChild = mDao.findParentsWithChild(1);
        assertThat(resultWithChild.parentEntity.parentId, is(1));
        assertThat(resultWithChild.parentEntity.name, is("parent 1"));
        assertThat(resultWithChild.parentEntity.relationId, is(0));
        assertThat(resultWithChild.childEntity.name, is("child 0"));
        assertThat(resultWithChild.childEntity.childId, is(0L));
    }

    @Test // exact repro for b/148240972
    public void relationIsNullWithConflictingColumn() {
        ParentEntity parent = createParent(1, 0);
        parent.name = "hasName";
        mDao.insert(parent);
        ParentWithChild read = mDao.findParentsWithChild(1);
        assertThat(read.parentEntity.name, is("hasName"));

        List<ParentWithChild> readList = mDao.getAllParentWithChild();
        assertThat(readList.size(), is(1));
        assertThat(readList.get(0).parentEntity.name, is("hasName"));
    }

    private static ParentEntity createParent(int id, Integer relationId) {
        ParentEntity item = new ParentEntity();
        item.parentId = id;
        item.relationId = relationId;
        item.name = "parent " + id;
        return item;
    }

    private static ChildEntity createChild(int id) {
        ChildEntity item = new ChildEntity();
        item.childId = id;
        item.name = "child " + id;
        return item;
    }

    @Database(entities = {ParentEntity.class, ChildEntity.class}, version = 1, exportSchema = false)
    abstract static class NullRelationDatabase extends RoomDatabase {
        abstract NullRelationDao getDao();
    }

    @Dao
    interface NullRelationDao {

        @Insert
        void insert(ParentEntity parentEntity);

        @Insert
        void insert(ChildEntity childEntity);

        @Transaction
        @Query("SELECT * FROM ParentEntity")
        List<ParentWithChildren> getAllParentWithChildren();

        @Transaction
        @Query("SELECT * FROM ParentEntity WHERE parentId = :parentId")
        ParentWithChild findParentsWithChild(int parentId);

        @Transaction
        @Query("SELECT * FROM ParentEntity")
        List<ParentWithChild> getAllParentWithChild();
    }

    @Entity
    static class ParentEntity {
        @PrimaryKey
        public int parentId;
        public Integer relationId;
        public String name;
    }

    @Entity
    static class ChildEntity {
        @PrimaryKey
        public long childId;
        public String name;
    }

    static class ParentWithChildren {
        @Embedded
        public ParentEntity parentEntity;
        @Relation(parentColumn = "relationId", entityColumn = "childId")
        public List<ChildEntity> childrenEntities;
    }

    static class ParentWithChild {
        @Embedded
        public ParentEntity parentEntity;
        @Relation(parentColumn = "relationId", entityColumn = "childId")
        public ChildEntity childEntity;
    }
}
