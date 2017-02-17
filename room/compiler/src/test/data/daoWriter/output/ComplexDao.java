package foo.bar;

import android.database.Cursor;
import com.android.support.lifecycle.ComputableLiveData;
import com.android.support.lifecycle.LiveData;
import com.android.support.room.InvalidationTracker.Observer;
import com.android.support.room.RoomDatabase;
import com.android.support.room.RoomSQLiteQuery;
import com.android.support.room.util.StringUtil;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.List;

public class ComplexDao_Impl extends ComplexDao {
    private final RoomDatabase __db;

    public ComplexDao_Impl(RoomDatabase __db) {
        this.__db = __db;
    }

    @Override
    public List<ComplexDao.FullName> fullNames(int id) {
        final String _sql = "SELECT name || lastName as fullName, uid as id FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = __db.query(_statement);
        try {
            final List<ComplexDao.FullName> _result = new ArrayList<ComplexDao.FullName>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final ComplexDao.FullName _item;
                _item = new ComplexDao.FullName();
                _item.fullName = _cursor.getString(0);
                _item.id = _cursor.getInt(1);
                _result.add(_item);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    public User getById(int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = __db.query(_statement);
        try {
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = __entityCursorConverter_fooBarUser(_cursor);
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
    public User findByName(String name, String lastName) {
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
        final Cursor _cursor = __db.query(_statement);
        try {
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = __entityCursorConverter_fooBarUser(_cursor);
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
    public List<User> loadAllByIds(int... ids) {
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
        final Cursor _cursor = __db.query(_statement);
        try {
            final List<User> _result = new ArrayList<User>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final User _item_1;
                _item_1 = __entityCursorConverter_fooBarUser(_cursor);
                _result.add(_item_1);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @Override
    int getAge(int id) {
        final String _sql = "SELECT ageColumn FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        final Cursor _cursor = __db.query(_statement);
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
    public int[] getAllAges(int... ids) {
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
        final Cursor _cursor = __db.query(_statement);
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
    public List<Integer> getAllAgesAsList(List<Integer> ids) {
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
        final Cursor _cursor = __db.query(_statement);
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
    public LiveData<User> getByIdLive(int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
        int _argIndex = 1;
        _statement.bindLong(_argIndex, id);
        return new ComputableLiveData<User>() {
            private Observer _observer;

            @Override
            protected User compute() {
                if (_observer == null) {
                    _observer = new Observer("user") {
                        @Override
                        public void onInvalidated() {
                            invalidate();
                        }
                    };
                    __db.getInvalidationTracker().addWeakObserver(_observer);
                }
                final Cursor _cursor = __db.query(_statement);
                try {
                    final User _result;
                    if(_cursor.moveToFirst()) {
                        _result = __entityCursorConverter_fooBarUser(_cursor);
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
        }.getLiveData();
    }

    @Override
    public LiveData<List<User>> loadUsersByIdsLive(int... ids) {
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
        return new ComputableLiveData<List<User>>() {
            private Observer _observer;

            @Override
            protected List<User> compute() {
                if (_observer == null) {
                    _observer = new Observer("user") {
                        @Override
                        public void onInvalidated() {
                            invalidate();
                        }
                    };
                    __db.getInvalidationTracker().addWeakObserver(_observer);
                }
                final Cursor _cursor = __db.query(_statement);
                try {
                    final List<User> _result = new ArrayList<User>(_cursor.getCount());
                    while(_cursor.moveToNext()) {
                        final User _item_1;
                        _item_1 = __entityCursorConverter_fooBarUser(_cursor);
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
        }.getLiveData();
    }

    private User __entityCursorConverter_fooBarUser(Cursor cursor) {
        User _entity = new User();
        int _columnIndex = 0;
        for (String _columnName : cursor.getColumnNames()) {
            switch(_columnName.hashCode()) {
                case 115792: {
                    if ("uid".equals(_columnName)) {
                        _entity.uid = cursor.getInt(_columnIndex);
                    }
                }
                case 3373707: {
                    if ("name".equals(_columnName)) {
                        _entity.name = cursor.getString(_columnIndex);
                    }
                }
                case -1459599807: {
                    if ("lastName".equals(_columnName)) {
                        final String _tmpLastName;
                        _tmpLastName = cursor.getString(_columnIndex);
                        _entity.setLastName(_tmpLastName);
                    }
                }
                case 1358970165: {
                    if ("ageColumn".equals(_columnName)) {
                        _entity.age = cursor.getInt(_columnIndex);
                    }
                }
            }
            _columnIndex ++;
        }
        return _entity;
    }
}
