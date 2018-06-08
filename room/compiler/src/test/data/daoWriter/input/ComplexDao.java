/*
 * Copyright (C) 2016 The Android Open Source Project
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

package foo.bar;
import androidx.room.*;
import java.util.List;
import androidx.lifecycle.LiveData;
@Dao
abstract class ComplexDao {
    static class FullName {
        public int id;
        public String fullName;
    }

    private final ComplexDatabase mDb;

    public ComplexDao(ComplexDatabase db) {
        mDb = db;
    }

    @Transaction
    public boolean transactionMethod(int i, String s, long l) {
        return true;
    }

    @Query("SELECT name || lastName as fullName, uid as id FROM user where uid = :id")
    abstract public List<FullName> fullNames(int id);

    @Query("SELECT * FROM user where uid = :id")
    abstract public User getById(int id);

    @Query("SELECT * FROM user where name LIKE :name AND lastName LIKE :lastName")
    abstract public User findByName(String name, String lastName);

    @Query("SELECT * FROM user where uid IN (:ids)")
    abstract public List<User> loadAllByIds(int... ids);

    @Query("SELECT ageColumn FROM user where uid = :id")
    abstract int getAge(int id);

    @Query("SELECT ageColumn FROM user where uid IN(:ids)")
    abstract public int[] getAllAges(int... ids);

    @Query("SELECT ageColumn FROM user where uid IN(:ids)")
    abstract public List<Integer> getAllAgesAsList(List<Integer> ids);

    @Query("SELECT * FROM user where uid = :id")
    abstract public LiveData<User> getByIdLive(int id);

    @Query("SELECT * FROM user where uid IN (:ids)")
    abstract public LiveData<List<User>> loadUsersByIdsLive(int... ids);

    @Query("SELECT ageColumn FROM user where uid IN(:ids1) OR uid IN (:ids2) OR uid IN (:ids3)")
    abstract public List<Integer> getAllAgesAsList(List<Integer> ids1,
            int[] ids2, int... ids3);
}
