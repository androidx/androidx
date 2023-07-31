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
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`numberData`,`stringData`,`nullablenumberData`,`nullablestringData`) VALUES (?,?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk.toLong())
                val _tmpFoo: Foo = entity.foo
                statement.bindLong(2, _tmpFoo.numberData)
                statement.bindString(3, _tmpFoo.stringData)
                val _tmpNullableFoo: Foo? = entity.nullableFoo
                if (_tmpNullableFoo != null) {
                    statement.bindLong(4, _tmpNullableFoo.numberData)
                    statement.bindString(5, _tmpNullableFoo.stringData)
                } else {
                    statement.bindNull(4)
                    statement.bindNull(5)
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