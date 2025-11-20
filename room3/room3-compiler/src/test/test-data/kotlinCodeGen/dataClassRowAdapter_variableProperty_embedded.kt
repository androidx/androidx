import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
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
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`pk`,`nullablenumberData`,`nullablestringData`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        val _tmpNullableFoo: Foo? = entity.nullableFoo
        if (_tmpNullableFoo != null) {
          statement.bindLong(2, _tmpNullableFoo.numberData)
          statement.bindText(3, _tmpNullableFoo.stringData)
        } else {
          statement.bindNull(2)
          statement.bindNull(3)
        }
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) { _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _columnIndexOfNumberData: Int = getColumnIndexOrThrow(_stmt, "nullablenumberData")
        val _columnIndexOfStringData: Int = getColumnIndexOrThrow(_stmt, "nullablestringData")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpNullableFoo: Foo?
          if (!(_stmt.isNull(_columnIndexOfNumberData) && _stmt.isNull(_columnIndexOfStringData))) {
            val _tmpNumberData: Long
            _tmpNumberData = _stmt.getLong(_columnIndexOfNumberData)
            val _tmpStringData: String
            _tmpStringData = _stmt.getText(_columnIndexOfStringData)
            _tmpNullableFoo = Foo(_tmpNumberData,_tmpStringData)
          } else {
            _tmpNullableFoo = null
          }
          _result = MyEntity()
          _result.pk = _stmt.getLong(_columnIndexOfPk).toInt()
          _result.nullableFoo = _tmpNullableFoo
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
