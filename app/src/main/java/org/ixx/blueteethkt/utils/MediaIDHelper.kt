package org.ixx.blueteethkt.utils

import java.lang.StringBuilder

object MediaIDHelper {

    // Media IDs used on browseable items of MediaBrowser
    const val MEDIA_ID_ROOT = "__ROOT__"
    const val MEDIA_ID_MUSICS_BY_ARTIST = "__BY_ARTIST__"
    const val MEDIA_ID_MUSICS_BY_ALBUM = "__BY_ALBUM__"
    const val MEDIA_ID_MUSICS_BY_SONG = "__BY_SONG__"
    const val MEDIA_ID_MUSICS_BY_PLAYLIST = "__BY_PLAYLIST__"
    const val MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__"
    const val MEDIA_ID_NOW_PLAYING = "__NOW_PLAYING__"

    private const val CATEGORY_SEPARATOR: Char = 31.toChar()
    private const val LEAF_SEPARATOR: Char = 30.toChar()

    fun createMediaID(musicID: String?, vararg categories: String?): String {
        // MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy
        // to find the category (like genre) that a music was selected from, so we
        // can correctly build the playing queue. This is specially useful when
        // one music can appear in more than one list, like "by genre -> genre_1"
        // and "by artist -> artist_1".
        val sb = StringBuilder()
        categories?.let {
            for ((index, category) in it.withIndex()) {
                if (index != 0) {
                    sb.append(CATEGORY_SEPARATOR)
                }
                sb.append(category)
            }
        }
        musicID?.let {
            sb.append(LEAF_SEPARATOR).append(musicID)
        }
        return sb.toString()
    }

    fun createBrowseCategoryMediaID(categoryType: String, categoryValue: String): String {
        return categoryType + CATEGORY_SEPARATOR + categoryValue
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing
     * queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    fun getHierarchy(mediaID: String): List<String> {
        val pos = mediaID.indexOf(LEAF_SEPARATOR)
        val id: String = if (pos >= 0) mediaID.substring(0, pos) else mediaID
        return id.split(CATEGORY_SEPARATOR.toString())
    }
}