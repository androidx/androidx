import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndex
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
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
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `MyEntity` (`valuePrimitiveLong`,`valuePrimitiveInt`,`valuePrimitiveByte`,`valuePrimitiveShort`,`valueFloat`,`valueDouble`,`valueBoolean`,`valueNullableBoolean`,`valueString`,`valueNullableString`,`valueChar`,`variablePrimitiveLong`,`variablePrimitiveInt`,`variablePrimitiveByte`,`variablePrimitiveShort`,`variableFloat`,`variableDouble`,`variableBoolean`,`variableNullableBoolean`,`variableString`,`variableNullableString`,`variableChar`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyEntity) {
        statement.bindLong(1, entity.valuePrimitiveLong)
        statement.bindLong(2, entity.valuePrimitiveInt.toLong())
        statement.bindLong(3, entity.valuePrimitiveByte.toLong())
        statement.bindLong(4, entity.valuePrimitiveShort.toLong())
        statement.bindDouble(5, entity.valueFloat.toDouble())
        statement.bindDouble(6, entity.valueDouble)
        val _tmp: Int = if (entity.valueBoolean) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmpValueNullableBoolean: Boolean? = entity.valueNullableBoolean
        val _tmp_1: Int? = _tmpValueNullableBoolean?.let { if (it) 1 else 0 }
        if (_tmp_1 == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmp_1.toLong())
        }
        statement.bindText(9, entity.valueString)
        val _tmpValueNullableString: String? = entity.valueNullableString
        if (_tmpValueNullableString == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpValueNullableString)
        }
        statement.bindLong(11, entity.valueChar.toLong())
        statement.bindLong(12, entity.variablePrimitiveLong)
        statement.bindLong(13, entity.variablePrimitiveInt.toLong())
        statement.bindLong(14, entity.variablePrimitiveByte.toLong())
        statement.bindLong(15, entity.variablePrimitiveShort.toLong())
        statement.bindDouble(16, entity.variableFloat.toDouble())
        statement.bindDouble(17, entity.variableDouble)
        val _tmp_2: Int = if (entity.variableBoolean) 1 else 0
        statement.bindLong(18, _tmp_2.toLong())
        val _tmpVariableNullableBoolean: Boolean? = entity.variableNullableBoolean
        val _tmp_3: Int? = _tmpVariableNullableBoolean?.let { if (it) 1 else 0 }
        if (_tmp_3 == null) {
          statement.bindNull(19)
        } else {
          statement.bindLong(19, _tmp_3.toLong())
        }
        statement.bindText(20, entity.variableString)
        val _tmpVariableNullableString: String? = entity.variableNullableString
        if (_tmpVariableNullableString == null) {
          statement.bindNull(21)
        } else {
          statement.bindText(21, _tmpVariableNullableString)
        }
        statement.bindLong(22, entity.variableChar.toLong())
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
        val _result: MyEntity
        if (_stmt.step()) {
          _result = __entityStatementConverter_MyEntity(_stmt)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityStatementConverter_MyEntity(statement: SQLiteStatement): MyEntity {
    val _entity: MyEntity
    val _columnIndexOfValuePrimitiveLong: Int = getColumnIndex(statement, "valuePrimitiveLong")
    val _columnIndexOfValuePrimitiveInt: Int = getColumnIndex(statement, "valuePrimitiveInt")
    val _columnIndexOfValuePrimitiveByte: Int = getColumnIndex(statement, "valuePrimitiveByte")
    val _columnIndexOfValuePrimitiveShort: Int = getColumnIndex(statement, "valuePrimitiveShort")
    val _columnIndexOfValueFloat: Int = getColumnIndex(statement, "valueFloat")
    val _columnIndexOfValueDouble: Int = getColumnIndex(statement, "valueDouble")
    val _columnIndexOfValueBoolean: Int = getColumnIndex(statement, "valueBoolean")
    val _columnIndexOfValueNullableBoolean: Int = getColumnIndex(statement, "valueNullableBoolean")
    val _columnIndexOfValueString: Int = getColumnIndex(statement, "valueString")
    val _columnIndexOfValueNullableString: Int = getColumnIndex(statement, "valueNullableString")
    val _columnIndexOfValueChar: Int = getColumnIndex(statement, "valueChar")
    val _columnIndexOfVariablePrimitiveLong: Int = getColumnIndex(statement, "variablePrimitiveLong")
    val _columnIndexOfVariablePrimitiveInt: Int = getColumnIndex(statement, "variablePrimitiveInt")
    val _columnIndexOfVariablePrimitiveByte: Int = getColumnIndex(statement, "variablePrimitiveByte")
    val _columnIndexOfVariablePrimitiveShort: Int = getColumnIndex(statement, "variablePrimitiveShort")
    val _columnIndexOfVariableFloat: Int = getColumnIndex(statement, "variableFloat")
    val _columnIndexOfVariableDouble: Int = getColumnIndex(statement, "variableDouble")
    val _columnIndexOfVariableBoolean: Int = getColumnIndex(statement, "variableBoolean")
    val _columnIndexOfVariableNullableBoolean: Int = getColumnIndex(statement, "variableNullableBoolean")
    val _columnIndexOfVariableString: Int = getColumnIndex(statement, "variableString")
    val _columnIndexOfVariableNullableString: Int = getColumnIndex(statement, "variableNullableString")
    val _columnIndexOfVariableChar: Int = getColumnIndex(statement, "variableChar")
    val _tmpValuePrimitiveLong: Long
    if (_columnIndexOfValuePrimitiveLong == -1) {
      _tmpValuePrimitiveLong = 0
    } else {
      _tmpValuePrimitiveLong = statement.getLong(_columnIndexOfValuePrimitiveLong)
    }
    val _tmpValuePrimitiveInt: Int
    if (_columnIndexOfValuePrimitiveInt == -1) {
      _tmpValuePrimitiveInt = 0
    } else {
      _tmpValuePrimitiveInt = statement.getLong(_columnIndexOfValuePrimitiveInt).toInt()
    }
    val _tmpValuePrimitiveByte: Byte
    if (_columnIndexOfValuePrimitiveByte == -1) {
      _tmpValuePrimitiveByte = 0
    } else {
      _tmpValuePrimitiveByte = statement.getLong(_columnIndexOfValuePrimitiveByte).toByte()
    }
    val _tmpValuePrimitiveShort: Short
    if (_columnIndexOfValuePrimitiveShort == -1) {
      _tmpValuePrimitiveShort = 0
    } else {
      _tmpValuePrimitiveShort = statement.getLong(_columnIndexOfValuePrimitiveShort).toShort()
    }
    val _tmpValueFloat: Float
    if (_columnIndexOfValueFloat == -1) {
      _tmpValueFloat = 0f
    } else {
      _tmpValueFloat = statement.getDouble(_columnIndexOfValueFloat).toFloat()
    }
    val _tmpValueDouble: Double
    if (_columnIndexOfValueDouble == -1) {
      _tmpValueDouble = 0.0
    } else {
      _tmpValueDouble = statement.getDouble(_columnIndexOfValueDouble)
    }
    val _tmpValueBoolean: Boolean
    if (_columnIndexOfValueBoolean == -1) {
      _tmpValueBoolean = false
    } else {
      val _tmp: Int
      _tmp = statement.getLong(_columnIndexOfValueBoolean).toInt()
      _tmpValueBoolean = _tmp != 0
    }
    val _tmpValueNullableBoolean: Boolean?
    if (_columnIndexOfValueNullableBoolean == -1) {
      _tmpValueNullableBoolean = null
    } else {
      val _tmp_1: Int?
      if (statement.isNull(_columnIndexOfValueNullableBoolean)) {
        _tmp_1 = null
      } else {
        _tmp_1 = statement.getLong(_columnIndexOfValueNullableBoolean).toInt()
      }
      _tmpValueNullableBoolean = _tmp_1?.let { it != 0 }
    }
    val _tmpValueString: String
    if (_columnIndexOfValueString == -1) {
      error("Missing column 'valueString' for a NON-NULL value, column not found in result.")
    } else {
      _tmpValueString = statement.getText(_columnIndexOfValueString)
    }
    val _tmpValueNullableString: String?
    if (_columnIndexOfValueNullableString == -1) {
      _tmpValueNullableString = null
    } else {
      if (statement.isNull(_columnIndexOfValueNullableString)) {
        _tmpValueNullableString = null
      } else {
        _tmpValueNullableString = statement.getText(_columnIndexOfValueNullableString)
      }
    }
    val _tmpValueChar: Char
    if (_columnIndexOfValueChar == -1) {
      _tmpValueChar = ' '
    } else {
      _tmpValueChar = statement.getLong(_columnIndexOfValueChar).toChar()
    }
    _entity = MyEntity(_tmpValuePrimitiveLong,_tmpValuePrimitiveInt,_tmpValuePrimitiveByte,_tmpValuePrimitiveShort,_tmpValueFloat,_tmpValueDouble,_tmpValueBoolean,_tmpValueNullableBoolean,_tmpValueString,_tmpValueNullableString,_tmpValueChar)
    if (_columnIndexOfVariablePrimitiveLong != -1) {
      _entity.variablePrimitiveLong = statement.getLong(_columnIndexOfVariablePrimitiveLong)
    }
    if (_columnIndexOfVariablePrimitiveInt != -1) {
      _entity.variablePrimitiveInt = statement.getLong(_columnIndexOfVariablePrimitiveInt).toInt()
    }
    if (_columnIndexOfVariablePrimitiveByte != -1) {
      _entity.variablePrimitiveByte = statement.getLong(_columnIndexOfVariablePrimitiveByte).toByte()
    }
    if (_columnIndexOfVariablePrimitiveShort != -1) {
      _entity.variablePrimitiveShort = statement.getLong(_columnIndexOfVariablePrimitiveShort).toShort()
    }
    if (_columnIndexOfVariableFloat != -1) {
      _entity.variableFloat = statement.getDouble(_columnIndexOfVariableFloat).toFloat()
    }
    if (_columnIndexOfVariableDouble != -1) {
      _entity.variableDouble = statement.getDouble(_columnIndexOfVariableDouble)
    }
    if (_columnIndexOfVariableBoolean != -1) {
      val _tmp_2: Int
      _tmp_2 = statement.getLong(_columnIndexOfVariableBoolean).toInt()
      _entity.variableBoolean = _tmp_2 != 0
    }
    if (_columnIndexOfVariableNullableBoolean != -1) {
      val _tmp_3: Int?
      if (statement.isNull(_columnIndexOfVariableNullableBoolean)) {
        _tmp_3 = null
      } else {
        _tmp_3 = statement.getLong(_columnIndexOfVariableNullableBoolean).toInt()
      }
      _entity.variableNullableBoolean = _tmp_3?.let { it != 0 }
    }
    if (_columnIndexOfVariableString != -1) {
      _entity.variableString = statement.getText(_columnIndexOfVariableString)
    }
    if (_columnIndexOfVariableNullableString != -1) {
      if (statement.isNull(_columnIndexOfVariableNullableString)) {
        _entity.variableNullableString = null
      } else {
        _entity.variableNullableString = statement.getText(_columnIndexOfVariableNullableString)
      }
    }
    if (_columnIndexOfVariableChar != -1) {
      _entity.variableChar = statement.getLong(_columnIndexOfVariableChar).toChar()
    }
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
