package foo.bar;

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

    public UpsertDao_Impl(RoomDatabase __db) {
        this.__db = __db;
        this.__upsertionAdapterOfUser = new EntityUpsertionAdapter<User>(new EntityInsertionAdapter<User>(__db) {
                    @Override
                    public String createQuery() {
                        return "INSERT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
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
                }, new EntityDeletionOrUpdateAdapter<User>(__db) {
                    @Override
                    public String createQuery() {
                        return "UPDATE `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
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
                        stmt.bindLong(5, value.uid);
                    }
        });
        this.__upsertionAdapterOfBook = new EntityUpsertionAdapter<Book>(new EntityInsertionAdapter<Book>(__db) {
                    @Override
                    public String createQuery() {
                        return "INSERT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
                    }

                    @Override
                    public void bind(SupportSQLiteStatement stmt, Book value) {
                        stmt.bindLong(1, value.bookId);
                        stmt.bindLong(2, value.uid);
                    }
                }, new EntityDeletionOrUpdateAdapter<Book>(__db) {
                    @Override
                    public String createQuery() {
                        return "UPDATE `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
                    }

                    @Override
                    public void bind(SupportSQLiteStatement stmt, Book value) {
                        stmt.bindLong(1, value.bookId);
                        stmt.bindLong(2, value.uid);
                        stmt.bindLong(3, value.bookId);
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
            long _result = __upsertionAdapterOfUser.upsertAndReturnId(user);
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
            long[] _result = __upsertionAdapterOfUser.upsertAndReturnIdsArray(users);
            __db.setTransactionSuccessful();
            return _result;
        } finally {
            __db.endTransaction();
        }
    }

    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}