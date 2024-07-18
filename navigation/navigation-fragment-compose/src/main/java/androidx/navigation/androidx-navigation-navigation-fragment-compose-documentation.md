# Module root

androidx.navigation navigation-fragment-compose

# Package androidx.navigation.fragment.compose

Navigation with Fragments support destinations written as Fragments. This artifacts builds
upon that base to allow you to add destinations written purely in Compose to your navigation
graph without rewriting your entire navigation structure.

It does this by wrapping each destination written in Compose in its own Fragment instance,
using reflection to call your `@Composable` function.
