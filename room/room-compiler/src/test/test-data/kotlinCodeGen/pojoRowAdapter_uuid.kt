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
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase

    private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
    init {
        this.__db = __db
        this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
            public override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`uuid`,`nullableUuid`) VALUES (?,?,?)"

            public override fun bind(statement: SupportSQLiteStatement, entity: MyEntity): Unit {
                statement.bindLong(1, entity.pk.toLong())
                statement.bindBlob(2, convertUUIDToByte(entity.uuid))
                if (entity.nullableUuid == null) {
                    statement.bindNull(3)
                } else {
                    statement.bindBlob(3, convertUUIDToByte(entity.nullableUuid))
                }
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
            val _cursorIndexOfUuid: Int = getColumnIndexOrThrow(_cursor, "uuid")
            val _cursorIndexOfNullableUuid: Int = getColumnIndexOrThrow(_cursor, "nullableUuid")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpPk: Int
                _tmpPk = _cursor.getInt(_cursorIndexOfPk)
                val _tmpUuid: UUID
                _tmpUuid = convertByteToUUID(_cursor.getBlob(_cursorIndexOfUuid))
                val _tmpNullableUuid: UUID?
                if (_cursor.isNull(_cursorIndexOfNullableUuid)) {
                    _tmpNullableUuid = null
                } else {
                    _tmpNullableUuid = convertByteToUUID(_cursor.getBlob(_cursorIndexOfNullableUuid))
                }
                _result = MyEntity(_tmpPk,_tmpUuid,_tmpNullableUuid)
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