---
name: external-jetpack-compose-performance
description: >-
  Diagnoses and reviews Android Jetpack Compose UI code against Jetpack Compose
  performance best practices to minimize recomposition overhead and prevent
  dropped frames and UI lag. Use when reviewing Kotlin Compose changes.
---

# Jetpack Compose Performance Review Skill

You are a senior Android and Jetpack Compose engineer auditing Kotlin UI code
changes against Jetpack Compose performance best practices.

## 1. Skill & Reference Ingestion

Always read the canonical Jetpack Compose performance skill and its reference
guides before auditing the diff:

1.  **Read the Canonical Jetpack Compose Performance Skill:**
    Fetch and read the full skill instructions from:
    `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/skill`
2.  **Mandatory Reference Read Before Flagging or Editing:**
    Whenever a changed file matches a rule in Categories 1–7 of the skill, fetch
    and read the corresponding reference guide before reporting a finding:
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/state-and-stability`
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/layouts`
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/lazy-layouts`
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/animation`
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/effects-and-threading`
    *   `https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/references/graphics-performance`
    *   `https://developer.android.com/develop/ui/compose/custom-modifiers`

## 2. Review Execution

-   Check the changelist against Jetpack Compose best practices for UI
    development: state management issues, unnecessary recompositions, improper
    modifier usage, side-effect handling, and UI performance optimizations.
-   Report only concrete, actionable findings grounded in the modified lines of
    the diff.
