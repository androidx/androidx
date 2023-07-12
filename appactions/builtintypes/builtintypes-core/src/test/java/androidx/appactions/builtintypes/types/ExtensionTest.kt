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

package androidx.appactions.builtintypes.types

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExtensionTest {

  class MyThing internal constructor(thing: Thing, val foo: String?, val bars: List<Int>) :
    GenericThing<MyThing, MyThing.Builder>(thing) {
    override val selfTypeName = "MyThing"
    override val additionalProperties: Map<String, Any?>
      get() = mapOf("foo" to foo, "bars" to bars)

    override fun toBuilderWithAdditionalPropertiesOnly(): Builder {
      return Builder().setFoo(foo).addBars(bars)
    }

    class Builder : GenericThing.Builder<Builder, MyThing>() {
      private var foo: String? = null
      private val bars: MutableList<Int> = mutableListOf()

      override val selfTypeName = "MyThingBuilder"
      override val additionalProperties: Map<String, Any?>
        get() = mapOf("foo" to foo, "bars" to bars)

      override fun buildFromThing(thing: Thing): MyThing {
        return MyThing(thing, foo, bars.toList())
      }

      fun setFoo(foo: String?) = apply { this.foo = foo }
      fun addBar(bar: Int) = apply { bars += bar }
      fun addBars(values: Iterable<Int>) = apply { bars += values }
    }
  }

  @Test
  fun extendedTypeSupportEquality() {
    val thing1 =
      MyThing.Builder()
        .setName("Jane")
        .setFoo("some string")
        .addBar(1)
        .addBars(listOf(2, 3))
        .build()
    val thing2 =
      MyThing.Builder()
        .setName("Jane")
        .setFoo("some string")
        .addBar(1)
        .addBars(listOf(2, 3))
        .build()

    assertThat(thing1).isEqualTo(thing2)
    assertThat(thing1.hashCode()).isEqualTo(thing2.hashCode())
  }

  @Test
  fun extendedTypeSupportsCopying() {
    val thing1 =
      MyThing.Builder()
        .setName("Jane")
        .setFoo("some string")
        .addBar(1)
        .addBars(listOf(2, 3))
        .build()
    val thing2 = thing1.toBuilder().setFoo("other string").setName("John").build()

    assertThat(thing1).isNotEqualTo(thing2)
    assertThat(thing2.name?.asText).isEqualTo("John")
    assertThat(thing2.foo).isEqualTo("other string")
    assertThat(thing1.bars).isEqualTo(thing2.bars)
  }
}
