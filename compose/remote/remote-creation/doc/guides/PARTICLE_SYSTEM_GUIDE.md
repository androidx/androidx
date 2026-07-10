# RemoteCompose Particle System Guide

The RemoteCompose particle system allows for efficient, high-performance animations of many similar objects (particles) using procedural creation APIs. It minimizes communication by running the particle evolution logic directly on the player side.

## Core Concepts

The system consists of two main operations:
1.  **`ParticlesCreate`**: Initializes a set of particles with starting values for a fixed set of variables.
2.  **`ParticlesLoop`**: Iterates over the particles, updates their variables using mathematical equations (RPN-based), and optionally restarts them based on a condition.

## Basic Workflow

### 1. Define Variables and Create the System
Use `createParticles` to define the variables each particle will track and their initial state.

```kotlin
val variables: Array<RFloat> = Array(4) { RFloat(this, 0f) }
val psId = createParticles(
    variables,
    arrayOf(
        cx,      // initial x
        cy,      // initial y
        rand() - 0.5f, // initial dx
        rand() - 0.5f  // initial dy
    ),
    pCount = 100
)
val (px, py, dx, dy) = variables
```

- **`variables`**: An array of `RFloat` objects representing the state of a single particle during the loop.
- **`initialValues`**: An array of values or expressions used to initialize each particle. You can use `index()` to vary initial values per particle.
- **`pCount`**: The total number of particles in the system.

### 2. Update and Draw Particles
Use `particlesLoops` to define how particles evolve over time and how they are rendered.

```kotlin
val dt = deltaTime() // Time since last frame

particlesLoops(
    psId,
    restartCondition = null, // Optional expression, restarts particle if > 0
    updateEquations = arrayOf(
        px + dx * dt, // New px
        py + dy * dt, // New py
        dx,           // dx remains same
        dy + 9.8f * dt // Add gravity to dy
    )
) {
    // Inside this block, 'variables' (px, py, etc.) hold the current particle's state
    drawCircle(px.toFloat(), py.toFloat(), 5f)
}
```

- **`restartCondition`**: An expression that, if it evaluates to > 0, triggers a call to the initialization equations for that specific particle.
- **`updateEquations`**: New values for each of the variables defined in `createParticles`.
- **`drawingBlock`**: Operations in this block are executed for *each* particle in every frame.

## Key APIs & Helpers

### Impulse Control
Particles are often used inside an `impulse` block to manage their lifetime and ensure they only run when triggered.

```kotlin
val eventTime = ContinuousSec().toFloat()
impulse(durationMs = 2000f, eventTime) {
    // System setup
    impulseProcess() {
        // particlesLoops and drawing
    }
}
```

### Randomness
- `rand()`: Returns a random float between 0 and 1 on the player side. Essential for varying particle behavior.

### Particle Index
- `index()`: Returns the index (0..pCount-1) of the particle currently being initialized or updated.

### Collisions and Logic: `particlesComparison`

The `particlesComparison` operation (implemented by `ParticlesCompare.java`) allows for conditional logic and inter-particle interactions. It can operate in two modes:

#### 1. Single-Particle Mode (Boundary/Condition Check)
Used to check a condition for each particle independently (e.g., hitting a wall).

```kotlin
particlesComparison(
    id = psId,
    min = -1f, max = -1f, // Process all particles
    condition = px + ballRad - w, // Evaluates per particle
    then = arrayOf(
        dx * -elastic, // Update dx (bounce)
        dy,
        w - ballRad - 1f, // Snap back inside x
        py
    )
) {
    // Optional: Draw something or trigger haptics when condition is met
    performHaptic(2)
}
```

#### 2. Dual-Particle Mode (Inter-particle Interaction)
If you provide both a `then` and an `else` (or two result blocks), the system can perform $O(N^2)$ comparisons between pairs of particles. This is useful for collisions between particles or gravity-like effects.

- **CMD1 / CMD2**: In dual-particle mode, special commands allow you to refer to the properties of the first and second particle in the pair.

### Performance Tips
- **Keep it Simple**: The drawing block and update equations are executed every frame for every particle.
- **N^2 Caution**: Dual-particle comparisons are computationally expensive ($O(N^2)$). Use them sparingly and for small particle counts.
- **Use `restartCondition`**: Instead of creating new systems, use the restart condition in `particlesLoops` to recycle particles (e.g., resetting a spark to the origin).

