import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Lazy
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "OPT_IN_USAGE_ERROR", "OPT_IN_USAGE", "MemberExtensionConflict", "CAN_BE_VAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  public override val fooGetter: Foo<MyEntity>
    get() = getFooGetter()

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>

  private val __providedFooConverter: Lazy<ProvidedFooConverter> = lazy {
    checkNotNull(__db.getDaoReturnTypeConverter(ProvidedFooConverter::class))
  }
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`) VALUES (?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
      }
    }
  }

  public override fun insertFoo(item: MyEntity): Foo<Long> = __providedFooConverter().convertProvided() {
    performSuspending<Long>(__db, false, true) { _connection ->
      val _result: Long = __insertAdapterOfMyEntity.insertAndReturnId(_connection, item)
      _result
    }
  }

  public override fun getDaoFoo(): Foo<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return DaoFooConverter.convertFromDao() {
      performSuspending<MyEntity>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getFunctionFoo(): Foo<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return FunctionFooConverter.convertFromFunction() {
      performSuspending<MyEntity>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  @JvmName("_privateGetFooGetter")
  private fun getFooGetter(): Foo<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return FunctionFooConverter.convertFromFunction() {
      performSuspending<MyEntity>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _result = MyEntity(_tmpPk)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  private fun __providedFooConverter(): ProvidedFooConverter = __providedFooConverter.value

  public companion object {
    public fun getRequiredColumnConverters(): List<KClass<*>> = emptyList()

    public fun getRequiredDaoReturnTypeConverters(): List<KClass<*>> = listOf(ProvidedFooConverter::class)
  }
}
