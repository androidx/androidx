package foo.bar;

import androidx.room.EntityInsertionAdapter;
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
public final class WriterDao_Impl implements WriterDao {
    private final RoomDatabase __db;

    private final EntityInsertionAdapter<User> __insertionAdapterOfUser;

    private final EntityInsertionAdapter<User> __insertionAdapterOfUser_1;

    private final EntityInsertionAdapter<Book> __insertionAdapterOfBook;

    public WriterDao_Impl(RoomDatabase __db) {
        this.__db = __db;
        this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
            @Override
            public String createQuery() {
                return "INSERT OR ABORT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
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
                return "INSERT OR REPLACE INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
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
                return "INSERT OR ABORT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
            }

            @Override
            public void bind(SupportSQLiteStatement stmt, Book value) {
                stmt.bindLong(1, value.bookId);
                stmt.bindLong(2, value.uid);
            }
        };
    }

    @Override
    public void insertUser(final User user) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser.insert(user);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void insertUsers(final User user1, final List<User> others) {
        __db.assertNotSuspendingTransaction();
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
    public void insertUsers(final User[] users) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser_1.insert(users);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public void insertUserAndBook(final User user, final Book book) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser.insert(user);
            __insertionAdapterOfBook.insert(book);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}