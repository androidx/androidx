package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RxRoom;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteConnectionUtil;
import androidx.room.util.StringUtil;
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
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.lang.Void;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class DeletionDao_Impl implements DeletionDao {
  private final RoomDatabase __db;

  private final EntityDeleteOrUpdateAdapter<User> __deleteAdapterOfUser;

  private final EntityDeletionOrUpdateAdapter<User> __deleteCompatAdapterOfUser;

  private final EntityDeleteOrUpdateAdapter<MultiPKeyEntity> __deleteAdapterOfMultiPKeyEntity;

  private final EntityDeleteOrUpdateAdapter<Book> __deleteAdapterOfBook;

  public DeletionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__deleteAdapterOfUser = new EntityDeleteOrUpdateAdapter<User>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `User` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final User entity) {
        statement.bindLong(1, entity.uid);
      }
    };
    this.__deleteCompatAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `User` WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final User entity) {
        statement.bindLong(1, entity.uid);
      }
    };
    this.__deleteAdapterOfMultiPKeyEntity = new EntityDeleteOrUpdateAdapter<MultiPKeyEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `MultiPKeyEntity` WHERE `name` = ? AND `lastName` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final MultiPKeyEntity entity) {
        statement.bindText(1, entity.name);
        statement.bindText(2, entity.lastName);
      }
    };
    this.__deleteAdapterOfBook = new EntityDeleteOrUpdateAdapter<Book>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Book` WHERE `bookId` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, @NonNull final Book entity) {
        statement.bindLong(1, entity.bookId);
      }
    };
  }

  @Override
  public void deleteUser(final User user) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __deleteAdapterOfUser.handle(_connection, user);
      return null;
    });
  }

  @Override
  public void deleteUsers(final User user1, final List<User> others) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __deleteAdapterOfUser.handle(_connection, user1);
      __deleteAdapterOfUser.handleMultiple(_connection, others);
      return null;
    });
  }

  @Override
  public void deleteArrayOfUsers(final User[] users) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __deleteAdapterOfUser.handleMultiple(_connection, users);
      return null;
    });
  }

  @Override
  public Integer deleteUserAndReturnCountObject(final User user) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfUser.handle(_connection, user);
      return _result;
    });
  }

  @Override
  public int deleteUserAndReturnCount(final User user) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfUser.handle(_connection, user);
      return _result;
    });
  }

  @Override
  public int deleteUserAndReturnCount(final User user1, final List<User> others) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfUser.handle(_connection, user1);
      _result += __deleteAdapterOfUser.handleMultiple(_connection, others);
      return _result;
    });
  }

  @Override
  public int deleteUserAndReturnCount(final User[] users) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfUser.handleMultiple(_connection, users);
      return _result;
    });
  }

  @Override
  public Completable deleteUserCompletable(final User user) {
    return Completable.fromCallable(new Callable<Void>() {
      @Override
      @Nullable
      public Void call() throws Exception {
        __db.beginTransaction();
        try {
          __deleteCompatAdapterOfUser.handle(user);
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
          _total += __deleteCompatAdapterOfUser.handle(user);
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
          _total += __deleteCompatAdapterOfUser.handle(user);
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
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfMultiPKeyEntity.handle(_connection, entity);
      return _result;
    });
  }

  @Override
  public void deleteUserAndBook(final User user, final Book book) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __deleteAdapterOfUser.handle(_connection, user);
      __deleteAdapterOfBook.handle(_connection, book);
      return null;
    });
  }

  @Override
  public int deleteByUid(final int uid) {
    final String _sql = "DELETE FROM user where uid = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, uid);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Completable deleteByUidCompletable(final int uid) {
    final String _sql = "DELETE FROM user where uid = ?";
    return RxRoom.createCompletable(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, uid);
        _stmt.step();
        return Unit.INSTANCE;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Single<Integer> deleteByUidSingle(final int uid) {
    final String _sql = "DELETE FROM user where uid = ?";
    return RxRoom.createSingle(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, uid);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Maybe<Integer> deleteByUidMaybe(final int uid) {
    final String _sql = "DELETE FROM user where uid = ?";
    return RxRoom.createMaybe(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, uid);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int deleteByUidList(final int... uid) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("DELETE FROM user where uid IN(");
    final int _inputSize = uid == null ? 1 : uid.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (uid == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (int _item : uid) {
            _stmt.bindLong(_argIndex, _item);
            _argIndex++;
          }
        }
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int deleteEverything() {
    final String _sql = "DELETE FROM user";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
