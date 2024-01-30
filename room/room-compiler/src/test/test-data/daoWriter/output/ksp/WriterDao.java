package foo.bar;

import androidx.annotation.NonNull;
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

    private final EntityInsertionAdapter<User> __insertionAdapterOfUser_2;

    private final EntityInsertionAdapter<Book> __insertionAdapterOfBook;

    public WriterDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT OR ABORT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindString(2, entity.name);
                statement.bindString(3, entity.getLastName());
                statement.bindLong(4, entity.age);
            }
        };
        this.__insertionAdapterOfUser_1 = new EntityInsertionAdapter<User>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT OR REPLACE INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final User entity) {
                statement.bindLong(1, entity.uid);
                statement.bindString(2, entity.name);
                statement.bindString(3, entity.getLastName());
                statement.bindLong(4, entity.age);
            }
        };
        this.__insertionAdapterOfUser_2 = new EntityInsertionAdapter<User>(__db) {
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
        };
        this.__insertionAdapterOfBook = new EntityInsertionAdapter<Book>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT OR ABORT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final Book entity) {
                statement.bindLong(1, entity.bookId);
                statement.bindLong(2, entity.uid);
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
    public void insertTwoUsers(final User userOne, final User userTwo) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __insertionAdapterOfUser_2.insert(userOne);
            __insertionAdapterOfUser_2.insert(userTwo);
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

    @NonNull
    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }
}