import androidx.room3.RoomDatabase
import androidx.room3.util.appendRowValuePlaceholders
import androidx.room3.util.getColumnIndex
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.room3.util.recursiveFetchMap
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.Pair
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getParentWithChild(): ParentWithChild {
    val _sql: String = "SELECT * FROM Parent"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfP1: Int = getColumnIndexOrThrow(_stmt, "p1")
        val _columnIndexOfP2: Int = getColumnIndexOrThrow(_stmt, "p2")
        val _collectionChild: MutableMap<Pair<Long, String>, Child?> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpPartialKey_0: Long
          _tmpPartialKey_0 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1: String
          _tmpPartialKey_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
          _collectionChild.put(_compositeKey, null)
        }
        _stmt.reset()
        __fetchRelationshipChildAsChild(_connection, _collectionChild)
        val _result: ParentWithChild
        if (_stmt.step()) {
          val _tmpParent: Parent
          val _tmpP1: Long
          _tmpP1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpP2: String
          _tmpP2 = _stmt.getText(_columnIndexOfP2)
          _tmpParent = Parent(_tmpP1,_tmpP2)
          val _tmpChild: Child?
          val _tmpPartialKey_0_1: Long
          _tmpPartialKey_0_1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1_1: String
          _tmpPartialKey_1_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0_1, _tmpPartialKey_1_1)
          _tmpChild = _collectionChild.get(_compositeKey_1)
          if (_tmpChild == null) {
            error("Relationship item 'child' was expected to be NON-NULL but is NULL in @Relation involving parent columns named 'p1', 'p2' and entityColumns named 'c1', 'c2''.")
          }
          _result = ParentWithChild(_tmpParent,_tmpChild)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'ParentWithChild'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getParentWithChildren(): ParentWithChildren {
    val _sql: String = "SELECT * FROM Parent"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfP1: Int = getColumnIndexOrThrow(_stmt, "p1")
        val _columnIndexOfP2: Int = getColumnIndexOrThrow(_stmt, "p2")
        val _collectionChildren: MutableMap<Pair<Long, String>, MutableList<Child>> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpPartialKey_0: Long
          _tmpPartialKey_0 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1: String
          _tmpPartialKey_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
          if (!_collectionChildren.containsKey(_compositeKey)) {
            _collectionChildren.put(_compositeKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipChildAsChild_1(_connection, _collectionChildren)
        val _result: ParentWithChildren
        if (_stmt.step()) {
          val _tmpParent: Parent
          val _tmpP1: Long
          _tmpP1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpP2: String
          _tmpP2 = _stmt.getText(_columnIndexOfP2)
          _tmpParent = Parent(_tmpP1,_tmpP2)
          val _tmpChildrenCollection: MutableList<Child>
          val _tmpPartialKey_0_1: Long
          _tmpPartialKey_0_1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1_1: String
          _tmpPartialKey_1_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0_1, _tmpPartialKey_1_1)
          _tmpChildrenCollection = _collectionChildren.getValue(_compositeKey_1)
          _result = ParentWithChildren(_tmpParent,_tmpChildrenCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'ParentWithChildren'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getParentWithChildrenJunction(): ParentWithChildrenJunction {
    val _sql: String = "SELECT * FROM Parent"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfP1: Int = getColumnIndexOrThrow(_stmt, "p1")
        val _columnIndexOfP2: Int = getColumnIndexOrThrow(_stmt, "p2")
        val _collectionChildren: MutableMap<Pair<Long, String>, MutableList<Child>> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpPartialKey_0: Long
          _tmpPartialKey_0 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1: String
          _tmpPartialKey_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
          if (!_collectionChildren.containsKey(_compositeKey)) {
            _collectionChildren.put(_compositeKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipChildAsChildWithParentChildXRef(_connection, _collectionChildren)
        val _result: ParentWithChildrenJunction
        if (_stmt.step()) {
          val _tmpParent: Parent
          val _tmpP1: Long
          _tmpP1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpP2: String
          _tmpP2 = _stmt.getText(_columnIndexOfP2)
          _tmpParent = Parent(_tmpP1,_tmpP2)
          val _tmpChildrenCollection: MutableList<Child>
          val _tmpPartialKey_0_1: Long
          _tmpPartialKey_0_1 = _stmt.getLong(_columnIndexOfP1)
          val _tmpPartialKey_1_1: String
          _tmpPartialKey_1_1 = _stmt.getText(_columnIndexOfP2)
          val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0_1, _tmpPartialKey_1_1)
          _tmpChildrenCollection = _collectionChildren.getValue(_compositeKey_1)
          _result = ParentWithChildrenJunction(_tmpParent,_tmpChildrenCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type 'ParentWithChildrenJunction'.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private suspend fun __fetchRelationshipChildAsChild(_connection: SQLiteConnection, _map: MutableMap<Pair<Long, String>, Child?>) {
    val __mapKeySet: Set<Pair<Long, String>> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, false) { _tmpMap ->
        __fetchRelationshipChildAsChild(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `c1`, `c2` FROM `Child`")
    _stringBuilder.append(" WHERE (`c1`, `c2`) IN (")
    val _inputSize: Int = __mapKeySet.size * 2
    appendRowValuePlaceholders(_stringBuilder, _inputSize, 2)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_compositeKey in __mapKeySet) {
      val _keyValue_0: Long = _compositeKey.first
      _stmt.bindLong(_argIndex, _keyValue_0)
      _argIndex++
      val _keyValue_1: String = _compositeKey.second
      _stmt.bindText(_argIndex, _keyValue_1)
      _argIndex++
    }
    try {
      val _itemKeyIndex_0: Int = getColumnIndex(_stmt, "c1")
      val _itemKeyIndex_1: Int = getColumnIndex(_stmt, "c2")
      if (_itemKeyIndex_0 == -1 || _itemKeyIndex_1 == -1) {
        return
      }
      val _columnIndexOfC1: Int = 0
      val _columnIndexOfC2: Int = 1
      while (_stmt.step()) {
        val _tmpPartialKey_0: Long
        _tmpPartialKey_0 = _stmt.getLong(_itemKeyIndex_0)
        val _tmpPartialKey_1: String
        _tmpPartialKey_1 = _stmt.getText(_itemKeyIndex_1)
        val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
        if (_map.containsKey(_compositeKey_1)) {
          val _item: Child
          val _tmpC1: Long
          _tmpC1 = _stmt.getLong(_columnIndexOfC1)
          val _tmpC2: String
          _tmpC2 = _stmt.getText(_columnIndexOfC2)
          _item = Child(_tmpC1,_tmpC2)
          _map.put(_compositeKey_1, _item)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private suspend fun __fetchRelationshipChildAsChild_1(_connection: SQLiteConnection, _map: MutableMap<Pair<Long, String>, MutableList<Child>>) {
    val __mapKeySet: Set<Pair<Long, String>> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, true) { _tmpMap ->
        __fetchRelationshipChildAsChild_1(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `c1`, `c2` FROM `Child`")
    _stringBuilder.append(" WHERE (`c1`, `c2`) IN (")
    val _inputSize: Int = __mapKeySet.size * 2
    appendRowValuePlaceholders(_stringBuilder, _inputSize, 2)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_compositeKey in __mapKeySet) {
      val _keyValue_0: Long = _compositeKey.first
      _stmt.bindLong(_argIndex, _keyValue_0)
      _argIndex++
      val _keyValue_1: String = _compositeKey.second
      _stmt.bindText(_argIndex, _keyValue_1)
      _argIndex++
    }
    try {
      val _itemKeyIndex_0: Int = getColumnIndex(_stmt, "c1")
      val _itemKeyIndex_1: Int = getColumnIndex(_stmt, "c2")
      if (_itemKeyIndex_0 == -1 || _itemKeyIndex_1 == -1) {
        return
      }
      val _columnIndexOfC1: Int = 0
      val _columnIndexOfC2: Int = 1
      while (_stmt.step()) {
        val _tmpPartialKey_0: Long
        _tmpPartialKey_0 = _stmt.getLong(_itemKeyIndex_0)
        val _tmpPartialKey_1: String
        _tmpPartialKey_1 = _stmt.getText(_itemKeyIndex_1)
        val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
        val _tmpRelation: MutableList<Child>? = _map.get(_compositeKey_1)
        if (_tmpRelation != null) {
          val _item: Child
          val _tmpC1: Long
          _tmpC1 = _stmt.getLong(_columnIndexOfC1)
          val _tmpC2: String
          _tmpC2 = _stmt.getText(_columnIndexOfC2)
          _item = Child(_tmpC1,_tmpC2)
          _tmpRelation.add(_item)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private suspend fun __fetchRelationshipChildAsChildWithParentChildXRef(_connection: SQLiteConnection, _map: MutableMap<Pair<Long, String>, MutableList<Child>>) {
    val __mapKeySet: Set<Pair<Long, String>> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, true) { _tmpMap ->
        __fetchRelationshipChildAsChildWithParentChildXRef(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `Child`.`c1` AS `c1`, `Child`.`c2` AS `c2`, _junction.`pk1`, _junction.`pk2` FROM `ParentChildXRef` AS _junction INNER JOIN `Child` ON _junction.`ck1` = `Child`.`c1` AND _junction.`ck2` = `Child`.`c2`")
    _stringBuilder.append(" WHERE (_junction.`pk1`, _junction.`pk2`) IN (")
    val _inputSize: Int = __mapKeySet.size * 2
    appendRowValuePlaceholders(_stringBuilder, _inputSize, 2)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_compositeKey in __mapKeySet) {
      val _keyValue_0: Long = _compositeKey.first
      _stmt.bindLong(_argIndex, _keyValue_0)
      _argIndex++
      val _keyValue_1: String = _compositeKey.second
      _stmt.bindText(_argIndex, _keyValue_1)
      _argIndex++
    }
    try {
      val _itemKeyIndex_0: Int = 2
      val _itemKeyIndex_1: Int = 3
      if (_itemKeyIndex_0 == -1 || _itemKeyIndex_1 == -1) {
        return
      }
      val _columnIndexOfC1: Int = 0
      val _columnIndexOfC2: Int = 1
      while (_stmt.step()) {
        val _tmpPartialKey_0: Long
        _tmpPartialKey_0 = _stmt.getLong(_itemKeyIndex_0)
        val _tmpPartialKey_1: String
        _tmpPartialKey_1 = _stmt.getText(_itemKeyIndex_1)
        val _compositeKey_1: Pair<Long, String> = Pair(_tmpPartialKey_0, _tmpPartialKey_1)
        val _tmpRelation: MutableList<Child>? = _map.get(_compositeKey_1)
        if (_tmpRelation != null) {
          val _item: Child
          val _tmpC1: Long
          _tmpC1 = _stmt.getLong(_columnIndexOfC1)
          val _tmpC2: String
          _tmpC2 = _stmt.getText(_columnIndexOfC2)
          _item = Child(_tmpC1,_tmpC2)
          _tmpRelation.add(_item)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
