package org.ixx.blueteethkt

import android.Manifest.permission
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.ixx.blueteethkt.utils.LogHelper


class MediaPlaybackActivity : AppCompatActivity() {
    private val TAG = LogHelper.makeLogTag(MediaPlaybackActivity::class.java)

    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42

    private var mMediaBrowser: MediaBrowser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        } else {
            initApp()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                } else {
                    initApp()
                }
            }
            else -> {
                Log.w(TAG, "onRequestPermissionsResult requestCode = $requestCode")
            }
        }
    }

    private fun initApp() {
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(R.layout.audio_player)

        Log.d(TAG, "Creating MediaBrowser");
        mMediaBrowser = MediaBrowser(this, ComponentName(this, MediaPlaybackService::class.java),
                mConnectionCallback, null)
    }

    private val mConnectionCallback = object : MediaBrowser.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "onConnected: session token ${mMediaBrowser?.sessionToken}");
            super.onConnected()
            val mediaController = MediaController(
                    this@MediaPlaybackActivity, mMediaBrowser?.sessionToken)
            mediaController.registerCallback(mMediaControllerCallback)
            this@MediaPlaybackActivity.mediaController = mediaController
        }

        override fun onConnectionFailed() {
            Log.d(TAG, "onConnectionFailed")
            super.onConnectionFailed()
        }

        override fun onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended")
            super.onConnectionSuspended()
        }
    }

    private val mMediaControllerCallback = object : MediaController.Callback() {

    }


    override fun onStart() {
        Log.v(TAG, "onStart")
        super.onStart()
        mMediaBrowser?.connect()
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        super.onStop()
        mMediaBrowser?.disconnect()
    }
}
