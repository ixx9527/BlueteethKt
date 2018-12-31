package org.ixx.blueteethkt

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import org.ixx.blueteethkt.utils.LogHelper

const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42

class MediaPlaybackActivity : AppCompatActivity() {
    private val TAG = LogHelper.makeLogTag(MediaPlaybackActivity::class.java)

    private var mMediaBrowser: MediaBrowser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            return
        }
        initApp()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        mMediaBrowser?.connect()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        mMediaBrowser?.disconnect()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty()
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                    return
                }
                initApp()
                // connect media browser
                mMediaBrowser?.connect()
            }
        }
    }

    private fun initApp() {
        volumeControlStream = AudioManager.STREAM_MUSIC

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.audio_player)

        Log.d(TAG, "Creating MediaBrowser")
        mMediaBrowser = MediaBrowser(this, ComponentName(this, MediaPlaybackService::class.java),
                mConnectionCallback, null)
    }

    private val mMediaControllerCallback = object : MediaController.Callback() {

    }

    private val mConnectionCallback = object : MediaBrowser.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.d(TAG, "onConnected: session token " + mMediaBrowser?.sessionToken)
            if (mMediaBrowser?.sessionToken == null) {
                throw IllegalArgumentException("No Session token")
            }
            val mediaController = MediaController(this@MediaPlaybackActivity,
                    mMediaBrowser?.sessionToken)
            mediaController.registerCallback(mMediaControllerCallback)
            setMediaController(mediaController)
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d(TAG, "onConnectionFailed")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Log.d(TAG, "onConnectionSuspended")
            mediaController = null
        }
    }
}