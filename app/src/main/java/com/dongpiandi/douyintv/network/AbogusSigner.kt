package com.dongpiandi.douyintv.network

import java.util.Base64
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kotlin adaptation of abogus 2.1.1 by sobird, licensed under MIT.
 * Upstream: https://github.com/sobird/abogus
 * See THIRD_PARTY_NOTICES.md for the full attribution and license text.
 */
class AbogusSigner(
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val random: () -> Double = { ThreadLocalRandom.current().nextDouble() },
    private val startedAt: Long = System.currentTimeMillis(),
) {
    private val counter = AtomicInteger(1)

    fun sign(
        query: String,
        body: String = "",
        userAgent: String = USER_AGENT,
        platform: String = "Win32",
        browser: String = "Chrome",
        now: Long = nowProvider(),
        initAt: Long = startedAt,
        requestCounter: Int = counter.getAndIncrement(),
    ): String {
        val env = Env(
            userAgent = userAgent,
            platform = platform,
            browser = browser,
        )
        val built = buildPlain(
            query = query,
            body = body,
            env = env,
            now = now,
            initAt = initAt,
            counter = requestCounter,
        )

        val prefix = obfuscate(byteArrayOf(3, 82), 1, env)
        val transformed = transform(built.plain, random)
        val encrypted = rc4Douyin(built.versionBlock + transformed, byteArrayOf(211.toByte()))
        return encodeBase64(prefix + encrypted, OUTPUT_ALPHABET)
    }

    fun verifyFp(now: Long = nowProvider()): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val chars = CharArray(36) { '\u0000' }
        intArrayOf(8, 13, 18, 23).forEach { chars[it] = '_' }
        chars[14] = '4'
        for (index in chars.indices) {
            if (chars[index] != '\u0000') continue
            val value = if (index == 19) {
                (randomIndex(62) and 3) or 8
            } else {
                randomIndex(62)
            }
            chars[index] = alphabet[value]
        }
        return "verify_${now.toString(36)}_${chars.concatToString()}"
    }

    private fun buildPlain(
        query: String,
        body: String,
        env: Env,
        now: Long,
        initAt: Long,
        counter: Int,
    ): BuiltPlain {
        val source = ByteArray(88)
        val hashes = hashInputs(env, query, body)
        source[24] = 41
        source[26] = ((now - ABOGUS_EPOCH) / 1_000L / 60L / 60L / 24L / 14L).toByte()

        val init = if (initAt > 0) initAt - 1 else 1_000L
        source[22] = init.toByte()
        source[27] = counterBucket(counter).toByte()
        source[28] = if (initAt > 0) ((now - initAt + 3) and 255).toByte() else 2
        writeLittleEndian(source, 29, now, 6)
        writeLittleEndian(source, 35, env.probe.toLong(), 2)
        writeLittleEndian(source, 38, env.random[4].toLong(), 2)
        source[40] = env.random[0].toByte()
        source[41] = env.random[1].toByte()
        source[42] = env.random[2].toByte()
        source[43] = env.random[3].toByte()
        writeLittleEndian(source, 44, env.track.toLong(), 4)
        source[66] = if (env.tampered) 3 else 12
        source[48] = hashes.params[9]
        source[49] = hashes.params[18]
        source[50] = 3
        source[51] = scan(
            bytes = hashes.params,
            start = 3,
            sentinel = 11,
            fallback = 12,
            flags = env.random[4],
            mask = 2,
        ).toByte()
        source[52] = hashes.body[10]
        source[53] = hashes.body[19]
        source[54] = 4
        source[55] = scan(
            bytes = hashes.body,
            start = 4,
            sentinel = 8,
            fallback = 9,
            flags = env.random[4],
            mask = 4,
        ).toByte()
        source[56] = hashes.probe[11]
        source[57] = hashes.probe[21]
        source[58] = 5
        source[59] = scan(
            bytes = hashes.probe,
            start = 5,
            sentinel = 12,
            fallback = 13,
            flags = env.random[4],
            mask = 8,
        ).toByte()
        writeLittleEndian(source, 60, init, 6)
        writeLittleEndian(source, 67, env.pageId.toLong(), 4)
        writeLittleEndian(source, 71, env.aid.toLong(), 4)

        val fingerprint = encodeLatin1(fingerprintSource(env))
        val marker = encodeLatin1("${((now + 3) and 255)},")
        source[79] = (fingerprint.size and 255).toByte()
        source[80] = ((fingerprint.size shr 8) and 255).toByte()
        source[84] = (marker.size and 255).toByte()
        source[85] = ((marker.size shr 8) and 255).toByte()

        val version = buildVersionBlock(env)
        val checksum = (
            version.map { it.toInt() and 255 } +
                CHECKSUM_SLOTS.map { source[it].toInt() and 255 }
            )
            .fold(0) { acc, value -> acc xor value }
            .toByte()
        val plain = ByteArray(SLOT_ORDER.size) { source[SLOT_ORDER[it]] } +
            fingerprint +
            marker +
            checksum
        return BuiltPlain(plain = plain, versionBlock = version)
    }

    private fun hashInputs(env: Env, query: String, body: String): Hashes {
        val params = sm3(sm3((query + HASH_SALT).toByteArray(Charsets.UTF_8)))
        val bodyHash = sm3(sm3((body + HASH_SALT).toByteArray(Charsets.UTF_8)))
        val probeKey = byteArrayOf(
            ((env.probe shr 8) and 255).toByte(),
            (env.probe and 255).toByte(),
            (env.track and 255).toByte(),
        )
        val probeString = encodeBase64(
            rc4Douyin(env.userAgent.trim().toByteArray(Charsets.UTF_8), probeKey),
            PROBE_ALPHABET,
        )
        val probe = sm3(probeString.toByteArray(Charsets.UTF_8))
        return Hashes(params = params, body = bodyHash, probe = probe)
    }

    private fun fingerprintSource(env: Env): String = listOf(
        env.viewportInnerWidth,
        env.viewportInnerHeight,
        env.viewportOuterWidth,
        env.viewportOuterHeight,
        env.screenAvailWidth,
        env.screenAvailHeight,
        env.screenWidth,
        env.screenHeight,
        env.platform,
    ).joinToString("|")

    private fun obfuscate(values: ByteArray, index: Int, env: Env): ByteArray {
        val randomValue = (random() * 65_535).toInt()
        var low = randomValue and 255
        var high = (randomValue shr 8) and 255
        when (index) {
            1 -> high = browserByte(env)
            2 -> {
                low = randomLowByte()
                high = permissionByte(env)
            }
        }
        return byteArrayOf(
            ((low and 170) or (values[0].toInt() and 85)).toByte(),
            ((low and 85) or (values[0].toInt() and 170)).toByte(),
            ((high and 170) or (values[1].toInt() and 85)).toByte(),
            ((high and 85) or (values[1].toInt() and 170)).toByte(),
        )
    }

    private fun browserByte(env: Env): Int {
        val offset = when (env.browser) {
            "Chrome" -> 0
            "Firefox" -> 40
            "Safari" -> 81
            "Edge" -> 125
            "Huawei" -> 170
            else -> 210
        }
        return ((random() * 40).toInt()) + offset
    }

    private fun randomLowByte(): Int {
        if (envFlag64()) {
            val value = (random() * 109).toInt()
            return value + 110 + (value % 2)
        }
        val value = (random() * 240).toInt()
        return if (value > 109) value + (value % 2) + 1 else value
    }

    private fun envFlag64(): Boolean {
        // DEFAULT_ENV uses random[4] == 1, whose 0x40 bit is clear.
        return false
    }

    private fun permissionByte(env: Env): Int {
        var value = (random() * 255).toInt() and 77
        if (env.permissionsOwn == "granted") value = value or 2
        if (env.permissionsSealed) value = value or 16
        if (env.permissionsProto == "grnated") value = value or 32
        if (env.permissionsSealed) value = value or 128
        return value
    }

    private fun transform(input: ByteArray, random: () -> Double): ByteArray {
        val output = ByteArray((input.size + 2) / 3 * 4)
        var offset = 0
        var read = 0
        while (read < input.size) {
            if (read + 2 < input.size) {
                val noise = ((random() * 1_000).toInt()) and 255
                val first = input[read].toInt() and 255
                val second = input[read + 1].toInt() and 255
                val third = input[read + 2].toInt() and 255
                output[offset++] = ((noise and 145) or (first and 110)).toByte()
                output[offset++] = ((noise and 66) or (second and 189)).toByte()
                output[offset++] = ((noise and 44) or (third and 211)).toByte()
                output[offset++] = ((first and 145) or (second and 66) or (third and 44)).toByte()
            } else {
                output[offset++] = input[read]
                if (read + 1 < input.size && input[read + 1] != 0.toByte()) {
                    output[offset++] = input[read + 1]
                }
            }
            read += 3
        }
        return output.copyOf(offset)
    }

    private fun counterBucket(counter: Int): Int = when {
        counter > 10_745 -> 3
        counter > 1_283 -> 4
        counter > 139 -> 5
        else -> 6
    }

    private fun scan(
        bytes: ByteArray,
        start: Int,
        sentinel: Int,
        fallback: Int,
        flags: Int,
        mask: Int,
    ): Int {
        var index = start
        var current = bytes[index].toInt() and 255
        while (current == sentinel) {
            index += 1
            current = if (index < bytes.size) bytes[index].toInt() and 255 else fallback
        }
        return if (flags and mask != 0) sentinel else current
    }

    private fun randomIndex(bound: Int): Int = (random() * bound).toInt()

    private data class Hashes(
        val params: ByteArray,
        val body: ByteArray,
        val probe: ByteArray,
    )

    private data class BuiltPlain(
        val plain: ByteArray,
        val versionBlock: ByteArray,
    )

    private data class Env(
        val userAgent: String,
        val platform: String,
        val browser: String,
        val screenWidth: Int = 1440,
        val screenHeight: Int = 900,
        val screenAvailWidth: Int = 1440,
        val screenAvailHeight: Int = 900,
        val viewportInnerWidth: Int = 800,
        val viewportInnerHeight: Int = 600,
        val viewportOuterWidth: Int = 1200,
        val viewportOuterHeight: Int = 856,
        val probe: Int = 37,
        val track: Int = 14,
        val random: IntArray = intArrayOf(0, 0, 0, 0, 1),
        val permissionsOwn: String = "granted",
        val permissionsProto: String = "grnated",
        val permissionsSealed: Boolean = true,
        val tampered: Boolean = true,
        val aid: Int = 6383,
        val pageId: Int = 6241,
    )

    private object Sm3 {
        private val initial = intArrayOf(
            0x7380166f,
            0x4914b2b9,
            0x172442d7,
            0xda8a0600.toInt(),
            0xa96f30bc.toInt(),
            0x163138aa,
            0xe38dee4d.toInt(),
            0xb0fb0e4e.toInt(),
        )

        fun hash(message: ByteArray): ByteArray {
            val bitLength = message.size.toLong() * 8
            val paddedLength = ((message.size + 9 + 63) / 64) * 64
            val padded = ByteArray(paddedLength)
            message.copyInto(padded)
            padded[message.size] = 0x80.toByte()
            for (index in 0 until 8) {
                padded[paddedLength - 1 - index] = ((bitLength ushr (index * 8)) and 255).toByte()
            }

            val state = initial.copyOf()
            for (blockStart in 0 until paddedLength step 64) {
                val w = IntArray(68)
                for (index in 0 until 16) {
                    val base = blockStart + index * 4
                    w[index] =
                        ((padded[base].toInt() and 255) shl 24) or
                        ((padded[base + 1].toInt() and 255) shl 16) or
                        ((padded[base + 2].toInt() and 255) shl 8) or
                        (padded[base + 3].toInt() and 255)
                }
                for (index in 16 until 68) {
                    w[index] =
                        p1(w[index - 16] xor w[index - 9] xor Integer.rotateLeft(w[index - 3], 15)) xor
                        Integer.rotateLeft(w[index - 13], 7) xor
                        w[index - 6]
                }
                val w1 = IntArray(64) { w[it] xor w[it + 4] }
                var a = state[0]
                var b = state[1]
                var c = state[2]
                var d = state[3]
                var e = state[4]
                var f = state[5]
                var g = state[6]
                var h = state[7]

                for (index in 0 until 64) {
                    val t = if (index < 16) 0x79cc4519 else 0x7a879d8a
                    val ss1 = Integer.rotateLeft(
                        Integer.rotateLeft(a, 12) + e + Integer.rotateLeft(t, index),
                        7,
                    )
                    val ss2 = ss1 xor Integer.rotateLeft(a, 12)
                    val tt1 = ff(a, b, c, index) + d + ss2 + w1[index]
                    val tt2 = gg(e, f, g, index) + h + ss1 + w[index]
                    d = c
                    c = Integer.rotateLeft(b, 9)
                    b = a
                    a = tt1
                    h = g
                    g = Integer.rotateLeft(f, 19)
                    f = e
                    e = p0(tt2)
                }
                state[0] = state[0] xor a
                state[1] = state[1] xor b
                state[2] = state[2] xor c
                state[3] = state[3] xor d
                state[4] = state[4] xor e
                state[5] = state[5] xor f
                state[6] = state[6] xor g
                state[7] = state[7] xor h
            }

            return ByteArray(32).also { output ->
                for (index in state.indices) {
                    output[index * 4] = (state[index] ushr 24).toByte()
                    output[index * 4 + 1] = (state[index] ushr 16).toByte()
                    output[index * 4 + 2] = (state[index] ushr 8).toByte()
                    output[index * 4 + 3] = state[index].toByte()
                }
            }
        }

        private fun ff(x: Int, y: Int, z: Int, index: Int): Int =
            if (index < 16) x xor y xor z else (x and y) or (x and z) or (y and z)

        private fun gg(x: Int, y: Int, z: Int, index: Int): Int =
            if (index < 16) x xor y xor z else (x and y) or (x.inv() and z)

        private fun p0(value: Int): Int =
            value xor Integer.rotateLeft(value, 9) xor Integer.rotateLeft(value, 17)

        private fun p1(value: Int): Int =
            value xor Integer.rotateLeft(value, 15) xor Integer.rotateLeft(value, 23)
    }

    private fun sm3(value: ByteArray): ByteArray = Sm3.hash(value)

    private fun rc4Douyin(input: ByteArray, key: ByteArray): ByteArray {
        require(key.isNotEmpty())
        val keyLength = minOf(key.size, 256)
        val box = IntArray(256) { 255 - it }
        var j = 0
        for (index in 0 until 256) {
            val current = box[index]
            j = (j * (current + 1) + (key[index % keyLength].toInt() and 255)) and 255
            val swapped = box[j]
            box[index] = swapped
            box[j] = current
        }
        var i = 0
        j = 0
        return ByteArray(input.size) { index ->
            i = (i + 1) and 255
            j = (j + box[i]) and 255
            val first = box[i]
            val second = box[j]
            box[i] = second
            box[j] = first
            (input[index].toInt() xor box[(first + second) and 255]).toByte()
        }
    }

    private fun writeLittleEndian(target: ByteArray, offset: Int, value: Long, length: Int) {
        for (index in 0 until length) {
            target[offset + index] = ((value ushr (8 * index)) and 255).toByte()
        }
    }

    private fun encodeLatin1(value: String): ByteArray = value.toByteArray(Charsets.ISO_8859_1)

    private fun encodeBase64(value: ByteArray, alphabet: String): String {
        val standard = Base64.getEncoder().encodeToString(value)
        return buildString(standard.length) {
            standard.forEach { char ->
                when (char) {
                    '=' -> append('=')
                    '+' -> append(alphabet[62])
                    '/' -> append(alphabet[63])
                    in 'A'..'Z' -> append(alphabet[char - 'A'])
                    in 'a'..'z' -> append(alphabet[26 + (char - 'a')])
                    in '0'..'9' -> append(alphabet[52 + (char - '0')])
                    else -> append(char)
                }
            }
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val HASH_SALT = "dhzx"
        const val ABOGUS_EPOCH = 1_721_836_800_000L
        const val PROBE_ALPHABET = "ckdp1h4ZKsUB80/Mfvw36XIgR25+WQAlEi7NLboqYTOPuzmFjJnryx9HVGDaStCe"
        const val OUTPUT_ALPHABET = "Dkdpgh2ZmsQB80/MfvV36XI1R45-WUAlEixNLwoqYTOPuzKFjJnry79HbGcaStCe"
        val VERSION_SEGMENTS = intArrayOf(1, 0, 1, 0)
        val SLOT_ORDER = intArrayOf(
            34, 44, 56, 61, 73, 29, 70, 45, 35, 49, 38, 66, 51, 68, 28, 48,
            64, 47, 30, 71, 26, 55, 31, 69, 59, 40, 62, 63, 27, 72, 41, 74,
            57, 52, 42, 39, 33, 67, 53, 43, 65, 46, 36, 24, 60, 32, 79, 80,
            84, 85,
        )
        val CHECKSUM_SLOTS = intArrayOf(
            24, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 38, 39, 40, 41,
            42, 43, 44, 45, 46, 47, 48, 49, 51, 52, 53, 55, 56, 57, 59, 60,
            61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 79, 80,
            84, 85,
        )

    }

    private fun buildVersionBlock(env: Env): ByteArray {
        val first = obfuscate(
            byteArrayOf(VERSION_SEGMENTS[0].toByte(), VERSION_SEGMENTS[1].toByte()),
            0,
            env,
        )
        val second = obfuscate(
            byteArrayOf(VERSION_SEGMENTS[2].toByte(), VERSION_SEGMENTS[3].toByte()),
            2,
            env,
        )
        return first + second
    }
}
