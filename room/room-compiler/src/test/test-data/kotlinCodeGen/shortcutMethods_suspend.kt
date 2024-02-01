import androidx.room.CoroutinesRoom
import androidx.room.EntityDeletionOrUpdateAdapter
import androidx.room.EntityInsertionAdapter
import androidx.room.EntityUpsertionAdapter
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>

  private val __deletionAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __updateAdapterOfMyEntity: EntityDeletionOrUpdateAdapter<MyEntity>

  private val __upsertionAdapterOfMyEntity: EntityUpsertionAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }
    this.__deletionAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
    this.__updateAdapterOfMyEntity = object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    }
    this.__upsertionAdapterOfMyEntity = EntityUpsertionAdapter<MyEntity>(object :
        EntityInsertionAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "INSERT INTO `MyEntity` (`pk`,`other`) VALUES (?,?)"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
      }
    }, object : EntityDeletionOrUpdateAdapter<MyEntity>(__db) {
      protected override fun createQuery(): String =
          "UPDATE `MyEntity` SET `pk` = ?,`other` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindString(2, entity.other)
        statement.bindLong(3, entity.pk.toLong())
      }
    })
  }

  public override suspend fun insert(vararg entities: MyEntity): List<Long> =
      CoroutinesRoom.execute(__db, true, object : Callable<List<Long>> {
    public override fun call(): List<Long> {
      __db.beginTransaction()
      try {
        val _result: List<Long> = __insertionAdapterOfMyEntity.insertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun delete(entity: MyEntity): Int = CoroutinesRoom.execute(__db, true,
      object : Callable<Int> {
    public override fun call(): Int {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __deletionAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun update(entity: MyEntity): Int = CoroutinesRoom.execute(__db, true,
      object : Callable<Int> {
    public override fun call(): Int {
      var _total: Int = 0
      __db.beginTransaction()
      try {
        _total += __updateAdapterOfMyEntity.handle(entity)
        __db.setTransactionSuccessful()
        return _total
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override suspend fun upsert(vararg entities: MyEntity): List<Long> =
      CoroutinesRoom.execute(__db, true, object : Callable<List<Long>> {
    public override fun call(): List<Long> {
      __db.beginTransaction()
      try {
        val _result: List<Long> = __upsertionAdapterOfMyEntity.upsertAndReturnIdsList(entities)
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
