import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.room.util.recursiveFetchMap
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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
        val _collectionArtist: MutableMap<Long, Artist?> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpKey: Long?
          if (_stmt.isNull(_cursorIndexOfArtistKey)) {
            _tmpKey = null
          } else {
            _tmpKey = _stmt.getLong(_cursorIndexOfArtistKey)
          }
          if (_tmpKey != null) {
            _collectionArtist.put(_tmpKey, null)
          }
        }
        _stmt.reset()
        __fetchRelationshipArtistAsArtist(_connection, _collectionArtist)
        val _result: SongWithArtist
        if (_stmt.step()) {
          val _tmpSong: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_cursorIndexOfSongId)
          val _tmpArtistKey: Long?
          if (_stmt.isNull(_cursorIndexOfArtistKey)) {
            _tmpArtistKey = null
          } else {
            _tmpArtistKey = _stmt.getLong(_cursorIndexOfArtistKey)
          }
          _tmpSong = Song(_tmpSongId,_tmpArtistKey)
          val _tmpArtist: Artist?
          val _tmpKey_1: Long?
          if (_stmt.isNull(_cursorIndexOfArtistKey)) {
            _tmpKey_1 = null
          } else {
            _tmpKey_1 = _stmt.getLong(_cursorIndexOfArtistKey)
          }
          if (_tmpKey_1 != null) {
            _tmpArtist = _collectionArtist.get(_tmpKey_1)
          } else {
            _tmpArtist = null
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

  public override fun getArtistAndSongs(): ArtistAndSongs {
    val _sql: String = "SELECT * FROM Artist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _collectionSongs: MutableMap<Long, MutableList<Song>> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_cursorIndexOfArtistId)
          if (!_collectionSongs.containsKey(_tmpKey)) {
            _collectionSongs.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipSongAsSong(_connection, _collectionSongs)
        val _result: ArtistAndSongs
        if (_stmt.step()) {
          val _tmpArtist: Artist
          val _tmpArtistId: Long
          _tmpArtistId = _stmt.getLong(_cursorIndexOfArtistId)
          _tmpArtist = Artist(_tmpArtistId)
          val _tmpSongsCollection: MutableList<Song>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_cursorIndexOfArtistId)
          _tmpSongsCollection = _collectionSongs.getValue(_tmpKey_1)
          _result = ArtistAndSongs(_tmpArtist,_tmpSongsCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <ArtistAndSongs>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPlaylistAndSongs(): PlaylistAndSongs {
    val _sql: String = "SELECT * FROM Playlist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _collectionSongs: MutableMap<Long, MutableList<Song>> = mutableMapOf()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_cursorIndexOfPlaylistId)
          if (!_collectionSongs.containsKey(_tmpKey)) {
            _collectionSongs.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipSongAsSong_1(_connection, _collectionSongs)
        val _result: PlaylistAndSongs
        if (_stmt.step()) {
          val _tmpPlaylist: Playlist
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_cursorIndexOfPlaylistId)
          _tmpPlaylist = Playlist(_tmpPlaylistId)
          val _tmpSongsCollection: MutableList<Song>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_cursorIndexOfPlaylistId)
          _tmpSongsCollection = _collectionSongs.getValue(_tmpKey_1)
          _result = PlaylistAndSongs(_tmpPlaylist,_tmpSongsCollection)
        } else {
          error("The query result was empty, but expected a single row to return a NON-NULL object of type <PlaylistAndSongs>.")
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipArtistAsArtist(_connection: SQLiteConnection,
      _map: MutableMap<Long, Artist?>) {
    val __mapKeySet: Set<Long> = _map.keys
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
    for (_item: Long in __mapKeySet) {
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "artistId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _cursorIndexOfArtistId: Int = 0
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Artist?
          val _tmpArtistId: Long
          _tmpArtistId = _stmt.getLong(_cursorIndexOfArtistId)
          _item_1 = Artist(_tmpArtistId)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong(_connection: SQLiteConnection,
      _map: MutableMap<Long, MutableList<Song>>) {
    val __mapKeySet: Set<Long> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, true) { _tmpMap ->
        __fetchRelationshipSongAsSong(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `songId`,`artistKey` FROM `Song` WHERE `artistKey` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: Long in __mapKeySet) {
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "artistKey")
      if (_itemKeyIndex == -1) {
        return
      }
      val _cursorIndexOfSongId: Int = 0
      val _cursorIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long?
        if (_stmt.isNull(_itemKeyIndex)) {
          _tmpKey = null
        } else {
          _tmpKey = _stmt.getLong(_itemKeyIndex)
        }
        if (_tmpKey != null) {
          val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
          if (_tmpRelation != null) {
            val _item_1: Song
            val _tmpSongId: Long
            _tmpSongId = _stmt.getLong(_cursorIndexOfSongId)
            val _tmpArtistKey: Long?
            if (_stmt.isNull(_cursorIndexOfArtistKey)) {
              _tmpArtistKey = null
            } else {
              _tmpArtistKey = _stmt.getLong(_cursorIndexOfArtistKey)
            }
            _item_1 = Song(_tmpSongId,_tmpArtistKey)
            _tmpRelation.add(_item_1)
          }
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong_1(_connection: SQLiteConnection,
      _map: MutableMap<Long, MutableList<Song>>) {
    val __mapKeySet: Set<Long> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchMap(_map, true) { _tmpMap ->
        __fetchRelationshipSongAsSong_1(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `Song`.`songId` AS `songId`,`Song`.`artistKey` AS `artistKey`,_junction.`playlistKey` FROM `PlaylistSongXRef` AS _junction INNER JOIN `Song` ON (_junction.`songKey` = `Song`.`songId`) WHERE _junction.`playlistKey` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: Long in __mapKeySet) {
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      // _junction.playlistKey
      val _itemKeyIndex: Int = 2
      if (_itemKeyIndex == -1) {
        return
      }
      val _cursorIndexOfSongId: Int = 0
      val _cursorIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_cursorIndexOfSongId)
          val _tmpArtistKey: Long?
          if (_stmt.isNull(_cursorIndexOfArtistKey)) {
            _tmpArtistKey = null
          } else {
            _tmpArtistKey = _stmt.getLong(_cursorIndexOfArtistKey)
          }
          _item_1 = Song(_tmpSongId,_tmpArtistKey)
          _tmpRelation.add(_item_1)
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
