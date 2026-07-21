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

package androidx.navigation3.runtime.samples.deeplink

import androidx.annotation.Sampled
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkSerializer
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

@Sampled
fun deepLinkSerializerSample() {
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
}

@Serializable object HomeKey : NavKey

private data class Product(val id: Int, val name: String)

@Serializable
private data class ProductInventoryKey(
    @Serializable(with = ProductSerializer::class) val product: Product,
    val count: Int,
) : NavKey

// serializer to deserialize string value into a Product object
private object ProductSerializer : DeepLinkSerializer<Product>() {
    override val serialName: String = "androidx.navigation3.ui.samples.Product"

    override fun deserialize(value: String): Product {
        val splitArgs = value.split("-", limit = 2)
        val id =
            splitArgs[0].toIntOrNull()
                ?: throw SerializationException("Invalid product id: ${splitArgs[0]}")
        val name = splitArgs[1]
        return Product(id, name)
    }

    override fun serialize(value: Product): String = "${value.id}-${value.name}"
}
