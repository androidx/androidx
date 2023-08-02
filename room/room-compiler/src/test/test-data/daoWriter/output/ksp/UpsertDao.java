package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UpsertDao_Impl implements UpsertDao {
    private final RoomDatabase __db;

    private final EntityUpsertionAdapter<User> __upsertionAdapterOfUser;

    private final EntityUpsertionAdapter<Book> __upsertionAdapterOfBook;

    public UpsertDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__upsertionAdapterOfUser = new EntityUpsertionAdapter<User>(new EntityInsertionAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindString(2, entity.name);
                statement.bindString(3, entity.getLastName());
                statement.bindLong(4, entity.age);
            }
        }, new EntityDeletionOrUpdateAdapter<User>(__db) {
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
        });
        this.__upsertionAdapterOfBook = new EntityUpsertionAdapter<Book>(new EntityInsertionAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
            }
        }, new EntityDeletionOrUpdateAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "UPDATE `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
                statement.bindLong(3, entity.bookId);
            }
        });
    }

    @Override
    public void upsertUser(final User user) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __upsertionAdapterOfUser.upsert(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void upsertUsers(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __upsertionAdapterOfUser.upsert(user1);
            __upsertionAdapterOfUser.upsert(others);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void upsertUsers(final User[] users) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __upsertionAdapterOfUser.upsert(users);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void upsertTwoUsers(final User userOne, final User userTwo) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __upsertionAdapterOfUser.upsert(userOne);
            __upsertionAdapterOfUser.upsert(userTwo);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void upsertUserAndBook(final User user, final Book book) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __upsertionAdapterOfUser.upsert(user);
            __upsertionAdapterOfBook.upsert(book);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public long upsertAndReturnId(final User user) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            final long _result = __upsertionAdapterOfUser.upsertAndReturnId(user);
            __db.setTransactionSuccessful();
            return _result;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public long[] upsertAndReturnIdsArray(final User[] users) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            final long[] _result = __upsertionAdapterOfUser.upsertAndReturnIdsArray(users);
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