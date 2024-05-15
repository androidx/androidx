import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMyEntity: EntityInsertAdapter<MyEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMyEntity = object : EntityInsertAdapter<MyEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`internalVal`,`internalVar`,`internalSetterVar`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        statement.bindLong(2, entity.internalVal)
        statement.bindLong(3, entity.internalVar)
        statement.bindLong(4, entity.internalSetterVar)
      }
    }
  }

  public override fun addEntity(item: MyEntity): Unit = performBlocking(__db, false, true) {
      _connection ->
    __insertAdapterOfMyEntity.insert(_connection, item)
  }

  public override fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfInternalVal: Int = getColumnIndexOrThrow(_stmt, "internalVal")
        val _cursorIndexOfInternalVar: Int = getColumnIndexOrThrow(_stmt, "internalVar")
        val _cursorIndexOfInternalSetterVar: Int = getColumnIndexOrThrow(_stmt, "internalSetterVar")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpInternalVal: Long
          _tmpInternalVal = _stmt.getLong(_cursorIndexOfInternalVal)
          _result = MyEntity(_tmpPk,_tmpInternalVal)
          _result.internalVar = _stmt.getLong(_cursorIndexOfInternalVar)
          _result.internalSetterVar = _stmt.getLong(_cursorIndexOfInternalSetterVar)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
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
