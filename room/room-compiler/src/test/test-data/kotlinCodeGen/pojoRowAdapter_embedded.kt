import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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
            val _cursorIndexOfNumberData: Int = getColumnIndexOrThrow(_cursor, "numberData")
            val _cursorIndexOfStringData: Int = getColumnIndexOrThrow(_cursor, "stringData")
            val _cursorIndexOfNumberData_1: Int = getColumnIndexOrThrow(_cursor, "nullablenumberData")
            val _cursorIndexOfStringData_1: Int = getColumnIndexOrThrow(_cursor, "nullablestringData")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpFoo: Foo
                val _tmpNumberData: Long
                _tmpNumberData = _cursor.getLong(_cursorIndexOfNumberData)
                val _tmpStringData: String
                _tmpStringData = _cursor.getString(_cursorIndexOfStringData)
                _tmpFoo = Foo(_tmpNumberData,_tmpStringData)
                val _tmpNullableFoo: Foo?
                if (!(_cursor.isNull(_cursorIndexOfNumberData_1) &&
                        _cursor.isNull(_cursorIndexOfStringData_1))) {
                    val _tmpNumberData_1: Long
                    _tmpNumberData_1 = _cursor.getLong(_cursorIndexOfNumberData_1)
                    val _tmpStringData_1: String
                    _tmpStringData_1 = _cursor.getString(_cursorIndexOfStringData_1)
                    _tmpNullableFoo = Foo(_tmpNumberData_1,_tmpStringData_1)
                } else {
                    _tmpNullableFoo = null
                }
                _result = MyEntity(_tmpPk,_tmpFoo,_tmpNullableFoo)
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