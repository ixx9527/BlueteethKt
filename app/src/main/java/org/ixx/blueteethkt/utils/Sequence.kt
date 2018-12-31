package org.ixx.blueteethkt.utils

open class Sequence(val length: Int) {

    open var current: Int = 0
        set(value) {
            if (value < 0 || value >= length) {
                throw IllegalArgumentException()
            }
            current = value
        }

    open fun reset() {
        current = 0
    }

    fun hasNext(): Boolean {
        return current < length - 1
    }

    fun hasPrev(): Boolean {
        return current > 0
    }

    fun next() {
        if (!hasNext()) throw IllegalStateException()
        ++current
    }

    fun prev() {
        if (!hasPrev()) throw IllegalStateException()
        --current
    }
}