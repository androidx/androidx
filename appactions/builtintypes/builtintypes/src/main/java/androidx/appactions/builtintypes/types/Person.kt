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
 * A person (alive, dead, undead, or fictional).
 *
 * See https://schema.org/Person for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractPerson] if you need to extend this type.
 */
@Document(
  name = "bit:Person",
  parent = [Thing::class],
)
public interface Person : Thing {
  /**
   * Email address.
   *
   * See https://schema.org/email for more context.
   */
  @get:Document.StringProperty
  public val email: String?
    get() = null

  /**
   * The telephone number.
   *
   * See https://schema.org/telephone for more context.
   */
  @get:Document.StringProperty(name = "telephone")
  public val telephoneNumber: String?
    get() = null

  /** Converts this [Person] to its builder with all the properties copied over. */
  public override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic @Document.BuilderProducer public fun Builder(): Builder<*> = PersonImpl.Builder()
  }

  /**
   * Builder for [Person].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractPerson.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
    /** Returns a built [Person]. */
    public override fun build(): Person

    /** Sets the `email`. */
    @Suppress("DocumentExceptions")
    public fun setEmail(text: String?): Self = throw NotImplementedError()

    /** Sets the `telephoneNumber`. */
    @Suppress("DocumentExceptions")
    public fun setTelephoneNumber(text: String?): Self = throw NotImplementedError()
  }
}

/**
 * An abstract implementation of [Person].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyPerson",
 *   parent = [Person::class],
 * )
 * class MyPerson internal constructor(
 *   person: Person,
 *   val foo: String,
 *   val bars: List<Int>,
 * ) : AbstractPerson<
 *   MyPerson,
 *   MyPerson.Builder
 * >(person) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyPerson"
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
 *     AbstractPerson.Builder<
 *       Builder,
 *       MyPerson> {...}
 * }
 * ```
 *
 * Also see [AbstractPerson.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractPerson<
  Self : AbstractPerson<Self, Builder>, Builder : AbstractPerson.Builder<Builder, Self>>
internal constructor(
  public final override val namespace: String,
  public final override val email: String?,
  public final override val telephoneNumber: String?,
  public final override val disambiguatingDescription: DisambiguatingDescription?,
  public final override val identifier: String,
  public final override val name: Name?,
) : Person {
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

  /** A copy-constructor that copies over properties from another [Person] instance. */
  public constructor(
    person: Person
  ) : this(
    person.namespace,
    person.email,
    person.telephoneNumber,
    person.disambiguatingDescription,
    person.identifier,
    person.name
  )

  /** Returns a concrete [Builder] with the additional, non-[Person] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  public final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setEmail(email)
      .setTelephoneNumber(telephoneNumber)
      .setDisambiguatingDescription(disambiguatingDescription)
      .setIdentifier(identifier)
      .setName(name)

  public final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (email != other.email) return false
    if (telephoneNumber != other.telephoneNumber) return false
    if (disambiguatingDescription != other.disambiguatingDescription) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  public final override fun hashCode(): Int =
    Objects.hash(
      namespace,
      email,
      telephoneNumber,
      disambiguatingDescription,
      identifier,
      name,
      additionalProperties
    )

  public final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
    }
    if (email != null) {
      attributes["email"] = email
    }
    if (telephoneNumber != null) {
      attributes["telephoneNumber"] = telephoneNumber
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
   * An abstract implementation of [Person.Builder].
   *
   * Allows for extension like:
   * ```kt
   * class MyPerson :
   *   : AbstractPerson<
   *     MyPerson,
   *     MyPerson.Builder>(...) {
   *
   *   class Builder
   *   : AbstractPerson.Builder<
   *       Builder,
   *       MyPerson
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyPerson.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromPerson(
   *       person: Person
   *     ): MyPerson {
   *       return MyPerson(
   *         person,
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
   * Also see [AbstractPerson].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<Self : Builder<Self, Built>, Built : AbstractPerson<Built, Self>> :
    Person.Builder<Self> {
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

    private var email: String? = null

    private var telephoneNumber: String? = null

    private var disambiguatingDescription: DisambiguatingDescription? = null

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [Person].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [Person]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle") protected abstract fun buildFromPerson(person: Person): Built

    public final override fun build(): Built =
      buildFromPerson(
        PersonImpl(namespace, email, telephoneNumber, disambiguatingDescription, identifier, name)
      )

    public final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    public final override fun setEmail(text: String?): Self {
      this.email = text
      return this as Self
    }

    public final override fun setTelephoneNumber(text: String?): Self {
      this.telephoneNumber = text
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
      if (email != other.email) return false
      if (telephoneNumber != other.telephoneNumber) return false
      if (disambiguatingDescription != other.disambiguatingDescription) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    public final override fun hashCode(): Int =
      Objects.hash(
        namespace,
        email,
        telephoneNumber,
        disambiguatingDescription,
        identifier,
        name,
        additionalProperties
      )

    @Suppress("BuilderSetStyle")
    public final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
      }
      if (email != null) {
        attributes["email"] = email!!
      }
      if (telephoneNumber != null) {
        attributes["telephoneNumber"] = telephoneNumber!!
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

private class PersonImpl : AbstractPerson<PersonImpl, PersonImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Person"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    email: String?,
    telephoneNumber: String?,
    disambiguatingDescription: DisambiguatingDescription?,
    identifier: String,
    name: Name?,
  ) : super(namespace, email, telephoneNumber, disambiguatingDescription, identifier, name)

  public constructor(person: Person) : super(person)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractPerson.Builder<Builder, PersonImpl>() {
    protected override val selfTypeName: String
      get() = "Person.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromPerson(person: Person): PersonImpl =
      person as? PersonImpl ?: PersonImpl(person)
  }
}
