import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndex
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
          "INSERT OR ABORT INTO `MyEntity` (`valuePrimitive`,`valueBoolean`,`valueString`,`valueNullableString`,`variablePrimitive`,`variableNullableBoolean`,`variableString`,`variableNullableString`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.valuePrimitive)
        val _tmp: Int = if (entity.valueBoolean) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        statement.bindText(3, entity.valueString)
        val _tmpValueNullableString: String? = entity.valueNullableString
        if (_tmpValueNullableString == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpValueNullableString)
        }
        statement.bindLong(5, entity.variablePrimitive)
        val _tmpVariableNullableBoolean: Boolean? = entity.variableNullableBoolean
        val _tmp_1: Int? = _tmpVariableNullableBoolean?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmp_1.toLong())
        }
        statement.bindText(7, entity.variableString)
        val _tmpVariableNullableString: String? = entity.variableNullableString
        if (_tmpVariableNullableString == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpVariableNullableString)
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
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityStatementConverter_MyEntity(statement: SQLiteStatement): MyEntity {
    val _entity: MyEntity
    val _cursorIndexOfValuePrimitive: Int = getColumnIndex(statement, "valuePrimitive")
    val _cursorIndexOfValueBoolean: Int = getColumnIndex(statement, "valueBoolean")
    val _cursorIndexOfValueString: Int = getColumnIndex(statement, "valueString")
    val _cursorIndexOfValueNullableString: Int = getColumnIndex(statement, "valueNullableString")
    val _cursorIndexOfVariablePrimitive: Int = getColumnIndex(statement, "variablePrimitive")
    val _cursorIndexOfVariableNullableBoolean: Int = getColumnIndex(statement,
        "variableNullableBoolean")
    val _cursorIndexOfVariableString: Int = getColumnIndex(statement, "variableString")
    val _cursorIndexOfVariableNullableString: Int = getColumnIndex(statement,
        "variableNullableString")
    val _tmpValuePrimitive: Long
    if (_cursorIndexOfValuePrimitive == -1) {
      _tmpValuePrimitive = 0
    } else {
      _tmpValuePrimitive = statement.getLong(_cursorIndexOfValuePrimitive)
    }
    val _tmpValueBoolean: Boolean
    if (_cursorIndexOfValueBoolean == -1) {
      _tmpValueBoolean = false
    } else {
      val _tmp: Int
      _tmp = statement.getLong(_cursorIndexOfValueBoolean).toInt()
      _tmpValueBoolean = _tmp != 0
    }
    val _tmpValueString: String
    if (_cursorIndexOfValueString == -1) {
      error("Missing value for a NON-NULL column 'valueString', found NULL value instead.")
    } else {
      _tmpValueString = statement.getText(_cursorIndexOfValueString)
    }
    val _tmpValueNullableString: String?
    if (_cursorIndexOfValueNullableString == -1) {
      _tmpValueNullableString = null
    } else {
      if (statement.isNull(_cursorIndexOfValueNullableString)) {
        _tmpValueNullableString = null
      } else {
        _tmpValueNullableString = statement.getText(_cursorIndexOfValueNullableString)
      }
    }
    _entity = MyEntity(_tmpValuePrimitive,_tmpValueBoolean,_tmpValueString,_tmpValueNullableString)
    if (_cursorIndexOfVariablePrimitive != -1) {
      _entity.variablePrimitive = statement.getLong(_cursorIndexOfVariablePrimitive)
    }
    if (_cursorIndexOfVariableNullableBoolean != -1) {
      val _tmp_1: Int?
      if (statement.isNull(_cursorIndexOfVariableNullableBoolean)) {
        _tmp_1 = null
      } else {
        _tmp_1 = statement.getLong(_cursorIndexOfVariableNullableBoolean).toInt()
      }
      _entity.variableNullableBoolean = _tmp_1?.let { it != 0 }
    }
    if (_cursorIndexOfVariableString != -1) {
      _entity.variableString = statement.getText(_cursorIndexOfVariableString)
    }
    if (_cursorIndexOfVariableNullableString != -1) {
      if (statement.isNull(_cursorIndexOfVariableNullableString)) {
        _entity.variableNullableString = null
      } else {
        _entity.variableNullableString = statement.getText(_cursorIndexOfVariableNullableString)
      }
    }
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
