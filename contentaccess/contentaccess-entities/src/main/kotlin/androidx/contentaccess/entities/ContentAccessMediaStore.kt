/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.contentaccess.entities

import android.provider.MediaStore
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey
import androidx.annotation.RequiresApi

class ContentAccessMediaStore {

    // TODO(obenabde): Investigate whether some of the columns marked as nullable here
    // actually have any default values.
    // TODO(obenabde): Formalize the comments below into a kdoc.
    open class MediaColumns {
        // PLEASE READ IF YOU EXTEND THIS CLASS:
        // ANY CLASS THAT EXTENDS THIS CLASS SHOULD ADD ALL THE FOLLOWING COLUMNS WITH THE
        // @RequiresApi AT THE API IT EXTENDED THE CLASS AT. THESE CONSTANTS ARE CURRENTLY
        // IN MediaStore.MediaColumns HOWEVER THEY WERE IN A SUBSET OF THE SUBCLASSES BEFORE
        // AND WERE LATER PROMOTED TO THIS CLASS SO ALL SUBCLASSES NOW INHERIT THEM. TO MAINTAIN
        // THE APPROPRIATE @RequiresApi LEVELS, WE DON'T ACTUALLY ADD THESE CONSTANTS TO THIS CLASS
        // BUT WE ADD THEM TO ALL THE SUBCLASSES WITH THE APPROPRIATE @RequiresApi API.
        // Columns that were promoted later from sublasses are:
        // DATE_TAKEN FROM ImageColumns,VideoColumns AT 29
        // BUCKET_ID FROM ImageColumns,VideoColumns AT 29
        // BUCKET_DISPLAY_NAME FROM ImageColumns,VideoColumns AT 29
        // ORIENTATION FROM ImageColumns AT 29
        // DURATION FROM AudioColumns,VideoColumns AT 29
        // ARTIST FROM AudioColumns, VideoColumns AT 30
        // ALBUM FROM AudioColumns, VideoColumns AT 30
        // COMPOSER FROM AudioColumns AT 30
        // RESOLUTION FROM VideoColumns AT 30
        // ALSO THE FOLLOWING COLUMN WAS NOT PRESENT IN AUDIOCOLUMNS IT SEEMS, SO ALL OTHER CLASSES
        // SHOULD ADD THEM
        // WIDTH
        // HEIGHT
        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @Deprecated("Apps may not have filesystem permissions to directly access this path. " +
                "Instead of trying to open this path directly, apps should use " +
                "ContentResolver#openFileDescriptor(Uri, String) to gain access.")
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATA)
        public var data: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.IS_DRM)
        public var isDrm: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.IS_TRASHED)
        public var isTrashed: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.SIZE)
        public var size: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
        public var displayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.TITLE)
        public var title: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_ADDED)
        public var dateAdded: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
        public var dateModified: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.MIME_TYPE)
        public var mimeType: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.IS_PENDING)
        public var isPending: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_EXPIRES)
        public var dateExpires: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        public var ownerPackageName: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.VOLUME_NAME)
        public var volumeName: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.RELATIVE_PATH)
        public var relativePath: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DOCUMENT_ID)
        public var documentId: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.INSTANCE_ID)
        public var instanceId: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIGINAL_DOCUMENT_ID)
        public var originalDocumentId: String? = null
    }

    open class ImageColumns : MediaColumns() {
        @JvmField
        @Deprecated("As of API 29, this value was only relevant for images hosted on Picasa, " +
                "which are no longer supported.")
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.PICASA_ID)
        public var picasaId: String? = null

        @JvmField
        @Deprecated("As of API 29, location details are no longer indexed for privacy reasons, " +
                "and this value is now always null. You can still manually obtain location " +
                "metadata using ExifInterface#getLatLong(float[]).")
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.LATITUDE)
        public var latitude: Float? = null

        @JvmField
        @Deprecated("As of API 29, location details are no longer indexed for privacy reasons, " +
                "and this value is now always null. You can still manually obtain location " +
                "metadata using ExifInterface#getLatLong(float[]).")
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.LONGITUDE)
        public var longitude: Float? = null

        @JvmField
        @Deprecated("As of API 29, all thumbnails should be obtained via MediaStore.Images" +
                ".Thumbnails#getThumbnail, as this value is no longer supported.")
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC)
        public var miniThumbMagic: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.DESCRIPTION)
        public var description: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.IS_PRIVATE)
        public var isPrivate: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.SCENE_CAPTURE_TYPE)
        public var sceneCaptureType: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.EXPOSURE_TIME)
        public var exposureTime: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.F_NUMBER)
        public var fNumber: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Images.ImageColumns.ISO)
        public var iso: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
        public var dateTaken: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_ID)
        public var bucketId: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        public var bucketDisplayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIENTATION)
        public var orientation: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DURATION)
        public var duration: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.COMPOSER)
        public var composer: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.RESOLUTION)
        public var resolution: String? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.WIDTH)
        public var width: Int? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.HEIGHT)
        public var height: Int? = null
    }

    @ContentEntity("content://media/external/images/media")
    class Image : ImageColumns()

    open class ImageThumbnailColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @Deprecated("Deprecated in API 29. Apps may not have filesystem permissions to directly " +
                "access this path. Instead of trying to open this path directly, apps should use " +
                "ContentResolver#loadThumbnail to gain access.")
        @ContentColumn(android.provider.MediaStore.Images.Thumbnails.DATA)
        public var data: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.Thumbnails.IMAGE_ID)
        public var imageId: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.Thumbnails.KIND)
        public var kind: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.Thumbnails.WIDTH)
        public var width: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Images.Thumbnails.HEIGHT)
        public var height: Int? = null
    }

    @Deprecated("Callers should migrate to using ContentResolver#loadThumbnail, since it offers " +
            "richer control over requested thumbnail sizes and cancellationbehavior.")
    @ContentEntity("content://media/external/images/thumbnails")
    class ImageThumbnail : ImageThumbnailColumns()

    open class VideoThumbnailColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @Deprecated("Deprecated in API 29. Apps may not have filesystem permissions to directly " +
                "access this path. Instead of trying to open this path directly, apps should use " +
                "ContentResolver#loadThumbnail to gain access.")
        @ContentColumn(android.provider.MediaStore.Video.Thumbnails.DATA)
        public var data: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.Thumbnails.VIDEO_ID)
        public var videoId: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.Thumbnails.KIND)
        public var kind: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.Thumbnails.WIDTH)
        public var width: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.Thumbnails.HEIGHT)
        public var height: Int? = null
    }

    @Deprecated("Callers should migrate to using ContentResolver#loadThumbnail, since it offers " +
            "richer control over requested thumbnail sizes and cancellationbehavior.")
    @ContentEntity("content://media/external/video/thumbnails")
    class VideoThumbnail : VideoThumbnailColumns()

    open class VideoColumns : MediaColumns() {
        @JvmField
        @Deprecated("As of API 29, location details are no longer indexed for privacy reasons, " +
                "and this value is now always null. You can still manually obtain location " +
                "metadata using ExifInterface#getLatLong(float[]).")
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.LATITUDE)
        public var latitude: Float? = null

        @JvmField
        @Deprecated("As of API 29, location details are no longer indexed for privacy reasons, " +
                "and this value is now always null. You can still manually obtain location " +
                "metadata using ExifInterface#getLatLong(float[]).")
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.LONGITUDE)
        public var longitude: Float? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.DESCRIPTION)
        public var description: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.IS_PRIVATE)
        public var isPrivate: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.TAGS)
        public var tags: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.CATEGORY)
        public var category: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.LANGUAGE)
        public var language: String? = null

        @JvmField
        @Deprecated("As for API 29, all thumbnails should be obtained via MediaStore.Images" +
                ".Thumbnails#getThumbnail, as this value is no longer supported.")
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.MINI_THUMB_MAGIC)
        public var miniThumbMagic: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.BOOKMARK)
        public var bookmark: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.COLOR_STANDARD)
        public var colorStandard: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.COLOR_TRANSFER)
        public var colorTransfer: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Video.VideoColumns.COLOR_RANGE)
        public var colorRange: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
        public var dateTaken: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_ID)
        public var bucketId: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        public var bucketDisplayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIENTATION)
        @RequiresApi(29)
        public var orientation: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DURATION)
        public var duration: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.COMPOSER)
        public var composer: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.RESOLUTION)
        public var resolution: String? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.WIDTH)
        public var width: Int? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.HEIGHT)
        public var height: Int? = null
    }

    @ContentEntity("content://media/external/video/media")
    class Video : VideoColumns()

    open class AudioColumns : MediaColumns() {
        @JvmField
        @Deprecated("Deprecated in API 30, These keys are generated using Locale.ROOT, which " +
                "means they don't reflect locale-specific sorting preferences. To apply " +
                "locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE. ")
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.TITLE_KEY)
        public var titleKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.BOOKMARK)
        public var bookmark: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.ARTIST_ID)
        public var artistId: Int? = null

        @JvmField
        @Deprecated("Deprecated in API 30, These keys are generated using Locale.ROOT, which " +
                "means they don't reflect locale-specific sorting preferences. To apply " +
                "locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE. ")
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.ARTIST_KEY)
        public var artistKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.ALBUM_ID)
        public var albumId: Int? = null

        @JvmField
        @Deprecated("Deprecated in API 30, These keys are generated using Locale.ROOT, which " +
                "means they don't reflect locale-specific sorting preferences. To apply " +
                "locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE. ")
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.ALBUM_KEY)
        public var albumKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.TRACK)
        public var track: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.YEAR)
        public var year: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_MUSIC)
        public var isMusic: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_PODCAST)
        public var isPodcast: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_RINGTONE)
        public var isRingtone: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_ALARM)
        public var isAlarm: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_NOTIFICATION)
        public var isNotification: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.GENRE)
        public var genre: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.GENRE_ID)
        public var genreId: Int? = null

        @JvmField
        @Deprecated("Deprecated in API 30, These keys are generated using Locale.ROOT, which " +
                "means they don't reflect locale-specific sorting preferences. To apply " +
                "locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE. ")
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.GENRE_KEY)
        public var genreKey: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.TITLE_RESOURCE_URI)
        public var titleResourceUri: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.Audio.AudioColumns.IS_AUDIOBOOK)
        public var isAudioBook: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
        public var dateTaken: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_ID)
        public var bucketId: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        public var bucketDisplayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIENTATION)
        @RequiresApi(29)
        public var orientation: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.DURATION)
        public var duration: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.COMPOSER)
        public var composer: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.RESOLUTION)
        public var resolution: String? = null
    }

    @ContentEntity("content://media/external/audio/media")
    class Audio : AudioColumns()

    open class AlbumColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @Deprecated("This constant was deprecated in API level 29. Apps may not have filesystem " +
                "permissions to directly access this path. Instead of trying to open this path " +
                "directly, apps should use ContentResolver#loadThumbnail to gain access.")
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ART)
        public var albumArt: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ALBUM_ID)
        public var albumId: Int? = null

        @JvmField
        @Deprecated("This constant was deprecated in API level 30. These keys are generated using" +
                " Locale.ROOT, which means they don't reflect locale-specific sorting preferences" +
                ". To apply locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE.")
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ALBUM_KEY)
        public var albumKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ARTIST_ID)
        public var artistId: Int? = null

        @JvmField
        @Deprecated("This constant was deprecated in API level 30. These keys are generated using" +
                " Locale.ROOT, which means they don't reflect locale-specific sorting preferences" +
                ". To apply locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE.")
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.ARTIST_KEY)
        public var artistKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.FIRST_YEAR)
        public var firstYear: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.LAST_YEAR)
        public var lastYear: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS)
        public var numberOfSongs: Int? = null
        // This is commented out because it doesn't really seem to be a column? It's in the
        // documentation but the resolver says it doesn't exist.
//        @JvmField
//        @ContentColumn(android.provider.MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS_FOR_ARTIST)
//        public var numberOfSongsForArtist: Int? = null
    }

    @ContentEntity("content://media/external/audio/albums")
    class Album : AlbumColumns()

    open class ArtistColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @Deprecated("This constant was deprecated in API level 30. These keys are generated using" +
                " Locale.ROOT, which means they don't reflect locale-specific sorting preferences" +
                ". To apply locale-specific sorting preferences, use " +
                "ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or " +
                "ContentResolver#QUERY_ARG_SORT_LOCALE.")
        @ContentColumn(android.provider.MediaStore.Audio.ArtistColumns.ARTIST_KEY)
        public var artistKey: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.ArtistColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS)
        public var numberOfAlbums: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS)
        public var numberOfTracks: Int? = null
    }

    @ContentEntity("content://media/external/audio/artists")
    class Artist : ArtistColumns()

    open class GenresColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.GenresColumns.NAME)
        public var name: String? = null
    }

    @ContentEntity("content://media/external/audio/genres")
    class Genre : GenresColumns()

    open class PlaylistColumns {

        @JvmField
        @ContentPrimaryKey("_id")
        public var _id: Int = 0

        @JvmField
        @Deprecated("This constant was deprecated in API level 29. Apps may not have filesystem " +
                "permissions to directly access this path. Instead of trying to open this path " +
                "directly, apps should use ContentResolver#openFileDescriptor(Uri, String) to " +
                "gain access.")
        @ContentColumn(android.provider.MediaStore.Audio.PlaylistsColumns.DATA)
        public var data: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.PlaylistsColumns.DATE_ADDED)
        public var dateAdded: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED)
        public var dateModified: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Audio.PlaylistsColumns.NAME)
        public var name: String? = null
    }

    @ContentEntity("content://media/external/audio/playlists")
    class Playlist : PlaylistColumns()

    open class DownloadColumns : MediaColumns() {

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.DownloadColumns.REFERER_URI)
        public var refererUri: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.DownloadColumns.DOWNLOAD_URI)
        public var downloadUri: String? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
        public var dateTaken: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_ID)
        public var bucketId: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        public var bucketDisplayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIENTATION)
        @RequiresApi(29)
        public var orientation: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.COMPOSER)
        public var composer: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.RESOLUTION)
        public var resolution: String? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.WIDTH)
        public var width: Int? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.HEIGHT)
        public var height: Int? = null
    }

    @ContentEntity("content://media/external/downloads")
    @RequiresApi(29)
    class Download : DownloadColumns()

    open class FileColumns : MediaColumns() {
        @JvmField
        @ContentColumn(android.provider.MediaStore.Files.FileColumns.PARENT)
        public var parent: Int? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE)
        public var mediaType: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
        public var dateTaken: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_ID)
        public var bucketId: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        public var bucketDisplayName: String? = null

        @JvmField
        @ContentColumn(android.provider.MediaStore.MediaColumns.ORIENTATION)
        @RequiresApi(29)
        public var orientation: Int? = null

        @JvmField
        @RequiresApi(29)
        @ContentColumn(android.provider.MediaStore.MediaColumns.DURATION)
        public var duration: Int? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ARTIST)
        public var artist: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.ALBUM)
        public var album: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.COMPOSER)
        public var composer: String? = null

        @JvmField
        @RequiresApi(30)
        @ContentColumn(android.provider.MediaStore.MediaColumns.RESOLUTION)
        public var resolution: String? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.WIDTH)
        public var width: Int? = null

        @JvmField
        @RequiresApi(16)
        @ContentColumn(android.provider.MediaStore.MediaColumns.HEIGHT)
        public var height: Int? = null
    }

    @ContentEntity
    class File : FileColumns()
}