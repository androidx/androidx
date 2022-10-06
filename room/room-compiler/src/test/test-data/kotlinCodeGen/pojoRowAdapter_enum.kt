import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import java.lang.IllegalArgumentException
import javax.`annotation`.processing.Generated
import kotlin.Int
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
            val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
            val _cursorIndexOfEnum: Int = getColumnIndexOrThrow(_cursor, "enum")
            val _cursorIndexOfNullableEnum: Int = getColumnIndexOrThrow(_cursor, "nullableEnum")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpEnum: Fruit
                _tmpEnum = checkNotNull(__Fruit_stringToEnum(_cursor.getString(_cursorIndexOfEnum)))
                val _tmpNullableEnum: Fruit?
                _tmpNullableEnum = __Fruit_stringToEnum(_cursor.getString(_cursorIndexOfNullableEnum))
                _result = MyEntity(_tmpPk,_tmpEnum,_tmpNullableEnum)
            } else {
                error("Cursor was empty, but expected a single item.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    private fun __Fruit_stringToEnum(_value: String?): Fruit? {
        if (_value == null) {
            return null
        }
        return when (_value) {
            "APPLE" -> Fruit.APPLE
            "BANANA" -> Fruit.BANANA
            else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " +
                _value)
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}