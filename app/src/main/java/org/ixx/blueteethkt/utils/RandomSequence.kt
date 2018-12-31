package org.ixx.blueteethkt.utils

class RandomSequence : Sequence {

    private var mShuffledSequence: MutableList<Int> = arrayListOf()

    override var current: Int
        get() = mShuffledSequence.indexOf(super.current)
        set(value) {
            current = mShuffledSequence.indexOf(value)
        }

    constructor(length: Int) : super(length) {
        for (i in 0 until length)
            mShuffledSequence.add(i)
        mShuffledSequence.shuffle()
    }


    override fun reset() {
        super.reset()
        mShuffledSequence.shuffle()
    }
}