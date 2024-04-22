package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.EntityUpsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.lang.Void;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.jvm.functions.Function1;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class UpsertDao_Impl implements UpsertDao {
  private final RoomDatabase __db;

  private final EntityUpsertAdapter<User> __upsertAdapterOfUser;

  private final EntityUpsertAdapter<Book> __upsertAdapterOfBook;

  public UpsertDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__upsertAdapterOfUser = new EntityUpsertAdapter<User>(new EntityInsertAdapter<User>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final User entity) {
        statement.bindLong(1, entity.uid);
        if (entity.name == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.name);
        }
        if (entity.getLastName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getLastName());
        }
        statement.bindLong(4, entity.age);
      }
    }, new EntityDeleteOrUpdateAdapter<User>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final User entity) {
        statement.bindLong(1, entity.uid);
        if (entity.name == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.name);
        }
        if (entity.getLastName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getLastName());
        }
        statement.bindLong(4, entity.age);
        statement.bindLong(5, entity.uid);
      }
    });
    this.__upsertAdapterOfBook = new EntityUpsertAdapter<Book>(new EntityInsertAdapter<Book>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Book entity) {
        statement.bindLong(1, entity.bookId);
        statement.bindLong(2, entity.uid);
      }
    }, new EntityDeleteOrUpdateAdapter<Book>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `Book` SET `bookId` = ?,`uid` = ? WHERE `bookId` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Book entity) {
        statement.bindLong(1, entity.bookId);
        statement.bindLong(2, entity.uid);
        statement.bindLong(3, entity.bookId);
      }
    });
  }

  @Override
  public void upsertUser(final User user) {
    DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
      @Override
      @NonNull
      public Void invoke(@NonNull final SQLiteConnection _connection) {
        __upsertAdapterOfUser.upsert(_connection, user);
        return null;
      }
    });
  }

  @Override
  public void upsertUsers(final User user1, final List<User> others) {
    DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
      @Override
      @NonNull
      public Void invoke(@NonNull final SQLiteConnection _connection) {
        __upsertAdapterOfUser.upsert(_connection, user1);
        __upsertAdapterOfUser.upsert(_connection, others);
        return null;
      }
    });
  }

  @Override
  public void upsertUsers(final User[] users) {
    DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
      @Override
      @NonNull
      public Void invoke(@NonNull final SQLiteConnection _connection) {
        __upsertAdapterOfUser.upsert(_connection, users);
        return null;
      }
    });
  }

  @Override
  public void upsertTwoUsers(final User userOne, final User userTwo) {
    DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
      @Override
      @NonNull
      public Void invoke(@NonNull final SQLiteConnection _connection) {
        __upsertAdapterOfUser.upsert(_connection, userOne);
        __upsertAdapterOfUser.upsert(_connection, userTwo);
        return null;
      }
    });
  }

  @Override
  public void upsertUserAndBook(final User user, final Book book) {
    DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Void>() {
      @Override
      @NonNull
      public Void invoke(@NonNull final SQLiteConnection _connection) {
        __upsertAdapterOfUser.upsert(_connection, user);
        __upsertAdapterOfBook.upsert(_connection, book);
        return null;
      }
    });
  }

  @Override
  public long upsertAndReturnId(final User user) {
    return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Long>() {
      @Override
      @NonNull
      public Long invoke(@NonNull final SQLiteConnection _connection) {
        return __upsertAdapterOfUser.upsertAndReturnId(_connection, user);
      }
    });
  }

  @Override
  public long[] upsertAndReturnIdsArray(final User[] users) {
    return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, long[]>() {
      @Override
      @NonNull
      public long[] invoke(@NonNull final SQLiteConnection _connection) {
        return __upsertAdapterOfUser.upsertAndReturnIdsArray(_connection, users);
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
