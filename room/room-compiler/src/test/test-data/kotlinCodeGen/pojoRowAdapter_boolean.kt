import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
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
          "INSERT OR ABORT INTO `MyEntity` (`pk`,`boolean`,`nullableBoolean`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.pk.toLong())
        val _tmp: Int = if (entity.boolean) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        val _tmpNullableBoolean: Boolean? = entity.nullableBoolean
        val _tmp_1: Int? = _tmpNullableBoolean?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp_1.toLong())
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
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfBoolean: Int = getColumnIndexOrThrow(_stmt, "boolean")
        val _cursorIndexOfNullableBoolean: Int = getColumnIndexOrThrow(_stmt, "nullableBoolean")
        val _result: MyEntity
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpBoolean: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfBoolean).toInt()
          _tmpBoolean = _tmp != 0
          val _tmpNullableBoolean: Boolean?
          val _tmp_1: Int?
          if (_stmt.isNull(_cursorIndexOfNullableBoolean)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getLong(_cursorIndexOfNullableBoolean).toInt()
          }
          _tmpNullableBoolean = _tmp_1?.let { it != 0 }
          _result = MyEntity(_tmpPk,_tmpBoolean,_tmpNullableBoolean)
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
