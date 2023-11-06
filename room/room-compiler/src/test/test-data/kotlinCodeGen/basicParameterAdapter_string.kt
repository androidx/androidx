import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
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

    public override fun stringParam(arg: String): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
        val _statement: RoomSQLiteQuery = acquire(_sql, 1)
        var _argIndex: Int = 1
        _statement.bindString(_argIndex, arg)
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

    public override fun nullableStringParam(arg: String?): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
        val _statement: RoomSQLiteQuery = acquire(_sql, 1)
        var _argIndex: Int = 1
        if (arg == null) {
            _statement.bindNull(_argIndex)
        } else {
            _statement.bindString(_argIndex, arg)
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