import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.SharedSQLiteStatement
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

    private val __preparedStmtOfInsertEntity: SharedSQLiteStatement
    init {
        this.__db = __db
        this.__preparedStmtOfInsertEntity = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "INSERT INTO MyEntity (pk) VALUES (?)"
                return _query
            }
        }
    }

    public override fun insertEntity(id: Long): Long {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertEntity.acquire()
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        try {
            __db.beginTransaction()
            try {
                val _result: Long = _stmt.executeInsert()
                __db.setTransactionSuccessful()
                return _result
            } finally {
                __db.endTransaction()
            }
        } finally {
            __preparedStmtOfInsertEntity.release(_stmt)
        }
    }

    public override fun getEntity(id: Long): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity WHERE pk = ?"
        val _statement: RoomSQLiteQuery = acquire(_sql, 1)
        var _argIndex: Int = 1
        _statement.bindLong(_argIndex, id)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Long
                _tmpPk = _cursor.getLong(_cursorIndexOfPk)
                _result = MyEntity(_tmpPk)
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