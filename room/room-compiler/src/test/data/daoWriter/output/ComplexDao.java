package foo.bar;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.lifecycle.LiveData;
import androidx.room.ParsedQuerySection;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.guava.GuavaRoom;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.QueryUtil;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Pair;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
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
            boolean _result = ComplexDao_Impl.super.transactionMethod(i, s, l);
            __db.setTransactionSuccessful();
            return _result;
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public List<ComplexDao.FullName> fullNames(final int id) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT name || lastName as fullName, uid as id FROM user where uid = "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":id", false, id));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfFullName = 0;
            final int _cursorIndexOfId = 1;
            final List<ComplexDao.FullName> _result = new ArrayList<ComplexDao.FullName>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final ComplexDao.FullName _item;
                _item = new ComplexDao.FullName();
                if (_cursor.isNull(_cursorIndexOfFullName)) {
                    _item.fullName = null;
                } else {
                    _item.fullName = _cursor.getString(_cursorIndexOfFullName);
                }
                _item.id = _cursor.getInt(_cursorIndexOfId);
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public User getById(final int id) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM user where uid = "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":id", false, id));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = new User();
                _result.uid = _cursor.getInt(_cursorIndexOfUid);
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _result.name = null;
                } else {
                    _result.name = _cursor.getString(_cursorIndexOfName);
                }
                final String _tmpLastName;
                if (_cursor.isNull(_cursorIndexOfLastName)) {
                    _tmpLastName = null;
                } else {
                    _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                }
                _result.setLastName(_tmpLastName);
                _result.age = _cursor.getInt(_cursorIndexOfAge);
            } else {
                _result = null;
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public User findByName(final String name, final String lastName) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM user where name LIKE "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":name", false, name));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(" AND lastName LIKE "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":lastName", false, lastName));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = new User();
                _result.uid = _cursor.getInt(_cursorIndexOfUid);
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _result.name = null;
                } else {
                    _result.name = _cursor.getString(_cursorIndexOfName);
                }
                final String _tmpLastName;
                if (_cursor.isNull(_cursorIndexOfLastName)) {
                    _tmpLastName = null;
                } else {
                    _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                }
                _result.setLastName(_tmpLastName);
                _result.age = _cursor.getInt(_cursorIndexOfAge);
            } else {
                _result = null;
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public List<User> loadAllByIds(final int... ids) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM user where uid IN ("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids", true, ids));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(")"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
            final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
            final List<User> _result = new ArrayList<User>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final User _item;
                _item = new User();
                _item.uid = _cursor.getInt(_cursorIndexOfUid);
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _item.name = null;
                } else {
                    _item.name = _cursor.getString(_cursorIndexOfName);
                }
                final String _tmpLastName;
                if (_cursor.isNull(_cursorIndexOfLastName)) {
                    _tmpLastName = null;
                } else {
                    _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                }
                _item.setLastName(_tmpLastName);
                _item.age = _cursor.getInt(_cursorIndexOfAge);
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    int getAge(final int id) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT ageColumn FROM user where uid = "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":id", false, id));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _result;
            if(_cursor.moveToFirst()) {
                _result = _cursor.getInt(0);
            } else {
                _result = 0;
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public int[] getAllAges(final int... ids) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT ageColumn FROM user where uid IN("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids", true, ids));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(")"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int[] _result = new int[_cursor.getCount()];
            int _index = 0;
            while(_cursor.moveToNext()) {
                final int _item;
                _item = _cursor.getInt(0);
                _result[_index] = _item;
                _index ++;
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public List<Integer> getAllAgesAsList(final List<Integer> ids) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT ageColumn FROM user where uid IN("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids", true, ids));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(")"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Integer _item;
                if (_cursor.isNull(0)) {
                    _item = null;
                } else {
                    _item = _cursor.getInt(0);
                }
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public List<Integer> getAllAgesAsList(final List<Integer> ids1, final int[] ids2,
            final int... ids3) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT ageColumn FROM user where uid IN("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids1", true, ids1));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(") OR uid IN ("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids2", true, ids2));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(") OR uid IN ("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids3", true, ids3));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(")"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Integer _item;
                if (_cursor.isNull(0)) {
                    _item = null;
                } else {
                    _item = _cursor.getInt(0);
                }
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public LiveData<User> getByIdLive(final int id) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM user where uid = "));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":id", false, id));
        return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, false, new Callable<User>() {
            private RoomSQLiteQuery _statement;

            @Override
            public User call() throws Exception {
                Cursor _cursor = null;
                try {
                    boolean _isLargeQuery = false;
                    final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
                    _statement = _resultPair.getFirst();
                    _isLargeQuery = _resultPair.getSecond();
                    _cursor = DBUtil.query(__db, _statement, false, null);
                    final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
                    final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
                    final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
                    final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
                    final User _result;
                    if(_cursor.moveToFirst()) {
                        _result = new User();
                        _result.uid = _cursor.getInt(_cursorIndexOfUid);
                        if (_cursor.isNull(_cursorIndexOfName)) {
                            _result.name = null;
                        } else {
                            _result.name = _cursor.getString(_cursorIndexOfName);
                        }
                        final String _tmpLastName;
                        if (_cursor.isNull(_cursorIndexOfLastName)) {
                            _tmpLastName = null;
                        } else {
                            _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                        }
                        _result.setLastName(_tmpLastName);
                        _result.age = _cursor.getInt(_cursorIndexOfAge);
                    } else {
                        _result = null;
                    }
                    if (_isLargeQuery) {
                        __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                        __db.setTransactionSuccessful();
                        __db.endTransaction();
                    }
                    return _result;
                } finally {
                    if (_cursor != null) {
                        _cursor.close();
                    }
                }
            }

            @Override
            protected void finalize() {
                if (_statement != null) {
                    _statement.release();
                }
            }
        });
    }

    @Override
    public LiveData<List<User>> loadUsersByIdsLive(final int... ids) {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM user where uid IN ("));
        _parsedQuerySections.add(ParsedQuerySection.Companion.bindVar(":ids", true, ids));
        _parsedQuerySections.add(ParsedQuerySection.Companion.text(")"));
        return __db.getInvalidationTracker().createLiveData(new String[]{"user"}, false, new Callable<List<User>>() {
            private RoomSQLiteQuery _statement;

            @Override
            public List<User> call() throws Exception {
                Cursor _cursor = null;
                try {
                    boolean _isLargeQuery = false;
                    final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
                    _statement = _resultPair.getFirst();
                    _isLargeQuery = _resultPair.getSecond();
                    _cursor = DBUtil.query(__db, _statement, false, null);
                    final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
                    final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
                    final int _cursorIndexOfLastName = CursorUtil.getColumnIndexOrThrow(_cursor, "lastName");
                    final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "ageColumn");
                    final List<User> _result = new ArrayList<User>(_cursor.getCount());
                    while(_cursor.moveToNext()) {
                        final User _item;
                        _item = new User();
                        _item.uid = _cursor.getInt(_cursorIndexOfUid);
                        if (_cursor.isNull(_cursorIndexOfName)) {
                            _item.name = null;
                        } else {
                            _item.name = _cursor.getString(_cursorIndexOfName);
                        }
                        final String _tmpLastName;
                        if (_cursor.isNull(_cursorIndexOfLastName)) {
                            _tmpLastName = null;
                        } else {
                            _tmpLastName = _cursor.getString(_cursorIndexOfLastName);
                        }
                        _item.setLastName(_tmpLastName);
                        _item.age = _cursor.getInt(_cursorIndexOfAge);
                        _result.add(_item);
                    }
                    if (_isLargeQuery) {
                        __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                        __db.setTransactionSuccessful();
                        __db.endTransaction();
                    }
                    return _result;
                } finally {
                    if (_cursor != null) {
                        _cursor.close();
                    }
                }
            }

            @Override
            protected void finalize() {
                if (_statement != null) {
                    _statement.release();
                }
            }
        });
    }

    @Override
    public List<Child1> getChild1List() {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM Child1"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "serial");
            final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
            final List<Child1> _result = new ArrayList<Child1>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Child1 _item;
                final int _tmpId;
                _tmpId = _cursor.getInt(_cursorIndexOfId);
                final String _tmpName;
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _tmpName = null;
                } else {
                    _tmpName = _cursor.getString(_cursorIndexOfName);
                }
                final Info _tmpInfo;
                if (! (_cursor.isNull(_cursorIndexOfSerial) && _cursor.isNull(_cursorIndexOfCode))) {
                    _tmpInfo = new Info();
                    _tmpInfo.serial = _cursor.getInt(_cursorIndexOfSerial);
                    if (_cursor.isNull(_cursorIndexOfCode)) {
                        _tmpInfo.code = null;
                    } else {
                        _tmpInfo.code = _cursor.getString(_cursorIndexOfCode);
                    }
                }  else  {
                    _tmpInfo = null;
                }
                _item = new Child1(_tmpId,_tmpName,_tmpInfo);
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public List<Child2> getChild2List() {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM Child2"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "serial");
            final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
            final List<Child2> _result = new ArrayList<Child2>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Child2 _item;
                final int _tmpId;
                _tmpId = _cursor.getInt(_cursorIndexOfId);
                final String _tmpName;
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _tmpName = null;
                } else {
                    _tmpName = _cursor.getString(_cursorIndexOfName);
                }
                final Info _tmpInfo;
                if (! (_cursor.isNull(_cursorIndexOfSerial) && _cursor.isNull(_cursorIndexOfCode))) {
                    _tmpInfo = new Info();
                    _tmpInfo.serial = _cursor.getInt(_cursorIndexOfSerial);
                    if (_cursor.isNull(_cursorIndexOfCode)) {
                        _tmpInfo.code = null;
                    } else {
                        _tmpInfo.code = _cursor.getString(_cursorIndexOfCode);
                    }
                }  else  {
                    _tmpInfo = null;
                }
                _item = new Child2(_tmpId,_tmpName,_tmpInfo);
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public ListenableFuture<List<Child1>> getChild1ListListenableFuture() {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT * FROM Child1"));
        final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
        final SupportSQLiteQuery[] _query = new SupportSQLiteQuery[]{null};
        return GuavaRoom.createListenableFuture(__db, false, new Callable<List<Child1>>() {
            @Override
            public List<Child1> call() throws Exception {
                Cursor _cursor = null;
                try {
                    boolean _isLargeQuery = false;
                    final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
                    _query[0] = _resultPair.getFirst();
                    _isLargeQuery = _resultPair.getSecond();
                    _cursor = DBUtil.query(__db, _query[0], false, _cancellationSignal);
                    final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
                    final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
                    final int _cursorIndexOfSerial = CursorUtil.getColumnIndexOrThrow(_cursor, "serial");
                    final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
                    final List<Child1> _result = new ArrayList<Child1>(_cursor.getCount());
                    while(_cursor.moveToNext()) {
                        final Child1 _item;
                        final int _tmpId;
                        _tmpId = _cursor.getInt(_cursorIndexOfId);
                        final String _tmpName;
                        if (_cursor.isNull(_cursorIndexOfName)) {
                            _tmpName = null;
                        } else {
                            _tmpName = _cursor.getString(_cursorIndexOfName);
                        }
                        final Info _tmpInfo;
                        if (! (_cursor.isNull(_cursorIndexOfSerial) && _cursor.isNull(_cursorIndexOfCode))) {
                            _tmpInfo = new Info();
                            _tmpInfo.serial = _cursor.getInt(_cursorIndexOfSerial);
                            if (_cursor.isNull(_cursorIndexOfCode)) {
                                _tmpInfo.code = null;
                            } else {
                                _tmpInfo.code = _cursor.getString(_cursorIndexOfCode);
                            }
                        }  else  {
                            _tmpInfo = null;
                        }
                        _item = new Child1(_tmpId,_tmpName,_tmpInfo);
                        _result.add(_item);
                    }
                    if (_isLargeQuery) {
                        __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                        __db.setTransactionSuccessful();
                        __db.endTransaction();
                    }
                    return _result;
                } finally {
                    if (_cursor != null) {
                        _cursor.close();
                    }
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                if ((_query[0] != null) && (_query[0] instanceof RoomSQLiteQuery)) {
                    ((RoomSQLiteQuery)_query[0]).release();
                }
            }
        }, true, _cancellationSignal);
    }

    @Override
    public List<UserSummary> getUserNames() {
        final List<ParsedQuerySection> _parsedQuerySections = new ArrayList<ParsedQuerySection>();
        _parsedQuerySections.add(ParsedQuerySection.Companion.text("SELECT `uid`, `name` FROM (SELECT * FROM User)"));
        __db.assertNotSuspendingTransaction();
        RoomSQLiteQuery _statement = null;
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            final Pair<RoomSQLiteQuery, Boolean> _resultPair = QueryUtil.prepareQuery(__db, false, "_tempTable", _parsedQuerySections, false);
            _statement = _resultPair.getFirst();
            _isLargeQuery = _resultPair.getSecond();
            _cursor = DBUtil.query(__db, _statement, false, null);
            final int _cursorIndexOfUid = 0;
            final int _cursorIndexOfName = 1;
            final List<UserSummary> _result = new ArrayList<UserSummary>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final UserSummary _item;
                _item = new UserSummary();
                _item.uid = _cursor.getInt(_cursorIndexOfUid);
                if (_cursor.isNull(_cursorIndexOfName)) {
                    _item.name = null;
                } else {
                    _item.name = _cursor.getString(_cursorIndexOfName);
                }
                _result.add(_item);
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
            if (_statement != null) {
                _statement.release();
            }
        }
    }

    @Override
    public User getUserViaRawQuery(final SupportSQLiteQuery rawQuery) {
        final SupportSQLiteQuery _internalQuery = rawQuery;
        __db.assertNotSuspendingTransaction();
        Cursor _cursor = null;
        try {
            boolean _isLargeQuery = false;
            _cursor = DBUtil.query(__db, _internalQuery, false, null);
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = __entityCursorConverter_fooBarUser(_cursor);
            } else {
                _result = null;
            }
            if (_isLargeQuery) {
                __db.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS _tempTable");
                __db.setTransactionSuccessful();
                __db.endTransaction();
            }
            return _result;
        } finally {
            if (_cursor != null) {
                _cursor.close();
            }
        }
    }

    public static List<Class<?>> getRequiredConverters() {
        return Collections.emptyList();
    }

    private User __entityCursorConverter_fooBarUser(Cursor cursor) {
        final User _entity;
        final int _cursorIndexOfUid = CursorUtil.getColumnIndex(cursor, "uid");
        final int _cursorIndexOfName = CursorUtil.getColumnIndex(cursor, "name");
        final int _cursorIndexOfLastName = CursorUtil.getColumnIndex(cursor, "lastName");
        final int _cursorIndexOfAge = CursorUtil.getColumnIndex(cursor, "ageColumn");
        _entity = new User();
        if (_cursorIndexOfUid != -1) {
            _entity.uid = cursor.getInt(_cursorIndexOfUid);
        }
        if (_cursorIndexOfName != -1) {
            if (cursor.isNull(_cursorIndexOfName)) {
                _entity.name = null;
            } else {
                _entity.name = cursor.getString(_cursorIndexOfName);
            }
        }
        if (_cursorIndexOfLastName != -1) {
            final String _tmpLastName;
            if (cursor.isNull(_cursorIndexOfLastName)) {
                _tmpLastName = null;
            } else {
                _tmpLastName = cursor.getString(_cursorIndexOfLastName);
            }
            _entity.setLastName(_tmpLastName);
        }
        if (_cursorIndexOfAge != -1) {
            _entity.age = cursor.getInt(_cursorIndexOfAge);
        }
        return _entity;
    }
}