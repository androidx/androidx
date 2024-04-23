package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class WriterDao_Impl implements WriterDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<User> __insertAdapterOfUser;

  private final EntityInsertAdapter<User> __insertAdapterOfUser_1;

  private final EntityInsertAdapter<User> __insertAdapterOfUser_2;

  private final EntityInsertAdapter<Book> __insertAdapterOfBook;

  public WriterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfUser = new EntityInsertAdapter<User>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
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
    };
    this.__insertAdapterOfUser_1 = new EntityInsertAdapter<User>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `User` (`uid`,`name`,`lastName`,`ageColumn`) VALUES (?,?,?,?)";
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
    };
    this.__insertAdapterOfUser_2 = new EntityInsertAdapter<User>() {
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
    };
    this.__insertAdapterOfBook = new EntityInsertAdapter<Book>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Book` (`bookId`,`uid`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Book entity) {
        statement.bindLong(1, entity.bookId);
        statement.bindLong(2, entity.uid);
      }
    };
  }

  @Override
  public void insertUser(final User user) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUser.insert(_connection, user);
      return null;
    });
  }

  @Override
  public void insertUsers(final User user1, final List<User> others) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUser.insert(_connection, user1);
      __insertAdapterOfUser.insert(_connection, others);
      return null;
    });
  }

  @Override
  public void insertUsers(final User[] users) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUser_1.insert(_connection, users);
      return null;
    });
  }

  @Override
  public void insertTwoUsers(final User userOne, final User userTwo) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUser_2.insert(_connection, userOne);
      __insertAdapterOfUser_2.insert(_connection, userTwo);
      return null;
    });
  }

  @Override
  public void insertUserAndBook(final User user, final Book book) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUser.insert(_connection, user);
      __insertAdapterOfBook.insert(_connection, book);
      return null;
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
