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
 * Status indicating that the number of objects have reached the limit and more objects cannot be
 * created.
 *
 * See https://schema.googleapis.com/ObjectCreationLimitReachedStatus for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractObjectCreationLimitReachedStatus] if you need to extend
 * this type.
 */
@Document(
  name = "bit:ObjectCreationLimitReachedStatus",
  parent = [ExecutionStatus::class],
)
public interface ObjectCreationLimitReachedStatus : ExecutionStatus {
  /**
   * Converts this [ObjectCreationLimitReachedStatus] to its builder with all the properties copied
   * over.
   */
  public override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic
    @Document.BuilderProducer
    public fun Builder(): Builder<*> = ObjectCreationLimitReachedStatusImpl.Builder()
  }

  /**
   * Builder for [ObjectCreationLimitReachedStatus].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractObjectCreationLimitReachedStatus.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : ExecutionStatus.Builder<Self> {
    /** Returns a built [ObjectCreationLimitReachedStatus]. */
    public override fun build(): ObjectCreationLimitReachedStatus
  }
}

/**
 * An abstract implementation of [ObjectCreationLimitReachedStatus].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyObjectCreationLimitReachedStatus",
 *   parent = [ObjectCreationLimitReachedStatus::class],
 * )
 * class MyObjectCreationLimitReachedStatus internal constructor(
 *   objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus,
 *   val foo: String,
 *   val bars: List<Int>,
 * ) : AbstractObjectCreationLimitReachedStatus<
 *   MyObjectCreationLimitReachedStatus,
 *   MyObjectCreationLimitReachedStatus.Builder
 * >(objectCreationLimitReachedStatus) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyObjectCreationLimitReachedStatus"
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
 *     AbstractObjectCreationLimitReachedStatus.Builder<
 *       Builder,
 *       MyObjectCreationLimitReachedStatus> {...}
 * }
 * ```
 *
 * Also see [AbstractObjectCreationLimitReachedStatus.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractObjectCreationLimitReachedStatus<
  Self : AbstractObjectCreationLimitReachedStatus<Self, Builder>,
  Builder : AbstractObjectCreationLimitReachedStatus.Builder<Builder, Self>>
internal constructor(
  public final override val namespace: String,
  public final override val disambiguatingDescription: DisambiguatingDescription?,
  public final override val identifier: String,
  public final override val name: Name?,
) : ObjectCreationLimitReachedStatus {
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

  /**
   * A copy-constructor that copies over properties from another [ObjectCreationLimitReachedStatus]
   * instance.
   */
  public constructor(
    objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
  ) : this(
    objectCreationLimitReachedStatus.namespace,
    objectCreationLimitReachedStatus.disambiguatingDescription,
    objectCreationLimitReachedStatus.identifier,
    objectCreationLimitReachedStatus.name
  )

  /**
   * Returns a concrete [Builder] with the additional, non-[ObjectCreationLimitReachedStatus]
   * properties copied over.
   */
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
   * An abstract implementation of [ObjectCreationLimitReachedStatus.Builder].
   *
   * Allows for extension like:
   * ```kt
   * class MyObjectCreationLimitReachedStatus :
   *   : AbstractObjectCreationLimitReachedStatus<
   *     MyObjectCreationLimitReachedStatus,
   *     MyObjectCreationLimitReachedStatus.Builder>(...) {
   *
   *   class Builder
   *   : AbstractObjectCreationLimitReachedStatus.Builder<
   *       Builder,
   *       MyObjectCreationLimitReachedStatus
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyObjectCreationLimitReachedStatus.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromObjectCreationLimitReachedStatus(
   *       objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
   *     ): MyObjectCreationLimitReachedStatus {
   *       return MyObjectCreationLimitReachedStatus(
   *         objectCreationLimitReachedStatus,
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
   * Also see [AbstractObjectCreationLimitReachedStatus].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<
    Self : Builder<Self, Built>, Built : AbstractObjectCreationLimitReachedStatus<Built, Self>> :
    ObjectCreationLimitReachedStatus.Builder<Self> {
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
     * Builds a concrete [Built] instance, given a built [ObjectCreationLimitReachedStatus].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [ObjectCreationLimitReachedStatus]-specific properties and the subclass specific
     * [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle")
    protected abstract fun buildFromObjectCreationLimitReachedStatus(
      objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
    ): Built

    public final override fun build(): Built =
      buildFromObjectCreationLimitReachedStatus(
        ObjectCreationLimitReachedStatusImpl(namespace, disambiguatingDescription, identifier, name)
      )

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

private class ObjectCreationLimitReachedStatusImpl :
  AbstractObjectCreationLimitReachedStatus<
    ObjectCreationLimitReachedStatusImpl, ObjectCreationLimitReachedStatusImpl.Builder
  > {
  protected override val selfTypeName: String
    get() = "ObjectCreationLimitReachedStatus"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    disambiguatingDescription: DisambiguatingDescription?,
    identifier: String,
    name: Name?,
  ) : super(namespace, disambiguatingDescription, identifier, name)

  public constructor(
    objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
  ) : super(objectCreationLimitReachedStatus)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder :
    AbstractObjectCreationLimitReachedStatus.Builder<
      Builder, ObjectCreationLimitReachedStatusImpl
    >() {
    protected override val selfTypeName: String
      get() = "ObjectCreationLimitReachedStatus.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromObjectCreationLimitReachedStatus(
      objectCreationLimitReachedStatus: ObjectCreationLimitReachedStatus
    ): ObjectCreationLimitReachedStatusImpl =
      objectCreationLimitReachedStatus as? ObjectCreationLimitReachedStatusImpl
        ?: ObjectCreationLimitReachedStatusImpl(objectCreationLimitReachedStatus)
  }
}
