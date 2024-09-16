import androidx.room.RoomDatabase
import androidx.room.util.ByteArrayWrapper
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.room.util.recursiveFetchMap
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.ByteArray
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.mutableMapOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getSongsWithArtist(): SongWithArtist {
    val _sql: String = "SELECT * FROM Song"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _collectionArtist: MutableMap<ByteArrayWrapper, Artist?> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpKey: ByteArrayWrapper
          _tmpKey = ByteArrayWrapper(_stmt.getBlob(_cursorIndexOfArtistKey))
          _collectionArtist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipArtistAsArtist(_connection, _collectionArtist)
        val _result: SongWithArtist
        if (_stmt.step()) {
          val _tmpSong: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_cursorIndexOfSongId)
          val _tmpArtistKey: ByteArray
          _tmpArtistKey = _stmt.getBlob(_cursorIndexOfArtistKey)
          _tmpSong = Song(_tmpSongId,_tmpArtistKey)
          val _tmpArtist: Artist?
          val _tmpKey_1: ByteArrayWrapper
          _tmpKey_1 = ByteArrayWrapper(_stmt.getBlob(_cursorIndexOfArtistKey))
          _tmpArtist = _collectionArtist.get(_tmpKey_1)
          if (_tmpArtist == null) {
            error("Relationship item 'artist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'artistKey' and entityColumn named 'artistId'.")
          }
          _result = SongWithArtist(_tmpSong,_tmpArtist)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <SongWithArtist>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipArtistAsArtist(_connection: SQLiteConnection,
      _map: MutableMap<ByteArrayWrapper, Artist?>) {
    val __mapKeySet: Set<ByteArrayWrapper> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, false) { _tmpMap ->
        __fetchRelationshipArtistAsArtist(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `artistId` FROM `Artist` WHERE `artistId` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: ByteArrayWrapper in __mapKeySet) {
      _stmt.bindBlob(_argIndex, _item.array)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "artistId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _cursorIndexOfArtistId: Int = 0
      while (_stmt.step()) {
        val _tmpKey: ByteArrayWrapper
        _tmpKey = ByteArrayWrapper(_stmt.getBlob(_itemKeyIndex))
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Artist
          val _tmpArtistId: ByteArray
          _tmpArtistId = _stmt.getBlob(_cursorIndexOfArtistId)
          _item_1 = Artist(_tmpArtistId)
          _map.put(_tmpKey, _item_1)
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
