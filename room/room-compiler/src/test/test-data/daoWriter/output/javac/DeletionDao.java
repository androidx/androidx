package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.RoomDatabase;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.lang.Void;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DeletionDao_Impl implements DeletionDao {
    private final RoomDatabase __db;

    private final EntityDeletionOrUpdateAdapter<User> __deletionAdapterOfUser;

    private final EntityDeletionOrUpdateAdapter<MultiPKeyEntity> __deletionAdapterOfMultiPKeyEntity;

    private final EntityDeletionOrUpdateAdapter<Book> __deletionAdapterOfBook;

    private final SharedSQLiteStatement __preparedStmtOfDeleteByUid;

    private final SharedSQLiteStatement __preparedStmtOfDeleteEverything;

    public DeletionDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__deletionAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "DELETE FROM `User` WHERE `uid` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
                statement.bindLong(1, entity.uid);
            }
        };
        this.__deletionAdapterOfMultiPKeyEntity = new EntityDeletionOrUpdateAdapter<MultiPKeyEntity>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "DELETE FROM `MultiPKeyEntity` WHERE `name` = ? AND `lastName` = ?";
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
            }
        };
        this.__deletionAdapterOfBook = new EntityDeletionOrUpdateAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "DELETE FROM `Book` WHERE `bookId` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement, final Book entity) {
                statement.bindLong(1, entity.bookId);
            }
        };
        this.__preparedStmtOfDeleteByUid = new SharedSQLiteStatement(__db) {
            @Override
            @NonNull
            public String createQuery() {
                final String _query = "DELETE FROM user where uid = ?";
                return _query;
            }
        };
        this.__preparedStmtOfDeleteEverything = new SharedSQLiteStatement(__db) {
            @Override
            @NonNull
            public String createQuery() {
                final String _query = "DELETE FROM user";
                return _query;
            }
        };
    }

    @Override
    public void deleteUser(final User user) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __deletionAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void deleteUsers(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __deletionAdapterOfUser.handle(user1);
            __deletionAdapterOfUser.handleMultiple(others);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void deleteArrayOfUsers(final User[] users) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __deletionAdapterOfUser.handleMultiple(users);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public Integer deleteUserAndReturnCountObject(final User user) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __deletionAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int deleteUserAndReturnCount(final User user) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __deletionAdapterOfUser.handle(user);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int deleteUserAndReturnCount(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __deletionAdapterOfUser.handle(user1);
            _total += __deletionAdapterOfUser.handleMultiple(others);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int deleteUserAndReturnCount(final User[] users) {
        __db.assertNotSuspendingTransaction();
        int _total = 0;
        __db.beginTransaction();
        try {
            _total += __deletionAdapterOfUser.handleMultiple(users);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public Completable deleteUserCompletable(final User user) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                __db.beginTransaction();
                try {
                    __deletionAdapterOfUser.handle(user);
                    __db.setTransactionSuccessful();
                    return null;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @Override
    public Single<Integer> deleteUserSingle(final User user) {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                int _total = 0;
                __db.beginTransaction();
                try {
                    _total += __deletionAdapterOfUser.handle(user);
                    __db.setTransactionSuccessful();
                    return _total;
                } finally {
                    __db.endTransaction();
                }
            }
        });
    }

    @Override
    public Maybe<Integer> deleteUserMaybe(final User user) {
        return Maybe.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                int _total = 0;
                __db.beginTransaction();
                try {
                    _total += __deletionAdapterOfUser.handle(user);
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
            _total += __deletionAdapterOfMultiPKeyEntity.handle(entity);
            __db.setTransactionSuccessful();
            return _total;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void deleteUserAndBook(final User user, final Book book) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __deletionAdapterOfUser.handle(user);
            __deletionAdapterOfBook.handle(book);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public int deleteByUid(final int uid) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUid.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, uid);
        try {
            __db.beginTransaction();
            try {
                final int _result = _stmt.executeUpdateDelete();
                __db.setTransactionSuccessful();
                return _result;
            } finally {
                __db.endTransaction();
            }
        } finally {
            __preparedStmtOfDeleteByUid.release(_stmt);
        }
    }

    @Override
    public Completable deleteByUidCompletable(final int uid) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUid.acquire();
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, uid);
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
                    __preparedStmtOfDeleteByUid.release(_stmt);
                }
            }
        });
    }

    @Override
    public Single<Integer> deleteByUidSingle(final int uid) {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUid.acquire();
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, uid);
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
                    __preparedStmtOfDeleteByUid.release(_stmt);
                }
            }
        });
    }

    @Override
    public Maybe<Integer> deleteByUidMaybe(final int uid) {
        return Maybe.fromCallable(new Callable<Integer>() {
            @Override
            @Nullable
            public Integer call() throws Exception {
                final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUid.acquire();
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, uid);
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
                    __preparedStmtOfDeleteByUid.release(_stmt);
                }
            }
        });
    }

    @Override
    public int deleteEverything() {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteEverything.acquire();
        try {
            __db.beginTransaction();
            try {
                final int _result = _stmt.executeUpdateDelete();
                __db.setTransactionSuccessful();
                return _result;
            } finally {
                __db.endTransaction();
            }
        } finally {
            __preparedStmtOfDeleteEverything.release(_stmt);
        }
    }

    @Override
    public int deleteByUidList(final int... uid) {
        __db.assertNotSuspendingTransaction();
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM user where uid IN(");
        final int _inputSize = uid == null ? 1 : uid.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        if (uid == null) {
            _stmt.bindNull(_argIndex);
        } else {
            for (int _item : uid) {
                _stmt.bindLong(_argIndex, _item);
                _argIndex++;
            }
        }
        __db.beginTransaction();
        try {
            final int _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
        } finally {
            __db.endTransaction();
        }
    }

    @NonNull
    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}