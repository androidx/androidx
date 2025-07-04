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
    val _sql: String = "SELECT * FROM Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _columnIndexOfArtistName: Int = getColumnIndexOrThrow(_stmt, "artistName")
        val _columnIndexOfAlbumId: Int = getColumnIndexOrThrow(_stmt, "albumId")
        val _columnIndexOfAlbumName: Int = getColumnIndexOrThrow(_stmt, "albumName")
        val _columnIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_stmt, "albumArtist")
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfSongArtist: Int = getColumnIndexOrThrow(_stmt, "songArtist")
        val _result: MutableMap<Artist, MutableMap<Album, MutableList<Song>>> = LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_columnIndexOfArtistId)
          val _tmpArtistName: String
          _tmpArtistName = _stmt.getText(_columnIndexOfArtistName)
          _key = Artist(_tmpArtistId,_tmpArtistName)
          val _values: MutableMap<Album, MutableList<Song>>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = LinkedHashMap<Album, MutableList<Song>>()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_columnIndexOfAlbumId) && _stmt.isNull(_columnIndexOfAlbumName) && _stmt.isNull(_columnIndexOfAlbumArtist)) {
            continue
          }
          val _key_1: Album
          val _tmpAlbumId: String
          _tmpAlbumId = _stmt.getText(_columnIndexOfAlbumId)
          val _tmpAlbumName: String
          _tmpAlbumName = _stmt.getText(_columnIndexOfAlbumName)
          val _tmpAlbumArtist: String
          _tmpAlbumArtist = _stmt.getText(_columnIndexOfAlbumArtist)
          _key_1 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
          val _values_1: MutableList<Song>
          if (_values.containsKey(_key_1)) {
            _values_1 = _values.getValue(_key_1)
          } else {
            _values_1 = mutableListOf()
            _values.put(_key_1, _values_1)
          }
          if (_stmt.isNull(_columnIndexOfSongId) && _stmt.isNull(_columnIndexOfAlbum) && _stmt.isNull(_columnIndexOfSongArtist)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_columnIndexOfSongId)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          val _tmpSongArtist: String
          _tmpSongArtist = _stmt.getText(_columnIndexOfSongArtist)
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
    val _sql: String = "SELECT * FROM Playlist JOIN (Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) ON Artist.artistName = Album.albumArtist)ON Playlist.playlistArtist = Artist.artistName"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfPlaylistArtist: Int = getColumnIndexOrThrow(_stmt, "playlistArtist")
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _columnIndexOfArtistName: Int = getColumnIndexOrThrow(_stmt, "artistName")
        val _columnIndexOfAlbumId: Int = getColumnIndexOrThrow(_stmt, "albumId")
        val _columnIndexOfAlbumName: Int = getColumnIndexOrThrow(_stmt, "albumName")
        val _columnIndexOfAlbumArtist: Int = getColumnIndexOrThrow(_stmt, "albumArtist")
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfSongArtist: Int = getColumnIndexOrThrow(_stmt, "songArtist")
        val _result: MutableMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>> = LinkedHashMap<Playlist, MutableMap<Artist, MutableMap<Album, MutableList<Song>>>>()
        while (_stmt.step()) {
          val _key: Playlist
          val _tmpPlaylistId: String
          _tmpPlaylistId = _stmt.getText(_columnIndexOfPlaylistId)
          val _tmpPlaylistArtist: String
          _tmpPlaylistArtist = _stmt.getText(_columnIndexOfPlaylistArtist)
          _key = Playlist(_tmpPlaylistId,_tmpPlaylistArtist)
          val _values: MutableMap<Artist, MutableMap<Album, MutableList<Song>>>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = LinkedHashMap<Artist, MutableMap<Album, MutableList<Song>>>()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_columnIndexOfArtistId) && _stmt.isNull(_columnIndexOfArtistName)) {
            continue
          }
          val _key_1: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_columnIndexOfArtistId)
          val _tmpArtistName: String
          _tmpArtistName = _stmt.getText(_columnIndexOfArtistName)
          _key_1 = Artist(_tmpArtistId,_tmpArtistName)
          val _values_1: MutableMap<Album, MutableList<Song>>
          if (_values.containsKey(_key_1)) {
            _values_1 = _values.getValue(_key_1)
          } else {
            _values_1 = LinkedHashMap<Album, MutableList<Song>>()
            _values.put(_key_1, _values_1)
          }
          if (_stmt.isNull(_columnIndexOfAlbumId) && _stmt.isNull(_columnIndexOfAlbumName) && _stmt.isNull(_columnIndexOfAlbumArtist)) {
            continue
          }
          val _key_2: Album
          val _tmpAlbumId: String
          _tmpAlbumId = _stmt.getText(_columnIndexOfAlbumId)
          val _tmpAlbumName: String
          _tmpAlbumName = _stmt.getText(_columnIndexOfAlbumName)
          val _tmpAlbumArtist: String
          _tmpAlbumArtist = _stmt.getText(_columnIndexOfAlbumArtist)
          _key_2 = Album(_tmpAlbumId,_tmpAlbumName,_tmpAlbumArtist)
          val _values_2: MutableList<Song>
          if (_values_1.containsKey(_key_2)) {
            _values_2 = _values_1.getValue(_key_2)
          } else {
            _values_2 = mutableListOf()
            _values_1.put(_key_2, _values_2)
          }
          if (_stmt.isNull(_columnIndexOfSongId) && _stmt.isNull(_columnIndexOfAlbum) && _stmt.isNull(_columnIndexOfSongArtist)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_columnIndexOfSongId)
          val _tmpAlbum: String
          _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          val _tmpSongArtist: String
          _tmpSongArtist = _stmt.getText(_columnIndexOfSongArtist)
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
