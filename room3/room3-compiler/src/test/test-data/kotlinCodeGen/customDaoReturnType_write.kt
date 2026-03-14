import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>

  private val __fooReturnTypeConverter: FooReturnTypeConverter = FooReturnTypeConverter()

  private val __deleteAdapterOfMyEntity: EntityDeleteOrUpdateAdapter<MyEntity>

  private val __updateAdapterOfMyEntity: EntityDeleteOrUpdateAdapter<MyEntity>

  private val __upsertAdapterOfMyEntity: EntityUpsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`) VALUES (?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
    this.__deleteAdapterOfMyEntity = object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `MyEntity` WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
    this.__updateAdapterOfMyEntity = object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `MyEntity` SET `pk` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindLong(2, entity.pk.toLong())
      }
    }
    this.__upsertAdapterOfMyEntity = EntityUpsertAdapter<MyEntity>(object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `MyEntity` (`pk`) VALUES (?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<MyEntity>() {
      protected override fun createQuery(): String = "UPDATE `MyEntity` SET `pk` = ? WHERE `pk` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindLong(2, entity.pk.toLong())
      }
    })
  }

  public override suspend fun insert(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __insertAdapterOfMyEntity.insert(_connection, item)
    }
  }

  public override suspend fun insertReturnId(item: MyEntity): Foo<Long> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      val _result: Long = __insertAdapterOfMyEntity.insertAndReturnId(_connection, item)
      _result
    }
  }

  public override fun insertBlocking(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convertBlocking() {
    performSuspending(__db, false, true) { _connection ->
      __insertAdapterOfMyEntity.insert(_connection, item)
    }
  }

  public override suspend fun delete(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __deleteAdapterOfMyEntity.handle(_connection, item)
    }
  }

  public override suspend fun deleteReturnChanges(item: MyEntity): Foo<Int> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      var _result: Int = 0
      _result += __deleteAdapterOfMyEntity.handleAndReturnChanges(_connection, item)
      _result
    }
  }

  public override suspend fun deleteBlocking(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __deleteAdapterOfMyEntity.handle(_connection, item)
    }
  }

  public override suspend fun update(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __updateAdapterOfMyEntity.handle(_connection, item)
    }
  }

  public override suspend fun updateReturnChanges(item: MyEntity): Foo<Int> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      var _result: Int = 0
      _result += __updateAdapterOfMyEntity.handleAndReturnChanges(_connection, item)
      _result
    }
  }

  public override suspend fun updateBlocking(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __updateAdapterOfMyEntity.handle(_connection, item)
    }
  }

  public override suspend fun upsert(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __upsertAdapterOfMyEntity.upsert(_connection, item)
    }
  }

  public override suspend fun upsertBlocking(item: MyEntity): Foo<Unit> = __fooReturnTypeConverter.convert() {
    performSuspending(__db, false, true) { _connection ->
      __upsertAdapterOfMyEntity.upsert(_connection, item)
    }
  }

  public override suspend fun insertWithId(pk: Int): Foo<Unit> {
    val _sql: String = "INSERT INTO MyEntity (pk) VALUES (?)"
    return __fooReturnTypeConverter.convert() {
      performSuspending(__db, false, true) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          var _argIndex: Int = 1
          _stmt.bindLong(_argIndex, pk.toLong())
          _stmt.step()
          Unit
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun insertWithIdBlocking(pk: Int): Foo<Unit> {
    val _sql: String = "INSERT INTO MyEntity (pk) VALUES (?)"
    return __fooReturnTypeConverter.convertBlocking() {
      performSuspending(__db, false, true) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          var _argIndex: Int = 1
          _stmt.bindLong(_argIndex, pk.toLong())
          _stmt.step()
          Unit
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
