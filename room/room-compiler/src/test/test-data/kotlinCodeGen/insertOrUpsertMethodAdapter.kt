import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Array
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

    private val __upsertionAdapterOfMyEntity: EntityUpsertionAdapter<MyEntity>
    init {
        this.__db = __db
        this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk)
                statement.bindString(2, entity.data)
            }
        }
        this.__upsertionAdapterOfMyEntity = EntityUpsertionAdapter<MyEntity>(object :
            EntityInsertionAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "INSERT INTO `MyEntity` (`pk`,`data`) VALUES (?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk)
                statement.bindString(2, entity.data)
            }
        }, object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "UPDATE `MyEntity` SET `pk` = ?,`data` = ? WHERE `pk` = ?"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk)
                statement.bindString(2, entity.data)
                statement.bindLong(3, entity.pk)
            }
        })
    }

    public override fun insertEntity(item: MyEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __insertionAdapterOfMyEntity.insert(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun insertEntityAndReturnRowId(item: MyEntity): Long {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: Long = __insertionAdapterOfMyEntity.insertAndReturnId(item)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun insertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long> {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: List<Long> = __insertionAdapterOfMyEntity.insertAndReturnIdsList(items)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun upsertEntity(item: MyEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __upsertionAdapterOfMyEntity.upsert(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun upsertEntityAndReturnRowId(item: MyEntity): Long {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: Long = __upsertionAdapterOfMyEntity.upsertAndReturnId(item)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun upsertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long> {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: List<Long> = __upsertionAdapterOfMyEntity.upsertAndReturnIdsList(items)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun upsertEntityListAndReturnRowIdsArray(items: List<MyEntity>): Array<Long> {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: Array<Long> = (__upsertionAdapterOfMyEntity.upsertAndReturnIdsArrayBox(items)) as
                Array<Long>
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun upsertEntityListAndReturnRowIdsOutArray(items: List<MyEntity>):
        Array<out Long> {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            val _result: Array<out Long> = __upsertionAdapterOfMyEntity.upsertAndReturnIdsArrayBox(items)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}