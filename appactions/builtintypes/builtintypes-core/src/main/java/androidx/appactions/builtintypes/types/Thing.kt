// Copyright 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package androidx.appactions.builtintypes.types

import androidx.appactions.builtintypes.properties.Description
import androidx.appactions.builtintypes.properties.DisambiguatingDescription
import androidx.appactions.builtintypes.properties.Name
import androidx.appactions.builtintypes.properties.Temporal
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Map
import kotlin.collections.emptyMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The most generic type of item.
 *
 * See http://schema.org/Thing for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [GenericThing] if you need to extend this type.
 */
public interface Thing {
  /** A description of the item. */
  public val description: Description?

  /**
   * A sub property of description. A short description of the item used to disambiguate from other,
   * similar items. Information from other properties (in particular, name) may be necessary for the
   * description to be useful for disambiguation.
   */
  public val disambiguatingDescription: DisambiguatingDescription?

  /**
   * The identifier property represents any kind of identifier for any kind of Thing, such as ISBNs,
   * GTIN codes, UUIDs etc.
   */
  public val identifier: String?

  /** The name of the item. */
  public val name: Name?

  /**
   * Can be used in cases where more specific properties (e.g. temporalCoverage, dateCreated,
   * dateModified, datePublished) are not known to be appropriate.
   */
  public val temporal: Temporal?

  /** Converts this [Thing] to its builder with all the properties copied over. */
  public fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder] with no properties set. */
    @JvmStatic public fun Builder(): Builder<*> = ThingImpl.Builder()
  }

  /**
   * Builder for [Thing].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [GenericThing.Builder] if you need to extend this builder.
   */
  @Suppress("StaticFinalBuilder")
  public interface Builder<Self : Builder<Self>> {
    /** Returns a built [Thing]. */
    public fun build(): Thing

    /** Sets the `description` to [String]. */
    public fun setDescription(text: String): Self = setDescription(Description(text))

    /** Sets the `description`. */
    public fun setDescription(description: Description?): Self

    /** Sets the `disambiguatingDescription` to [String]. */
    public fun setDisambiguatingDescription(text: String): Self =
      setDisambiguatingDescription(DisambiguatingDescription(text))

    /** Sets the `disambiguatingDescription` to a canonical [DisambiguatingDescriptionValue]. */
    public fun setDisambiguatingDescription(canonicalValue: DisambiguatingDescriptionValue): Self =
      setDisambiguatingDescription(DisambiguatingDescription(canonicalValue))

    /** Sets the `disambiguatingDescription`. */
    public fun setDisambiguatingDescription(
      disambiguatingDescription: DisambiguatingDescription?
    ): Self

    /** Sets the `identifier`. */
    public fun setIdentifier(text: String?): Self

    /** Sets the `name` to [String]. */
    public fun setName(text: String): Self = setName(Name(text))

    /** Sets the `name`. */
    public fun setName(name: Name?): Self

    /** Sets the `temporal` to [LocalDateTime]. */
    public fun setTemporal(localDateTime: LocalDateTime): Self =
      setTemporal(Temporal(localDateTime))

    /** Sets the `temporal` to [ZonedDateTime]. */
    public fun setTemporal(zonedDateTime: ZonedDateTime): Self =
      setTemporal(Temporal(zonedDateTime))

    /** Sets the `temporal` to [String]. */
    public fun setTemporal(text: String): Self = setTemporal(Temporal(text))

    /** Sets the `temporal`. */
    public fun setTemporal(temporal: Temporal?): Self
  }

  /**
   * A canonical value that may be assigned to [DisambiguatingDescription] properties in the context
   * of [Thing].
   *
   * Represents an open enum. See [Companion] for the different possible variants. More variants may
   * be added over time.
   */
  public class DisambiguatingDescriptionValue
  private constructor(
    public override val textValue: String,
  ) : DisambiguatingDescription.CanonicalValue() {
    public override fun toString(): String = """Thing.DisambiguatingDescriptionValue($textValue)"""

    public companion object {
      @JvmField
      public val ALBUM: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Album")

      @JvmField
      public val AUDIOBOOK: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Audiobook")

      @JvmField
      public val EPISODE: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Episode")

      @JvmField
      public val MOVIE: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Movie")

      @JvmField
      public val MUSIC: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Music")

      @JvmField
      public val OTHER: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Other")

      @JvmField
      public val PHOTOGRAPH: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Photograph")

      @JvmField
      public val PODCAST: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Podcast")

      @JvmField
      public val SONG: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Song")

      @JvmField
      public val SOUNDTRACK: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Soundtrack")

      @JvmField
      public val TELEVISION_CHANNEL: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Television channel")

      @JvmField
      public val TELEVISION_SHOW: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Television show")

      @JvmField
      public val TRAILER: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Trailer")

      @JvmField
      public val VIDEO: DisambiguatingDescriptionValue = DisambiguatingDescriptionValue("Video")

      @JvmField
      public val VIDEO_GAME: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("Video game")
    }
  }
}

/**
 * A generic implementation of [Thing].
 *
 * Allows for extension like:
 * ```kt
 * class MyThing internal constructor(
 *   thing: Thing,
 *   val foo: String,
 *   val bars: List<Int>,
 * ) : GenericThing<
 *   MyThing,
 *   MyThing.Builder
 * >(thing) {
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
 *     GenericThing.Builder<
 *       Builder,
 *       MyThing> {...}
 * }
 * ```
 *
 * Also see [GenericThing.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class GenericThing<
  Self : GenericThing<Self, Builder>, Builder : GenericThing.Builder<Builder, Self>>
internal constructor(
  public final override val description: Description?,
  public final override val disambiguatingDescription: DisambiguatingDescription?,
  public final override val identifier: String?,
  public final override val name: Name?,
  public final override val temporal: Temporal?,
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
  ) : this(
    thing.description,
    thing.disambiguatingDescription,
    thing.identifier,
    thing.name,
    thing.temporal
  )

  /** Returns a concrete [Builder] with the additional, non-[Thing] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  public final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setDescription(description)
      .setDisambiguatingDescription(disambiguatingDescription)
      .setIdentifier(identifier)
      .setName(name)
      .setTemporal(temporal)

  public final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (description != other.description) return false
    if (disambiguatingDescription != other.disambiguatingDescription) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (temporal != other.temporal) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  public final override fun hashCode(): Int =
    Objects.hash(
      description,
      disambiguatingDescription,
      identifier,
      name,
      temporal,
      additionalProperties
    )

  public final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (description != null) {
      attributes["description"] = description.toString(includeWrapperName = false)
    }
    if (disambiguatingDescription != null) {
      attributes["disambiguatingDescription"] =
        disambiguatingDescription.toString(includeWrapperName = false)
    }
    if (identifier != null) {
      attributes["identifier"] = identifier
    }
    if (name != null) {
      attributes["name"] = name.toString(includeWrapperName = false)
    }
    if (temporal != null) {
      attributes["temporal"] = temporal.toString(includeWrapperName = false)
    }
    attributes += additionalProperties.map { (k, v) -> k to v.toString() }
    val commaSeparated = attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
    return """$selfTypeName($commaSeparated)"""
  }

  /**
   * A generic implementation of [Thing.Builder].
   *
   * Allows for extension like:
   * ```kt
   * class MyThing :
   *   : GenericThing<
   *     MyThing,
   *     MyThing.Builder>(...) {
   *
   *   class Builder
   *   : Builder<
   *       Builder,
   *       MyThing
   *   >() {
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
   * Also see [GenericThing].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<Self : Builder<Self, Built>, Built : GenericThing<Built, Self>> :
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

    private var description: Description? = null

    private var disambiguatingDescription: DisambiguatingDescription? = null

    private var identifier: String? = null

    private var name: Name? = null

    private var temporal: Temporal? = null

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
      buildFromThing(ThingImpl(description, disambiguatingDescription, identifier, name, temporal))

    public final override fun setDescription(description: Description?): Self {
      this.description = description
      return this as Self
    }

    public final override fun setDisambiguatingDescription(
      disambiguatingDescription: DisambiguatingDescription?
    ): Self {
      this.disambiguatingDescription = disambiguatingDescription
      return this as Self
    }

    public final override fun setIdentifier(text: String?): Self {
      this.identifier = text
      return this as Self
    }

    public final override fun setName(name: Name?): Self {
      this.name = name
      return this as Self
    }

    public final override fun setTemporal(temporal: Temporal?): Self {
      this.temporal = temporal
      return this as Self
    }

    @Suppress("BuilderSetStyle")
    public final override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class.java != other::class.java) return false
      other as Self
      if (description != other.description) return false
      if (disambiguatingDescription != other.disambiguatingDescription) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (temporal != other.temporal) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    public final override fun hashCode(): Int =
      Objects.hash(
        description,
        disambiguatingDescription,
        identifier,
        name,
        temporal,
        additionalProperties
      )

    @Suppress("BuilderSetStyle")
    public final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (description != null) {
        attributes["description"] = description!!.toString(includeWrapperName = false)
      }
      if (disambiguatingDescription != null) {
        attributes["disambiguatingDescription"] =
          disambiguatingDescription!!.toString(includeWrapperName = false)
      }
      if (identifier != null) {
        attributes["identifier"] = identifier!!
      }
      if (name != null) {
        attributes["name"] = name!!.toString(includeWrapperName = false)
      }
      if (temporal != null) {
        attributes["temporal"] = temporal!!.toString(includeWrapperName = false)
      }
      attributes += additionalProperties.map { (k, v) -> k to v.toString() }
      val commaSeparated =
        attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
      return """$selfTypeName($commaSeparated)"""
    }
  }
}

internal class ThingImpl : GenericThing<ThingImpl, ThingImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Thing"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    description: Description?,
    disambiguatingDescription: DisambiguatingDescription?,
    identifier: String?,
    name: Name?,
    temporal: Temporal?,
  ) : super(description, disambiguatingDescription, identifier, name, temporal)

  public constructor(thing: Thing) : super(thing)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  internal class Builder : GenericThing.Builder<Builder, ThingImpl>() {
    protected override val selfTypeName: String
      get() = "Thing.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromThing(thing: Thing): ThingImpl =
      thing as? ThingImpl ?: ThingImpl(thing)
  }
}
