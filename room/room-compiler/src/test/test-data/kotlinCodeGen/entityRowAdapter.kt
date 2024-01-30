import android.database.Cursor
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndex
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
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

    private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
    init {
        this.__db = __db
        this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`valuePrimitive`,`valueBoolean`,`valueString`,`valueNullableString`,`variablePrimitive`,`variableNullableBoolean`,`variableString`,`variableNullableString`) VALUES (?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.valuePrimitive)
                val _tmp: Int = if (entity.valueBoolean) 1 else 0
                statement.bindLong(2, _tmp.toLong())
                statement.bindString(3, entity.valueString)
                val _tmpValueNullableString: String? = entity.valueNullableString
                if (_tmpValueNullableString == null) {
                    statement.bindNull(4)
                } else {
                    statement.bindString(4, _tmpValueNullableString)
                }
                statement.bindLong(5, entity.variablePrimitive)
                val _tmpVariableNullableBoolean: Boolean? = entity.variableNullableBoolean
                val _tmp_1: Int? = _tmpVariableNullableBoolean?.let { if (it) 1 else 0 }
                if (_tmp_1 == null) {
                    statement.bindNull(6)
                } else {
                    statement.bindLong(6, _tmp_1.toLong())
                }
                statement.bindString(7, entity.variableString)
                val _tmpVariableNullableString: String? = entity.variableNullableString
                if (_tmpVariableNullableString == null) {
                    statement.bindNull(8)
                } else {
                    statement.bindString(8, _tmpVariableNullableString)
                }
            }
        }
    }

    public override fun addEntity(item: MyEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __insertionAdapterOfMyEntity.insert(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun getEntity(): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
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
            _statement.release()
        }
    }

    private fun __entityCursorConverter_MyEntity(cursor: Cursor): MyEntity {
        val _entity: MyEntity
        val _cursorIndexOfValuePrimitive: Int = getColumnIndex(cursor, "valuePrimitive")
        val _cursorIndexOfValueBoolean: Int = getColumnIndex(cursor, "valueBoolean")
        val _cursorIndexOfValueString: Int = getColumnIndex(cursor, "valueString")
        val _cursorIndexOfValueNullableString: Int = getColumnIndex(cursor, "valueNullableString")
        val _cursorIndexOfVariablePrimitive: Int = getColumnIndex(cursor, "variablePrimitive")
        val _cursorIndexOfVariableNullableBoolean: Int = getColumnIndex(cursor,
            "variableNullableBoolean")
        val _cursorIndexOfVariableString: Int = getColumnIndex(cursor, "variableString")
        val _cursorIndexOfVariableNullableString: Int = getColumnIndex(cursor, "variableNullableString")
        val _tmpValuePrimitive: Long
        if (_cursorIndexOfValuePrimitive == -1) {
            _tmpValuePrimitive = 0
        } else {
            _tmpValuePrimitive = cursor.getLong(_cursorIndexOfValuePrimitive)
        }
        val _tmpValueBoolean: Boolean
        if (_cursorIndexOfValueBoolean == -1) {
            _tmpValueBoolean = false
        } else {
            val _tmp: Int
            _tmp = cursor.getInt(_cursorIndexOfValueBoolean)
            _tmpValueBoolean = _tmp != 0
        }
        val _tmpValueString: String
        if (_cursorIndexOfValueString == -1) {
            error("Missing value for a NON-NULL column 'valueString', found NULL value instead.")
        } else {
            _tmpValueString = cursor.getString(_cursorIndexOfValueString)
        }
        val _tmpValueNullableString: String?
        if (_cursorIndexOfValueNullableString == -1) {
            _tmpValueNullableString = null
        } else {
            if (cursor.isNull(_cursorIndexOfValueNullableString)) {
                _tmpValueNullableString = null
            } else {
                _tmpValueNullableString = cursor.getString(_cursorIndexOfValueNullableString)
            }
        }
        _entity = MyEntity(_tmpValuePrimitive,_tmpValueBoolean,_tmpValueString,_tmpValueNullableString)
        if (_cursorIndexOfVariablePrimitive != -1) {
            _entity.variablePrimitive = cursor.getLong(_cursorIndexOfVariablePrimitive)
        }
        if (_cursorIndexOfVariableNullableBoolean != -1) {
            val _tmp_1: Int?
            if (cursor.isNull(_cursorIndexOfVariableNullableBoolean)) {
                _tmp_1 = null
            } else {
                _tmp_1 = cursor.getInt(_cursorIndexOfVariableNullableBoolean)
            }
            _entity.variableNullableBoolean = _tmp_1?.let { it != 0 }
        }
        if (_cursorIndexOfVariableString != -1) {
            _entity.variableString = cursor.getString(_cursorIndexOfVariableString)
        }
        if (_cursorIndexOfVariableNullableString != -1) {
            if (cursor.isNull(_cursorIndexOfVariableNullableString)) {
                _entity.variableNullableString = null
            } else {
                _entity.variableNullableString = cursor.getString(_cursorIndexOfVariableNullableString)
            }
        }
        return _entity
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}