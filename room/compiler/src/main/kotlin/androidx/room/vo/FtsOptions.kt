/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.FtsOptions.MatchInfo
import androidx.room.FtsOptions.Order
import androidx.room.migration.bundle.FtsOptionsBundle
import java.util.Locale

data class FtsOptions(
    val tokenizer: String,
    val tokenizerArgs: List<String>,
    val contentEntity: Entity?,
    val languageIdColumnName: String,
    val matchInfo: MatchInfo,
    val notIndexedColumns: List<String>,
    val prefixSizes: List<Int>,
    val preferredOrder: Order
) : HasSchemaIdentity {

    override fun getIdKey(): String {
        val identityKey = SchemaIdentityKey()
        identityKey.append(tokenizer)
        identityKey.append(tokenizerArgs.joinToString())
        identityKey.append(contentEntity?.tableName ?: "")
        identityKey.append(languageIdColumnName)
        identityKey.append(matchInfo.name)
        identityKey.append(notIndexedColumns.joinToString())
        identityKey.append(prefixSizes.joinToString { it.toString() })
        identityKey.append(preferredOrder.name)
        return identityKey.hash()
    }

    fun databaseDefinition(includeTokenizer: Boolean = true): List<String> {
        return mutableListOf<String>().apply {
            if (includeTokenizer && (
                tokenizer != androidx.room.FtsOptions.TOKENIZER_SIMPLE ||
                    tokenizerArgs.isNotEmpty()
                )
            ) {
                val tokenizeAndArgs = listOf("tokenize=$tokenizer") +
                    tokenizerArgs.map { "`$it`" }
                add(tokenizeAndArgs.joinToString(separator = " "))
            }

            if (contentEntity != null) {
                add("content=`${contentEntity.tableName}`")
            }

            if (languageIdColumnName.isNotEmpty()) {
                add("languageid=`$languageIdColumnName`")
            }

            if (matchInfo != MatchInfo.FTS4) {
                add("matchinfo=${matchInfo.name.lowercase(Locale.US)}")
            }

            notIndexedColumns.forEach {
                add("notindexed=`$it`")
            }

            if (prefixSizes.isNotEmpty()) {
                add("prefix=`${prefixSizes.joinToString(separator = ",") { it.toString() }}`")
            }

            if (preferredOrder != Order.ASC) {
                add("order=$preferredOrder")
            }
        }
    }

    fun toBundle() = FtsOptionsBundle(
        tokenizer,
        tokenizerArgs,
        contentEntity?.tableName ?: "",
        languageIdColumnName,
        matchInfo.name,
        notIndexedColumns,
        prefixSizes,
        preferredOrder.name
    )

    companion object {
        val defaultTokenizers = listOf(
            androidx.room.FtsOptions.TOKENIZER_SIMPLE,
            androidx.room.FtsOptions.TOKENIZER_PORTER,
            androidx.room.FtsOptions.TOKENIZER_ICU,
            androidx.room.FtsOptions.TOKENIZER_UNICODE61
        )
    }
}