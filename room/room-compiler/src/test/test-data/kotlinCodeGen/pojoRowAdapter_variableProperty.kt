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
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`variablePrimitive`,`variableString`,`variableNullableString`) VALUES (?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk.toLong())
                statement.bindLong(2, entity.variablePrimitive)
                statement.bindString(3, entity.variableString)
                val _tmpVariableNullableString: String? = entity.variableNullableString
                if (_tmpVariableNullableString == null) {
                    statement.bindNull(4)
                } else {
                    statement.bindString(4, _tmpVariableNullableString)
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
            val _cursorIndexOfVariablePrimitive: Int = getColumnIndexOrThrow(_cursor, "variablePrimitive")
            val _cursorIndexOfVariableString: Int = getColumnIndexOrThrow(_cursor, "variableString")
            val _cursorIndexOfVariableNullableString: Int = getColumnIndexOrThrow(_cursor,
                "variableNullableString")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                _result = MyEntity(_tmpPk)
                _result.variablePrimitive = _cursor.getLong(_cursorIndexOfVariablePrimitive)
                _result.variableString = _cursor.getString(_cursorIndexOfVariableString)
                if (_cursor.isNull(_cursorIndexOfVariableNullableString)) {
                    _result.variableNullableString = null
                } else {
                    _result.variableNullableString = _cursor.getString(_cursorIndexOfVariableNullableString)
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