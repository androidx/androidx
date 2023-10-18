import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndex
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteQuery
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
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

    public override fun getEntity(sql: SupportSQLiteQuery): MyEntity {
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, sql, false, null)
        try {
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                _result = __entityCursorConverter_MyEntity(_cursor)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
            }
            return _result
        } finally {
            _cursor.close()
        }
    }

    private fun __entityCursorConverter_MyEntity(cursor: Cursor): MyEntity {
        val _entity: MyEntity
        val _cursorIndexOfPk: Int = getColumnIndex(cursor, "pk")
        val _cursorIndexOfDoubleColumn: Int = getColumnIndex(cursor, "doubleColumn")
        val _cursorIndexOfFloatColumn: Int = getColumnIndex(cursor, "floatColumn")
        val _tmpPk: Long
        if (_cursorIndexOfPk == -1) {
            _tmpPk = 0
        } else {
            _tmpPk = cursor.getLong(_cursorIndexOfPk)
        }
        val _tmpDoubleColumn: Double
        if (_cursorIndexOfDoubleColumn == -1) {
            _tmpDoubleColumn = 0.0
        } else {
            _tmpDoubleColumn = cursor.getDouble(_cursorIndexOfDoubleColumn)
        }
        val _tmpFloatColumn: Float
        if (_cursorIndexOfFloatColumn == -1) {
            _tmpFloatColumn = 0f
        } else {
            _tmpFloatColumn = cursor.getFloat(_cursorIndexOfFloatColumn)
        }
        _entity = MyEntity(_tmpPk,_tmpDoubleColumn,_tmpFloatColumn)
        return _entity
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}