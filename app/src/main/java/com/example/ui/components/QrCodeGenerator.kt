package com.example.ui.components

import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, ECC Level L, Versions 1..6).
 * Generates genuine, camera-scannable QR matrices for PayPal & Ko-fi donation URLs
 * without external binary dependencies.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val dataCodewords: Int,
        val ecCodewords: Int,
        val alignmentCenters: IntArray
    )

    // Level L (7% error correction) capacities for Versions 1..6 (single block each)
    private val VERSION_SPECS = listOf(
        VersionSpec(1, 21, 19, 7, intArrayOf()),
        VersionSpec(2, 25, 34, 10, intArrayOf(6, 18)),
        VersionSpec(3, 29, 55, 15, intArrayOf(6, 22)),
        VersionSpec(4, 33, 80, 20, intArrayOf(6, 26)),
        VersionSpec(5, 37, 108, 26, intArrayOf(6, 30)),
        VersionSpec(6, 41, 136, 36, intArrayOf(6, 34)) // Note: V6-L has 2 blocks of (68, 18)
    )

    // Precomputed 15-bit format info strings for ECC Level L (01) and masks 0..7 (already XORed with 0x5412)
    private val FORMAT_INFO_L = intArrayOf(
        0x77C4, 0x72F3, 0x7DAA, 0x789D,
        0x662F, 0x6318, 0x6C41, 0x6976
    )

    // GF(256) log/exp tables with primitive polynomial 0x11D
    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    /**
     * Encodes [text] into a scannable square QR boolean matrix (true = dark module).
     */
    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        // Byte mode header: 4-bit mode (0100) + 8-bit length + 8*N bits + 4-bit terminator
        val spec = VERSION_SPECS.firstOrNull { bytes.size + 2 <= it.dataCodewords }
            ?: VERSION_SPECS.last()

        val dataCodewords = buildDataCodewords(bytes, spec.dataCodewords)
        val allCodewords = if (spec.version == 6) {
            // Version 6-L uses 2 blocks of 68 data + 18 EC codewords interleaved
            val b1Data = dataCodewords.copyOfRange(0, 68)
            val b2Data = dataCodewords.copyOfRange(68, 136)
            val b1Ec = computeReedSolomon(b1Data, 18)
            val b2Ec = computeReedSolomon(b2Data, 18)
            ByteArray(136 + 36).apply {
                var idx = 0
                for (i in 0 until 68) {
                    this[idx++] = b1Data[i]
                    this[idx++] = b2Data[i]
                }
                for (i in 0 until 18) {
                    this[idx++] = b1Ec[i]
                    this[idx++] = b2Ec[i]
                }
            }
        } else {
            val ec = computeReedSolomon(dataCodewords, spec.ecCodewords)
            dataCodewords + ec
        }

        val size = spec.size
        val isFunction = Array(size) { BooleanArray(size) }
        val baseModules = Array(size) { BooleanArray(size) }

        drawFunctionPatterns(baseModules, isFunction, spec)
        placeCodewords(baseModules, isFunction, allCodewords)

        // Evaluate all 8 masks and pick the lowest penalty for crisp scan reliability
        var bestPenalty = Int.MAX_VALUE
        var bestMatrix = baseModules

        for (mask in 0..7) {
            val candidate = Array(size) { r -> baseModules[r].copyOf() }
            applyMask(candidate, isFunction, mask)
            drawFormatBits(candidate, mask)
            val penalty = computePenalty(candidate)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMatrix = candidate
            }
        }
        return bestMatrix
    }

    private fun buildDataCodewords(payload: ByteArray, capacity: Int): ByteArray {
        val bits = ArrayList<Int>(capacity * 8)

        fun appendBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        val cappedLen = payload.size.coerceAtMost(capacity - 2)
        appendBits(0b0100, 4) // Byte mode indicator
        appendBits(cappedLen, 8) // Character count indicator (Versions 1..9)
        for (i in 0 until cappedLen) {
            appendBits(payload[i].toInt() and 0xFF, 8)
        }

        val totalBits = capacity * 8
        // Terminator (up to 4 zero bits)
        repeat(minOf(4, totalBits - bits.size)) { bits.add(0) }
        // Pad to byte boundary
        while (bits.size % 8 != 0) bits.add(0)

        val result = ByteArray(capacity)
        var byteIdx = 0
        var bitIdx = 0
        while (bitIdx < bits.size && byteIdx < capacity) {
            var b = 0
            for (k in 0 until 8) {
                b = (b shl 1) or bits[bitIdx + k]
            }
            result[byteIdx++] = b.toByte()
            bitIdx += 8
        }

        // Pad codewords alternating 0xEC and 0x11
        var padToggle = true
        while (byteIdx < capacity) {
            result[byteIdx++] = if (padToggle) 0xEC.toByte() else 0x11.toByte()
            padToggle = !padToggle
        }
        return result
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun computeReedSolomon(data: ByteArray, ecCount: Int): ByteArray {
        // Generator polynomial coefficients of degree ecCount
        var gen = IntArray(ecCount + 1)
        gen[0] = 1
        for (i in 0 until ecCount) {
            val next = IntArray(ecCount + 1)
            val factor = GF_EXP[i]
            for (j in 0..i) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], factor)
            }
            gen = next
        }

        val rem = IntArray(ecCount)
        for (rawByte in data) {
            val factor = (rawByte.toInt() and 0xFF) xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until ecCount) {
                    rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return ByteArray(ecCount) { rem[it].toByte() }
    }

    private fun drawFunctionPatterns(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        spec: VersionSpec
    ) {
        val size = spec.size

        fun setFunc(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        // 1. Finder patterns + 1-module white separator
        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r in 0 until size && c in 0 until size) {
                        val inBox = dr in 0..6 && dc in 0..6
                        val isDark = inBox && (
                            dr == 0 || dr == 6 || dc == 0 || dc == 6 ||
                                (dr in 2..4 && dc in 2..4)
                            )
                        setFunc(r, c, isDark)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            setFunc(6, i, dark)
            setFunc(i, 6, dark)
        }

        // 3. Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                if (isFunction[cy][cx]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                        setFunc(cy + dr, cx + dc, dist != 1)
                    }
                }
            }
        }

        // 4. Reserve format information areas + dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunc(8, size - 1 - i, false)
            setFunc(size - 1 - i, 8, false)
        }
        setFunc(size - 8, 8, true) // Dark module
    }

    private fun placeCodewords(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: ByteArray
    ) {
        val size = modules.size
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var upward = true
        var col = size - 1

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing column
            for (step in 0 until size) {
                val row = if (upward) size - 1 - step else step
                for (dx in 0..1) {
                    val c = col - dx
                    if (!isFunction[row][c]) {
                        val dark = if (bitIndex < totalBits) {
                            val b = codewords[bitIndex ushr 3].toInt() and 0xFF
                            ((b ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[row][c] = dark
                        bitIndex++
                    }
                }
            }
            upward = !upward
            col -= 2
        }
    }

    private fun applyMask(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, mask: Int) {
        val size = modules.size
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (isFunction[r][c]) continue
                val invert = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> ((r / 2) + (c / 3)) % 2 == 0
                    5 -> ((r * c) % 2 + (r * c) % 3) == 0
                    6 -> (((r * c) % 2 + (r * c) % 3) % 2) == 0
                    7 -> (((r + c) % 2 + (r * c) % 3) % 2) == 0
                    else -> false
                }
                if (invert) modules[r][c] = !modules[r][c]
            }
        }
    }

    private fun drawFormatBits(modules: Array<BooleanArray>, mask: Int) {
        val size = modules.size
        val bits = FORMAT_INFO_L[mask]
        fun bit(i: Int): Boolean = ((bits ushr i) and 1) != 0

        // Around top-left finder
        for (i in 0..5) modules[8][i] = bit(14 - i)
        modules[8][7] = bit(8)
        modules[8][8] = bit(7)
        modules[7][8] = bit(6)
        for (i in 0..5) modules[5 - i][8] = bit(5 - i)

        // Split across top-right & bottom-left
        for (i in 0..6) modules[size - 1 - i][8] = bit(14 - i)
        for (i in 0..7) modules[8][size - 8 + i] = bit(7 - i)
    }

    private fun computePenalty(modules: Array<BooleanArray>): Int {
        val size = modules.size
        var penalty = 0

        // Rule 1: Runs of 5+ same-colored modules in rows & cols
        for (r in 0 until size) {
            var runLen = 1
            for (c in 1 until size) {
                if (modules[r][c] == modules[r][c - 1]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (c in 0 until size) {
            var runLen = 1
            for (r in 1 until size) {
                if (modules[r][c] == modules[r - 1][c]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }

        // Rule 2: 2x2 blocks of same color
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = modules[r][c]
                if (color == modules[r][c + 1] && color == modules[r + 1][c] && color == modules[r + 1][c + 1]) {
                    penalty += 3
                }
            }
        }
        return penalty
    }
}
