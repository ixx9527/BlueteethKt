package org.ixx.blueteethkt.utils

object LogHelper {
    private const val LOG_PREFIX = "music_"
    private const val LOG_PREFIX_LENGTH = LOG_PREFIX.length
    private const val MAX_LOG_TAG_LENGTH = 23

    fun makeLogTag(str: String): String {
        if (str.length > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.subSequence(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        }
        return LOG_PREFIX + str
    }

    fun makeLogTag(cls: Class<*>): String {
        return makeLogTag(cls.simpleName)
    }
}