import androidx.paging.DataSource
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.paging.LimitOffsetDataSource
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.recursiveFetchMap
import androidx.room.util.toSQLiteConnection
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

  public override fun getSongsWithArtist(): DataSource.Factory<Int, SongWithArtist> {
    val _sql: String = "SELECT * FROM Song"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return object : DataSource.Factory<Int, SongWithArtist>() {
      public override fun create(): LimitOffsetDataSource<SongWithArtist> {
        val _connection: SQLiteConnection = toSQLiteConnection(__db.openHelper.writableDatabase)
        return object : LimitOffsetDataSource<SongWithArtist>(__db, _statement, false, true, "Artist", "Song") {
          protected override fun convertRows(statement: SQLiteStatement): List<SongWithArtist> {
            val _columnIndexOfSongId: Int = getColumnIndexOrThrow(statement, "songId")
            val _columnIndexOfArtistKey: Int = getColumnIndexOrThrow(statement, "artistKey")
            val _collectionArtist: MutableMap<Long, Artist?> = mutableMapOf()
            while (statement.step()) {
              val _tmpKey: Long
              _tmpKey = statement.getLong(_columnIndexOfArtistKey)
              _collectionArtist.put(_tmpKey, null)
            }
            statement.reset()
            __fetchRelationshipArtistAsArtist(_connection, _collectionArtist)
            val _res: MutableList<SongWithArtist> = mutableListOf()
            while (statement.step()) {
              val _item: SongWithArtist
              val _tmpSong: Song
              val _tmpSongId: Long
              _tmpSongId = statement.getLong(_columnIndexOfSongId)
              val _tmpArtistKey: Long
              _tmpArtistKey = statement.getLong(_columnIndexOfArtistKey)
              _tmpSong = Song(_tmpSongId,_tmpArtistKey)
              val _tmpArtist: Artist?
              val _tmpKey_1: Long
              _tmpKey_1 = statement.getLong(_columnIndexOfArtistKey)
              _tmpArtist = _collectionArtist.get(_tmpKey_1)
              if (_tmpArtist == null) {
                error("Relationship item 'artist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'artistKey' and entityColumn named 'artistId'.")
              }
              _item = SongWithArtist(_tmpSong,_tmpArtist)
              _res.add(_item)
            }
            return _res
          }
        }
      }
    }
  }

  public override fun getArtistAndSongs(): DataSource.Factory<Int, ArtistAndSongs> {
    val _sql: String = "SELECT * FROM Artist"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return object : DataSource.Factory<Int, ArtistAndSongs>() {
      public override fun create(): LimitOffsetDataSource<ArtistAndSongs> {
        val _connection: SQLiteConnection = toSQLiteConnection(__db.openHelper.writableDatabase)
        return object : LimitOffsetDataSource<ArtistAndSongs>(__db, _statement, false, true, "Song", "Artist") {
          protected override fun convertRows(statement: SQLiteStatement): List<ArtistAndSongs> {
            val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(statement, "artistId")
            val _collectionSongs: MutableMap<Long, MutableList<Song>> = mutableMapOf()
            while (statement.step()) {
              val _tmpKey: Long
              _tmpKey = statement.getLong(_columnIndexOfArtistId)
              if (!_collectionSongs.containsKey(_tmpKey)) {
                _collectionSongs.put(_tmpKey, mutableListOf())
              }
            }
            statement.reset()
            __fetchRelationshipSongAsSong(_connection, _collectionSongs)
            val _res: MutableList<ArtistAndSongs> = mutableListOf()
            while (statement.step()) {
              val _item: ArtistAndSongs
              val _tmpArtist: Artist
              val _tmpArtistId: Long
              _tmpArtistId = statement.getLong(_columnIndexOfArtistId)
              _tmpArtist = Artist(_tmpArtistId)
              val _tmpSongsCollection: MutableList<Song>
              val _tmpKey_1: Long
              _tmpKey_1 = statement.getLong(_columnIndexOfArtistId)
              _tmpSongsCollection = _collectionSongs.getValue(_tmpKey_1)
              _item = ArtistAndSongs(_tmpArtist,_tmpSongsCollection)
              _res.add(_item)
            }
            return _res
          }
        }
      }
    }
  }

  public override fun getPlaylistAndSongs(): DataSource.Factory<Int, PlaylistAndSongs> {
    val _sql: String = "SELECT * FROM Playlist"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    return object : DataSource.Factory<Int, PlaylistAndSongs>() {
      public override fun create(): LimitOffsetDataSource<PlaylistAndSongs> {
        val _connection: SQLiteConnection = toSQLiteConnection(__db.openHelper.writableDatabase)
        return object : LimitOffsetDataSource<PlaylistAndSongs>(__db, _statement, false, true, "PlaylistSongXRef", "Song", "Playlist") {
          protected override fun convertRows(statement: SQLiteStatement): List<PlaylistAndSongs> {
            val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(statement, "playlistId")
            val _collectionSongs: MutableMap<Long, MutableList<Song>> = mutableMapOf()
            while (statement.step()) {
              val _tmpKey: Long
              _tmpKey = statement.getLong(_columnIndexOfPlaylistId)
              if (!_collectionSongs.containsKey(_tmpKey)) {
                _collectionSongs.put(_tmpKey, mutableListOf())
              }
            }
            statement.reset()
            __fetchRelationshipSongAsSong_1(_connection, _collectionSongs)
            val _res: MutableList<PlaylistAndSongs> = mutableListOf()
            while (statement.step()) {
              val _item: PlaylistAndSongs
              val _tmpPlaylist: Playlist
              val _tmpPlaylistId: Long
              _tmpPlaylistId = statement.getLong(_columnIndexOfPlaylistId)
              _tmpPlaylist = Playlist(_tmpPlaylistId)
              val _tmpSongsCollection: MutableList<Song>
              val _tmpKey_1: Long
              _tmpKey_1 = statement.getLong(_columnIndexOfPlaylistId)
              _tmpSongsCollection = _collectionSongs.getValue(_tmpKey_1)
              _item = PlaylistAndSongs(_tmpPlaylist,_tmpSongsCollection)
              _res.add(_item)
            }
            return _res
          }
        }
      }
    }
  }

  private fun __fetchRelationshipArtistAsArtist(_connection: SQLiteConnection, _map: MutableMap<Long, Artist?>) {
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
      val _columnIndexOfArtistId: Int = 0
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Artist
          val _tmpArtistId: Long
          _tmpArtistId = _stmt.getLong(_columnIndexOfArtistId)
          _item_1 = Artist(_tmpArtistId)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong(_connection: SQLiteConnection, _map: MutableMap<Long, MutableList<Song>>) {
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
      val _columnIndexOfSongId: Int = 0
      val _columnIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId)
          val _tmpArtistKey: Long
          _tmpArtistKey = _stmt.getLong(_columnIndexOfArtistKey)
          _item_1 = Song(_tmpSongId,_tmpArtistKey)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipSongAsSong_1(_connection: SQLiteConnection, _map: MutableMap<Long, MutableList<Song>>) {
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
      val _columnIndexOfSongId: Int = 0
      val _columnIndexOfArtistKey: Int = 1
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<Song>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Song
          val _tmpSongId: Long
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId)
          val _tmpArtistKey: Long
          _tmpArtistKey = _stmt.getLong(_columnIndexOfArtistKey)
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
