package org.ixx.blueteethkt.utils

import android.media.MediaMetadata
import android.os.Parcel
import android.os.Parcelable

class Song(val songId: Long, var metadata: MediaMetadata, var sortKey: Long?) : Parcelable {

    override fun equals(o: Any?): Boolean {
        if (this == o) return true
        if (o == null || o !is Song) return false
        return songId == o.songId
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel?, flags: Int) {
        sortKey?.let { out?.writeLong(it) }
        out?.writeLong(songId)
        out?.writeParcelable(metadata, flags)
    }

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song {
            val metadata = parcel.readParcelable<MediaMetadata>(null)
            val songId = parcel.readLong()
            val sortKey = parcel.readLong()
            return Song(songId, metadata, sortKey)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}