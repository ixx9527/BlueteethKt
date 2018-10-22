package org.ixx.blueteethkt.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.support.v4.content.ContextCompat
import android.util.Log
import org.ixx.blueteethkt.MusicUtils
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class MusicProvider(val context: Context) {
    private val TAG = LogHelper.makeLogTag(MusicProvider::class.java)

    companion object {
        // Public constants
        const val UNKOWN = "UNKNOWN"
        // Uri source of this track
        const val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
        // Sort key for this tack
        const val CUSTOM_METADATA_SORT_KEY = "__SORT_KEY__"
    }

    private val mMusicList: MutableList<MediaMetadata> = arrayListOf()
    private val mMusicListById: ConcurrentMap<Long, Song> = ConcurrentHashMap()
    private val mMusicListByMediaId: ConcurrentMap<String, Song> = ConcurrentHashMap()

    // Album Name --> list of Metadata
    private val mMusicListByAlbum: ConcurrentMap<String, MutableList<MediaMetadata>> = ConcurrentHashMap()
    // Playlist Name --> list of Metadata
    private val mMusicListByPlaylist: ConcurrentMap<String, MutableList<MediaMetadata>> = ConcurrentHashMap()
    // Artist Name --> Map of (album name --> album metadata)
    private val mArtistAlbumDb: ConcurrentMap<String, Map<String, MediaMetadata>> = ConcurrentHashMap()

    internal enum class State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    @Volatile
    private var mCurrentState = State.NON_INITIALIZED

    val isInitialized: Boolean
        get() = mCurrentState == State.INITIALIZED

    fun getMusicList(): Iterable<MediaMetadata> {
        return mMusicList
    }

    /**
     * Get albums of a certain artist
     *
     */
    fun getAlbumByArtist(artist: String): Iterable<MediaMetadata> {
        TODO("还没想好")
    }

    /**
     * Get music tracks of the given album
     *
     */
    fun getMusicsByAlbum(album: String): Iterable<MediaMetadata> {
        TODO("还没想好")
    }

    /**
     * Get music tracks of the given playlist
     *
     */
    fun getMusicsByPlaylist(playlist: String): Iterable<MediaMetadata> {
        TODO("还没想好")
    }

    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    fun getMusicById(musicId: Long): Song? {
        return mMusicListById[musicId] ?: null
    }

    /**
     * Get an iterator over the list of artists
     *
     * @return list of artists
     */
    fun getArtists(): Iterable<String> {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList()
        }
        return mArtistAlbumDb.keys
    }

    /**
     * Get an iterator over the list of albums
     *
     * @return list of albums
     */
    fun getAlbums(): Iterable<MediaMetadata> {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList()
        }
        val albumList = arrayListOf<MediaMetadata>()
        for (artist_albums in mArtistAlbumDb.values) {
            albumList.addAll(artist_albums.values)
        }
        return albumList
    }

    /**
     * Get an iterator over the list of playlists
     *
     * @return list of playlists
     */
    fun getPlaylists(): Iterable<String> {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList()
        }
        return mMusicListByPlaylist.keys
    }

    /**
     * Get the list of music tracks from disk and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    fun retrieveMediaAsync(callback: (success: Boolean) -> Unit) {
        Log.d(TAG, "retrieveMediaAsync called")
        if (mCurrentState == State.INITIALIZED) {
            // Nothing to do, execute callback immediately
            callback.invoke(true)
            return
        }

        // Asynchronously load the music catalog in a separate thread
        object : AsyncTask<Void, Void, State>() {
            override fun doInBackground(vararg params: Void?): State {
                if (mCurrentState == State.INITIALIZED) {
                    return mCurrentState
                }
                mCurrentState = State.INITIALIZING
                if (retrieveMedia()) {
                    mCurrentState = State.INITIALIZED
                } else {
                    mCurrentState = State.NON_INITIALIZED
                }
                return mCurrentState
            }

            override fun onPostExecute(current: State) {
                callback.invoke(current == State.INITIALIZED)
            }
        }.execute()
    }

    @Synchronized
    private fun retrieveMedia(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
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
                .putString(MediaMetadata.METADATA_KEY_TITLE, title ?: UNKOWN)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album ?: UNKOWN)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist ?: UNKOWN)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
        // Retrieve album art
        val albumArtData = retriever.embeddedPicture
        if (retriever.embeddedPicture != null) {
            var bitmap = BitmapFactory.decodeByteArray(albumArtData, 0, albumArtData.size)
            bitmap = MusicUtils.resizeBitmap(bitmap, getDefaultAlbumArt())
        }
        retriever.release()
        return metadataBuilder.build()
    }

    private fun getDefaultAlbumArt(): Bitmap {
        TODO("get default bitmap from resources")
    }

    private fun addMusicToAlbumList(metadata: MediaMetadata) {
        var thisAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        if (thisAlbum == null) {
            thisAlbum = UNKOWN
        }
        if (!mMusicListByAlbum.containsKey(thisAlbum)) {
            mMusicListByAlbum[thisAlbum] = arrayListOf()
        }
        mMusicListByAlbum[thisAlbum]?.add(metadata)
    }

    private fun addMusicToArtistList(metadata: MediaMetadata) {
        var thisArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        if (thisArtist == null) {
            thisArtist = UNKOWN
        }
        var thisAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        if (thisAlbum == null) {
            thisAlbum = UNKOWN
        }
        if (!mArtistAlbumDb.containsKey(thisArtist)) {
            mArtistAlbumDb[thisArtist] = ConcurrentHashMap()
        }
        val albumsMap = mArtistAlbumDb[thisArtist]
        TODO("一堆逻辑")
    }
}