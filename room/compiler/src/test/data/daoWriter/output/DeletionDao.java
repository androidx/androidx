package foo.bar;

import com.android.support.db.SupportSQLiteStatement;
import com.android.support.room.EntityDeletionOrUpdateAdapter;
import com.android.support.room.RoomDatabase;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class DeletionDao_Impl implements DeletionDao {
  private final RoomDatabase __db;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfUser;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfMultiPKeyEntity;

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
}