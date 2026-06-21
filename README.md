# java-grpc-prometheus

![Build Status](https://github.com/tayvs/java-grpc-prometheus/actions/workflows/bazel_test.yaml/badge.svg)

Java interceptors which can be used to monitor Grpc services using Prometheus.

> **Fork notice.** This is a maintained fork of
> [`grpc-ecosystem/java-grpc-prometheus`](https://github.com/grpc-ecosystem/java-grpc-prometheus). The Maven groupId
> moved from `me.dinowernli` to **`io.github.tayvs`**, the artifact is split into a core library plus one provider
> module per supported Prometheus client version, and the interceptor API now takes a `Provider` instead of a
> `Configuration`. See the [Migration guide](#migrating-from-medinowernlijava-grpc-prometheus) below.

## Features

The features of this library include two monitoring grpc interceptors, `MonitoringServerInterceptor` and
`MonitoringClientInterceptor`. These interceptors can be attached separately to grpc servers and client stubs
respectively. For each RPC, the interceptors increment the following Prometheus metrics, broken down by method type,
service name, method name, and response code:

* Server
    * `grpc_server_started_total`: Total number of RPCs started on the server.
    * `grpc_server_handled_total`: Total number of RPCs completed on the server, regardless of success or failure.
    * `grpc_server_handled_latency_seconds`: (Optional) Histogram of response latency of rpcs handled by the server, in
      seconds.
    * `grpc_server_msg_received_total`: Total number of stream messages received from the client.
    * `grpc_server_msg_sent_total`: Total number of stream messages sent by the server.
* Client
    * `grpc_client_started_total`: Total number of RPCs started on the client.
    * `grpc_client_completed`: Total number of RPCs completed on the client, regardless of success or failure.
    * `grpc_client_completed_latency_seconds`: (Optional) Histogram of rpc response latency for completed rpcs, in
      seconds.
    * `grpc_client_msg_received_total`: Total number of stream messages received from the server.
    * `grpc_client_msg_sent_total`: Total number of stream messages sent by the client.

The names above are what the **`simpleclient` 0.16+**, **`*-metrics`** and **`*-metrics-legacy`** modules expose.

### Metric names on the legacy `simpleclient` (0.9) module

The `java-grpc-prometheus-simpleclient-legacy` module runs on `io.prometheus:simpleclient:0.9.0`, which predates the
automatic `_total` counter suffix introduced in simpleclient **0.10.0**. On that module the counters are exposed
**without** the `_total` suffix (the histograms are unchanged):

* Server: `grpc_server_started`, `grpc_server_handled`, `grpc_server_handled_latency_seconds` (histogram),
  `grpc_server_msg_received`, `grpc_server_msg_sent`
* Client: `grpc_client_started`, `grpc_client_completed`, `grpc_client_completed_latency_seconds` (histogram),
  `grpc_client_msg_received`, `grpc_client_msg_sent`

If you migrate from this module to any of the others, update dashboards/alerts to add the `_total` suffix on the
counter series. (The library configures identical names everywhere; the difference is purely the Prometheus client's
exposition behaviour — see [Breaking changes](#breaking-changes-between-supported-prometheus-clients).)

Note that by passing a `Configuration` instance to a `Provider`, it is possible to configure the following:

* Whether or not a latency histogram is recorded for RPCs (`cheapMetricsOnly()` vs `allMetrics()`).
* Which histogram buckets to use for the latency metrics (`withLatencyBuckets(...)`).
* Whether the response `code` label is added to the latency histogram (`withCodeLabelInLatencyHistogram()`).
* (Optional) Which headers you want to be applied to metrics as added labels (`withLabelHeaders(...)`).

The Prometheus registry the metrics get registered with is no longer configured through `Configuration`; it is passed
to the `Provider` constructor instead (see [Custom registry](#custom-registry)).

The server interceptors have an identical implementation in
Golang, [go-grpc-prometheus](https://github.com/mwitkow/go-grpc-prometheus), brought to you
by [@MWitkow](http://twitter.com/mwitkow).

## Installation

This fork is published under the `io.github.tayvs` groupId. It is split into a provider-agnostic **core** library and
several **provider** modules, one per supported Prometheus client version. Add exactly one provider module — it pulls
in the core library and the matching `io.prometheus` client transitively.

### Which module do I use?

Pick the provider module that matches the Prometheus client you already have (or want) on your classpath:

| Prometheus client on your classpath              | Maven artifact (`io.github.tayvs`)        | Pulls in                              |
|--------------------------------------------------|-------------------------------------------|---------------------------------------|
| `prometheus-metrics-core` **1.6+** (tested 1.8)  | `java-grpc-prometheus-metrics`            | `io.prometheus:prometheus-metrics-core:1.8.0` |
| `prometheus-metrics-core` **1.5.x**              | `java-grpc-prometheus-metrics-legacy`     | `io.prometheus:prometheus-metrics-core:1.5.0` |
| `simpleclient` **0.16.x** (the 0.x client)       | `java-grpc-prometheus-simpleclient`       | `io.prometheus:simpleclient:0.16.0`   |
| `simpleclient` **0.9.x** (older 0.x client)      | `java-grpc-prometheus-simpleclient-legacy`| `io.prometheus:simpleclient:0.9.0`    |

The current version is `1.0.0-RC1-SNAPSHOT`.

**Why a 1.5 and a 1.6+ split?** `prometheus-metrics-core` changed metric-name validation in **1.6.0**: reserved
suffixes (`_total`, `_count`, `_sum`, `_bucket`, `_created`, `_info`) that were rejected at registration time before
1.6 are now allowed and validated at scrape time, while cross-metric name collisions became stricter. The two metrics
modules are otherwise identical — `*-metrics` targets 1.6+ (built/tested against 1.8.0) and `*-metrics-legacy` targets
the pre-1.6 `1.5.x` line so callers on either side of that boundary get a build that compiles and registers cleanly
against their client.

> Each provider module ships its `Provider` in the same package, `dev.tayvs.grpc.prometheus.Provider`. Put **only one**
> provider module on the classpath at a time.

### Breaking changes between supported Prometheus clients

The four provider modules exist because the underlying `io.prometheus` clients introduced incompatible changes across
versions. The ones that affect this library:

* **`simpleclient` 0.9 → 0.10 — counter `_total` suffix.** Starting in simpleclient 0.10.0, counters are exposed with
  an automatic `_total` suffix. The `*-simpleclient-legacy` module (0.9.0) therefore emits counter series **without**
  `_total` (e.g. `grpc_server_started`), while `*-simpleclient` (0.16.0) emits them **with** `_total`
  (e.g. `grpc_server_started_total`). Same metric names in code, different exposed series.
* **`simpleclient` 0.x → `prometheus-metrics-core` 1.x — new client and registry API.** The 1.x line is a full rewrite:
  the registry type changed from `io.prometheus.client.CollectorRegistry` to
  `io.prometheus.metrics.model.registry.PrometheusRegistry`, and the metric builders no longer use
  `namespace`/`subsystem`. This is why the `simpleclient` modules take a `CollectorRegistry` and the `*-metrics` modules
  take a `PrometheusRegistry` (see [Custom registry](#custom-registry)). Counters are exposed with `_total` on 1.x.
* **`prometheus-metrics-core` 1.5 → 1.6 — suffix-based name validation.** 1.6.0 changed metric-name validation: the
  reserved suffixes `_total`, `_count`, `_sum`, `_bucket`, `_created`, `_info` are no longer rejected at registration
  time (validation moved to scrape time), and cross-metric name collisions became stricter. The `*-metrics-legacy`
  module (1.5.0) registers under the old pre-1.6 rules; `*-metrics` (1.8.0) registers under the new ones. This
  library's own metric names are collision-free under both rule sets, so the exposed series are identical between the
  two metrics modules — pick whichever matches the `prometheus-metrics-core` version already on your classpath.

### Maven

```xml
<dependency>
  <groupId>io.github.tayvs</groupId>
  <artifactId>java-grpc-prometheus-metrics</artifactId>
  <version>1.0.0-RC1-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.tayvs:java-grpc-prometheus-metrics:1.0.0-RC1-SNAPSHOT'
```

> Snapshots are published to the Maven Central snapshot repository
> (`https://central.sonatype.com/repository/maven-snapshots/`). Add it as a repository until a non-`SNAPSHOT` release
> is cut.

## Usage

The interceptors are created from a `Provider`. The `Provider` bundles a `Configuration` with the
version-specific Prometheus registry; convenience factory methods cover the common cases:

* `Provider.cheapMetricsOnly()` — counters only, no latency histograms.
* `Provider.allMetrics()` — counters plus latency histograms.
* `new Provider(Configuration ...)` — full control over the `Configuration`.

In order to attach the monitoring server interceptor to your gRPC server, you can do the following:

```java
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(new Provider(Configuration.cheapMetricsOnly()));
grpcServer = ServerBuilder.forPort(GRPC_PORT)
    .addService(ServerInterceptors.intercept(
        HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
    .build();
```

In order to attach the monitoring client interceptor to your gRPC client, you can do the following:

```java
MonitoringClientInterceptor monitoringInterceptor =
    MonitoringClientInterceptor.create(new Provider(Configuration.cheapMetricsOnly()));
grpcStub = HelloServiceGrpc.newStub(NettyChannelBuilder.forAddress(REMOTE_HOST, GRPC_PORT)
    .intercept(monitoringInterceptor)
    .build());
```

If you want to instruct the interceptor to use a specific header of interest, for example "header-1" as a label on all
produced metrics, you can do the following, which will cause the metrics to carry an extra label "header_1" whose
value is filled from the header value on each request:

```java
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        new Provider(
            Configuration
                .cheapMetricsOnly()
                .withLabelHeaders(Arrays.asList("header-1"))));
grpcServer = ServerBuilder.forPort(GRPC_PORT)
    .addService(ServerInterceptors.intercept(
        HelloServiceGrpc.bindService(new HelloServiceImpl()), monitoringInterceptor))
    .build();
```

### Custom registry

The Prometheus registry is supplied to the `Provider` constructor. The registry type depends on the provider module you
use: the `*-metrics` modules take an `io.prometheus.metrics.model.registry.PrometheusRegistry`, while the
`simpleclient` modules take an `io.prometheus.client.CollectorRegistry`. When omitted, the provider uses that client's
default registry.

For the `prometheus-metrics-core` (1.6+ / `*-metrics`) module:

```java
PrometheusRegistry registry = new PrometheusRegistry();
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        new Provider(Configuration.cheapMetricsOnly(), registry));
```

For the `simpleclient` modules:

```java
CollectorRegistry collectorRegistry = new CollectorRegistry();
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        new Provider(Configuration.cheapMetricsOnly(), collectorRegistry));
```

If you're using Spring Boot with micrometer-registry-prometheus you should inject the registry that is already provided
in the application context and pass it to the `Provider`:

```java
@Autowired
private CollectorRegistry collectorRegistry; // or PrometheusRegistry, depending on the module

// use the provided registry
MonitoringServerInterceptor monitoringInterceptor =
    MonitoringServerInterceptor.create(
        new Provider(Configuration.cheapMetricsOnly(), collectorRegistry));
```

## Migrating from `me.dinowernli:java-grpc-prometheus`

If you are upgrading from the original `grpc-ecosystem/java-grpc-prometheus` (`me.dinowernli` coordinates), the changes
are:

1. **New coordinates.** Replace `me.dinowernli:java-grpc-prometheus:0.3.0` with one of the `io.github.tayvs` provider
   artifacts from the [module table](#which-module-do-i-use) above. The Java package is unchanged on the original
   project but is `dev.tayvs.grpc.prometheus` here, so update your imports.
2. **Wrap `Configuration` in a `Provider`.** Every `MonitoringServerInterceptor.create(...)` /
   `MonitoringClientInterceptor.create(...)` call now takes a `Provider`:
   ```diff
   - MonitoringServerInterceptor.create(Configuration.cheapMetricsOnly());
   + MonitoringServerInterceptor.create(new Provider(Configuration.cheapMetricsOnly()));
   ```
   or use the shorthand `Provider.cheapMetricsOnly()` / `Provider.allMetrics()`.
3. **`Configuration.withCollectorRegistry(...)` is gone.** Pass the registry to the `Provider` constructor instead
   (see [Custom registry](#custom-registry)):
   ```diff
   - Configuration.cheapMetricsOnly().withCollectorRegistry(registry)
   + new Provider(Configuration.cheapMetricsOnly(), registry)
   ```
4. **Pick the right provider module for your Prometheus client.** The original published a single artifact against the
   `simpleclient` API. This fork additionally supports the modern `prometheus-metrics-core` (1.5 and 1.6+) clients —
   choose the module that matches your client version.

Metric names and labels are unchanged, so existing dashboards and alerts keep working.

## Related reading

* [gRPC](http://grpc.io)
* [Prometheus](http://prometheus.io)
</content>
</invoke>
