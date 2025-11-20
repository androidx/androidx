# Reference documentation

go/androidx/reference_documentation

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'asfalcone' reviewed: '2025-09-15' }
*-->

[TOC]

Our reference docs (Javadocs and KotlinDocs) are published to
https://developer.android.com/reference/androidx/packages and may be built
locally.

## Generate docs

To build API reference docs for both Java and Kotlin source code using Dackka,
run the Gradle task:

```
./gradlew docs
```

Location of generated refdocs:

*   docs-public (what is published to DAC):
    `{androidx-main}/out/androidx/docs-public/build/docs`
*   docs-tip-of-tree: `{androidx-main}/out/androidx/docs-tip-of-tree/build/docs`

The generated docs are plain HTML pages with links that do not work locally.
These issues are fixed when the docs are published to DAC, but to preview a
local version of the docs with functioning links and CSS, run:

```
python3 development/offlinifyDocs/offlinify_dackka_docs.py
```

You will need to have the `bs4` Python package installed. The CSS used is not
the same as what will be used when the docs are published.

By default, this command converts the tip-of-tree docs for all libraries. To see
more options, run:

```
python3 development/offlinifyDocs/offlinify_dackka_docs.py --help
```

## Release docs

To build API reference docs for published artifacts formatted for use on
[d.android.com](http://d.android.com), run the Gradle command:

```
./gradlew zipDocs
```

This will create the artifact `{androidx-main}/out/dist/docs-public-0.zip`. This
command builds docs based on the version specified in
`{androidx-main-checkout}/frameworks/support/docs-public/build.gradle` and uses
the prebuilt checked into
`{androidx-main-checkout}/prebuilts/androidx/internal/androidx/`. We
colloquially refer to this two step process of (1) updating `docs-public` and
(2) checking in a prebuilt artifact into the prebuilts directory as
[The Prebuilts Dance](/docs/releasing_prebuilts_dance.md#the-prebuilts-dance™).
So, to build javadocs that will be published to
https://developer.android.com/reference/androidx/packages, both of these steps
need to be completed.
