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
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

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
        mDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
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

        // Pojo with non-null relationship has 1 child.
        assertThat(loaded.get(1).parentEntity.parentId, is(1));
        assertThat(loaded.get(1).childrenEntities.size(), is(1));
    }

    private static ParentEntity createParent(int id, Integer relationId) {
        ParentEntity item = new ParentEntity();
        item.parentId = id;
        item.relationId = relationId;
        return item;
    }

    private static ChildEntity createChild(int id) {
        ChildEntity item = new ChildEntity();
        item.childId = id;
        return item;
    }

    @Database(entities = {ParentEntity.class, ChildEntity.class}, version = 1)
    abstract static class NullRelationDatabase extends RoomDatabase {
        abstract NullRelationDao getDao();
    }

    @Dao
    interface NullRelationDao {

        @Insert
        void insert(ParentEntity parentEntity);

        @Insert
        void insert(ChildEntity childEntity);

        @Query("SELECT * FROM ParentEntity")
        List<ParentWithChildren> getAllParentWithChildren();
    }

    @Entity
    static class ParentEntity {
        @PrimaryKey
        public int parentId;
        public Integer relationId;
    }

    @Entity
    static class ChildEntity {
        @PrimaryKey
        public long childId;
    }

    static class ParentWithChildren {
        @Embedded
        public ParentEntity parentEntity;
        @Relation(parentColumn = "relationId", entityColumn = "childId")
        public List<ChildEntity> childrenEntities;
    }
}
