# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Responds to Market Data API release 1.4.0: `?dte` on `GET /v1/options/chain/{underlying}/`
changed from an integer to a string, so it can express a range or a list, not just a single
day count.

### Added

- `ExpirationFilter.dteRange(int minDays, int maxDays)` — days-to-expiration filter for an
  inclusive range, wire form `?dte=MIN-MAX`.
- `ExpirationFilter.dteIn(List<Integer> days)` — days-to-expiration filter for a discrete set
  of days, wire form `?dte=D1,D2,...`.

### Changed

- **BREAKING:** `ExpirationFilter.Dte` now wraps a `String` instead of an `int`, matching the
  API's wider `?dte` syntax. `ExpirationFilter.dte(int)` still exists and behaves the same from
  the caller's side, but the record itself changed shape:
  - `Dte.days()` is gone; use `Dte.value()` (the raw wire-form string) instead.
  - The canonical constructor is now `new ExpirationFilter.Dte(String)`; code constructing
    `new ExpirationFilter.Dte(int)` directly (instead of going through
    `ExpirationFilter.dte(int)`) no longer compiles and must pass a `String` or switch to the
    factory method.

## [1.0.0] - 2026-06-29

First stable release of the Market Data Java &amp; Kotlin SDK — a single JVM
artifact, idiomatic from both Java and Kotlin, covering the full v1 API surface
with sync + async parity on every endpoint.

### Added

- **`MarketDataClient`** — no-arg constructor (everything resolved from the
  configuration cascade) and a 4-arg constructor for tests/short-lived runtimes.
  Single shared `HttpClient` (HTTP/2), 99 s request / 2 s connect timeouts,
  `close()` for resource release, and `getRateLimits()`.
- **Configuration cascade** — explicit constructor parameters → `MARKETDATA_*`
  environment variables → `.env` in the working directory → built-in defaults.
  `baseUrl` / `apiVersion` are normalized and validated at construction.
- **Demo mode** — without a token the client omits the `Authorization` header
  and serves public, read-only endpoints; `validateOnStartup` verifies auth on
  construction otherwise.
- **Options resource** (`client.options()`) — `lookup`, `expirations`,
  `strikes`, `quote`, `quotes` (per-symbol fan-out map), and `chain` with the
  full filter surface (sealed `ExpirationFilter` / `StrikeFilter`, greeks,
  `expiration=all`, `countback`).
- **Stocks resource** (`client.stocks()`) — `candles`, `quote`, `quotes` and
  `prices` (batched multi-symbol), `news`, and `earnings`. `StockResolution`
  value type for open-ended resolutions; intraday windows over ~1 year are
  auto-split, fetched concurrently, and merged.
- **Markets resource** (`client.markets()`) — `status`, the exchange
  open/closed calendar with `date` / `from`-`to` / `countback` windowing and
  `country` selection.
- **Funds resource** (`client.funds()`) — `candles`, NAV OHLC series at
  daily-and-up resolutions (`FundResolution`).
- **Utilities resource** (`client.utilities()`) — service `status` and auth
  validation.
- **Universal parameters & formats** — `dateFormat` / `mode` / `limit` /
  `offset` / `columns` set fluently on every resource (immutable configured
  copies), plus an `asCsv()` facet (with `human` / `headers` shaping) for every
  endpoint. `columns` projection enforces the Option A strict-decoding contract.
- **Reliability** — retry with exponential backoff (4 total attempts,
  `Retry-After` honored), client-level and per-response rate-limit snapshots
  with preflight, `/status/` stale-while-revalidate cache gating retries, and a
  50-permit async concurrency pool.
- **Sealed `MarketDataException` hierarchy** — seven canonical subtypes
  (`AuthenticationError`, `BadRequestError`, `NotFoundError`, `RateLimitError`,
  `ServerError`, `NetworkError`, `ParseError`), each carrying support context
  (`requestId`, `requestUrl`, `statusCode`, `timestamp`) and `getSupportInfo()`.
- **First-class Kotlin interop** — `@NullMarked` (JSpecify) on every public
  package, no Kotlin-stdlib or coroutines runtime dependency, SAM callbacks, and
  Kotlin-reserved-word-free public API.
- **Packaging** — MIT license, SemVer, version auto-detected from the JAR
  manifest, published to Maven Central as `app.marketdata:marketdata-sdk-java`.

[Unreleased]: https://github.com/MarketDataApp/sdk-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/MarketDataApp/sdk-java/releases/tag/v1.0.0
