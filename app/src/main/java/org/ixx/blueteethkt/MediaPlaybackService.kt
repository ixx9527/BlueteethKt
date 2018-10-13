package org.ixx.blueteethkt

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import org.ixx.blueteethkt.utils.LogHelper
import org.ixx.blueteethkt.utils.MusicProvider
import org.ixx.blueteethkt.utils.Playback

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
        // 对每个访问端做一些访问权限判断等
        return BrowserRoot("__ROOT__", null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        Log.v(TAG, "onGetRoot, parentMediaId = $parentMediaId, result = $result")
        // 根据访问权限返回播放列表相关信息
        if (parentMediaId == null) {
            result.sendResult(null)
            return
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