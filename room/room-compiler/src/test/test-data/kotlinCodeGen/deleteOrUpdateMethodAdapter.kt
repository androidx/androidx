import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.RoomDatabase
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

    private val __deletionAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

    private val __updateAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>
    init {
        this.__db = __db
        this.__deletionAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk)
            }
        }
        this.__updateAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "UPDATE OR ABORT `MyEntity` SET `pk` = ?,`data` = ? WHERE `pk` = ?"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.pk)
                statement.bindString(2, entity.data)
                statement.bindLong(3, entity.pk)
            }
        }
    }

    public override fun deleteEntity(item: MyEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __deletionAdapterOfMyEntity.handle(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun deleteEntityAndReturnCount(item: MyEntity): Int {
        __db.assertNotSuspendingTransaction()
        var _total: Int = 0
        __db.beginTransaction()
        try {
            _total += __deletionAdapterOfMyEntity.handle(item)
            __db.setTransactionSuccessful()
            return _total
        } finally {
            __db.endTransaction()
        }
    }

    public override fun updateEntity(item: MyEntity) {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __updateAdapterOfMyEntity.handle(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun updateEntityAndReturnCount(item: MyEntity): Int {
        __db.assertNotSuspendingTransaction()
        var _total: Int = 0
        __db.beginTransaction()
        try {
            _total += __updateAdapterOfMyEntity.handle(item)
            __db.setTransactionSuccessful()
            return _total
        } finally {
            __db.endTransaction()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}