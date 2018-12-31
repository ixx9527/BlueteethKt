package org.ixx.blueteethkt

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import org.ixx.blueteethkt.utils.*
import org.ixx.blueteethkt.utils.MediaIDHelper.MEDIA_ID_ROOT


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

    enum class RepeatMode {
        REPEAT_NONE, REPEAT_ALL, REPEAT_CURRENT
    }

    enum class ShuffleMode {
        SHUFFLE_NONE, SHUFFLE_RANDOM
    }

    // Music catalog manager
    private val mMusicProvider: MusicProvider by lazy {
        Log.d(TAG, "Create MusicProvider")
        MusicProvider(this)
    }
    private val mSession: MediaSession by lazy {
        Log.d(TAG, "Create MediaSession")
        MediaSession(this, "MediaPlaybackService")
    }
    // "Now playing" queue:
    private var mPlayingQueue: MutableList<MediaSession.QueueItem> = arrayListOf()
    private val mQueueSequence = createSequence(0)
    // Indicates whether the service was started.
    private val mServiceStarted: Boolean = false
    private val mPlayback: Playback by lazy {
        Log.d(TAG, "Create Playback")
        Playback(this, mMusicProvider)
    }

    // Default mode is repeat none
    private val mRepeatMode = RepeatMode.REPEAT_NONE
    // Default mode is shuffle none
    private val mShuffleMode = ShuffleMode.SHUFFLE_NONE
    // Extra information for this session
    private val mExtras: Bundle = Bundle()

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        Log.d(TAG, "Init MediaSession")
        // Set extra information
        mExtras.putInt(REPEAT_MODE, mRepeatMode.ordinal)
        mExtras.putInt(SHUFFLE_MODE, mShuffleMode.ordinal)
        mSession.setExtras(mExtras)
        // Enable callbacks from MediaButtons and TransportControls
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        val stateBuilder = PlaybackState.Builder().setActions(
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE)
        mSession.setPlaybackState(stateBuilder.build())
        // MediaSessionCallback() has methods that handle callbacks from a media controller
        mSession.setCallback(MediaSessionCallback())
        // Set the session's token so that client activities can communicate with it.
        sessionToken = mSession.sessionToken

        Log.d(TAG, "Init Playback")
        mPlayback.state = PlaybackState.STATE_NONE
        mPlayback.callback = this
        mPlayback.start();
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Log.d(TAG, "onGetRoot: clientPackageName = {$clientPackageName}, clientUid = {$clientUid}, rootHints = {$rootHints}")
        // Allow everyone to browse
        return BrowserRoot(MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        Log.d(TAG, "onLoadChildren, parentMediaId = {$parentMediaId}")
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

    private fun createSequence(length: Int): Sequence {
        // Create new sequence based on current shuffle mode
        return if (mShuffleMode == ShuffleMode.SHUFFLE_RANDOM) RandomSequence(length) else Sequence(length)
    }

    inner class MediaSessionCallback : MediaSession.Callback() {

    }
}