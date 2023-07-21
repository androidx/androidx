import android.database.Cursor
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.convertByteToUUID
import androidx.room.util.convertUUIDToByte
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import java.util.UUID
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
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
            public override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuidData`,`genericData`) VALUES (?,?,?)"

            public override fun bind(statement: SupportSQLiteStatement, entity: MyEntity): Unit {
                val _data: Long = entity.pk.data
                statement.bindLong(1, _data)
                val _data_1: UUID = entity.uuidData.data
                statement.bindBlob(2, convertUUIDToByte(_data_1))
                val _password: String = entity.genericData.password
                statement.bindString(3, _password)
            }
        }
    }

    public override fun addEntity(item: MyEntity): Unit {
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
            val _cursorIndexOfUuidData: Int = getColumnIndexOrThrow(_cursor, "uuidData")
            val _cursorIndexOfGenericData: Int = getColumnIndexOrThrow(_cursor, "genericData")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: LongValueClass
                val _data: Long
                _data = _cursor.getLong(_cursorIndexOfPk)
                _tmpPk = LongValueClass(_data)
                val _tmpUuidData: UUIDValueClass
                val _data_1: UUID
                _data_1 = convertByteToUUID(_cursor.getBlob(_cursorIndexOfUuidData))
                _tmpUuidData = UUIDValueClass(_data_1)
                val _tmpGenericData: GenericValueClass<String>
                val _password: String
                _password = _cursor.getString(_cursorIndexOfGenericData)
                _tmpGenericData = GenericValueClass<String>(_password)
                _result = MyEntity(_tmpPk,_tmpUuidData,_tmpGenericData)
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