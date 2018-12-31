package org.ixx.blueteethkt.utils

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.util.Log
import org.ixx.blueteethkt.MusicUtils
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class MusicProvider(val mContext: Context) {
    private val TAG = LogHelper.makeLogTag(MusicProvider::class.java)

    companion object {
        // Public constants
        const val UNKNOWN = "UNKNOWN"
        // Uri source of this track
        const val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
        // Sort key for this tack
        const val CUSTOM_METADATA_SORT_KEY = "__SORT_KEY__"
    }

    // Album Name --> list of Metadata
    private val mMusicListByAlbum: ConcurrentMap<String, MutableList<MediaMetadata>> = ConcurrentHashMap()
    // Playlist Name --> list of Metadata
    private val mMusicListByPlaylist: ConcurrentMap<String, MutableList<MediaMetadata>> = ConcurrentHashMap()
    // Artist Name --> Map of (album name --> album metadata)
    private val mArtistAlbumDb: ConcurrentMap<String, ConcurrentHashMap<String, MediaMetadata>> = ConcurrentHashMap()

    // Property
    val mMusicList: MutableList<MediaMetadata> = arrayListOf()

    private val mMusicListById: ConcurrentMap<Long, Song> = ConcurrentHashMap()
    private val mMusicListByMediaId: ConcurrentMap<String, Song> = ConcurrentHashMap()

    enum class State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    @Volatile
    private var mCurrentState = State.NON_INITIALIZED

    val isInitialized: Boolean
        get() = mCurrentState == State.INITIALIZED

    init {
        mMusicListByPlaylist.put(MediaIDHelper.MEDIA_ID_NOW_PLAYING, arrayListOf())
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return list of artists
     */
    fun getArtists(): Iterable<String> {
        return if (isInitialized) mArtistAlbumDb.keys else Collections.emptyList()
    }

    /**
     * Get an iterator over the list of albums
     *
     * @ return list of albums
     */
    fun getAlbums(): Iterable<MediaMetadata> {
        return if (isInitialized) {
            val albumList: ArrayList<MediaMetadata> = arrayListOf()
            for (artist_albums in mArtistAlbumDb.values) {
                albumList.addAll(artist_albums.values)
            }
            albumList
        } else Collections.emptyList()
    }

    /**
     * Get an iterator over the list of playlists
     *
     * @return list of playlists
     */
    fun getPlaylists(): Iterable<String> {
        return if (isInitialized) mMusicListByPlaylist.keys else Collections.emptyList()
    }

    /**
     * Get albums of a certain artist
     *
     */
    fun getAlbumByArtist(artist: String): Iterable<MediaMetadata> {
        return if (isInitialized) mArtistAlbumDb[artist]?.values
                ?: Collections.emptyList() else Collections.emptyList()
    }

    /**
     * Get music tracks of the given album
     *
     */
    fun getMusicsByAlbum(album: String): Iterable<MediaMetadata> {
        return if (isInitialized) mMusicListByAlbum[album]
                ?: Collections.emptyList() else Collections.emptyList()
    }

    /**
     * Get music tracks of the given playlist
     *
     */
    fun getMusicsByPlaylist(playlist: String): Iterable<MediaMetadata> {
        return if (isInitialized) mMusicListByPlaylist[playlist]
                ?: Collections.emptyList() else Collections.emptyList()
    }

    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    fun getMusicById(musicId: Long): Song? {
        return mMusicListById[musicId]
    }

    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    fun getMusicByMediaId(musicId: String): Song? {
        return mMusicListByMediaId[musicId]
    }

    /**
     * Very basic implementation of a search that filter music tracks which title containing
     * the given query.
     *
     */
    fun searchMusic(titleQuery: String): Iterable<MediaMetadata> {
        return if (isInitialized) {
            val result: ArrayList<MediaMetadata> = arrayListOf()
            val titleQueryInLowerCase = titleQuery.toLowerCase()
            for (song in mMusicListByMediaId.values) {
                if (song.metadata
                                .getString(MediaMetadata.METADATA_KEY_TITLE)
                                .toLowerCase()
                                .contains(titleQueryInLowerCase)) {
                    result.add(song.metadata)
                }
            }
            result
        } else Collections.emptyList()
    }

    /**
     * Get the list of music tracks from disk and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    fun retrieveMediaAsync(callback: (Boolean) -> Unit) {
        Log.d(TAG, "retrieveMediaAsync called")
        if (isInitialized) {
            // Nothing to do, execute callback immediately
            callback.invoke(true)
        } else {
            // Asynchronously load the music catalog in a separate thread
            doAsync {
                if (isInitialized) {
                    return@doAsync
                }
                mCurrentState = State.INITIALIZING
                if (retrieveMedia()) {
                    mCurrentState = State.INITIALIZED
                } else {
                    mCurrentState = State.NON_INITIALIZED
                }
                uiThread {
                    callback.invoke(isInitialized)
                }
            }
        }
    }

    @Synchronized
    fun retrieveMedia(): Boolean {
        if (mContext.checkSelfPermission(READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val musicPath = "/mnt/sdcard/Music/"
        retrieveMediaInFolder(File(musicPath))
        return true
    }

    private fun retrieveMediaInFolder(path: File) {
        if (path.exists()) {
            val files = path.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    retrieveMediaInFolder(file)
                } else {
                    val thisId = System.currentTimeMillis()
                    val thisPath = file.absolutePath
                    val metadata = retrieveMediaMetadata(thisId, thisPath)
                    metadata ?: continue
                    val thisSong = Song(thisId, metadata, null)
                    // Construct per feature database
                    mMusicList.add(metadata)
                    mMusicListById.put(thisId, thisSong)
                    mMusicListByMediaId.put(thisId.toString(), thisSong)
                    addMusicToAlbumList(metadata)
                    addMusicToArtistList(metadata)
                }
            }
        } else {
            Log.e(TAG, "文件路径（${path.absolutePath}）不存在!")
        }
    }

    @Synchronized
    private fun retrieveMediaMetadata(musicId: Long, musicPath: String): MediaMetadata? {
        Log.d(TAG, "getting metadata for music: $musicPath")
        val retriever = MediaMetadataRetriever()
        if (!File(musicPath).exists()) {
            return null
        }
        retriever.setDataSource(musicPath)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val durationString: String? = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationString?.toLong() ?: 0
        val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, musicId.toString())
                .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, musicPath)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title ?: UNKNOWN)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album ?: UNKNOWN)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist ?: UNKNOWN)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
        // Retrieve album art
        val albumArtData = retriever.embeddedPicture
        if (retriever.embeddedPicture != null) {
            var bitmap = BitmapFactory.decodeByteArray(albumArtData, 0, albumArtData.size)
            bitmap = MusicUtils.resizeBitmap(bitmap, getDefaultAlbumArt())
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
        }
        retriever.release()
        return metadataBuilder.build()
    }

    private fun getDefaultAlbumArt(): Bitmap {
        TODO("get default bitmap from resources")
    }

    private fun addMusicToAlbumList(metadata: MediaMetadata) {
        var thisAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: UNKNOWN
        if (!mMusicListByAlbum.containsKey(thisAlbum)) {
            mMusicListByAlbum[thisAlbum] = arrayListOf()
        }
        mMusicListByAlbum[thisAlbum]?.add(metadata)
    }

    private fun addMusicToArtistList(metadata: MediaMetadata) {
        TODO("一堆逻辑")
    }
}