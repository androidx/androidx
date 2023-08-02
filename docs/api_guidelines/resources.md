## Resources {#resources}

Generally, follow the official Android guidelines for
[app resources](https://developer.android.com/guide/topics/resources/providing-resources).
Special guidelines for library resources are noted below.

### Defining new resources

Libraries may define new value and attribute resources using the standard
application directory structure used by Android Gradle Plugin:

```
src/main/res/
  values/
    attrs.xml   Theme attributes and styleables
    dimens.xml  Dimensional values
    public.xml  Public resource definitions
    ...
```

However, some libraries may still be using non-standard, legacy directory
structures such as `res-public` for their public resource declarations or a
top-level `res` directory and accompanying custom source set in `build.gradle`.
These libraries will eventually be migrated to follow standard guidelines.

#### Naming conventions

Libraries follow the Android platform's resource naming conventions, which use
`camelCase` for attributes and `underline_delimited` for values. For example,
`R.attr.fontProviderPackage` and `R.dimen.material_blue_grey_900`.

#### Attribute formats

At build time, attribute definitions are pooled globally across all libraries
used in an application, which means attribute `format`s *must* be identical for
a given `name` to avoid a conflict.

Within Jetpack, new attribute names *must* be globally unique. Libraries *may*
reference existing public attributes from their dependencies. See below for more
information on public attributes.

When adding a new attribute, the format should be defined *once* in an `<attr
/>` element in the definitions block at the top of `src/main/res/attrs.xml`.
Subsequent references in `<declare-styleable>` elements *must* not include a
`format`:

`src/main/res/attrs.xml`

```xml
<resources>
  <attr name="fontProviderPackage" format="string" />

  <declare-styleable name="FontFamily">
      <attr name="fontProviderPackage" />
  </declare-styleable>
</resources>
```

#### Translatable strings

Translatable strings *must* be contained within files named `strings.xml` to be
picked up for translation.

### Public resources

Library resources in Jetpack are private by default, which means developers are
discouraged from referencing any defined attributes or values from XML or code;
however, library resources may be declared public to make them available to
developers.

Public library resources are considered API surface and are thus subject to the
same API consistency and documentation requirements as Java APIs.

Libraries will typically only expose theme attributes, ex. `<attr />` elements,
as public API so that developers can set and retrieve the values stored in
styles and themes. Exposing values -- such as `<dimen />` and `<string />` -- or
images -- such as drawable XML and PNGs -- locks the current state of those
elements as public API that cannot be changed without a major version bump. That
means changing a publicly-visible icon would be considered a breaking change.

#### Documentation

All public resource definitions should be documented, including top-level
definitions and re-uses inside `<styleable>` elements:

`src/main/res/attrs.xml`

```xml
<resources>
  <!-- String specifying the application package for a Font Provider. -->
  <attr name="fontProviderPackage" format="string" />

  <!-- Attributes that are read when parsing a <fontfamily> tag. -->
  <declare-styleable name="FontFamily">
      <!-- The package for the Font Provider to be used for the request. This is
           used to verify the identity of the provider. -->
      <attr name="fontProviderPackage" />
  </declare-styleable>
</resources>
```

`src/main/res/colors.xml`

```xml
<resources>
  <!-- Color for Material Blue-Grey 900. -->
  <color name="material_blue_grey_900">#ff263238</color>
</resources>
```

#### Public declaration

Resources are declared public by providing a separate `<public />` element with
a matching type:

`src/main/res/public.xml`

```xml
<resources>
  <public name="fontProviderPackage" type="attr" />
  <public name="material_blue_grey_900" type="color" />
</resources>
```

#### More information

See also the official Android Gradle Plugin documentation for
[Private Resources](https://developer.android.com/studio/projects/android-library#PrivateResources).

### Manifest entries (`AndroidManifest.xml`) {#resources-manifest}

#### Metadata tags (`<meta-data>`) {#resources-manifest-metadata}

Developers **must not** add `<application>`-level `<meta-data>` tags to library
manifests or advise developers to add such tags to their application manifests.
Doing so may *inadvertently cause denial-of-service attacks against other apps*
(see b/194303997).

Assume a library adds a single item of meta-data at the application level. When
an app uses the library, that meta-data will be merged into the resulting app's
application entry via manifest merger.

If another app attempts to obtain a list of all activities associated with the
primary app, that list will contain multiple copies of the `ApplicationInfo`,
each of which in turn contains a copy of the library's meta-data. As a result,
one `<metadata>` tag may become hundreds of KB on the binder call to obtain the
list -- resulting in apps hitting transaction too large exceptions and crashing.

```xml {.bad}
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.librarypackage">
  <application>
    <meta-data
        android:name="keyName"
        android:value="@string/value" />
  </application>
</manifest>
```

Instead, developers may consider adding `<metadata>` nested inside of
placeholder `<service>` tags.

```xml {.good}
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.librarypackage">
  <application>
    <service
        android:name="androidx.librarypackage.MetadataHolderService"
        android:enabled="false"
        android:exported="false">
      <meta-data
          android:name="androidx.librarypackage.MetadataHolderService.KEY_NAME"
          android:resource="@string/value" />
    </service>
  </application>
```

Note: there is no need to provide a definition for the `MetadataHolderService`
class, as it is merely a placeholder and will never be called.
