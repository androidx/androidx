package foo.bar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.RoomDatabase;
import androidx.room.RoomRawQuery;
import androidx.room.RoomSQLiteQuery;
import androidx.room.guava.GuavaRoom;
import androidx.room.paging.LimitOffsetPagingSource;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class ComplexDao_Impl extends ComplexDao {
  private final RoomDatabase __db;

  public ComplexDao_Impl(final ComplexDatabase __db) {
    super(__db);
    this.__db = __db;
  }

  @Override
  public boolean transactionMethod(final int i, final String s, final long l) {
    return DBUtil.performBlocking(__db, false, true, new Function1<SQLiteConnection, Boolean>() {
      @Override
      @NonNull
      public Boolean invoke(@NonNull final SQLiteConnection _connection) {
        return ComplexDao_Impl.super.transactionMethod(i, s, l);
      }
    });
  }

  @Override
  public List<ComplexDao.FullName> fullNames(final int id) {
    final String _sql = "SELECT name || lastName as fullName, uid as id FROM user where uid = ?";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<ComplexDao.FullName>>() {
      @Override
      @NonNull
      public List<ComplexDao.FullName> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          _stmt.bindLong(_argIndex, id);
          final int _cursorIndexOfFullName = 0;
          final int _cursorIndexOfId = 1;
          final List<ComplexDao.FullName> _result = new ArrayList<ComplexDao.FullName>();
          while (_stmt.step()) {
            final ComplexDao.FullName _item;
            _item = new ComplexDao.FullName();
            if (_stmt.isNull(_cursorIndexOfFullName)) {
              _item.fullName = null;
            } else {
              _item.fullName = _stmt.getText(_cursorIndexOfFullName);
            }
            _item.id = (int) (_stmt.getLong(_cursorIndexOfId));
            _result.add(_item);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public User getById(final int id) {
    final String _sql = "SELECT * FROM user where uid = ?";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, User>() {
      @Override
      @NonNull
      public User invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          _stmt.bindLong(_argIndex, id);
          final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uid");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastName");
          final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ageColumn");
          final User _result;
          if (_stmt.step()) {
            _result = new User();
            _result.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _stmt.getText(_cursorIndexOfName);
            }
            final String _tmpLastName;
            if (_stmt.isNull(_cursorIndexOfLastName)) {
              _tmpLastName = null;
            } else {
              _tmpLastName = _stmt.getText(_cursorIndexOfLastName);
            }
            _result.setLastName(_tmpLastName);
            _result.age = (int) (_stmt.getLong(_cursorIndexOfAge));
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public User findByName(final String name, final String lastName) {
    final String _sql = "SELECT * FROM user where name LIKE ? AND lastName LIKE ?";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, User>() {
      @Override
      @NonNull
      public User invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (name == null) {
            _stmt.bindNull(_argIndex);
          } else {
            _stmt.bindText(_argIndex, name);
          }
          _argIndex = 2;
          if (lastName == null) {
            _stmt.bindNull(_argIndex);
          } else {
            _stmt.bindText(_argIndex, lastName);
          }
          final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uid");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastName");
          final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ageColumn");
          final User _result;
          if (_stmt.step()) {
            _result = new User();
            _result.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _stmt.getText(_cursorIndexOfName);
            }
            final String _tmpLastName;
            if (_stmt.isNull(_cursorIndexOfLastName)) {
              _tmpLastName = null;
            } else {
              _tmpLastName = _stmt.getText(_cursorIndexOfLastName);
            }
            _result.setLastName(_tmpLastName);
            _result.age = (int) (_stmt.getLong(_cursorIndexOfAge));
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<User> loadAllByIds(final int... ids) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT * FROM user where uid IN (");
    final int _inputSize = ids == null ? 1 : ids.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<User>>() {
      @Override
      @NonNull
      public List<User> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (ids == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (int _item : ids) {
              _stmt.bindLong(_argIndex, _item);
              _argIndex++;
            }
          }
          final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uid");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastName");
          final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ageColumn");
          final List<User> _result = new ArrayList<User>();
          while (_stmt.step()) {
            final User _item_1;
            _item_1 = new User();
            _item_1.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _item_1.name = null;
            } else {
              _item_1.name = _stmt.getText(_cursorIndexOfName);
            }
            final String _tmpLastName;
            if (_stmt.isNull(_cursorIndexOfLastName)) {
              _tmpLastName = null;
            } else {
              _tmpLastName = _stmt.getText(_cursorIndexOfLastName);
            }
            _item_1.setLastName(_tmpLastName);
            _item_1.age = (int) (_stmt.getLong(_cursorIndexOfAge));
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  int getAge(final int id) {
    final String _sql = "SELECT ageColumn FROM user where uid = ?";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, Integer>() {
      @Override
      @NonNull
      public Integer invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          _stmt.bindLong(_argIndex, id);
          final int _result;
          if (_stmt.step()) {
            _result = (int) (_stmt.getLong(0));
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public int[] getAllAges(final int... ids) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
    final int _inputSize = ids == null ? 1 : ids.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, int[]>() {
      @Override
      @NonNull
      public int[] invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (ids == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (int _item : ids) {
              _stmt.bindLong(_argIndex, _item);
              _argIndex++;
            }
          }
          final List<Integer> _listResult = new ArrayList<Integer>();
          while (_stmt.step()) {
            final Integer _item_1;
            if (_stmt.isNull(0)) {
              _item_1 = null;
            } else {
              _item_1 = (int) (_stmt.getLong(0));
            }
            _listResult.add(_item_1);
          }
          final int[] _tmpArrayResult = new int[_listResult.size()];
          int _index = 0;
          for (int _listItem : _listResult) {
            _tmpArrayResult[_index] = _listItem;
            _index++;
          }
          final int[] _result = _tmpArrayResult;
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<Integer> getAllAgesAsList(final List<Integer> ids) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
    final int _inputSize = ids == null ? 1 : ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<Integer>>() {
      @Override
      @NonNull
      public List<Integer> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (ids == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (Integer _item : ids) {
              if (_item == null) {
                _stmt.bindNull(_argIndex);
              } else {
                _stmt.bindLong(_argIndex, _item);
              }
              _argIndex++;
            }
          }
          final List<Integer> _result = new ArrayList<Integer>();
          while (_stmt.step()) {
            final Integer _item_1;
            if (_stmt.isNull(0)) {
              _item_1 = null;
            } else {
              _item_1 = (int) (_stmt.getLong(0));
            }
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<Integer> getAllAgesAsList(final List<Integer> ids1, final int[] ids2,
      final int... ids3) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
    final int _inputSize = ids1 == null ? 1 : ids1.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") OR uid IN (");
    final int _inputSize_1 = ids2 == null ? 1 : ids2.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize_1);
    _stringBuilder.append(") OR uid IN (");
    final int _inputSize_2 = ids3 == null ? 1 : ids3.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize_2);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<Integer>>() {
      @Override
      @NonNull
      public List<Integer> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (ids1 == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (Integer _item : ids1) {
              if (_item == null) {
                _stmt.bindNull(_argIndex);
              } else {
                _stmt.bindLong(_argIndex, _item);
              }
              _argIndex++;
            }
          }
          _argIndex = 1 + _inputSize;
          if (ids2 == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (int _item_1 : ids2) {
              _stmt.bindLong(_argIndex, _item_1);
              _argIndex++;
            }
          }
          _argIndex = 1 + _inputSize + _inputSize_1;
          if (ids3 == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (int _item_2 : ids3) {
              _stmt.bindLong(_argIndex, _item_2);
              _argIndex++;
            }
          }
          final List<Integer> _result = new ArrayList<Integer>();
          while (_stmt.step()) {
            final Integer _item_3;
            if (_stmt.isNull(0)) {
              _item_3 = null;
            } else {
              _item_3 = (int) (_stmt.getLong(0));
            }
            _result.add(_item_3);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public LiveData<User> getByIdLive(final int id) {
    final String _sql = "SELECT * FROM user where uid = ?";
    return __db.getInvalidationTracker().createLiveData(new String[] {"user"}, false, new Function1<SQLiteConnection, User>() {
      @Override
      @Nullable
      public User invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          _stmt.bindLong(_argIndex, id);
          final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uid");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastName");
          final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ageColumn");
          final User _result;
          if (_stmt.step()) {
            _result = new User();
            _result.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _result.name = null;
            } else {
              _result.name = _stmt.getText(_cursorIndexOfName);
            }
            final String _tmpLastName;
            if (_stmt.isNull(_cursorIndexOfLastName)) {
              _tmpLastName = null;
            } else {
              _tmpLastName = _stmt.getText(_cursorIndexOfLastName);
            }
            _result.setLastName(_tmpLastName);
            _result.age = (int) (_stmt.getLong(_cursorIndexOfAge));
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public LiveData<List<User>> loadUsersByIdsLive(final int... ids) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT * FROM user where uid IN (");
    final int _inputSize = ids == null ? 1 : ids.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return __db.getInvalidationTracker().createLiveData(new String[] {"user"}, false, new Function1<SQLiteConnection, List<User>>() {
      @Override
      @Nullable
      public List<User> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          int _argIndex = 1;
          if (ids == null) {
            _stmt.bindNull(_argIndex);
          } else {
            for (int _item : ids) {
              _stmt.bindLong(_argIndex, _item);
              _argIndex++;
            }
          }
          final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uid");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastName");
          final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ageColumn");
          final List<User> _result = new ArrayList<User>();
          while (_stmt.step()) {
            final User _item_1;
            _item_1 = new User();
            _item_1.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _item_1.name = null;
            } else {
              _item_1.name = _stmt.getText(_cursorIndexOfName);
            }
            final String _tmpLastName;
            if (_stmt.isNull(_cursorIndexOfLastName)) {
              _tmpLastName = null;
            } else {
              _tmpLastName = _stmt.getText(_cursorIndexOfLastName);
            }
            _item_1.setLastName(_tmpLastName);
            _item_1.age = (int) (_stmt.getLong(_cursorIndexOfAge));
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<Child1> getChild1List() {
    final String _sql = "SELECT * FROM Child1";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<Child1>>() {
      @Override
      @NonNull
      public List<Child1> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          final int _cursorIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfSerial = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "serial");
          final int _cursorIndexOfCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "code");
          final List<Child1> _result = new ArrayList<Child1>();
          while (_stmt.step()) {
            final Child1 _item;
            final int _tmpId;
            _tmpId = (int) (_stmt.getLong(_cursorIndexOfId));
            final String _tmpName;
            if (_stmt.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _stmt.getText(_cursorIndexOfName);
            }
            final Info _tmpInfo;
            if (!(_stmt.isNull(_cursorIndexOfSerial) && _stmt.isNull(_cursorIndexOfCode))) {
              _tmpInfo = new Info();
              _tmpInfo.serial = (int) (_stmt.getLong(_cursorIndexOfSerial));
              if (_stmt.isNull(_cursorIndexOfCode)) {
                _tmpInfo.code = null;
              } else {
                _tmpInfo.code = _stmt.getText(_cursorIndexOfCode);
              }
            } else {
              _tmpInfo = null;
            }
            _item = new Child1(_tmpId,_tmpName,_tmpInfo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<Child2> getChild2List() {
    final String _sql = "SELECT * FROM Child2";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<Child2>>() {
      @Override
      @NonNull
      public List<Child2> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          final int _cursorIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfSerial = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "serial");
          final int _cursorIndexOfCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "code");
          final List<Child2> _result = new ArrayList<Child2>();
          while (_stmt.step()) {
            final Child2 _item;
            final int _tmpId;
            _tmpId = (int) (_stmt.getLong(_cursorIndexOfId));
            final String _tmpName;
            if (_stmt.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _stmt.getText(_cursorIndexOfName);
            }
            final Info _tmpInfo;
            if (!(_stmt.isNull(_cursorIndexOfSerial) && _stmt.isNull(_cursorIndexOfCode))) {
              _tmpInfo = new Info();
              _tmpInfo.serial = (int) (_stmt.getLong(_cursorIndexOfSerial));
              if (_stmt.isNull(_cursorIndexOfCode)) {
                _tmpInfo.code = null;
              } else {
                _tmpInfo.code = _stmt.getText(_cursorIndexOfCode);
              }
            } else {
              _tmpInfo = null;
            }
            _item = new Child2(_tmpId,_tmpName,_tmpInfo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public ListenableFuture<List<Child1>> getChild1ListListenableFuture() {
    final String _sql = "SELECT * FROM Child1";
    return GuavaRoom.createListenableFuture(__db, true, false, new Function1<SQLiteConnection, List<Child1>>() {
      @Override
      public List<Child1> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          final int _cursorIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
          final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
          final int _cursorIndexOfSerial = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "serial");
          final int _cursorIndexOfCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "code");
          final List<Child1> _result = new ArrayList<Child1>();
          while (_stmt.step()) {
            final Child1 _item;
            final int _tmpId;
            _tmpId = (int) (_stmt.getLong(_cursorIndexOfId));
            final String _tmpName;
            if (_stmt.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _stmt.getText(_cursorIndexOfName);
            }
            final Info _tmpInfo;
            if (!(_stmt.isNull(_cursorIndexOfSerial) && _stmt.isNull(_cursorIndexOfCode))) {
              _tmpInfo = new Info();
              _tmpInfo.serial = (int) (_stmt.getLong(_cursorIndexOfSerial));
              if (_stmt.isNull(_cursorIndexOfCode)) {
                _tmpInfo.code = null;
              } else {
                _tmpInfo.code = _stmt.getText(_cursorIndexOfCode);
              }
            } else {
              _tmpInfo = null;
            }
            _item = new Child1(_tmpId,_tmpName,_tmpInfo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public List<UserSummary> getUserNames() {
    final String _sql = "SELECT `uid`, `name` FROM (SELECT * FROM User)";
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, List<UserSummary>>() {
      @Override
      @NonNull
      public List<UserSummary> invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          final int _cursorIndexOfUid = 0;
          final int _cursorIndexOfName = 1;
          final List<UserSummary> _result = new ArrayList<UserSummary>();
          while (_stmt.step()) {
            final UserSummary _item;
            _item = new UserSummary();
            _item.uid = (int) (_stmt.getLong(_cursorIndexOfUid));
            if (_stmt.isNull(_cursorIndexOfName)) {
              _item.name = null;
            } else {
              _item.name = _stmt.getText(_cursorIndexOfName);
            }
            _result.add(_item);
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @Override
  public PagingSource<Integer, Child1> loadItems() {
    final String _sql = "SELECT * FROM Child1 ORDER BY id ASC";
    final RoomRawQuery _rawQuery = new RoomRawQuery(_sql);
    return new LimitOffsetPagingSource<Child1>(_rawQuery, __db, "Child1") {
      @Override
      protected Object convertRows(final RoomRawQuery limitOffsetQuery, final int itemCount,
          final Continuation<? super List<? extends Child1>> arg2) {
        return DBUtil.performSuspending(__db, true, false, new Function1<SQLiteConnection, List<Child1>>() {
          @Override
          @NonNull
          public List<Child1> invoke(@NonNull final SQLiteConnection _connection) {
            final SQLiteStatement _stmt = _connection.prepare(limitOffsetQuery.getSql());
            limitOffsetQuery.getBindingFunction().invoke(_stmt);
            try {
              final int _cursorIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
              final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
              final int _cursorIndexOfSerial = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "serial");
              final int _cursorIndexOfCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "code");
              final List<Child1> _result = new ArrayList<Child1>();
              while (_stmt.step()) {
                final Child1 _item;
                final int _tmpId;
                _tmpId = (int) (_stmt.getLong(_cursorIndexOfId));
                final String _tmpName;
                if (_stmt.isNull(_cursorIndexOfName)) {
                  _tmpName = null;
                } else {
                  _tmpName = _stmt.getText(_cursorIndexOfName);
                }
                final Info _tmpInfo;
                if (!(_stmt.isNull(_cursorIndexOfSerial) && _stmt.isNull(_cursorIndexOfCode))) {
                  _tmpInfo = new Info();
                  _tmpInfo.serial = (int) (_stmt.getLong(_cursorIndexOfSerial));
                  if (_stmt.isNull(_cursorIndexOfCode)) {
                    _tmpInfo.code = null;
                  } else {
                    _tmpInfo.code = _stmt.getText(_cursorIndexOfCode);
                  }
                } else {
                  _tmpInfo = null;
                }
                _item = new Child1(_tmpId,_tmpName,_tmpInfo);
                _result.add(_item);
              }
              return _result;
            } finally {
              _stmt.close();
            }
          }
        }, arg2);
      }
    };
  }

  @Override
  public User getUserViaRawQuery(final SupportSQLiteQuery rawQuery) {
    final RoomRawQuery _rawQuery = RoomSQLiteQuery.copyFrom(rawQuery).toRoomRawQuery();
    final String _sql = _rawQuery.getSql();
    return DBUtil.performBlocking(__db, true, false, new Function1<SQLiteConnection, User>() {
      @Override
      @NonNull
      public User invoke(@NonNull final SQLiteConnection _connection) {
        final SQLiteStatement _stmt = _connection.prepare(_sql);
        try {
          _rawQuery.getBindingFunction().invoke(_stmt);
          final User _result;
          if (_stmt.step()) {
            _result = __entityStatementConverter_fooBarUser(_stmt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _stmt.close();
        }
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private User __entityStatementConverter_fooBarUser(@NonNull final SQLiteStatement statement) {
    final User _entity;
    final int _cursorIndexOfUid = SQLiteStatementUtil.getColumnIndex(statement, "uid");
    final int _cursorIndexOfName = SQLiteStatementUtil.getColumnIndex(statement, "name");
    final int _cursorIndexOfLastName = SQLiteStatementUtil.getColumnIndex(statement, "lastName");
    final int _cursorIndexOfAge = SQLiteStatementUtil.getColumnIndex(statement, "ageColumn");
    _entity = new User();
    if (_cursorIndexOfUid != -1) {
      _entity.uid = (int) (statement.getLong(_cursorIndexOfUid));
    }
    if (_cursorIndexOfName != -1) {
      if (statement.isNull(_cursorIndexOfName)) {
        _entity.name = null;
      } else {
        _entity.name = statement.getText(_cursorIndexOfName);
      }
    }
    if (_cursorIndexOfLastName != -1) {
      final String _tmpLastName;
      if (statement.isNull(_cursorIndexOfLastName)) {
        _tmpLastName = null;
      } else {
        _tmpLastName = statement.getText(_cursorIndexOfLastName);
      }
      _entity.setLastName(_tmpLastName);
    }
    if (_cursorIndexOfAge != -1) {
      _entity.age = (int) (statement.getLong(_cursorIndexOfAge));
    }
    return _entity;
  }
}
