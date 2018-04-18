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

import static java.util.Collections.singletonList;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RelationWithReservedKeywordTest {
    private MyDatabase mDb;

    @Before
    public void initDb() {
        mDb = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                MyDatabase.class).build();
    }

    @Test
    public void loadRelation() {
        Category category = new Category(1, "cat1");
        mDb.getDao().insert(category);
        Topic topic = new Topic(2, 1, "foo");
        mDb.getDao().insert(topic);
        List<CategoryWithTopics> categoryWithTopics = mDb.getDao().loadAll();
        assertThat(categoryWithTopics.size(), is(1));
        assertThat(categoryWithTopics.get(0).category, is(category));
        assertThat(categoryWithTopics.get(0).topics, is(singletonList(topic)));
    }

    @Entity(tableName = "categories")
    static class Category {

        @PrimaryKey(autoGenerate = true)
        public final long id;

        public final String name;

        Category(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Category category = (Category) o;
            //noinspection SimplifiableIfStatement
            if (id != category.id) return false;
            return name != null ? name.equals(category.name) : category.name == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    @Dao
    interface MyDao {
        @Transaction
        @Query("SELECT * FROM categories")
        List<CategoryWithTopics> loadAll();

        @Insert
        void insert(Category... categories);

        @Insert
        void insert(Topic... topics);
    }

    @Database(
            entities = {Category.class, Topic.class},
            version = 1,
            exportSchema = false)
    abstract static class MyDatabase extends RoomDatabase {
        abstract MyDao getDao();
    }


    @SuppressWarnings("WeakerAccess")
    static class CategoryWithTopics {
        @Embedded
        public Category category;

        @Relation(
                parentColumn = "id",
                entityColumn = "category_id",
                entity = Topic.class)
        public List<Topic> topics;
    }

    @Entity(
            tableName = "topics",
            foreignKeys = @ForeignKey(
                    entity = Category.class,
                    parentColumns = "id",
                    childColumns = "category_id",
                    onDelete = ForeignKey.CASCADE),
            indices = @Index("category_id"))
    static class Topic {

        @PrimaryKey(autoGenerate = true)
        public final long id;

        @ColumnInfo(name = "category_id")
        public final long categoryId;

        public final String to;

        Topic(long id, long categoryId, String to) {
            this.id = id;
            this.categoryId = categoryId;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Topic topic = (Topic) o;
            if (id != topic.id) return false;
            //noinspection SimplifiableIfStatement
            if (categoryId != topic.categoryId) return false;
            return to != null ? to.equals(topic.to) : topic.to == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (int) (categoryId ^ (categoryId >>> 32));
            result = 31 * result + (to != null ? to.hashCode() : 0);
            return result;
        }
    }
}
