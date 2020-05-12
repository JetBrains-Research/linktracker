package org.intellij.plugin.tracker.core

import kotlin.experimental.and

/**
 * Hash algorithm by Bob Jenkins, 1996.
 *
 * You may use this code any way you wish, private, educational, or commercial.  It's free.
 * See: http://burtleburtle.net/bob/hash/doobs.html
 *
 * Use for hash table lookup, or anything where one collision in 2^^32
 * is acceptable.  Do NOT use for cryptographic purposes.
 *
 * Java port by Gray Watson http://256.com/gray/
 */
class JenkinsHash {

    // internal variables used in the various calculations
    internal var a: Long = 0
    internal var b: Long = 0
    internal var c: Long = 0

    /**
     * Convert a byte into a long value without making it negative.
     */
    private fun byteToLong(b: Byte): Long {
        var `val` = (b and 0x7F).toLong()
        if ((b and 0x80.toByte()) != 0.toByte()) {
            `val` += 128
        }
        return `val`
    }

    /**
     * Do addition and turn into 4 bytes.
     */
    private fun add(`val`: Long, add: Long): Long {
        return `val` + add and MAX_VALUE
    }

    /**
     * Do subtraction and turn into 4 bytes.
     */
    private fun subtract(`val`: Long, subtract: Long): Long {
        return `val` - subtract and MAX_VALUE
    }

    /**
     * Left shift val by shift bits and turn in 4 bytes.
     */
    private fun xor(`val`: Long, xor: Long): Long {
        return `val` xor xor and MAX_VALUE
    }

    /**
     * Left shift val by shift bits.  Cut down to 4 bytes.
     */
    private fun leftShift(`val`: Long, shift: Int): Long {
        return `val` shl shift and MAX_VALUE
    }

    /**
     * Convert 4 bytes from the buffer at offset into a long value.
     */
    private fun fourByteToLong(bytes: ByteArray, offset: Int): Long {
        return (byteToLong(bytes[offset + 0])
                + (byteToLong(bytes[offset + 1]) shl 8)
                + (byteToLong(bytes[offset + 2]) shl 16)
                + (byteToLong(bytes[offset + 3]) shl 24))
    }

    /**
     * Mix up the values in the hash function.
     */
    private fun hashMix() {
        a = subtract(a, b)
        a = subtract(a, c)
        a = xor(a, c shr 13)
        b = subtract(b, c)
        b = subtract(b, a)
        b = xor(b, leftShift(a, 8))
        c = subtract(c, a)
        c = subtract(c, b)
        c = xor(c, b shr 13)
        a = subtract(a, b)
        a = subtract(a, c)
        a = xor(a, c shr 12)
        b = subtract(b, c)
        b = subtract(b, a)
        b = xor(b, leftShift(a, 16))
        c = subtract(c, a)
        c = subtract(c, b)
        c = xor(c, b shr 5)
        a = subtract(a, b)
        a = subtract(a, c)
        a = xor(a, c shr 3)
        b = subtract(b, c)
        b = subtract(b, a)
        b = xor(b, leftShift(a, 10))
        c = subtract(c, a)
        c = subtract(c, b)
        c = xor(c, b shr 15)
    }

    /**
     * Hash a variable-length key into a 32-bit value.  Every bit of the
     * key affects every bit of the return value.  Every 1-bit and 2-bit
     * delta achieves avalanche.  The best hash table sizes are powers of 2.
     *
     * @param buffer Byte array that we are hashing on.
     * @param initialValue Initial value of the hash if we are continuing from
     * a previous run.  0 if none.
     * @return Hash value for the buffer.
     */
    @JvmOverloads
    fun hash(buffer: ByteArray, initialValue: Long = 0): Long {
        var len: Int
        var pos: Int

        // set up the internal state
        // the golden ratio; an arbitrary value
        a = 0x09e3779b9L
        // the golden ratio; an arbitrary value
        b = 0x09e3779b9L
        // the previous hash value
        c = initialValue

        // handle most of the key
        pos = 0
        len = buffer.size
        while (len >= 12) {
            a = add(a, fourByteToLong(buffer, pos))
            b = add(b, fourByteToLong(buffer, pos + 4))
            c = add(c, fourByteToLong(buffer, pos + 8))
            hashMix()
            pos += 12
            len -= 12
        }

        c += buffer.size.toLong()

        // all the case statements fall through to the next on purpose
        when (len) {
            11 -> {
                c = add(c, leftShift(byteToLong(buffer[pos + 10]), 24))
                c = add(c, leftShift(byteToLong(buffer[pos + 9]), 16))
                c = add(c, leftShift(byteToLong(buffer[pos + 8]), 8))
                b = add(b, leftShift(byteToLong(buffer[pos + 7]), 24))
                b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16))
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            10 -> {
                c = add(c, leftShift(byteToLong(buffer[pos + 9]), 16))
                c = add(c, leftShift(byteToLong(buffer[pos + 8]), 8))
                b = add(b, leftShift(byteToLong(buffer[pos + 7]), 24))
                b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16))
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            9 -> {
                c = add(c, leftShift(byteToLong(buffer[pos + 8]), 8))
                b = add(b, leftShift(byteToLong(buffer[pos + 7]), 24))
                b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16))
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            // the first byte of c is reserved for the length
            8 -> {
                b = add(b, leftShift(byteToLong(buffer[pos + 7]), 24))
                b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16))
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            7 -> {
                b = add(b, leftShift(byteToLong(buffer[pos + 6]), 16))
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            6 -> {
                b = add(b, leftShift(byteToLong(buffer[pos + 5]), 8))
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            5 -> {
                b = add(b, byteToLong(buffer[pos + 4]))
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            4 -> {
                a = add(a, leftShift(byteToLong(buffer[pos + 3]), 24))
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            3 -> {
                a = add(a, leftShift(byteToLong(buffer[pos + 2]), 16))
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            2 -> {
                a = add(a, leftShift(byteToLong(buffer[pos + 1]), 8))
                a = add(a, byteToLong(buffer[pos + 0]))
            }
            1 -> a = add(a, byteToLong(buffer[pos + 0]))
        }// case 0: nothing left to add
        hashMix()

        return c
    }

    companion object {

        // max value to limit it to 4 bytes
        private val MAX_VALUE = 0xFFFFFFFFL
    }
}
/**
 * See hash(byte[] buffer, long initialValue)
 * @param buffer Byte array that we are hashing on.
 * @return Hash value for the buffer.
 */