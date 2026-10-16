# FinBaba 🧘‍♂️💰

FinBaba is a privacy-first, local-native Android financial studio designed to unpack Indian mutual fund portfolios. Instead of stopping at fund-level nominal valuations, FinBaba evaluates true underlying asset exposures across 700+ individual company securities, maps SEBI market capitalization splits, and simulates disciplined fresh capital allocation without selling existing units.

---

## Key Features

- **App Vault Architecture**: Built as a modular launcher hub designed to house multiple specialized financial utilities in one cohesive studio.
- **Deep Look-Through Engine**: Aggregates hundreds of underlying equities across fund houses to identify actual stock concentration, hidden multi-fund overlap, and industry sector exposure.
- **SEBI Market Cap Breakdown**: Dynamic real-time distribution across Large Cap (top 100), Mid Cap (101–250), Small Cap (251+), and Micro Cap holdings.
- **Smart Inflow Rebalancer**: A What-If capital simulation sandbox that calculates portfolio drift against target weights and suggests exact rupee amounts to allocate exclusively to underweight funds (outside a $\pm 5\%$ band) with zero capital gains tax or redemption exit loads.
- **Local-First & Private**: Operates offline without requiring cloud logins, third-party credentials, or tracking. Data is seeded locally from asset storage and persists directly on-device.
- **AMFI Live Sync**: Optional on-demand closing NAV fetching via the open `api.mfapi.in` REST endpoint.

---

## Tech Stack

- **Platform**: Android (Min SDK: 26 / Target SDK: 35)
- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with unidirectional data flow (`StateFlow`, `Coroutines`)
- **Networking**: Retrofit 2, OkHttp 4
- **Serialization**: Moshi Kotlin
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with version catalogs (`libs.versions.toml`)

---

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone [https://github.com/](https://github.com/)<your-username>/FinBaba.git
