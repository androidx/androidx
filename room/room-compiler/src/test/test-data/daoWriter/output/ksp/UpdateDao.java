package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeleteOrUpdateAdapter;
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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UpdateDao_Impl implements UpdateDao {
    private final RoomDatabase __db;

    private final EntityDeleteOrUpdateAdapter<User> __updateAdapterOfUser;

    private final EntityDeleteOrUpdateAdapter<User> __updateAdapterOfUser_1;

    private final EntityDeletionOrUpdateAdapter<User> __updateCompatAdapterOfUser;

    private final EntityDeleteOrUpdateAdapter<MultiPKeyEntity> __updateAdapterOfMultiPKeyEntity;

    private final EntityDeleteOrUpdateAdapter<Book> __updateAdapterOfBook;

    public UpdateDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__updateAdapterOfUser = new EntityDeleteOrUpdateAdapter<User>() {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SQLiteStatement statement, @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindText(2, entity.name);
                statement.bindText(3, entity.getLastName());
                statement.bindLong(4, entity.age);
                statement.bindLong(5, entity.uid);
            }
        };
        this.__updateAdapterOfUser_1 = new EntityDeleteOrUpdateAdapter<User>() {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SQLiteStatement statement, @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindText(2, entity.name);
                statement.bindText(3, entity.getLastName());
                statement.bindLong(4, entity.age);
                statement.bindLong(5, entity.uid);
            }
        };
        this.__updateCompatAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
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
        this.__updateAdapterOfMultiPKeyEntity = new EntityDeleteOrUpdateAdapter<MultiPKeyEntity>() {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `MultiPKeyEntity` SET `name` = ?,`lastName` = ? WHERE `name` = ? AND `lastName` = ?";
            }

            @Override
            protected void bind(@NonNull final SQLiteStatement statement,
                    @NonNull final MultiPKeyEntity entity) {
                statement.bindText(1, entity.name);
                statement.bindText(2, entity.lastName);
                statement.bindText(3, entity.name);
                statement.bindText(4, entity.lastName);
            }
        };
        this.__updateAdapterOfBook = new EntityDeleteOrUpdateAdapter<Book>() {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE OR ABORT `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
            }

            @Override
            protected void bind(@NonNull final SQLiteStatement statement, @NonNull final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
                statement.bindLong(3, entity.bookId);
            }
        };
    }

    @Override
    public void updateUser(final User user) {
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                __updateAdapterOfUser.handle(_connection, user);
                return null;
            }
        });
    }

    @Override
    public void updateUsers(final User user1, final List<User> others) {
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                __updateAdapterOfUser.handle(_connection, user1);
                __updateAdapterOfUser.handleMultiple(_connection, others);
                return null;
            }
        });
    }

    @Override
    public void updateArrayOfUsers(final User[] users) {
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                __updateAdapterOfUser.handleMultiple(_connection, users);
                return null;
            }
        });
    }

    @Override
    public void updateTwoUsers(final User userOne, final User userTwo) {
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                __updateAdapterOfUser_1.handle(_connection, userOne);
                __updateAdapterOfUser_1.handle(_connection, userTwo);
                return null;
            }
        });
    }

    @Override
    public int updateUserAndReturnCount(final User user) {
        return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Integer>() {
            @Override
            @NonNull
            public Integer invoke(@NonNull final SQLiteConnection _connection) {
                int _result = 0;
                _result += __updateAdapterOfUser.handle(_connection, user);
                return _result;
            }
        });
    }

    @Override
    public int updateUserAndReturnCount(final User user1, final List<User> others) {
        return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Integer>() {
            @Override
            @NonNull
            public Integer invoke(@NonNull final SQLiteConnection _connection) {
                int _result = 0;
                _result += __updateAdapterOfUser.handle(_connection, user1);
                _result += __updateAdapterOfUser.handleMultiple(_connection, others);
                return _result;
            }
        });
    }

    @Override
    public int updateUserAndReturnCount(final User[] users) {
        return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Integer>() {
            @Override
            @NonNull
            public Integer invoke(@NonNull final SQLiteConnection _connection) {
                int _result = 0;
                _result += __updateAdapterOfUser.handleMultiple(_connection, users);
                return _result;
            }
        });
    }

    @Override
    public Integer updateUserAndReturnCountObject(final User user) {
        return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Integer>() {
            @Override
            @NonNull
            public Integer invoke(@NonNull final SQLiteConnection _connection) {
                int _result = 0;
                _result += __updateAdapterOfUser.handle(_connection, user);
                return _result;
            }
        });
    }

    @Override
    public Completable updateUserAndReturnCountCompletable(final User user) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                __db.beginTransaction();
                try {
                    __updateCompatAdapterOfUser.handle(user);
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
                    _total += __updateCompatAdapterOfUser.handle(user);
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
                    _total += __updateCompatAdapterOfUser.handle(user);
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
        return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Integer>() {
            @Override
            @NonNull
            public Integer invoke(@NonNull final SQLiteConnection _connection) {
                int _result = 0;
                _result += __updateAdapterOfMultiPKeyEntity.handle(_connection, entity);
                return _result;
            }
        });
    }

    @Override
    public void updateUserAndBook(final User user, final Book book) {
        DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
            @Override
            @NonNull
            public Void invoke(@NonNull final SQLiteConnection _connection) {
                __updateAdapterOfUser.handle(_connection, user);
                __updateAdapterOfBook.handle(_connection, book);
                return null;
            }
        });
    }

    @Override
    public void updateAndAge(final User user) {
        DBUtil.performBlocking(__db, false, true, (_connection) -> {
            UpdateDao.super.updateAndAge(user);
            return Unit.INSTANCE;
        });
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