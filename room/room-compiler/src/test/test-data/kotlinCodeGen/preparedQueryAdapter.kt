import androidx.room.RoomDatabase
import androidx.room.SharedSQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase

    private val __preparedStmtOfInsertEntity: SharedSQLiteStatement

    private val __preparedStmtOfUpdateEntity: SharedSQLiteStatement

    private val __preparedStmtOfUpdateEntityReturnInt: SharedSQLiteStatement

    private val __preparedStmtOfDeleteEntity: SharedSQLiteStatement
    init {
        this.__db = __db
        this.__preparedStmtOfInsertEntity = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "INSERT INTO MyEntity (id) VALUES (?)"
                return _query
            }
        }
        this.__preparedStmtOfUpdateEntity = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "UPDATE MyEntity SET text = ?"
                return _query
            }
        }
        this.__preparedStmtOfUpdateEntityReturnInt = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "UPDATE MyEntity SET text = ? WHERE id = ?"
                return _query
            }
        }
        this.__preparedStmtOfDeleteEntity = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "DELETE FROM MyEntity"
                return _query
            }
        }
    }

    public override fun insertEntity(id: Long): Unit {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertEntity.acquire()
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        __db.beginTransaction()
        try {
            _stmt.executeInsert()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
            __preparedStmtOfInsertEntity.release(_stmt)
        }
    }

    public override fun insertEntityReturnLong(id: Long): Long {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertEntity.acquire()
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        __db.beginTransaction()
        try {
            val _result: Long = _stmt.executeInsert()
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
            __preparedStmtOfInsertEntity.release(_stmt)
        }
    }

    public override fun updateEntity(text: String): Unit {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateEntity.acquire()
        var _argIndex: Int = 1
        _stmt.bindString(_argIndex, text)
        __db.beginTransaction()
        try {
            _stmt.executeUpdateDelete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
            __preparedStmtOfUpdateEntity.release(_stmt)
        }
    }

    public override fun updateEntityReturnInt(id: Long, text: String): Int {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfUpdateEntityReturnInt.acquire()
        var _argIndex: Int = 1
        _stmt.bindString(_argIndex, text)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        __db.beginTransaction()
        try {
            val _result: Int = _stmt.executeUpdateDelete()
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
            __preparedStmtOfUpdateEntityReturnInt.release(_stmt)
        }
    }

    public override fun deleteEntity(): Unit {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteEntity.acquire()
        __db.beginTransaction()
        try {
            _stmt.executeUpdateDelete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
            __preparedStmtOfDeleteEntity.release(_stmt)
        }
    }

    public override fun deleteEntityReturnInt(): Int {
        __db.assertNotSuspendingTransaction()
        val _stmt: SupportSQLiteStatement = __preparedStmtOfDeleteEntity.acquire()
        __db.beginTransaction()
        try {
            val _result: Int = _stmt.executeUpdateDelete()
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
            __preparedStmtOfDeleteEntity.release(_stmt)
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}