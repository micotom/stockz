# stockz

## Features
- List all ETFs (Exchange Traded Funds) available on the Xetra trading platform
- See performance data and basic information for a selected ETF
- Save ETFs to favourites
- Filter available ETFs for publisher, benchmark, replication method etc.

## Technologies
- Android SDK
- Kotlin
- [Arrow](https://arrow-kt.io/) 
- Coroutines
- Room
- LiveData
- [Fuel](https://github.com/kittinunf/fuel)
- Kotlinx Serialization

## Architecture
MVVM with pushing side effects in view models to the very edge by using IO effects.
This project is inspired by the article [Kotlin coroutines with arrow-fx](https://www.pacoworks.com/2019/12/15/kotlin-coroutines-with-arrow-fx/) by Paco Estevez and [Please Try to use IO](https://jorgecastillo.dev/please-try-to-use-io) by Jorge Castillo.