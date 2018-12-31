package org.ixx.blueteethkt.utils

import android.media.MediaMetadata
import android.os.Parcel
import android.os.Parcelable

class Song(val songId: Long, var metadata: MediaMetadata, var sortKey: Long?) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this == other) return true
        if (other == null || other !is Song) return false
        return songId == other.songId
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        sortKey?.let { parcel.writeLong(it) }
        parcel.writeLong(songId)
        parcel.writeParcelable(metadata, flags)
    }

    override fun describeContents(): Int {
        return 0
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