# Guide to Building RemoteCompose Component-Based Examples

RemoteCompose provides two distinct component/modifier systems. Choose the guide based on your preferred API style:

### [Procedural API Guide](PROCEDURAL_COMPONENTS_GUIDE.md) (`RemoteComposeContextAndroid`)
Best for demos registered in `DemosCreation.java`. Uses raw units (pixels/floats) and procedural construction.

### [Compose-like API Guide](COMPOSE_COMPONENTS_GUIDE.md) (`@RemoteComposable`)
Best for demos registered in `DemosCompose.kt`. Uses standard Compose-like syntax, units (`dp`, `sp`), and state management.
