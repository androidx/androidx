import android.database.Cursor
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import javax.`annotation`.processing.Generated
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
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`primitive`,`string`,`nullableString`,`fieldString`,`nullableFieldString`,`variablePrimitive`,`variableString`,`variableNullableString`,`variableFieldString`,`variableNullableFieldString`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk.toLong())
                statement.bindLong(2, entity.primitive)
                statement.bindString(3, entity.string)
                val _tmpNullableString: String? = entity.nullableString
                if (_tmpNullableString == null) {
                    statement.bindNull(4)
                } else {
                    statement.bindString(4, _tmpNullableString)
                }
                statement.bindString(5, entity.fieldString)
                val _tmpNullableFieldString: String? = entity.nullableFieldString
                if (_tmpNullableFieldString == null) {
                    statement.bindNull(6)
                } else {
                    statement.bindString(6, _tmpNullableFieldString)
                }
                statement.bindLong(7, entity.variablePrimitive)
                statement.bindString(8, entity.variableString)
                val _tmpVariableNullableString: String? = entity.variableNullableString
                if (_tmpVariableNullableString == null) {
                    statement.bindNull(9)
                } else {
                    statement.bindString(9, _tmpVariableNullableString)
                }
                statement.bindString(10, entity.variableFieldString)
                val _tmpVariableNullableFieldString: String? = entity.variableNullableFieldString
                if (_tmpVariableNullableFieldString == null) {
                    statement.bindNull(11)
                } else {
                    statement.bindString(11, _tmpVariableNullableFieldString)
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
            val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
            val _cursorIndexOfPrimitive: Int = getColumnIndexOrThrow(_cursor, "primitive")
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _cursorIndexOfNullableString: Int = getColumnIndexOrThrow(_cursor, "nullableString")
            val _cursorIndexOfFieldString: Int = getColumnIndexOrThrow(_cursor, "fieldString")
            val _cursorIndexOfNullableFieldString: Int = getColumnIndexOrThrow(_cursor,
                "nullableFieldString")
            val _cursorIndexOfVariablePrimitive: Int = getColumnIndexOrThrow(_cursor, "variablePrimitive")
            val _cursorIndexOfVariableString: Int = getColumnIndexOrThrow(_cursor, "variableString")
            val _cursorIndexOfVariableNullableString: Int = getColumnIndexOrThrow(_cursor,
                "variableNullableString")
            val _cursorIndexOfVariableFieldString: Int = getColumnIndexOrThrow(_cursor,
                "variableFieldString")
            val _cursorIndexOfVariableNullableFieldString: Int = getColumnIndexOrThrow(_cursor,
                "variableNullableFieldString")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpPrimitive: Long
                _tmpPrimitive = _cursor.getLong(_cursorIndexOfPrimitive)
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                val _tmpNullableString: String?
                if (_cursor.isNull(_cursorIndexOfNullableString)) {
                    _tmpNullableString = null
                } else {
                    _tmpNullableString = _cursor.getString(_cursorIndexOfNullableString)
                }
                val _tmpFieldString: String
                _tmpFieldString = _cursor.getString(_cursorIndexOfFieldString)
                val _tmpNullableFieldString: String?
                if (_cursor.isNull(_cursorIndexOfNullableFieldString)) {
                    _tmpNullableFieldString = null
                } else {
                    _tmpNullableFieldString = _cursor.getString(_cursorIndexOfNullableFieldString)
                }
                _result =
                    MyEntity(_tmpPk,_tmpPrimitive,_tmpString,_tmpNullableString,_tmpFieldString,_tmpNullableFieldString)
                _result.variablePrimitive = _cursor.getLong(_cursorIndexOfVariablePrimitive)
                _result.variableString = _cursor.getString(_cursorIndexOfVariableString)
                if (_cursor.isNull(_cursorIndexOfVariableNullableString)) {
                    _result.variableNullableString = null
                } else {
                    _result.variableNullableString = _cursor.getString(_cursorIndexOfVariableNullableString)
                }
                _result.variableFieldString = _cursor.getString(_cursorIndexOfVariableFieldString)
                if (_cursor.isNull(_cursorIndexOfVariableNullableFieldString)) {
                    _result.variableNullableFieldString = null
                } else {
                    _result.variableNullableFieldString =
                        _cursor.getString(_cursorIndexOfVariableNullableFieldString)
                }
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