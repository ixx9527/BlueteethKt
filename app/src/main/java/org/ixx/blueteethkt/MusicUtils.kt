package org.ixx.blueteethkt

import android.graphics.Bitmap


object MusicUtils {

    fun resizeBitmap(bitmap: Bitmap, ref: Bitmap): Bitmap {
        val w = ref.width
        val h = ref.height
        return Bitmap.createScaledBitmap(bitmap, w, h, false)
    }
}