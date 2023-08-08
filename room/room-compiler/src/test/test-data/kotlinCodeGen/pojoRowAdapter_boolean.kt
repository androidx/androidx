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
import kotlin.Boolean
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
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`boolean`,`nullableBoolean`) VALUES (?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk.toLong())
                val _tmp: Int = if (entity.boolean) 1 else 0
                statement.bindLong(2, _tmp.toLong())
                val _tmpNullableBoolean: Boolean? = entity.nullableBoolean
                val _tmp_1: Int? = _tmpNullableBoolean?.let { if (it) 1 else 0 }
                if (_tmp_1 == null) {
                    statement.bindNull(3)
                } else {
                    statement.bindLong(3, _tmp_1.toLong())
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
            val _cursorIndexOfBoolean: Int = getColumnIndexOrThrow(_cursor, "boolean")
            val _cursorIndexOfNullableBoolean: Int = getColumnIndexOrThrow(_cursor, "nullableBoolean")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpBoolean: Boolean
                val _tmp: Int
                _tmp = _cursor.getInt(_cursorIndexOfBoolean)
                _tmpBoolean = _tmp != 0
                val _tmpNullableBoolean: Boolean?
                val _tmp_1: Int?
                if (_cursor.isNull(_cursorIndexOfNullableBoolean)) {
                    _tmp_1 = null
                } else {
                    _tmp_1 = _cursor.getInt(_cursorIndexOfNullableBoolean)
                }
                _tmpNullableBoolean = _tmp_1?.let { it != 0 }
                _result = MyEntity(_tmpPk,_tmpBoolean,_tmpNullableBoolean)
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