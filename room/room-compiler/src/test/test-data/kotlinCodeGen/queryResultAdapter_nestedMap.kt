import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun singleNested(): Map<Artist, Map<Album, List<Song>>> {
    val _sql: String =
        "SELECT * FROM Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _cursorIndexOfArtistName: Int = getColumnIndexOrThrow(_stmt, "artistName")
        val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_stmt, "albumId")
        val _cursorIndexOfAlbumName: Int = getColumnIndexOrThrow(_stmt, "albumName")
        val _cursorIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_stmt, "albumArtist")
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _cursorIndexOfSongArtist: Int = getColumnIndexOrThrow(_stmt, "songArtist")
        val _result: MutableMap<Artist, MutableMap<Album, MutableList<Song>>> =
            LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          val _tmpArtistName: String
          _tmpArtistName = _stmt.getText(_cursorIndexOfArtistName)
          _key = Artist(_tmpArtistId,_tmpArtistName)
          val _values: MutableMap<Album, MutableList<Song>>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = LinkedHashMap<Album, MutableList<Song>>()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndexOfAlbumId) && _stmt.isNull(_cursorIndexOfAlbumName) &&
              _stmt.isNull(_cursorIndexOfAlbumArtist)) {
            continue
          }
          val _key_1: Album
          val _tmpAlbumId: String
          _tmpAlbumId = _stmt.getText(_cursorIndexOfAlbumId)
          val _tmpAlbumName: String
          _tmpAlbumName = _stmt.getText(_cursorIndexOfAlbumName)
          val _tmpAlbumArtist: String
          _tmpAlbumArtist = _stmt.getText(_cursorIndexOfAlbumArtist)
          _key_1 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
          val _values_1: MutableList<Song>
          if (_values.containsKey(_key_1)) {
            _values_1 = _values.getValue(_key_1)
          } else {
            _values_1 = mutableListOf()
            _values.put(_key_1, _values_1)
          }
          if (_stmt.isNull(_cursorIndexOfSongId) && _stmt.isNull(_cursorIndexOfAlbum) &&
              _stmt.isNull(_cursorIndexOfSongArtist)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_cursorIndexOfAlbum)
          val _tmpSongArtist: String
          _tmpSongArtist = _stmt.getText(_cursorIndexOfSongArtist)
          _value = Song(_tmpSongId,_tmpAlbum,_tmpSongArtist)
          _values_1.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun doubleNested(): Map<Playlist, Map<Artist, Map<Album, List<Song>>>> {
    val _sql: String =
        "SELECT * FROM Playlist JOIN (Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist)ON Playlist.playlistArtist = Artist.artistName"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _cursorIndexOfPlaylistArtist: Int = getColumnIndexOrThrow(_stmt, "playlistArtist")
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _cursorIndexOfArtistName: Int = getColumnIndexOrThrow(_stmt, "artistName")
        val _cursorIndexOfAlbumId: Int = getColumnIndexOrThrow(_stmt, "albumId")
        val _cursorIndexOfAlbumName: Int = getColumnIndexOrThrow(_stmt, "albumName")
        val _cursorIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_stmt, "albumArtist")
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _cursorIndexOfSongArtist: Int = getColumnIndexOrThrow(_stmt, "songArtist")
        val _result: MutableMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>>
            = LinkedHashMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>>()
        while (_stmt.step()) {
          val _key: Playlist
          val _tmpPlaylistId: String
          _tmpPlaylistId = _stmt.getText(_cursorIndexOfPlaylistId)
          val _tmpPlaylistArtist: String
          _tmpPlaylistArtist = _stmt.getText(_cursorIndexOfPlaylistArtist)
          _key = Playlist(_tmpPlaylistId,_tmpPlaylistArtist)
          val _values: MutableMap<Artist, MutableMap<Album, MutableList<Song>>>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndexOfArtistId) && _stmt.isNull(_cursorIndexOfArtistName)) {
            continue
          }
          val _key_1: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          val _tmpArtistName: String
          _tmpArtistName = _stmt.getText(_cursorIndexOfArtistName)
          _key_1 = Artist(_tmpArtistId,_tmpArtistName)
          val _values_1: MutableMap<Album, MutableList<Song>>
          if (_values.containsKey(_key_1)) {
            _values_1 = _values.getValue(_key_1)
          } else {
            _values_1 = LinkedHashMap<Album, MutableList<Song>>()
            _values.put(_key_1, _values_1)
          }
          if (_stmt.isNull(_cursorIndexOfAlbumId) && _stmt.isNull(_cursorIndexOfAlbumName) &&
              _stmt.isNull(_cursorIndexOfAlbumArtist)) {
            continue
          }
          val _key_2: Album
          val _tmpAlbumId: String
          _tmpAlbumId = _stmt.getText(_cursorIndexOfAlbumId)
          val _tmpAlbumName: String
          _tmpAlbumName = _stmt.getText(_cursorIndexOfAlbumName)
          val _tmpAlbumArtist: String
          _tmpAlbumArtist = _stmt.getText(_cursorIndexOfAlbumArtist)
          _key_2 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
          val _values_2: MutableList<Song>
          if (_values_1.containsKey(_key_2)) {
            _values_2 = _values_1.getValue(_key_2)
          } else {
            _values_2 = mutableListOf()
            _values_1.put(_key_2, _values_2)
          }
          if (_stmt.isNull(_cursorIndexOfSongId) && _stmt.isNull(_cursorIndexOfAlbum) &&
              _stmt.isNull(_cursorIndexOfSongArtist)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_cursorIndexOfAlbum)
          val _tmpSongArtist: String
          _tmpSongArtist = _stmt.getText(_cursorIndexOfSongArtist)
          _value = Song(_tmpSongId,_tmpAlbum,_tmpSongArtist)
          _values_2.add(_value)
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
