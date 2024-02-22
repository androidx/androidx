package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
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
import kotlin.jvm.functions.Function1;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UpdateDao_Impl implements UpdateDao {
    private final RoomDatabase __db;

    private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser;

    private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser_1;

    private final EntityDeletionOrUpdateAdapter<MultiPKeyEntity> __updateAdapterOfMultiPKeyEntity;

    private final EntityDeletionOrUpdateAdapter<Book> __updateAdapterOfBook;

    public UpdateDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__updateAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindString(2, entity.name);
                statement.bindString(3, entity.getLastName());
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
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindString(2, entity.name);
                statement.bindString(3, entity.getLastName());
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
                    @NonNull final MultiPKeyEntity entity) {
                statement.bindString(1, entity.name);
                statement.bindString(2, entity.lastName);
                statement.bindString(3, entity.name);
                statement.bindString(4, entity.lastName);
            }
        };
        this.__updateAdapterOfBook = new EntityDeletionOrUpdateAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
                statement.bindLong(3, entity.bookId);
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
        final String _sql = "UPDATE User SET ageColumn = ageColumn + 1 WHERE uid = ?";
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                final SQLiteStatement _stmt = _connection.prepare(_sql);
                try {
                    int _argIndex = 1;
                    _stmt.bindText(_argIndex, uid);
                    _stmt.step();
                    return null;
                } finally {
                    _stmt.close();
                }
            }
        });
    }

    @Override
    public void ageUserAll() {
        final String _sql = "UPDATE User SET ageColumn = ageColumn + 1";
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                final SQLiteStatement _stmt = _connection.prepare(_sql);
                try {
                    _stmt.step();
                    return null;
                } finally {
                    _stmt.close();
                }
            }
        });
    }

    @Override
    public Completable ageUserAllCompletable() {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                final String _sql = "UPDATE User SET ageColumn = ageColumn + 1";
                final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
                __db.beginTransaction();
                try {
                    _stmt.executeUpdateDelete();
                    __db.setTransactionSuccessful();
                    return null;
                } finally {
                    __db.endTransaction();
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
                final String _sql = "UPDATE User SET ageColumn = ageColumn + 1";
                final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
                __db.beginTransaction();
                try {
                    final Integer _result = _stmt.executeUpdateDelete();
                    __db.setTransactionSuccessful();
                    return _result;
                } finally {
                    __db.endTransaction();
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
                final String _sql = "UPDATE User SET ageColumn = ageColumn + 1";
                final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
                __db.beginTransaction();
                try {
                    final Integer _result = _stmt.executeUpdateDelete();
                    __db.setTransactionSuccessful();
                    return _result;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @NonNull
    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}