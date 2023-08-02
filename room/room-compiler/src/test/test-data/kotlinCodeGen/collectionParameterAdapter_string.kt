import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import java.lang.Class
import java.lang.StringBuilder
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun listOfString(arg: List<String>): MyEntity {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
        val _inputSize: Int = arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: String in arg) {
            _statement.bindString(_argIndex, _item)
            _argIndex++
        }
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                _result = MyEntity(_tmpString)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun nullableListOfString(arg: List<String>?): MyEntity {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
        val _inputSize: Int = if (arg == null) 1 else arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        if (arg == null) {
            _statement.bindNull(_argIndex)
        } else {
            for (_item: String in arg) {
                _statement.bindString(_argIndex, _item)
                _argIndex++
            }
        }
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                _result = MyEntity(_tmpString)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun listOfNullableString(arg: List<String?>): MyEntity {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
        val _inputSize: Int = arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: String? in arg) {
            if (_item == null) {
                _statement.bindNull(_argIndex)
            } else {
                _statement.bindString(_argIndex, _item)
            }
            _argIndex++
        }
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                _result = MyEntity(_tmpString)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun setOfString(arg: Set<String>): MyEntity {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE string IN (")
        val _inputSize: Int = arg.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: String in arg) {
            _statement.bindString(_argIndex, _item)
            _argIndex++
        }
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                _result = MyEntity(_tmpString)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}