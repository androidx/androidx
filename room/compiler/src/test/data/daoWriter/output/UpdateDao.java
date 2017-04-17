package foo.bar;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityDeletionOrUpdateAdapter;
import android.arch.persistence.room.RoomDatabase;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class UpdateDao_Impl implements UpdateDao {
  private final RoomDatabase __db;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfUser;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfMultiPKeyEntity;

  public UpdateDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__updateAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `User` SET `uid` = ?,`name` = ?,`lastName` = ?,`ageColumn` = ? WHERE `uid` = ?";
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
    };
    this.__updateAdapterOfMultiPKeyEntity = new EntityDeletionOrUpdateAdapter<MultiPKeyEntity>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `MultiPKeyEntity` SET `name` = ?,`lastName` = ? WHERE `name` = ? AND `lastName` = ?";
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
        if (value.name == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.name);
        }
        if (value.lastName == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.lastName);
        }
      }
    };
  }

  @Override
  public void updateUser(User user) {
    __db.beginTransaction();
    try {
      __updateAdapterOfUser.handle(user);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateUsers(User user1, List<User> others) {
    __db.beginTransaction();
    try {
      __updateAdapterOfUser.handle(user1);
      __updateAdapterOfUser.handleMultiple(others);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateArrayOfUsers(User[] users) {
    __db.beginTransaction();
    try {
      __updateAdapterOfUser.handleMultiple(users);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int updateUserAndReturnCount(User user) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__updateAdapterOfUser.handle(user);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int updateUserAndReturnCount(User user1, List<User> others) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__updateAdapterOfUser.handle(user1);
      _total +=__updateAdapterOfUser.handleMultiple(others);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int updateUserAndReturnCount(User[] users) {
    int _total = 0;
    __db.beginTransaction();
    try {
      _total +=__updateAdapterOfUser.handleMultiple(users);
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
      _total +=__updateAdapterOfMultiPKeyEntity.handle(entity);
      __db.setTransactionSuccessful();
      return _total;
    } finally {
      __db.endTransaction();
    }
  }
}
