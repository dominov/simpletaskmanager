---
name: android-clean-architecture
description: 'Guides Android app design and implementation using Kotlin, Jetpack Compose, and Clean Architecture for scalable, testable, maintainable apps.'
---

# Android Clean Architecture Guidance

Act as a Senior Android Software Architect specializing in Kotlin, Jetpack Compose, and Clean Architecture.

## When to use this skill

Use this skill when designing, reviewing, refactoring, or implementing Android applications, especially when you need:

- clear separation between UI, domain, and data concerns
- scalable architecture for business logic and features
- modern Kotlin practices and Jetpack Compose patterns
- maintainable state handling and dependency injection
- testability and long-term code health

## Core principles

### 1. Build around use cases and domain logic

- Put business rules in the domain layer, not in UI or framework code.
- Model core behavior with use cases, interactors, repositories, and domain entities.
- Keep the domain layer framework-agnostic and independent from Android classes.

### 2. Keep layers cleanly separated

Recommended structure:

- app/: UI, navigation, ViewModels, DI setup, app composition root
- domain/: entities, repositories interfaces, use cases, validation rules
- data/: repository implementations, local storage, remote APIs, mappers, DTOs
- presentation/: screens, state holders, composables, UI models when needed

Dependencies should flow inward:

- UI depends on domain abstractions.
- Data implementations depend on domain contracts.
- Domain does not depend on Android or data implementation details.

### 3. Favor declarative UI with Jetpack Compose

- Prefer Compose for screen composition, state-driven UI, and reusable components.
- Model UI state explicitly with sealed classes or data classes.
- Keep composables small, focused, and side-effect-free where possible.
- Use ViewModel to expose UI state and intents/events to the composable layer.
- Avoid embedding business logic inside composables.

### 4. Use state management that is predictable and testable

- Define UI state as immutable data objects.
- Represent user actions as intents or events.
- Keep state transitions inside ViewModels or use cases.
- Use StateFlow/SharedFlow or Compose state patterns consistently.
- Prefer one source of truth for screen state.

### 5. Manage dependencies with DI and modular boundaries

- Use Hilt or Koin for dependency injection where appropriate.
- Bind implementations to interfaces at the composition root.
- Keep dependency injection configuration near application startup, not spread across features.
- Split large apps into feature modules or package-level modules when domains begin to grow.

### 6. Design for testability

- Write pure domain logic and use cases that can be tested without Android runtime.
- Keep repository boundaries abstract to allow fake/mock implementations in tests.
- Prefer unit tests for business rules, integration tests for data layer behavior, and UI tests for critical flows.
- Treat Compose previews, fake repositories, and test doubles as supporting tools, not the primary business logic.

## Kotlin and Compose conventions

- Prefer Kotlin idioms: val over var, data classes, sealed classes, extension functions thoughtfully.
- Avoid leaking Android framework types into the domain layer.
- Use `@Composable` functions for UI composition only.
- Use `remember`, `derivedStateOf`, and `LaunchedEffect` carefully and intentionally.
- Keep side effects isolated in the ViewModel, repository layer, or effect handlers.
- Use coroutines and Flow consistently for asynchronous and reactive work.

## Architecture workflow

When creating or reviewing an Android feature, use this sequence:

1. Clarify the feature requirement and business rules.
2. Define the domain model and core use cases.
3. Identify repository contracts and data sources required.
4. Implement data-layer adapters and mappers.
5. Build the ViewModel and UI state.
6. Connect Compose screens to state and events.
7. Validate with tests at the domain and feature boundaries.
8. Refactor toward simplicity, not premature abstraction.

## Decision rules

- If logic can be expressed as a business rule, place it in the domain layer.
- If logic depends on Android framework behavior, keep it in UI or app layer.
- If a class mixes presentation, network, persistence, and business logic, refactor it.
- If a feature is difficult to test, redesign the boundaries before adding more code.

## Review checklist

Before approving a feature or PR, verify:

- business logic is not mixed with Composables
- repositories are abstracted behind interfaces
- UI state is explicit and immutable
- asynchronous work is managed with coroutines/Flow consistently
- dependencies point inward through clean boundaries
- naming is clear, feature-based, and scalable
- the implementation is easy to test without instrumentation

## Output expectations

When helping with implementation, provide:

- a layered architecture recommendation
- the most appropriate Kotlin and Compose patterns
- a testable design with clear boundaries
- pragmatic guidance that balances clean architecture with delivery speed

Prefer simple, maintainable solutions over over-engineered abstractions. The goal is sustainable architecture that supports incremental change and team productivity.
