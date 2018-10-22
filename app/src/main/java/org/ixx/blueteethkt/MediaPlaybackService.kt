package org.ixx.blueteethkt

import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.browse.MediaBrowser.MediaItem
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import org.ixx.blueteethkt.utils.LogHelper
import org.ixx.blueteethkt.utils.MediaIDHelper
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SONG
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_NOW_PLAYING
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_ROOT
import org.ixx.blueteethkt.utils.MusicProvider
import org.ixx.blueteethkt.utils.Playback
import java.util.*

class MediaPlaybackService : MediaBrowserService(), Playback.Callback {
    private val TAG = LogHelper.makeLogTag(MediaPlaybackService::class.java)

    companion object {
        const val ACTION_CMD = "com.android.music.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
        const val CMD_REPEAT = "CMD_PAUSE"
        const val REPEAT_MODE = "REPEAT_MODE"
        const val CMD_SHUFFLE = "CMD_SHUFFLE"
        const val SHUFFLE_MODE = "SHUFFLE_MODE"
    }

    private var mMusicProvider: MusicProvider? = null
    private var mSession: MediaSession? = null
    private var mPlayingQueue: List<MediaSession.QueueItem>? = null
    private var mPlayback: Playback? = null

    override fun onCreate() {
        Log.v(TAG, "onCreate")
        super.onCreate()

        mMusicProvider = MusicProvider(this)

        createMediaSession()

        Log.v(TAG, "Create Playback")
        mPlayback = Playback(this, mMusicProvider)
        mPlayback?.callback = this
        mPlayback?.state = PlaybackState.STATE_NONE
        mPlayback?.start()
    }

    private fun createMediaSession() {
        Log.v(TAG, "Create MusicProvider")
        // Start a new MediaSession
        mSession = MediaSession(this, "MediaPlaybackService")

        // TODO : Set extra information: REPEAT_MODE, SHUFFLE_MODE

        // Set an initial PlaybackState with ACTION_PLAY,
        // so media buttons can start the player
        val stateBuilder = PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE)
        mSession?.setPlaybackState(stateBuilder.build())
        // MediaSessionCallback() has methods that handle callbacks from a media controller
        mSession?.setCallback(MediaSessionCallback())
        // Set the session's token so that client activities can communicate with it.
        sessionToken = mSession?.sessionToken
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Log.v(TAG, "onGetRoot, clientPackageName = $clientPackageName, clientUid = $clientUid")
        // Allow everyone to browse
        return BrowserRoot(MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        Log.v(TAG, "onGetRoot, parentMediaId = $parentMediaId, result = $result")
        //  Browsing not allowed
        if (parentMediaId == null) {
            result.sendResult(null)
            return
        }
        mMusicProvider?.let { musicProvider ->
            if (!musicProvider.isInitialized) {
                // Use result.detach to allow calling result.sendResult from another thread:
                result.detach()

                musicProvider.retrieveMediaAsync { success ->
                    Log.d(TAG, "Received catalog result, success: $success")
                    if (success) {
                        onLoadChildren(parentMediaId, result)
                    } else {
                        result.sendResult(Collections.emptyList())
                    }
                }
            } else {
                // If our music catalog is already loaded/cached, load them into result immediately
                val mediaItems = arrayListOf<MediaItem>()

                when {
                    parentMediaId == MEDIA_ID_ROOT -> {
                        Log.d(TAG, "OnLoadChildren.ROOT")
                        mediaItems.add(MediaItem(MediaDescription.Builder()
                                .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST)
                                .setTitle("Artists")
                                .build(), MediaItem.FLAG_BROWSABLE))
                        mediaItems.add(MediaItem(MediaDescription.Builder()
                                .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM)
                                .setTitle("Albums")
                                .build(), MediaItem.FLAG_BROWSABLE))
                        mediaItems.add(MediaItem(MediaDescription.Builder()
                                .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_SONG)
                                .setTitle("Songs")
                                .build(), MediaItem.FLAG_BROWSABLE))
                        mediaItems.add(MediaItem(MediaDescription.Builder()
                                .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST)
                                .setTitle("Playlists")
                                .build(), MediaItem.FLAG_BROWSABLE))
                    }
                    parentMediaId == MEDIA_ID_MUSICS_BY_ARTIST -> {
                        Log.d(TAG, "OnLoadChildren.ARTIST")
                        for (artist in musicProvider.getArtists()) {
                            val item = MediaItem(MediaDescription.Builder()
                                    .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(MEDIA_ID_MUSICS_BY_ARTIST, artist))
                                    .setTitle(artist)
                                    .build(), MediaItem.FLAG_BROWSABLE)
                            mediaItems.add(item)
                        }
                    }
                    parentMediaId == MEDIA_ID_MUSICS_BY_PLAYLIST -> {
                        Log.d(TAG, "OnLoadChildren.PLAYLIST")
                        for (playlist in musicProvider.getPlaylists()) {
                            val item = MediaItem(MediaDescription.Builder()
                                    .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(MEDIA_ID_MUSICS_BY_PLAYLIST, playlist))
                                    .setTitle(playlist)
                                    .build(), MediaItem.FLAG_BROWSABLE)
                            mediaItems.add(item)
                        }
                    }
                    parentMediaId == MEDIA_ID_MUSICS_BY_ALBUM -> {
                        Log.d(TAG, "OnLoadChildren.ALBUM")
                        loadAlbum(musicProvider.getAlbums(), mediaItems)
                    }
                    parentMediaId == MEDIA_ID_MUSICS_BY_SONG -> {
                        Log.d(TAG, "OnLoadChildren.SONG")
                        val hierarchyAwareMediaID = MediaIDHelper.createBrowseCategoryMediaID(parentMediaId, MEDIA_ID_MUSICS_BY_SONG)
                        loadSong(musicProvider.getMusicList(), mediaItems, hierarchyAwareMediaID)
                    }
                    parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST) -> {
                        val artist = MediaIDHelper.getHierarchy(parentMediaId)[1]
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_ARTIST artist = $artist")
                        loadAlbum(musicProvider.getAlbumByArtist(artist), mediaItems)
                    }
                    parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_ALBUM) -> {
                        val album = MediaIDHelper.getHierarchy(parentMediaId)[1]
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_ALBUM album = $album")
                        loadSong(musicProvider.getMusicsByAlbum(album), mediaItems, parentMediaId)
                    }
                    parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_PLAYLIST) -> {
                        val playlist = MediaIDHelper.getHierarchy(parentMediaId)[1]
                        Log.d(TAG, "OnLoadChildren.SONGS_BY_PLAYLIST playlist = $playlist")
                        val playingQueue = mPlayingQueue
                        if (playlist == MEDIA_ID_NOW_PLAYING && playingQueue != null && playingQueue.isNotEmpty()) {
                            loadPlayingQueue(mediaItems, parentMediaId)
                        } else {
                            loadSong(musicProvider.getMusicsByPlaylist(playlist), mediaItems, parentMediaId)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Skipping unmatched parentMediaId: $parentMediaId")
                    }
                }
                Log.d(TAG,
                        "OnLoadChildren sending ${mediaItems.size} results for $parentMediaId")
                result.sendResult(mediaItems)
            }
        }
    }

    private fun loadAlbum(albumList: Iterable<MediaMetadata>, mediaItems: ArrayList<MediaItem>) {
        for (albumMetadata in albumList) {
            val albumName = albumMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val artistName = albumMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val albumExtra = Bundle()
            albumExtra.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS,
                    albumMetadata.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS))
            val item = MediaItem(MediaDescription.Builder()
                    .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(MEDIA_ID_MUSICS_BY_ALBUM, albumName))
                    .setTitle(albumName)
                    .setSubtitle(artistName)
                    .setIconBitmap(
                            albumMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                    .setExtras(albumExtra)
                    .build(), MediaItem.FLAG_BROWSABLE)
            mediaItems.add(item)
        }
    }

    private fun loadSong(songList: Iterable<MediaMetadata>, mediaItems: ArrayList<MediaItem>, parentId: String) {
        for (metadata in songList) {
            val hierarchyAwareMediaID = MediaIDHelper.createMediaID(metadata.description.mediaId, parentId)
            val songExtra = Bundle()
            songExtra.putLong(MediaMetadata.METADATA_KEY_DURATION,
                    metadata.getLong(MediaMetadata.METADATA_KEY_DURATION))
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artistName = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val item = MediaItem(MediaDescription.Builder()
                    .setMediaId(hierarchyAwareMediaID)
                    .setTitle(title)
                    .setSubtitle(artistName)
                    .setExtras(songExtra)
                    .build(), MediaItem.FLAG_PLAYABLE)
            mediaItems.add(item)
        }
    }

    private fun loadPlayingQueue(mediaItems: MutableList<MediaItem>, parentId: String) {
        mPlayingQueue?.let { playingQueue ->
            for (queueItem in playingQueue) {
                val mediaItem = MediaItem(queueItem.description, MediaItem.FLAG_PLAYABLE)
                mediaItems.add(mediaItem)
            }
        }
    }

    inner class MediaSessionCallback : MediaSession.Callback() {
        override fun onPlay() {
            Log.d(TAG, "onPlay");
            super.onPlay()
        }
    }

    override fun onCompletion() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlaybackStatusChanged(state: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(error: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}