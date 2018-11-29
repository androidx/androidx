package foo.bar;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings("unchecked")
public final class ComplexDao_Impl extends ComplexDao {
    private final RoomDatabase __db;

    public ComplexDao_Impl(ComplexDatabase __db) {
        super(__db);
        this.__db = __db;
    }

    @Override
    public boolean transactionMethod(final int i, final String s, final long l) {
        __db.beginTransaction();
        try {
            boolean _result = super.transactionMethod(i, s, l);
            __db.setTransactionSuccessful();
            return _result;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public List<ComplexDao.FullName> fullNames(final int id) {
        final String _sql = "SELECT name || lastName as fullName, uid as id FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final List<ComplexDao.FullName> _result = new ArrayList<ComplexDao.FullName>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final ComplexDao.FullName _item;
                _item = new ComplexDao.FullName();
                _item.fullName = _cursor.getString(_cursorIndexOfFullName);
                _item.id = _cursor.getInt(_cursorIndexOfId);
                _result.add(_item);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public User getById(final int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = new User();
                _result.uid = _cursor.getInt(_cursorIndexOfUid);
                _result.name = _cursor.getString(_cursorIndexOfName);
                final String _tmpLastName;
                _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                _result.setLastName(_tmpLastName);
                _result.age = _cursor.getInt(_cursorIndexOfAge);
            } else {
                _result = null;
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public User findByName(final String name, final String lastName) {
        final String _sql = "SELECT * FROM user where name LIKE ? AND lastName LIKE ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
        int _argIndex = 1;
        if (name == null) {
            _statement.bindNull(_argIndex);
        } else {
            _statement.bindString(_argIndex, name);
        }
        _argIndex = 2;
        if (lastName == null) {
            _statement.bindNull(_argIndex);
        } else {
            _statement.bindString(_argIndex, lastName);
        }
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = new User();
                _result.uid = _cursor.getInt(_cursorIndexOfUid);
                _result.name = _cursor.getString(_cursorIndexOfName);
                final String _tmpLastName;
                _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                _result.setLastName(_tmpLastName);
                _result.age = _cursor.getInt(_cursorIndexOfAge);
            } else {
                _result = null;
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public List<User> loadAllByIds(final int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT * FROM user where uid IN (");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
        int _argIndex = 1;
        for (int _item : ids) {
            _statement.bindLong(_argIndex, _item);
            _argIndex ++;
        }
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final List<User> _result = new ArrayList<User>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final User _item_1;
                _item_1 = new User();
                _item_1.uid = _cursor.getInt(_cursorIndexOfUid);
                _item_1.name = _cursor.getString(_cursorIndexOfName);
                final String _tmpLastName;
                _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                _item_1.setLastName(_tmpLastName);
                _item_1.age = _cursor.getInt(_cursorIndexOfAge);
                _result.add(_item_1);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    int getAge(final int id) {
        final String _sql = "SELECT ageColumn FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int _result;
            if(_cursor.moveToFirst()) {
                _result = _cursor.getInt(0);
            } else {
                _result = 0;
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public int[] getAllAges(final int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
        int _argIndex = 1;
        for (int _item : ids) {
            _statement.bindLong(_argIndex, _item);
            _argIndex ++;
        }
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final int[] _result = new int[_cursor.getCount()];
            int _index = 0;
            while(_cursor.moveToNext()) {
                final int _item_1;
                _item_1 = _cursor.getInt(0);
                _result[_index] = _item_1;
                _index ++;
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public List<Integer> getAllAgesAsList(final List<Integer> ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
        int _argIndex = 1;
        for (Integer _item : ids) {
            if (_item == null) {
                _statement.bindNull(_argIndex);
            } else {
                _statement.bindLong(_argIndex, _item);
            }
            _argIndex ++;
        }
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Integer _item_1;
                if (_cursor.isNull(0)) {
                    _item_1 = null;
                } else {
                    _item_1 = _cursor.getInt(0);
                }
                _result.add(_item_1);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public LiveData<User> getByIdLive(final int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, new Callable<User>() {
            @Override
            public User call() throws Exception {
                final Cursor _cursor = DBUtil.query(__db, _statement, false);
                try {
                    final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
                    final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
                    final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
                    final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
                    final User _result;
                    if(_cursor.moveToFirst()) {
                        _result = new User();
                        _result.uid = _cursor.getInt(_cursorIndexOfUid);
                        _result.name = _cursor.getString(_cursorIndexOfName);
                        final String _tmpLastName;
                        _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                        _result.setLastName(_tmpLastName);
                        _result.age = _cursor.getInt(_cursorIndexOfAge);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _cursor.close();
                }
            }

            @Override
            protected void finalize() {
                _statement.release();
            }
        });
    }

    @Override
    public LiveData<List<User>> loadUsersByIdsLive(final int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT * FROM user where uid IN (");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
        int _argIndex = 1;
        for (int _item : ids) {
            _statement.bindLong(_argIndex, _item);
            _argIndex ++;
        }
        return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, new Callable<List<User>>() {
            @Override
            public List<User> call() throws Exception {
                final Cursor _cursor = DBUtil.query(__db, _statement, false);
                try {
                    final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
                    final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
                    final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
                    final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
                    final List<User> _result = new ArrayList<User>(_cursor.getCount());
                    while(_cursor.moveToNext()) {
                        final User _item_1;
                        _item_1 = new User();
                        _item_1.uid = _cursor.getInt(_cursorIndexOfUid);
                        _item_1.name = _cursor.getString(_cursorIndexOfName);
                        final String _tmpLastName;
                        _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                        _item_1.setLastName(_tmpLastName);
                        _item_1.age = _cursor.getInt(_cursorIndexOfAge);
                        _result.add(_item_1);
                    }
                    return _result;
                } finally {
                    _cursor.close();
                }
            }

            @Override
            protected void finalize() {
                _statement.release();
            }
        });
    }

    @Override
    public List<Integer> getAllAgesAsList(final List<Integer> ids1, final int[] ids2,
            final int... ids3) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT ageColumn FROM user where uid IN(");
        final int _inputSize = ids1.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(") OR uid IN (");
        final int _inputSize_1 = ids2.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize_1);
        _stringBuilder.append(") OR uid IN (");
        final int _inputSize_2 = ids3.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize_2);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize + _inputSize_1 + _inputSize_2;
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
        int _argIndex = 1;
        for (Integer _item : ids1) {
            if (_item == null) {
                _statement.bindNull(_argIndex);
            } else {
                _statement.bindLong(_argIndex, _item);
            }
            _argIndex ++;
        }
        _argIndex = 1 + _inputSize;
        for (int _item_1 : ids2) {
            _statement.bindLong(_argIndex, _item_1);
            _argIndex ++;
        }
        _argIndex = 1 + _inputSize + _inputSize_1;
        for (int _item_2 : ids3) {
            _statement.bindLong(_argIndex, _item_2);
            _argIndex ++;
        }
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
            final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Integer _item_3;
                if (_cursor.isNull(0)) {
                    _item_3 = null;
                } else {
                    _item_3 = _cursor.getInt(0);
                }
                _result.add(_item_3);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }
}