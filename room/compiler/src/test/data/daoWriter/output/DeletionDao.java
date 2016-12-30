package foo.bar;

import com.android.support.db.SupportSQLiteStatement;
import com.android.support.room.EntityDeletionOrUpdateAdapter;
import com.android.support.room.RoomDatabase;
import com.android.support.room.SharedSQLiteStatement;
import com.android.support.room.util.StringUtil;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;

public class DeletionDao_Impl implements DeletionDao {
  private final RoomDatabase __db;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfUser;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfMultiPKeyEntity;

  private final SharedSQLiteStatement _preparedStmtOfDeleteByUid;

  public DeletionDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__deletionAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `User` WHERE `uid` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, User value) {
        stmt.bindLong(1, value.uid);
      }
    };
    this.__deletionAdapterOfMultiPKeyEntity = new EntityDeletionOrUpdateAdapter<MultiPKeyEntity>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `MultiPKeyEntity` WHERE `name` = ? AND `lastName` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, MultiPKeyEntity value) {
        if (value.name == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.name);
        }
        if (value.lastName == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.lastName);
        }
      }
    };
    this._preparedStmtOfDeleteByUid = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        String _query = "DELETE FROM user where uid = ?";
        return _query;
      }
    };
  }

  @Override
  public void deleteUser(User user) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfUser.handle(user);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteUsers(User user1, List<User> others) {
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
  public void deleteArrayOfUsers(User[] users) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfUser.handleMultiple(users);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int deleteUserAndReturnCount(User user) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__deletionAdapterOfUser.handle(user);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int deleteUserAndReturnCount(User user1, List<User> others) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__deletionAdapterOfUser.handle(user1);
      _total +=__deletionAdapterOfUser.handleMultiple(others);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int deleteUserAndReturnCount(User[] users) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__deletionAdapterOfUser.handleMultiple(users);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int multiPKey(MultiPKeyEntity entity) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__deletionAdapterOfMultiPKeyEntity.handle(entity);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int deleteByUid(int uid) {
    final SupportSQLiteStatement _stmt = _preparedStmtOfDeleteByUid.acquire();
    try {
      int _argIndex = 1;
      _stmt.bindLong(_argIndex, uid);
      return _stmt.executeUpdateDelete();
    } finally {
      _preparedStmtOfDeleteByUid.release(_stmt);
    }
  }

  @Override
  public int deleteByUidList(int... uid) {
    StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("DELETE FROM user where uid IN(");
    final int _inputSize = uid.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    String _sql = _stringBuilder.toString();
    SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
    int _argIndex = 1;
    for (int _item : uid) {
      _stmt.bindLong(_argIndex, _item);
      _argIndex ++;
    }
    return _stmt.executeUpdateDelete();
  }
}
