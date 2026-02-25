import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __fooReturnTypeConverter: FooReturnTypeConverter = FooReturnTypeConverter()
  init {
    this.__db = __db
  }

  public override suspend fun getFooSingleColumn(): Foo<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    return __fooReturnTypeConverter.convert(__db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
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

  public override suspend fun getFooList(): Foo<List<MyEntity>> {
    val _sql: String = "SELECT * FROM MyEntity"
    return __fooReturnTypeConverter.convert(__db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getBlockingFooList(): Foo<List<MyEntity>> {
    val _sql: String = "SELECT * FROM MyEntity"
    return __fooReturnTypeConverter.convertBlocking(__db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _result: MutableList<MyEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: MyEntity
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            _item = MyEntity(_tmpPk)
            _result.add(_item)
          }
          _result
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
