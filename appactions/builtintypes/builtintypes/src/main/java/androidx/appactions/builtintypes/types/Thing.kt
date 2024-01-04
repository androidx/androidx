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

import androidx.appactions.builtintypes.properties.DisambiguatingDescription
import androidx.appactions.builtintypes.properties.Name
import androidx.appsearch.`annotation`.Document
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.NotImplementedError
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Map
import kotlin.collections.emptyMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.jvm.JvmStatic

/**
 * The most generic type of item.
 *
 * See https://schema.org/Thing for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractThing] if you need to extend this type.
 */
@Document(name = "bit:Thing")
public interface Thing {
  /** Represents the AppSearch document's namespace. */
  @get:Document.Namespace
  public val namespace: String
    get() = ""

  /**
   * A sub property of description. A short description of the item used to disambiguate from other,
   * similar items. Information from other properties (in particular, name) may be necessary for the
   * description to be useful for disambiguation.
   *
   * See https://schema.org/disambiguatingDescription for more context.
   */
  @get:Document.DocumentProperty
  public val disambiguatingDescription: DisambiguatingDescription?
    get() = null

  /**
   * The identifier property represents any kind of identifier for any kind of Thing, such as ISBNs,
   * GTIN codes, UUIDs etc.
   *
   * See https://schema.org/identifier for more context.
   */
  @get:Document.Id
  public val identifier: String
    get() = ""

  /**
   * The name of the item.
   *
   * See https://schema.org/name for more context.
   */
  @get:Document.DocumentProperty
  public val name: Name?
    get() = null

  /** Converts this [Thing] to its builder with all the properties copied over. */
  public fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic @Document.BuilderProducer public fun Builder(): Builder<*> = ThingImpl.Builder()
  }

  /**
   * Builder for [Thing].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractThing.Builder] if you need to extend this builder.
   */
  @Suppress("StaticFinalBuilder")
  public interface Builder<Self : Builder<Self>> {
    /** Returns a built [Thing]. */
    public fun build(): Thing

    /** Sets the `namespace`. */
    @Suppress("DocumentExceptions")
    public fun setNamespace(namespace: String): Self = throw NotImplementedError()

    /** Sets the `disambiguatingDescription` to [String]. */
    public fun setDisambiguatingDescription(text: String): Self =
      setDisambiguatingDescription(DisambiguatingDescription(text))

    /** Sets the `disambiguatingDescription`. */
    @Suppress("DocumentExceptions")
    public fun setDisambiguatingDescription(
      disambiguatingDescription: DisambiguatingDescription?
    ): Self = throw NotImplementedError()

    /** Sets the `identifier`. */
    @Suppress("DocumentExceptions")
    public fun setIdentifier(text: String): Self = throw NotImplementedError()

    /** Sets the `name` to [String]. */
    public fun setName(text: String): Self = setName(Name(text))

    /** Sets the `name`. */
    @Suppress("DocumentExceptions")
    public fun setName(name: Name?): Self = throw NotImplementedError()
  }
}

/**
 * An abstract implementation of [Thing].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyThing",
 *   parent = [Thing::class],
 * )
 * class MyThing internal constructor(
 *   thing: Thing,
 *   val foo: String,
 *   val bars: List<Int>,
 * ) : AbstractThing<
 *   MyThing,
 *   MyThing.Builder
 * >(thing) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyThing"
 *
 *   override val additionalProperties: Map<String, Any?>
 *     get() = mapOf("foo" to foo, "bars" to bars)
 *
 *   override fun toBuilderWithAdditionalPropertiesOnly(): Builder {
 *     return Builder()
 *       .setFoo(foo)
 *       .addBars(bars)
 *   }
 *
 *   class Builder :
 *     AbstractThing.Builder<
 *       Builder,
 *       MyThing> {...}
 * }
 * ```
 *
 * Also see [AbstractThing.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractThing<
  Self : AbstractThing<Self, Builder>, Builder : AbstractThing.Builder<Builder, Self>>
internal constructor(
  public final override val namespace: String,
  public final override val disambiguatingDescription: DisambiguatingDescription?,
  public final override val identifier: String,
  public final override val name: Name?,
) : Thing {
  /**
   * Human readable name for the concrete [Self] class.
   *
   * Used in the [toString] output.
   */
  protected abstract val selfTypeName: String

  /**
   * The additional properties that exist on the concrete [Self] class.
   *
   * Used for equality comparison and computing the hash code.
   */
  protected abstract val additionalProperties: Map<String, Any?>

  /** A copy-constructor that copies over properties from another [Thing] instance. */
  public constructor(
    thing: Thing
  ) : this(thing.namespace, thing.disambiguatingDescription, thing.identifier, thing.name)

  /** Returns a concrete [Builder] with the additional, non-[Thing] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  public final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setDisambiguatingDescription(disambiguatingDescription)
      .setIdentifier(identifier)
      .setName(name)

  public final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (disambiguatingDescription != other.disambiguatingDescription) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  public final override fun hashCode(): Int =
    Objects.hash(namespace, disambiguatingDescription, identifier, name, additionalProperties)

  public final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
    }
    if (disambiguatingDescription != null) {
      attributes["disambiguatingDescription"] =
        disambiguatingDescription.toString(includeWrapperName = false)
    }
    if (identifier.isNotEmpty()) {
      attributes["identifier"] = identifier
    }
    if (name != null) {
      attributes["name"] = name.toString(includeWrapperName = false)
    }
    attributes += additionalProperties.map { (k, v) -> k to v.toString() }
    val commaSeparated = attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
    return """$selfTypeName($commaSeparated)"""
  }

  /**
   * An abstract implementation of [Thing.Builder].
   *
   * Allows for extension like:
   * ```kt
   * class MyThing :
   *   : AbstractThing<
   *     MyThing,
   *     MyThing.Builder>(...) {
   *
   *   class Builder
   *   : AbstractThing.Builder<
   *       Builder,
   *       MyThing
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyThing.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromThing(
   *       thing: Thing
   *     ): MyThing {
   *       return MyThing(
   *         thing,
   *         foo,
   *         bars.toList()
   *       )
   *     }
   *
   *     fun setFoo(string: String): Builder {
   *       return apply { foo = string }
   *     }
   *
   *     fun addBar(int: Int): Builder {
   *       return apply { bars += int }
   *     }
   *
   *     fun addBars(values: Iterable<Int>): Builder {
   *       return apply { bars += values }
   *     }
   *   }
   * }
   * ```
   *
   * Also see [AbstractThing].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<Self : Builder<Self, Built>, Built : AbstractThing<Built, Self>> :
    Thing.Builder<Self> {
    /**
     * Human readable name for the concrete [Self] class.
     *
     * Used in the [toString] output.
     */
    @get:Suppress("GetterOnBuilder") protected abstract val selfTypeName: String

    /**
     * The additional properties that exist on the concrete [Self] class.
     *
     * Used for equality comparison and computing the hash code.
     */
    @get:Suppress("GetterOnBuilder") protected abstract val additionalProperties: Map<String, Any?>

    private var namespace: String = ""

    private var disambiguatingDescription: DisambiguatingDescription? = null

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [Thing].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [Thing]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle") protected abstract fun buildFromThing(thing: Thing): Built

    public final override fun build(): Built =
      buildFromThing(ThingImpl(namespace, disambiguatingDescription, identifier, name))

    public final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    public final override fun setDisambiguatingDescription(
      disambiguatingDescription: DisambiguatingDescription?
    ): Self {
      this.disambiguatingDescription = disambiguatingDescription
      return this as Self
    }

    public final override fun setIdentifier(text: String): Self {
      this.identifier = text
      return this as Self
    }

    public final override fun setName(name: Name?): Self {
      this.name = name
      return this as Self
    }

    @Suppress("BuilderSetStyle")
    public final override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class.java != other::class.java) return false
      other as Self
      if (namespace != other.namespace) return false
      if (disambiguatingDescription != other.disambiguatingDescription) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    public final override fun hashCode(): Int =
      Objects.hash(namespace, disambiguatingDescription, identifier, name, additionalProperties)

    @Suppress("BuilderSetStyle")
    public final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
      }
      if (disambiguatingDescription != null) {
        attributes["disambiguatingDescription"] =
          disambiguatingDescription!!.toString(includeWrapperName = false)
      }
      if (identifier.isNotEmpty()) {
        attributes["identifier"] = identifier
      }
      if (name != null) {
        attributes["name"] = name!!.toString(includeWrapperName = false)
      }
      attributes += additionalProperties.map { (k, v) -> k to v.toString() }
      val commaSeparated =
        attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
      return """$selfTypeName($commaSeparated)"""
    }
  }
}

private class ThingImpl : AbstractThing<ThingImpl, ThingImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Thing"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    disambiguatingDescription: DisambiguatingDescription?,
    identifier: String,
    name: Name?,
  ) : super(namespace, disambiguatingDescription, identifier, name)

  public constructor(thing: Thing) : super(thing)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractThing.Builder<Builder, ThingImpl>() {
    protected override val selfTypeName: String
      get() = "Thing.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromThing(thing: Thing): ThingImpl =
      thing as? ThingImpl ?: ThingImpl(thing)
  }
}
