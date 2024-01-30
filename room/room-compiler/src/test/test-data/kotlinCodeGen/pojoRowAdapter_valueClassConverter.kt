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
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuidData`,`nullableUuidData`,`nullableLongData`,`doubleNullableLongData`,`genericData`) VALUES (?,?,?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                val _data: Long = checkNotNull(entity.pk.data) {
                    "Cannot bind NULLABLE value 'data' of inline class 'LongValueClass' to a NOT NULL column."
                }
                statement.bindLong(1, _data)
                val _data_1: UUID = checkNotNull(entity.uuidData.data) {
                    "Cannot bind NULLABLE value 'data' of inline class 'UUIDValueClass' to a NOT NULL column."
                }
                statement.bindBlob(2, convertUUIDToByte(_data_1))
                val _tmpNullableUuidData: UUIDValueClass? = entity.nullableUuidData
                val _data_2: UUID? = _tmpNullableUuidData?.data
                if (_data_2 == null) {
                    statement.bindNull(3)
                } else {
                    statement.bindBlob(3, convertUUIDToByte(_data_2))
                }
                val _data_3: Long = checkNotNull(entity.nullableLongData.data) {
                    "Cannot bind NULLABLE value 'data' of inline class 'NullableLongValueClass' to a NOT NULL column."
                }
                statement.bindLong(4, _data_3)
                val _tmpDoubleNullableLongData: NullableLongValueClass? = entity.doubleNullableLongData
                val _data_4: Long? = _tmpDoubleNullableLongData?.data
                if (_data_4 == null) {
                    statement.bindNull(5)
                } else {
                    statement.bindLong(5, _data_4)
                }
                val _password: String = checkNotNull(entity.genericData.password) {
                    "Cannot bind NULLABLE value 'password' of inline class 'GenericValueClass<String>' to a NOT NULL column."
                }
                statement.bindString(6, _password)
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
            val _cursorIndexOfUuidData: Int = getColumnIndexOrThrow(_cursor, "uuidData")
            val _cursorIndexOfNullableUuidData: Int = getColumnIndexOrThrow(_cursor, "nullableUuidData")
            val _cursorIndexOfNullableLongData: Int = getColumnIndexOrThrow(_cursor, "nullableLongData")
            val _cursorIndexOfDoubleNullableLongData: Int = getColumnIndexOrThrow(_cursor,
                "doubleNullableLongData")
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
                val _tmpNullableUuidData: UUIDValueClass?
                if (_cursor.isNull(_cursorIndexOfNullableUuidData)) {
                    _tmpNullableUuidData = null
                } else {
                    val _data_2: UUID
                    _data_2 = convertByteToUUID(_cursor.getBlob(_cursorIndexOfNullableUuidData))
                    _tmpNullableUuidData = UUIDValueClass(_data_2)
                }
                val _tmpNullableLongData: NullableLongValueClass
                val _data_3: Long
                _data_3 = _cursor.getLong(_cursorIndexOfNullableLongData)
                _tmpNullableLongData = NullableLongValueClass(_data_3)
                val _tmpDoubleNullableLongData: NullableLongValueClass?
                if (_cursor.isNull(_cursorIndexOfDoubleNullableLongData)) {
                    _tmpDoubleNullableLongData = null
                } else {
                    val _data_4: Long
                    _data_4 = _cursor.getLong(_cursorIndexOfDoubleNullableLongData)
                    _tmpDoubleNullableLongData = NullableLongValueClass(_data_4)
                }
                val _tmpGenericData: GenericValueClass<String>
                val _password: String
                _password = _cursor.getString(_cursorIndexOfGenericData)
                _tmpGenericData = GenericValueClass<String>(_password)
                _result =
                    MyEntity(_tmpPk,_tmpUuidData,_tmpNullableUuidData,_tmpNullableLongData,_tmpDoubleNullableLongData,_tmpGenericData)
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