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

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;

import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;

@Generated("android.arch.persistence.room.RoomProcessor")
public class WriterDao_Impl implements WriterDao {
    private final RoomDatabase __db;

    private final EntityInsertionAdapter __insertionAdapterOfUser;

    private final EntityInsertionAdapter __insertionAdapterOfUser_1;

    private final EntityInsertionAdapter __insertionAdapterOfBook;

    public WriterDao_Impl(RoomDatabase __db) {
        this.__db = __db;
        this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
            @Override
            public String createQuery() {
                return "INSERT OR ABORT INTO `User`(`uid`,`name`,`lastName`,`ageColumn`) VALUES"
                        + " (?,?,?,?)";
            }

            @Override
            public void bind(SupportSQLiteStatement stmt, User value) {
                stmt.bindLong(1, value.uid);
                if (value.name == null) {
                    stmt.bindNull(2);
                } else {
                    stmt.bindString(2, value.name);
                }
                if (value.getLastName() == null) {
                    stmt.bindNull(3);
                } else {
                    stmt.bindString(3, value.getLastName());
                }
                stmt.bindLong(4, value.age);
            }
        };
        this.__insertionAdapterOfUser_1 = new EntityInsertionAdapter<User>(__db) {
            @Override
            public String createQuery() {
                return "INSERT OR REPLACE INTO `User`(`uid`,`name`,`lastName`,`ageColumn`) VALUES"
                        + " (?,?,?,?)";
            }

            @Override
            public void bind(SupportSQLiteStatement stmt, User value) {
                stmt.bindLong(1, value.uid);
                if (value.name == null) {
                    stmt.bindNull(2);
                } else {
                    stmt.bindString(2, value.name);
                }
                if (value.getLastName() == null) {
                    stmt.bindNull(3);
                } else {
                    stmt.bindString(3, value.getLastName());
                }
                stmt.bindLong(4, value.age);
            }
        };
        this.__insertionAdapterOfBook = new EntityInsertionAdapter<Book>(__db) {
            @Override
            public String createQuery() {
                return "INSERT OR ABORT INTO `Book`(`bookId`,`uid`) VALUES (?,?)";
            }

            @Override
            public void bind(SupportSQLiteStatement stmt, Book value) {
                stmt.bindLong(1, value.bookId);
                stmt.bindLong(2, value.uid);
            }
        };
    }

    @Override
    public void insertUser(User user) {
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser.insert(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void insertUsers(User user1, List<User> others) {
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser.insert(user1);
            __insertionAdapterOfUser.insert(others);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void insertUsers(User[] users) {
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser_1.insert(users);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void insertUserAndBook(User user, Book book) {
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser.insert(user);
            __insertionAdapterOfBook.insert(book);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }
}
