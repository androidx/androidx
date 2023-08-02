import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import java.lang.Class
import java.lang.StringBuilder
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
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

    public override fun getLiveData(vararg arg: String?): LiveData<MyEntity> {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
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
        return __db.invalidationTracker.createLiveData(arrayOf("MyEntity"), false, object :
            Callable<MyEntity?> {
            public override fun call(): MyEntity? {
                val _cursor: Cursor = query(__db, _statement, false, null)
                try {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
                    val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
                    val _result: MyEntity?
                    if (_cursor.moveToFirst()) {
                        val _tmpPk: Int
                        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                        val _tmpOther: String
                        _tmpOther = _cursor.getString(_cursorIndexOfOther)
                        _result = MyEntity(_tmpPk,_tmpOther)
                    } else {
                        _result = null
                    }
                    return _result
                } finally {
                    _cursor.close()
                }
            }

            protected fun finalize() {
                _statement.release()
            }
        })
    }

    public override fun getLiveDataNullable(vararg arg: String?): LiveData<MyEntity?> {
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
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
        return __db.invalidationTracker.createLiveData(arrayOf("MyEntity"), false, object :
            Callable<MyEntity?> {
            public override fun call(): MyEntity? {
                val _cursor: Cursor = query(__db, _statement, false, null)
                try {
                    val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
                    val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
                    val _result: MyEntity?
                    if (_cursor.moveToFirst()) {
                        val _tmpPk: Int
                        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                        val _tmpOther: String
                        _tmpOther = _cursor.getString(_cursorIndexOfOther)
                        _result = MyEntity(_tmpPk,_tmpOther)
                    } else {
                        _result = null
                    }
                    return _result
                } finally {
                    _cursor.close()
                }
            }

            protected fun finalize() {
                _statement.release()
            }
        })
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}