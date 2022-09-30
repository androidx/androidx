import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class MyDao_Impl : MyDao {
    private val __db: RoomDatabase

    public constructor(__db: RoomDatabase) {
        this.__db = __db
    }

    public override fun getEntity(): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfInt: Int = getColumnIndexOrThrow(_cursor, "int")
            val _cursorIndexOfShort: Int = getColumnIndexOrThrow(_cursor, "short")
            val _cursorIndexOfByte: Int = getColumnIndexOrThrow(_cursor, "byte")
            val _cursorIndexOfLong: Int = getColumnIndexOrThrow(_cursor, "long")
            val _cursorIndexOfChar: Int = getColumnIndexOrThrow(_cursor, "char")
            val _cursorIndexOfFloat: Int = getColumnIndexOrThrow(_cursor, "float")
            val _cursorIndexOfDouble: Int = getColumnIndexOrThrow(_cursor, "double")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpInt: Int
                _tmpInt = _cursor.getInt(_cursorIndexOfInt)
                val _tmpShort: Short
                _tmpShort = _cursor.getShort(_cursorIndexOfShort)
                val _tmpByte: Byte
                _tmpByte = _cursor.getShort(_cursorIndexOfByte).toByte()
                val _tmpLong: Long
                _tmpLong = _cursor.getLong(_cursorIndexOfLong)
                val _tmpChar: Char
                _tmpChar = _cursor.getInt(_cursorIndexOfChar).toChar()
                val _tmpFloat: Float
                _tmpFloat = _cursor.getFloat(_cursorIndexOfFloat)
                val _tmpDouble: Double
                _tmpDouble = _cursor.getDouble(_cursorIndexOfDouble)
                _result = MyEntity(_tmpInt,_tmpShort,_tmpByte,_tmpLong,_tmpChar,_tmpFloat,_tmpDouble)
            } else {
                error("Cursor was empty, but expected a single item.")
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