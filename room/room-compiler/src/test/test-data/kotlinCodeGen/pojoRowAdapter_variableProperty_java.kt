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
          "INSERT OR ABORT INTO `MyEntity` (`mValue`,`mNullableValue`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.getValue())
        val _tmpMNullableValue: String? = entity.getNullableValue()
        if (_tmpMNullableValue == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpMNullableValue)
        }
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
        val _cursorIndexOfMValue: Int = getColumnIndexOrThrow(_stmt, "mValue")
        val _cursorIndexOfMNullableValue: Int = getColumnIndexOrThrow(_stmt, "mNullableValue")
        val _result: MyEntity
        if (_stmt.step()) {
          _result = MyEntity()
          val _tmpMValue: Long
          _tmpMValue = _stmt.getLong(_cursorIndexOfMValue)
          _result.setValue(_tmpMValue)
          val _tmpMNullableValue: String?
          if (_stmt.isNull(_cursorIndexOfMNullableValue)) {
            _tmpMNullableValue = null
          } else {
            _tmpMNullableValue = _stmt.getText(_cursorIndexOfMNullableValue)
          }
          _result.setNullableValue(_tmpMNullableValue)
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
