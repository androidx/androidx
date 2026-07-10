/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.ui.samples

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

@Serializable private object HomeKey : NavKey

/** Resources for [CustomArgTypeFlattenedArgsSample] */
@Serializable private data class Name(val firstName: String, val lastName: String)

@Serializable private data class ProfileKey(val name: Name, val age: Int) : NavKey

/** Resources for [CustomArgTypeKSerializerSample] */
private data class Product(val id: Int, val name: String)

@Serializable
private data class ProductInventoryKey(
    @Serializable(with = ProductSerializer::class) val product: Product,
    val count: Int,
) : NavKey

// serializer to deserialize string value into a Product object
private object ProductSerializer : KSerializer<Product> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.navigation3.ui.samples.Product", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Product) {
        encoder.encodeString("${value.id}-${value.name}")
    }

    override fun deserialize(decoder: Decoder): Product {
        val argValue = decoder.decodeString()
        val splitArgs = argValue.split("-", limit = 2)
        if (splitArgs.size != 2) {
            throw SerializationException(
                "Invalid product format: $argValue. Expected format: id-name"
            )
        }
        val id =
            splitArgs[0].toIntOrNull()
                ?: throw SerializationException("Invalid product id: ${splitArgs[0]}")
        val name = splitArgs[1]
        return Product(id, name)
    }
}

/** Resources for [ListCustomArgTypeKSerializerSample] */
private data class Hat(val id: Int, val weight: Double)

@Serializable
private data class HatListKey(val hats: List<@Serializable(with = HatSerializer::class) Hat>) :
    NavKey

// serializer to deserialize string value into a Hat object
private object HatSerializer : KSerializer<Hat> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.navigation3.ui.samples.Hat", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Hat) {
        encoder.encodeString("${value.id}:${value.weight}")
    }

    override fun deserialize(decoder: Decoder): Hat {
        val argValue = decoder.decodeString()
        val splitArgs = argValue.split(":", limit = 2)
        if (splitArgs.size != 2) {
            throw SerializationException(
                "Invalid hat format: $argValue. Expected format: id:weight"
            )
        }
        val id =
            splitArgs[0].toIntOrNull()
                ?: throw SerializationException("Invalid product id: ${splitArgs[0]}")
        val weight =
            splitArgs[1].toDoubleOrNull()
                ?: throw SerializationException("Invalid weight: ${splitArgs[1]}")
        return Hat(id, weight)
    }
}

@Composable
fun CustomArgTypeFlattenedArgsSample() {
    val matcher =
        UriDeepLinkMatcher(
            uriPattern =
                DeepLinkUri(
                    "http://www.nav3example.com/profile?firstName={firstName}&lastName={lastName}&age={age}"
                ),
            serializer = serializer<ProfileKey>(),
        )

    val request =
        DeepLinkRequest(
            uri =
                DeepLinkUri("http://www.nav3example.com/profile?firstName=john&lastName=doe&age=25")
        )
    val startKey = matcher.match(request)?.key ?: HomeKey
    val backStack = rememberNavBackStack(startKey)

    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<HomeKey> { RedBox("Deeplink failed. Displaying default Home screen") }
                entry<ProfileKey> { GreenBox(it.toString()) }
            },
    )
}

@Composable
fun CustomArgTypeKSerializerSample() {
    val matcher =
        UriDeepLinkMatcher(
            uriPattern =
                DeepLinkUri("http://www.nav3example.com/products?product={product}&count={count}"),
            serializer = serializer<ProductInventoryKey>(),
        )

    val request =
        DeepLinkRequest(
            uri = DeepLinkUri("http://www.nav3example.com/products?product=123-productA&count=12")
        )
    val startKey = matcher.match(request)?.key ?: HomeKey
    val backStack = rememberNavBackStack(startKey)

    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<HomeKey> { RedBox("Deeplink failed. Displaying default Home screen") }
                entry<ProductInventoryKey> { GreenBox(it.toString()) }
            },
    )
}

@Composable
fun ListCustomArgTypeKSerializerSample() {
    val matcher =
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("http://www.nav3example.com/hatList?hats={hats}"),
            serializer = serializer<HatListKey>(),
        )

    val request =
        DeepLinkRequest(
            uri = DeepLinkUri("http://www.nav3example.com/hatList?hats=2342:4.5&hats=2643:5.6")
        )
    val startKey = matcher.match(request)?.key ?: HomeKey
    val backStack = rememberNavBackStack(startKey)

    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<HomeKey> { RedBox("Deeplink failed. Displaying default Home screen") }
                entry<HatListKey> { GreenBox(it.toString()) }
            },
    )
}
