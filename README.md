# slot-config-service

Spring Cloud Config-backed service for the `slot-central` microservices platform — centralizes externalized configuration and feature flags across services (e.g. reel strips, paytables, bonus game configs currently living in `slot-game-engine-service`; the `gaffing.enabled` feature flag; other per-environment/per-service settings).

This service does not own game math, wallet, or floor state itself — it serves as the config backend that other services can migrate to pull their externalized YAML/JSON configuration from at runtime, rather than baking config into each service's own resources directory.

This repository is part of a re-architecture of the `slot-central-server-express-rmq` Node.js EGM slot-floor backend into Spring Boot microservices.

Scaffolding in progress.
