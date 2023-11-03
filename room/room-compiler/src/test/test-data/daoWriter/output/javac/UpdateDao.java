package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.RoomDatabase;
import androidx.room.SharedSQLiteStatement;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.lang.Void;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UpdateDao_Impl implements UpdateDao {
    private final RoomDatabase __db;

    private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser;

    private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser_1;

    private final EntityDeletionOrUpdateAdapter<MultiPKeyEntity> __updateAdapterOfMultiPKeyEntity;

    private final EntityDeletionOrUpdateAdapter<Book> __updateAdapterOfBook;

    private final SharedSQLiteStatement __preparedStmtOfAgeUserByUid;

    private final SharedSQLiteStatement __preparedStmtOfAgeUserAll;

    public UpdateDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__updateAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
                statement.bindLong(1, entity.uid);
                if (entity.name == null) {
                    statement.bindNull(2);
                } else {
                    statement.bindString(2, entity.name);
                }
                if (entity.getLastName() == null) {
                    statement.bindNull(3);
                } else {
                    statement.bindString(3, entity.getLastName());
                }
                statement.bindLong(4, entity.age);
                statement.bindLong(5, entity.uid);
            }
        };
        this.__updateAdapterOfUser_1 = new EntityDeletionOrUpdateAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
                statement.bindLong(1, entity.uid);
                if (entity.name == null) {
                    statement.bindNull(2);
                } else {
                    statement.bindString(2, entity.name);
                }
                if (entity.getLastName() == null) {
                    statement.bindNull(3);
                } else {
                    statement.bindString(3, entity.getLastName());
                }
                statement.bindLong(4, entity.age);
                statement.bindLong(5, entity.uid);
            }
        };
        this.__updateAdapterOfMultiPKeyEntity = new EntityDeletionOrUpdateAdapter<MultiPKeyEntity>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `MultiPKeyEntity` SET `name` = ?,`lastName` = ? WHERE `name` = ? AND `lastName` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    final MultiPKeyEntity entity) {
                if (entity.name == null) {
                    statement.bindNull(1);
                } else {
                    statement.bindString(1, entity.name);
                }
                if (entity.lastName == null) {
                    statement.bindNull(2);
                } else {
                    statement.bindString(2, entity.lastName);
                }
                if (entity.name == null) {
                    statement.bindNull(3);
                } else {
                    statement.bindString(3, entity.name);
                }
                if (entity.lastName == null) {
                    statement.bindNull(4);
                } else {
                    statement.bindString(4, entity.lastName);
                }
            }
        };
        this.__updateAdapterOfBook = new EntityDeletionOrUpdateAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement, final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
                statement.bindLong(3, entity.bookId);
            }
        };
        this.__preparedStmtOfAgeUserByUid = new SharedSQLiteStatement(__db) {
            @Override
            @NonNull
            public String createQuery() {
                final String _query = "UPDATE User SET ageColumn = ageColumn + 1 WHERE uid = ?";
                return _query;
            }
        };
        this.__preparedStmtOfAgeUserAll = new SharedSQLiteStatement(__db) {
            @Override
            @NonNull
            public String createQuery() {
                final String _query = "UPDATE User SET ageColumn = ageColumn + 1";
                return _query;
            }
        };
    }

    @Override
    public void updateUser(final User user) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __updateAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void updateUsers(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __updateAdapterOfUser.handle(user1);
            __updateAdapterOfUser.handleMultiple(others);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void updateArrayOfUsers(final User[] users) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __updateAdapterOfUser.handleMultiple(users);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void updateTwoUsers(final User userOne, final User userTwo) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __updateAdapterOfUser_1.handle(userOne);
            __updateAdapterOfUser_1.handle(userTwo);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int updateUserAndReturnCount(final User user) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __updateAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int updateUserAndReturnCount(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __updateAdapterOfUser.handle(user1);
            _total += __updateAdapterOfUser.handleMultiple(others);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int updateUserAndReturnCount(final User[] users) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __updateAdapterOfUser.handleMultiple(users);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public Integer updateUserAndReturnCountObject(final User user) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __updateAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public Completable updateUserAndReturnCountCompletable(final User user) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                __db.beginTransaction();
                try {
                    __updateAdapterOfUser.handle(user);
                    __db.setTransactionSuccessful();
                    return null;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @Override
    public Single<Integer> updateUserAndReturnCountSingle(final User user) {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                int _total = 0;
                __db.beginTransaction();
                try {
                    _total += __updateAdapterOfUser.handle(user);
                    __db.setTransactionSuccessful();
                    return _total;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @Override
    public Maybe<Integer> updateUserAndReturnCountMaybe(final User user) {
        return Maybe.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                int _total = 0;
                __db.beginTransaction();
                try {
                    _total += __updateAdapterOfUser.handle(user);
                    __db.setTransactionSuccessful();
                    return _total;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @Override
    public int multiPKey(final MultiPKeyEntity entity) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __updateAdapterOfMultiPKeyEntity.handle(entity);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void updateUserAndBook(final User user, final Book book) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __updateAdapterOfUser.handle(user);
            __updateAdapterOfBook.handle(book);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void updateAndAge(final User user) {
        __db.beginTransaction();
        try {
            UpdateDao.super.updateAndAge(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void ageUserByUid(final String uid) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement _stmt = __preparedStmtOfAgeUserByUid.acquire();
        int _argIndex = 1;
        if (uid == null) {
            _stmt.bindNull(_argIndex);
        } else {
            _stmt.bindString(_argIndex, uid);
        }
        try {
            __db.beginTransaction();
            try {
                _stmt.executeUpdateDelete();
                __db.setTransactionSuccessful();
            } finally {
                __db.endTransaction();
            }
        } finally {
            __preparedStmtOfAgeUserByUid.release(_stmt);
        }
    }

    @Override
    public void ageUserAll() {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement _stmt = __preparedStmtOfAgeUserAll.acquire();
        try {
            __db.beginTransaction();
            try {
                _stmt.executeUpdateDelete();
                __db.setTransactionSuccessful();
            } finally {
                __db.endTransaction();
            }
        } finally {
            __preparedStmtOfAgeUserAll.release(_stmt);
        }
    }

    @Override
    public Completable ageUserAllCompletable() {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfAgeUserAll.acquire();
                try {
                    __db.beginTransaction();
                    try {
                        _stmt.executeUpdateDelete();
                        __db.setTransactionSuccessful();
                        return null;
                    } finally {
                        __db.endTransaction();
                    }
                } finally {
                    __preparedStmtOfAgeUserAll.release(_stmt);
                }
            }
        });
    }

    @Override
    public Single<Integer> ageUserAllSingle() {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfAgeUserAll.acquire();
                try {
                    __db.beginTransaction();
                    try {
                        final Integer _result = _stmt.executeUpdateDelete();
                        __db.setTransactionSuccessful();
                        return _result;
                    } finally {
                        __db.endTransaction();
                    }
                } finally {
                    __preparedStmtOfAgeUserAll.release(_stmt);
                }
            }
        });
    }

    @Override
    public Maybe<Integer> ageUserAllMaybe() {
        return Maybe.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfAgeUserAll.acquire();
                try {
                    __db.beginTransaction();
                    try {
                        final Integer _result = _stmt.executeUpdateDelete();
                        __db.setTransactionSuccessful();
                        return _result;
                    } finally {
                        __db.endTransaction();
                    }
                } finally {
                    __preparedStmtOfAgeUserAll.release(_stmt);
                }
            }
        });
    }

    @NonNull
    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}