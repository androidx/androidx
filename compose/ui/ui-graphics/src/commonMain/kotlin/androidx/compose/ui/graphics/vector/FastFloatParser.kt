/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalUnsignedTypes::class)

package androidx.compose.ui.graphics.vector

/**
 * The code below is adapted from:
 *     https://github.com/fastfloat/fast_float
 *     https://github.com/lemire/fast_double_parser/
 * The original C++ implementations are licensed under Apache 2.0
 */

internal class FloatResult(var value: Float = Float.NaN, var isValid: Boolean = false)

internal class FastFloatParser {
    companion object {
        private const val FloatMinExponent = -10
        private const val FloatMaxExponent = 10
        private const val FloatSmallestExponent = -325
        private const val FloatMaxExponentNumber = 1024

        private val PowersOfTen = floatArrayOf(
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f
        )

        /**
         * Parses the next float in the char sequence [s], starting at offset [start], until at most
         * the end offset [end]. The results are written in [result]. When a result is valid, the
         * `result.isValid` is set to `true` and `result.value` contains the parsed float value. If
         * parsing is unsuccessful, `result.isValid` is false and `result.value` is set to
         * [Float.NaN].
         *
         * This function returns the offset inside the char sequence right after parsing stopped,
         * successfully or unsuccessfully.
         */
        fun nextFloat(s: String, start: Int, end: Int, result: FloatResult): Int {
            result.value = Float.NaN
            result.isValid = false

            if (start == end) return start

            var index = start
            var c = s[index]

            // Check for leading negative sign
            val isNegative = c == '-'
            if (isNegative) {
                index++
                if (index == end) return index

                // Safe access, we just checked the bounds
                c = s[index]
                if (!c.isDigit && c != '.') return index
            }

            // TODO: Should we use an unsigned long here?
            var significand = 0L
            val significandStartIndex = index

            // Parse the integer part
            while (index != end && c.isDigit) {
                significand = 10L * significand + (c.code - '0'.code).toLong()
                c = charAt(s, ++index)
            }

            val significandEndIndex = index
            var digitCount = index - significandStartIndex

            var exponent = 0
            var exponentStartIndex = index
            var exponentEndIndex = index

            // Parse the fraction
            if (index != end && c == '.') {
                index++
                exponentStartIndex = index

                while (end - index >= 4) {
                    val digits = parseFourDigits(s, index)
                    if (digits < 0) break
                    significand = 10_000L * significand + digits.toLong()
                    index += 4
                }

                c = charAt(s, index)
                while (index != end && c.isDigit) {
                    significand = 10L * significand + (c.code - '0'.code).toLong()
                    c = charAt(s, ++index)
                }

                exponent = exponentStartIndex - index
                exponentEndIndex = index
                digitCount -= exponent
            }

            if (digitCount == 0) return index

            // Parse the exponent part of the float, if present
            var exponentNumber = 0
            if ((c.code or 0x20) == 'e'.code) {
                c = charAt(s, ++index)

                val isExponentNegative = c == '-'
                if (isExponentNegative || c == '+') {
                    index++
                }

                c = s[index]
                while (index != end && c.isDigit) {
                    if (exponentNumber < FloatMaxExponentNumber) {
                        exponentNumber = 10 * exponentNumber + (c.code - '0'.code)
                    }
                    c = charAt(s, ++index)
                }

                if (isExponentNegative) exponentNumber = -exponentNumber
                exponent += exponentNumber
            }

            // TODO: check for f/F suffix?

            var tooManyDigits = false

            // If we have too many digits we need to retry differently and avoid overflows
            if (digitCount > 19) {
                var retryIndex = significandStartIndex
                c = s[retryIndex]

                // First check for the case where the number is 0.0000000xxxx (could be all zeroes)
                while (index != end && (c == '0' || c == '.')) {
                    if (c == '0') digitCount--
                    c = charAt(s, ++retryIndex)
                }

                if (digitCount > 19) {
                    tooManyDigits = true

                    significand = 0
                    retryIndex = significandStartIndex
                    c = s[retryIndex]

                    while (
                        retryIndex != significandEndIndex &&
                        significand.toULong() < 1000000000000000000UL
                    ) {
                        significand = 10L * significand + (c.code - '0'.code).toLong()
                        c = charAt(s, ++retryIndex)
                    }

                    if (significand.toULong() >= 1000000000000000000UL) {
                        exponent = significandEndIndex - retryIndex + exponentNumber
                    } else {
                        retryIndex = exponentStartIndex
                        c = s[retryIndex]

                        while (
                            retryIndex != exponentEndIndex &&
                            significand.toULong() < 1000000000000000000UL
                        ) {
                            significand = 10L * significand + (c.code - '0'.code).toLong()
                            c = charAt(s, ++retryIndex)
                        }
                        exponent = exponentStartIndex - retryIndex + exponentNumber
                    }
                }
            }

            // Fast path
            if (exponent in FloatMinExponent..FloatMaxExponent &&
                !tooManyDigits &&
                significand.toULong() <= 1UL shl 24
            ) {
                var f = significand.toFloat()
                if (exponent < 0) {
                    f /= PowersOfTen[-exponent]
                } else {
                    f *= PowersOfTen[exponent]
                }

                result.isValid = true
                result.value = if (isNegative) -f else f

                return index
            }

            // Now we need to take the slow path, please refer to the original C++ code for a
            // complete description of the algorithm

            if (significand == 0L) {
                result.isValid = true
                result.value = if (isNegative) -0.0f else 0.0f
                return index
            }

            if (exponent !in -126..127) {
                try {
                    result.value = s.substring(start, index).toFloat()
                } finally {
                    result.isValid = true
                }
                return index
            }

            val significandFactor = Mantissa64[exponent - FloatSmallestExponent].toLong()
            var lz = significand.countLeadingZeroBits()
            significand = significand shl lz

            val upper = fullMultiplicationHighBits(significand, significandFactor)
            val upperBit = (upper ushr 63).toInt()
            var mantissa = upper ushr (upperBit + 9)
            lz += 1 xor upperBit

            if (upper and 0x1ff == 0x1ffL || upper and 0x1ff == 0L && mantissa and 3L == 1L) {
                try {
                    result.value = s.substring(start, index).toFloat()
                } finally {
                    result.isValid = true
                }
                return index
            }

            mantissa += 1
            mantissa = mantissa ushr 1

            if (mantissa >= 1L shl 53) {
                mantissa = 1L shl 52
                lz--
            }

            mantissa = mantissa and (1L shl 52).inv()

            val adjustedExponent = (((152170L + 65536L) * exponent) shr 16) + 1024 + 63
            val realExponent = adjustedExponent - lz
            if (realExponent < 1 || realExponent > 2046) {
                try {
                    result.value = s.substring(start, index).toFloat()
                } finally {
                    result.isValid = true
                }
                return index
            }

            mantissa = mantissa or (realExponent shl 52)
            mantissa = mantissa or if (isNegative) 1L shl 63 else 0L

            result.isValid = true
            result.value = Double.fromBits(mantissa).toFloat()

            return index
        }

        private val Mantissa64 = ulongArrayOf(
            0xa5ced43b7e3e9188UL, 0xcf42894a5dce35eaUL,
            0x818995ce7aa0e1b2UL, 0xa1ebfb4219491a1fUL,
            0xca66fa129f9b60a6UL, 0xfd00b897478238d0UL,
            0x9e20735e8cb16382UL, 0xc5a890362fddbc62UL,
            0xf712b443bbd52b7bUL, 0x9a6bb0aa55653b2dUL,
            0xc1069cd4eabe89f8UL, 0xf148440a256e2c76UL,
            0x96cd2a865764dbcaUL, 0xbc807527ed3e12bcUL,
            0xeba09271e88d976bUL, 0x93445b8731587ea3UL,
            0xb8157268fdae9e4cUL, 0xe61acf033d1a45dfUL,
            0x8fd0c16206306babUL, 0xb3c4f1ba87bc8696UL,
            0xe0b62e2929aba83cUL, 0x8c71dcd9ba0b4925UL,
            0xaf8e5410288e1b6fUL, 0xdb71e91432b1a24aUL,
            0x892731ac9faf056eUL, 0xab70fe17c79ac6caUL,
            0xd64d3d9db981787dUL, 0x85f0468293f0eb4eUL,
            0xa76c582338ed2621UL, 0xd1476e2c07286faaUL,
            0x82cca4db847945caUL, 0xa37fce126597973cUL,
            0xcc5fc196fefd7d0cUL, 0xff77b1fcbebcdc4fUL,
            0x9faacf3df73609b1UL, 0xc795830d75038c1dUL,
            0xf97ae3d0d2446f25UL, 0x9becce62836ac577UL,
            0xc2e801fb244576d5UL, 0xf3a20279ed56d48aUL,
            0x9845418c345644d6UL, 0xbe5691ef416bd60cUL,
            0xedec366b11c6cb8fUL, 0x94b3a202eb1c3f39UL,
            0xb9e08a83a5e34f07UL, 0xe858ad248f5c22c9UL,
            0x91376c36d99995beUL, 0xb58547448ffffb2dUL,
            0xe2e69915b3fff9f9UL, 0x8dd01fad907ffc3bUL,
            0xb1442798f49ffb4aUL, 0xdd95317f31c7fa1dUL,
            0x8a7d3eef7f1cfc52UL, 0xad1c8eab5ee43b66UL,
            0xd863b256369d4a40UL, 0x873e4f75e2224e68UL,
            0xa90de3535aaae202UL, 0xd3515c2831559a83UL,
            0x8412d9991ed58091UL, 0xa5178fff668ae0b6UL,
            0xce5d73ff402d98e3UL, 0x80fa687f881c7f8eUL,
            0xa139029f6a239f72UL, 0xc987434744ac874eUL,
            0xfbe9141915d7a922UL, 0x9d71ac8fada6c9b5UL,
            0xc4ce17b399107c22UL, 0xf6019da07f549b2bUL,
            0x99c102844f94e0fbUL, 0xc0314325637a1939UL,
            0xf03d93eebc589f88UL, 0x96267c7535b763b5UL,
            0xbbb01b9283253ca2UL, 0xea9c227723ee8bcbUL,
            0x92a1958a7675175fUL, 0xb749faed14125d36UL,
            0xe51c79a85916f484UL, 0x8f31cc0937ae58d2UL,
            0xb2fe3f0b8599ef07UL, 0xdfbdcece67006ac9UL,
            0x8bd6a141006042bdUL, 0xaecc49914078536dUL,
            0xda7f5bf590966848UL, 0x888f99797a5e012dUL,
            0xaab37fd7d8f58178UL, 0xd5605fcdcf32e1d6UL,
            0x855c3be0a17fcd26UL, 0xa6b34ad8c9dfc06fUL,
            0xd0601d8efc57b08bUL, 0x823c12795db6ce57UL,
            0xa2cb1717b52481edUL, 0xcb7ddcdda26da268UL,
            0xfe5d54150b090b02UL, 0x9efa548d26e5a6e1UL,
            0xc6b8e9b0709f109aUL, 0xf867241c8cc6d4c0UL,
            0x9b407691d7fc44f8UL, 0xc21094364dfb5636UL,
            0xf294b943e17a2bc4UL, 0x979cf3ca6cec5b5aUL,
            0xbd8430bd08277231UL, 0xece53cec4a314ebdUL,
            0x940f4613ae5ed136UL, 0xb913179899f68584UL,
            0xe757dd7ec07426e5UL, 0x9096ea6f3848984fUL,
            0xb4bca50b065abe63UL, 0xe1ebce4dc7f16dfbUL,
            0x8d3360f09cf6e4bdUL, 0xb080392cc4349decUL,
            0xdca04777f541c567UL, 0x89e42caaf9491b60UL,
            0xac5d37d5b79b6239UL, 0xd77485cb25823ac7UL,
            0x86a8d39ef77164bcUL, 0xa8530886b54dbdebUL,
            0xd267caa862a12d66UL, 0x8380dea93da4bc60UL,
            0xa46116538d0deb78UL, 0xcd795be870516656UL,
            0x806bd9714632dff6UL, 0xa086cfcd97bf97f3UL,
            0xc8a883c0fdaf7df0UL, 0xfad2a4b13d1b5d6cUL,
            0x9cc3a6eec6311a63UL, 0xc3f490aa77bd60fcUL,
            0xf4f1b4d515acb93bUL, 0x991711052d8bf3c5UL,
            0xbf5cd54678eef0b6UL, 0xef340a98172aace4UL,
            0x9580869f0e7aac0eUL, 0xbae0a846d2195712UL,
            0xe998d258869facd7UL, 0x91ff83775423cc06UL,
            0xb67f6455292cbf08UL, 0xe41f3d6a7377eecaUL,
            0x8e938662882af53eUL, 0xb23867fb2a35b28dUL,
            0xdec681f9f4c31f31UL, 0x8b3c113c38f9f37eUL,
            0xae0b158b4738705eUL, 0xd98ddaee19068c76UL,
            0x87f8a8d4cfa417c9UL, 0xa9f6d30a038d1dbcUL,
            0xd47487cc8470652bUL, 0x84c8d4dfd2c63f3bUL,
            0xa5fb0a17c777cf09UL, 0xcf79cc9db955c2ccUL,
            0x81ac1fe293d599bfUL, 0xa21727db38cb002fUL,
            0xca9cf1d206fdc03bUL, 0xfd442e4688bd304aUL,
            0x9e4a9cec15763e2eUL, 0xc5dd44271ad3cdbaUL,
            0xf7549530e188c128UL, 0x9a94dd3e8cf578b9UL,
            0xc13a148e3032d6e7UL, 0xf18899b1bc3f8ca1UL,
            0x96f5600f15a7b7e5UL, 0xbcb2b812db11a5deUL,
            0xebdf661791d60f56UL, 0x936b9fcebb25c995UL,
            0xb84687c269ef3bfbUL, 0xe65829b3046b0afaUL,
            0x8ff71a0fe2c2e6dcUL, 0xb3f4e093db73a093UL,
            0xe0f218b8d25088b8UL, 0x8c974f7383725573UL,
            0xafbd2350644eeacfUL, 0xdbac6c247d62a583UL,
            0x894bc396ce5da772UL, 0xab9eb47c81f5114fUL,
            0xd686619ba27255a2UL, 0x8613fd0145877585UL,
            0xa798fc4196e952e7UL, 0xd17f3b51fca3a7a0UL,
            0x82ef85133de648c4UL, 0xa3ab66580d5fdaf5UL,
            0xcc963fee10b7d1b3UL, 0xffbbcfe994e5c61fUL,
            0x9fd561f1fd0f9bd3UL, 0xc7caba6e7c5382c8UL,
            0xf9bd690a1b68637bUL, 0x9c1661a651213e2dUL,
            0xc31bfa0fe5698db8UL, 0xf3e2f893dec3f126UL,
            0x986ddb5c6b3a76b7UL, 0xbe89523386091465UL,
            0xee2ba6c0678b597fUL, 0x94db483840b717efUL,
            0xba121a4650e4ddebUL, 0xe896a0d7e51e1566UL,
            0x915e2486ef32cd60UL, 0xb5b5ada8aaff80b8UL,
            0xe3231912d5bf60e6UL, 0x8df5efabc5979c8fUL,
            0xb1736b96b6fd83b3UL, 0xddd0467c64bce4a0UL,
            0x8aa22c0dbef60ee4UL, 0xad4ab7112eb3929dUL,
            0xd89d64d57a607744UL, 0x87625f056c7c4a8bUL,
            0xa93af6c6c79b5d2dUL, 0xd389b47879823479UL,
            0x843610cb4bf160cbUL, 0xa54394fe1eedb8feUL,
            0xce947a3da6a9273eUL, 0x811ccc668829b887UL,
            0xa163ff802a3426a8UL, 0xc9bcff6034c13052UL,
            0xfc2c3f3841f17c67UL, 0x9d9ba7832936edc0UL,
            0xc5029163f384a931UL, 0xf64335bcf065d37dUL,
            0x99ea0196163fa42eUL, 0xc06481fb9bcf8d39UL,
            0xf07da27a82c37088UL, 0x964e858c91ba2655UL,
            0xbbe226efb628afeaUL, 0xeadab0aba3b2dbe5UL,
            0x92c8ae6b464fc96fUL, 0xb77ada0617e3bbcbUL,
            0xe55990879ddcaabdUL, 0x8f57fa54c2a9eab6UL,
            0xb32df8e9f3546564UL, 0xdff9772470297ebdUL,
            0x8bfbea76c619ef36UL, 0xaefae51477a06b03UL,
            0xdab99e59958885c4UL, 0x88b402f7fd75539bUL,
            0xaae103b5fcd2a881UL, 0xd59944a37c0752a2UL,
            0x857fcae62d8493a5UL, 0xa6dfbd9fb8e5b88eUL,
            0xd097ad07a71f26b2UL, 0x825ecc24c873782fUL,
            0xa2f67f2dfa90563bUL, 0xcbb41ef979346bcaUL,
            0xfea126b7d78186bcUL, 0x9f24b832e6b0f436UL,
            0xc6ede63fa05d3143UL, 0xf8a95fcf88747d94UL,
            0x9b69dbe1b548ce7cUL, 0xc24452da229b021bUL,
            0xf2d56790ab41c2a2UL, 0x97c560ba6b0919a5UL,
            0xbdb6b8e905cb600fUL, 0xed246723473e3813UL,
            0x9436c0760c86e30bUL, 0xb94470938fa89bceUL,
            0xe7958cb87392c2c2UL, 0x90bd77f3483bb9b9UL,
            0xb4ecd5f01a4aa828UL, 0xe2280b6c20dd5232UL,
            0x8d590723948a535fUL, 0xb0af48ec79ace837UL,
            0xdcdb1b2798182244UL, 0x8a08f0f8bf0f156bUL,
            0xac8b2d36eed2dac5UL, 0xd7adf884aa879177UL,
            0x86ccbb52ea94baeaUL, 0xa87fea27a539e9a5UL,
            0xd29fe4b18e88640eUL, 0x83a3eeeef9153e89UL,
            0xa48ceaaab75a8e2bUL, 0xcdb02555653131b6UL,
            0x808e17555f3ebf11UL, 0xa0b19d2ab70e6ed6UL,
            0xc8de047564d20a8bUL, 0xfb158592be068d2eUL,
            0x9ced737bb6c4183dUL, 0xc428d05aa4751e4cUL,
            0xf53304714d9265dfUL, 0x993fe2c6d07b7fabUL,
            0xbf8fdb78849a5f96UL, 0xef73d256a5c0f77cUL,
            0x95a8637627989aadUL, 0xbb127c53b17ec159UL,
            0xe9d71b689dde71afUL, 0x9226712162ab070dUL,
            0xb6b00d69bb55c8d1UL, 0xe45c10c42a2b3b05UL,
            0x8eb98a7a9a5b04e3UL, 0xb267ed1940f1c61cUL,
            0xdf01e85f912e37a3UL, 0x8b61313bbabce2c6UL,
            0xae397d8aa96c1b77UL, 0xd9c7dced53c72255UL,
            0x881cea14545c7575UL, 0xaa242499697392d2UL,
            0xd4ad2dbfc3d07787UL, 0x84ec3c97da624ab4UL,
            0xa6274bbdd0fadd61UL, 0xcfb11ead453994baUL,
            0x81ceb32c4b43fcf4UL, 0xa2425ff75e14fc31UL,
            0xcad2f7f5359a3b3eUL, 0xfd87b5f28300ca0dUL,
            0x9e74d1b791e07e48UL, 0xc612062576589ddaUL,
            0xf79687aed3eec551UL, 0x9abe14cd44753b52UL,
            0xc16d9a0095928a27UL, 0xf1c90080baf72cb1UL,
            0x971da05074da7beeUL, 0xbce5086492111aeaUL,
            0xec1e4a7db69561a5UL, 0x9392ee8e921d5d07UL,
            0xb877aa3236a4b449UL, 0xe69594bec44de15bUL,
            0x901d7cf73ab0acd9UL, 0xb424dc35095cd80fUL,
            0xe12e13424bb40e13UL, 0x8cbccc096f5088cbUL,
            0xafebff0bcb24aafeUL, 0xdbe6fecebdedd5beUL,
            0x89705f4136b4a597UL, 0xabcc77118461cefcUL,
            0xd6bf94d5e57a42bcUL, 0x8637bd05af6c69b5UL,
            0xa7c5ac471b478423UL, 0xd1b71758e219652bUL,
            0x83126e978d4fdf3bUL, 0xa3d70a3d70a3d70aUL,
            0xccccccccccccccccUL, 0x8000000000000000UL,
            0xa000000000000000UL, 0xc800000000000000UL,
            0xfa00000000000000UL, 0x9c40000000000000UL,
            0xc350000000000000UL, 0xf424000000000000UL,
            0x9896800000000000UL, 0xbebc200000000000UL,
            0xee6b280000000000UL, 0x9502f90000000000UL,
            0xba43b74000000000UL, 0xe8d4a51000000000UL,
            0x9184e72a00000000UL, 0xb5e620f480000000UL,
            0xe35fa931a0000000UL, 0x8e1bc9bf04000000UL,
            0xb1a2bc2ec5000000UL, 0xde0b6b3a76400000UL,
            0x8ac7230489e80000UL, 0xad78ebc5ac620000UL,
            0xd8d726b7177a8000UL, 0x878678326eac9000UL,
            0xa968163f0a57b400UL, 0xd3c21bcecceda100UL,
            0x84595161401484a0UL, 0xa56fa5b99019a5c8UL,
            0xcecb8f27f4200f3aUL, 0x813f3978f8940984UL,
            0xa18f07d736b90be5UL, 0xc9f2c9cd04674edeUL,
            0xfc6f7c4045812296UL, 0x9dc5ada82b70b59dUL,
            0xc5371912364ce305UL, 0xf684df56c3e01bc6UL,
            0x9a130b963a6c115cUL, 0xc097ce7bc90715b3UL,
            0xf0bdc21abb48db20UL, 0x96769950b50d88f4UL,
            0xbc143fa4e250eb31UL, 0xeb194f8e1ae525fdUL,
            0x92efd1b8d0cf37beUL, 0xb7abc627050305adUL,
            0xe596b7b0c643c719UL, 0x8f7e32ce7bea5c6fUL,
            0xb35dbf821ae4f38bUL, 0xe0352f62a19e306eUL,
            0x8c213d9da502de45UL, 0xaf298d050e4395d6UL,
            0xdaf3f04651d47b4cUL, 0x88d8762bf324cd0fUL,
            0xab0e93b6efee0053UL, 0xd5d238a4abe98068UL,
            0x85a36366eb71f041UL, 0xa70c3c40a64e6c51UL,
            0xd0cf4b50cfe20765UL, 0x82818f1281ed449fUL,
            0xa321f2d7226895c7UL, 0xcbea6f8ceb02bb39UL,
            0xfee50b7025c36a08UL, 0x9f4f2726179a2245UL,
            0xc722f0ef9d80aad6UL, 0xf8ebad2b84e0d58bUL,
            0x9b934c3b330c8577UL, 0xc2781f49ffcfa6d5UL,
            0xf316271c7fc3908aUL, 0x97edd871cfda3a56UL,
            0xbde94e8e43d0c8ecUL, 0xed63a231d4c4fb27UL,
            0x945e455f24fb1cf8UL, 0xb975d6b6ee39e436UL,
            0xe7d34c64a9c85d44UL, 0x90e40fbeea1d3a4aUL,
            0xb51d13aea4a488ddUL, 0xe264589a4dcdab14UL,
            0x8d7eb76070a08aecUL, 0xb0de65388cc8ada8UL,
            0xdd15fe86affad912UL, 0x8a2dbf142dfcc7abUL,
            0xacb92ed9397bf996UL, 0xd7e77a8f87daf7fbUL,
            0x86f0ac99b4e8dafdUL, 0xa8acd7c0222311bcUL,
            0xd2d80db02aabd62bUL, 0x83c7088e1aab65dbUL,
            0xa4b8cab1a1563f52UL, 0xcde6fd5e09abcf26UL,
            0x80b05e5ac60b6178UL, 0xa0dc75f1778e39d6UL,
            0xc913936dd571c84cUL, 0xfb5878494ace3a5fUL,
            0x9d174b2dcec0e47bUL, 0xc45d1df942711d9aUL,
            0xf5746577930d6500UL, 0x9968bf6abbe85f20UL,
            0xbfc2ef456ae276e8UL, 0xefb3ab16c59b14a2UL,
            0x95d04aee3b80ece5UL, 0xbb445da9ca61281fUL,
            0xea1575143cf97226UL, 0x924d692ca61be758UL,
            0xb6e0c377cfa2e12eUL, 0xe498f455c38b997aUL,
            0x8edf98b59a373fecUL, 0xb2977ee300c50fe7UL,
            0xdf3d5e9bc0f653e1UL, 0x8b865b215899f46cUL,
            0xae67f1e9aec07187UL, 0xda01ee641a708de9UL,
            0x884134fe908658b2UL, 0xaa51823e34a7eedeUL,
            0xd4e5e2cdc1d1ea96UL, 0x850fadc09923329eUL,
            0xa6539930bf6bff45UL, 0xcfe87f7cef46ff16UL,
            0x81f14fae158c5f6eUL, 0xa26da3999aef7749UL,
            0xcb090c8001ab551cUL, 0xfdcb4fa002162a63UL,
            0x9e9f11c4014dda7eUL, 0xc646d63501a1511dUL,
            0xf7d88bc24209a565UL, 0x9ae757596946075fUL,
            0xc1a12d2fc3978937UL, 0xf209787bb47d6b84UL,
            0x9745eb4d50ce6332UL, 0xbd176620a501fbffUL,
            0xec5d3fa8ce427affUL, 0x93ba47c980e98cdfUL,
            0xb8a8d9bbe123f017UL, 0xe6d3102ad96cec1dUL,
            0x9043ea1ac7e41392UL, 0xb454e4a179dd1877UL,
            0xe16a1dc9d8545e94UL, 0x8ce2529e2734bb1dUL,
            0xb01ae745b101e9e4UL, 0xdc21a1171d42645dUL,
            0x899504ae72497ebaUL, 0xabfa45da0edbde69UL,
            0xd6f8d7509292d603UL, 0x865b86925b9bc5c2UL,
            0xa7f26836f282b732UL, 0xd1ef0244af2364ffUL,
            0x8335616aed761f1fUL, 0xa402b9c5a8d3a6e7UL,
            0xcd036837130890a1UL, 0x802221226be55a64UL,
            0xa02aa96b06deb0fdUL, 0xc83553c5c8965d3dUL,
            0xfa42a8b73abbf48cUL, 0x9c69a97284b578d7UL,
            0xc38413cf25e2d70dUL, 0xf46518c2ef5b8cd1UL,
            0x98bf2f79d5993802UL, 0xbeeefb584aff8603UL,
            0xeeaaba2e5dbf6784UL, 0x952ab45cfa97a0b2UL,
            0xba756174393d88dfUL, 0xe912b9d1478ceb17UL,
            0x91abb422ccb812eeUL, 0xb616a12b7fe617aaUL,
            0xe39c49765fdf9d94UL, 0x8e41ade9fbebc27dUL,
            0xb1d219647ae6b31cUL, 0xde469fbd99a05fe3UL,
            0x8aec23d680043beeUL, 0xada72ccc20054ae9UL,
            0xd910f7ff28069da4UL, 0x87aa9aff79042286UL,
            0xa99541bf57452b28UL, 0xd3fa922f2d1675f2UL,
            0x847c9b5d7c2e09b7UL, 0xa59bc234db398c25UL,
            0xcf02b2c21207ef2eUL, 0x8161afb94b44f57dUL,
            0xa1ba1ba79e1632dcUL, 0xca28a291859bbf93UL,
            0xfcb2cb35e702af78UL, 0x9defbf01b061adabUL,
            0xc56baec21c7a1916UL, 0xf6c69a72a3989f5bUL,
            0x9a3c2087a63f6399UL, 0xc0cb28a98fcf3c7fUL,
            0xf0fdf2d3f3c30b9fUL, 0x969eb7c47859e743UL,
            0xbc4665b596706114UL, 0xeb57ff22fc0c7959UL,
            0x9316ff75dd87cbd8UL, 0xb7dcbf5354e9beceUL,
            0xe5d3ef282a242e81UL, 0x8fa475791a569d10UL,
            0xb38d92d760ec4455UL, 0xe070f78d3927556aUL,
            0x8c469ab843b89562UL, 0xaf58416654a6babbUL,
            0xdb2e51bfe9d0696aUL, 0x88fcf317f22241e2UL,
            0xab3c2fddeeaad25aUL, 0xd60b3bd56a5586f1UL,
            0x85c7056562757456UL, 0xa738c6bebb12d16cUL,
            0xd106f86e69d785c7UL, 0x82a45b450226b39cUL,
            0xa34d721642b06084UL, 0xcc20ce9bd35c78a5UL,
            0xff290242c83396ceUL, 0x9f79a169bd203e41UL,
            0xc75809c42c684dd1UL, 0xf92e0c3537826145UL,
            0x9bbcc7a142b17ccbUL, 0xc2abf989935ddbfeUL,
            0xf356f7ebf83552feUL, 0x98165af37b2153deUL,
            0xbe1bf1b059e9a8d6UL, 0xeda2ee1c7064130cUL,
            0x9485d4d1c63e8be7UL, 0xb9a74a0637ce2ee1UL,
            0xe8111c87c5c1ba99UL, 0x910ab1d4db9914a0UL,
            0xb54d5e4a127f59c8UL, 0xe2a0b5dc971f303aUL,
            0x8da471a9de737e24UL, 0xb10d8e1456105dadUL,
            0xdd50f1996b947518UL, 0x8a5296ffe33cc92fUL,
            0xace73cbfdc0bfb7bUL, 0xd8210befd30efa5aUL,
            0x8714a775e3e95c78UL, 0xa8d9d1535ce3b396UL,
            0xd31045a8341ca07cUL, 0x83ea2b892091e44dUL,
            0xa4e4b66b68b65d60UL, 0xce1de40642e3f4b9UL,
            0x80d2ae83e9ce78f3UL, 0xa1075a24e4421730UL,
            0xc94930ae1d529cfcUL, 0xfb9b7cd9a4a7443cUL,
            0x9d412e0806e88aa5UL, 0xc491798a08a2ad4eUL,
            0xf5b5d7ec8acb58a2UL, 0x9991a6f3d6bf1765UL,
            0xbff610b0cc6edd3fUL, 0xeff394dcff8a948eUL,
            0x95f83d0a1fb69cd9UL, 0xbb764c4ca7a4440fUL,
            0xea53df5fd18d5513UL, 0x92746b9be2f8552cUL,
            0xb7118682dbb66a77UL, 0xe4d5e82392a40515UL,
            0x8f05b1163ba6832dUL, 0xb2c71d5bca9023f8UL,
            0xdf78e4b2bd342cf6UL, 0x8bab8eefb6409c1aUL,
            0xae9672aba3d0c320UL, 0xda3c0f568cc4f3e8UL,
            0x8865899617fb1871UL, 0xaa7eebfb9df9de8dUL,
            0xd51ea6fa85785631UL, 0x8533285c936b35deUL,
            0xa67ff273b8460356UL, 0xd01fef10a657842cUL,
            0x8213f56a67f6b29bUL, 0xa298f2c501f45f42UL,
            0xcb3f2f7642717713UL, 0xfe0efb53d30dd4d7UL,
            0x9ec95d1463e8a506UL, 0xc67bb4597ce2ce48UL,
            0xf81aa16fdc1b81daUL, 0x9b10a4e5e9913128UL,
            0xc1d4ce1f63f57d72UL, 0xf24a01a73cf2dccfUL,
            0x976e41088617ca01UL, 0xbd49d14aa79dbc82UL,
            0xec9c459d51852ba2UL, 0x93e1ab8252f33b45UL,
            0xb8da1662e7b00a17UL, 0xe7109bfba19c0c9dUL,
            0x906a617d450187e2UL, 0xb484f9dc9641e9daUL,
            0xe1a63853bbd26451UL, 0x8d07e33455637eb2UL,
            0xb049dc016abc5e5fUL, 0xdc5c5301c56b75f7UL,
            0x89b9b3e11b6329baUL, 0xac2820d9623bf429UL,
            0xd732290fbacaf133UL, 0x867f59a9d4bed6c0UL,
            0xa81f301449ee8c70UL, 0xd226fc195c6a2f8cUL,
            0x83585d8fd9c25db7UL, 0xa42e74f3d032f525UL,
            0xcd3a1230c43fb26fUL, 0x80444b5e7aa7cf85UL,
            0xa0555e361951c366UL, 0xc86ab5c39fa63440UL,
            0xfa856334878fc150UL, 0x9c935e00d4b9d8d2UL,
            0xc3b8358109e84f07UL, 0xf4a642e14c6262c8UL,
            0x98e7e9cccfbd7dbdUL, 0xbf21e44003acdd2cUL,
            0xeeea5d5004981478UL, 0x95527a5202df0ccbUL,
            0xbaa718e68396cffdUL, 0xe950df20247c83fdUL,
            0x91d28b7416cdd27eUL, 0xb6472e511c81471dUL,
            0xe3d8f9e563a198e5UL, 0x8e679c2f5e44ff8fUL
        )
    }
}

private inline val Char.isDigit get() = (this - '0').toChar().code < 10

private inline fun charAt(s: CharSequence, index: Int) = if (index < s.length) {
    s[index]
} else {
    '\u0000'
}

private inline fun fullMultiplicationHighBits(x: Long, y: Long): Long {
    val xLo = x and 0xffffffffL
    val xHi = x ushr 32
    val yLo = y and 0xffffffffL
    val yHi = y ushr 32

    val xTimesYHi = xHi * yHi
    val xTimesYMid = xLo * yHi
    val yTimesXMid = xHi * yLo
    val xTimesYLo = xLo * yLo

    val carry =
        yTimesXMid +
        (xTimesYLo ushr 32) +
        (xTimesYMid and 0xffffffffL)

    return xTimesYHi + (carry ushr 32) + (xTimesYMid ushr 32)
}

private inline fun parseFourDigits(str: CharSequence, offset: Int): Int {
    val v = (str[offset].code.toLong()
        or (str[offset + 1].code.toLong() shl 16)
        or (str[offset + 2].code.toLong() shl 32)
        or (str[offset + 3].code.toLong() shl 48))

    val base = v - 0x0030003000300030L
    val predicate = v + 0x0046004600460046L or base
    return if (predicate and 0xff80_ff80_ff80_ff80UL.toLong() != 0L) {
        -1
    } else {
        (base * 0x03e80064000a0001L ushr 48).toInt()
    }
}
