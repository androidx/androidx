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

@Dao
abstract interface DeletionDao {
    @Delete
    void deleteUser(User user);
    @Delete
    void deleteUsers(User user1, List<User> others);
    @Delete
    void deleteArrayOfUsers(User[] users);

    @Delete
    int deleteUserAndReturnCount(User user);
    @Delete
    int deleteUserAndReturnCount(User user1, List<User> others);
    @Delete
    int deleteUserAndReturnCount(User[] users);

    @Delete
    int multiPKey(MultiPKeyEntity entity);

    @Query("DELETE FROM user where uid = :uid")
    int deleteByUid(int uid);

    @Query("DELETE FROM user where uid IN(:uid)")
    int deleteByUidList(int... uid);

    @Delete
    void deleteUserAndBook(User user, Book book);

    @Query("DELETE FROM user")
    int deleteEverything();
}
