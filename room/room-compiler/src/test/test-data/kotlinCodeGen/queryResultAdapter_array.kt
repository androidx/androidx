import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.Long
import kotlin.LongArray
import kotlin.Short
import kotlin.ShortArray
import kotlin.String
import kotlin.Suppress
import kotlin.arrayOfNulls
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

    public override fun queryOfArray(): Array<MyEntity> {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
            val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
            val _cursorIndexOfOther2: Int = getColumnIndexOrThrow(_cursor, "other2")
            val _tmpResult: Array<MyEntity?> = arrayOfNulls<MyEntity>(_cursor.getCount())
            var _index: Int = 0
            while (_cursor.moveToNext()) {
                val _item: MyEntity
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpOther: String
                _tmpOther = _cursor.getString(_cursorIndexOfOther)
                val _tmpOther2: Long
                _tmpOther2 = _cursor.getLong(_cursorIndexOfOther2)
                _item = MyEntity(_tmpPk,_tmpOther,_tmpOther2)
                _tmpResult[_index] = _item
                _index++
            }
            val _result: Array<MyEntity> = (_tmpResult) as Array<MyEntity>
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun queryOfArrayWithLong(): Array<Long> {
        val _sql: String = "SELECT pk FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _tmpResult: Array<Long?> = arrayOfNulls<Long>(_cursor.getCount())
            var _index: Int = 0
            while (_cursor.moveToNext()) {
                val _item: Long
                _item = _cursor.getLong(0)
                _tmpResult[_index] = _item
                _index++
            }
            val _result: Array<Long> = (_tmpResult) as Array<Long>
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun queryOfLongArray(): LongArray {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _tmpResult: Array<Long?> = arrayOfNulls<Long>(_cursor.getCount())
            var _index: Int = 0
            while (_cursor.moveToNext()) {
                val _item: Long
                _item = _cursor.getLong(0)
                _tmpResult[_index] = _item
                _index++
            }
            val _result: LongArray = ((_tmpResult) as Array<Long>).toLongArray()
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun queryOfShortArray(): ShortArray {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _tmpResult: Array<Short?> = arrayOfNulls<Short>(_cursor.getCount())
            var _index: Int = 0
            while (_cursor.moveToNext()) {
                val _item: Short
                _item = _cursor.getShort(0)
                _tmpResult[_index] = _item
                _index++
            }
            val _result: ShortArray = ((_tmpResult) as Array<Short>).toShortArray()
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