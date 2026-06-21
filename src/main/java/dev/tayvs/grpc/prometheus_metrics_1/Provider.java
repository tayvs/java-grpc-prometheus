package dev.tayvs.grpc.prometheus;

import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.util.function.Function;

public class Provider implements ProviderI<PrometheusRegistry> {

  public static Provider cheapMetricsOnly() {
    return new Provider(Configuration.cheapMetricsOnly());
  }

  public static Provider allMetrics() {
    return new Provider(Configuration.allMetrics());
  }

  private final Configuration config;
  private final PrometheusRegistry collectorRegistry;
  private final ClientMetricsI.FactoryI clientMetricsFactoryProvider;
  private final ServerMetricsI.FactoryI serverMetricsFactoryProvider;

  public Provider(Configuration config) {
    this(config, PrometheusRegistry.defaultRegistry);
  }

  public Provider(Configuration config, PrometheusRegistry collectorRegistry) {
    this(config, collectorRegistry, ClientMetrics.Factory::new, ServerMetrics.Factory::new);
  }

  public Provider(
      Configuration config,
      PrometheusRegistry collectorRegistry,
      Function<Provider, ClientMetricsI.FactoryI> clientMetricsFactoryProvider,
      Function<Provider, ServerMetricsI.FactoryI> serverMetricsFactoryProvider) {
    this.config = config;
    this.collectorRegistry = collectorRegistry;
    this.clientMetricsFactoryProvider = clientMetricsFactoryProvider.apply(this);
    this.serverMetricsFactoryProvider = serverMetricsFactoryProvider.apply(this);
  }

  @Override
  public ClientMetricsI.FactoryI clientMetricsFactory() {
    return clientMetricsFactoryProvider;
  }

  @Override
  public ServerMetricsI.FactoryI serverMetricsFactory() {
    return serverMetricsFactoryProvider;
  }

  @Override
  public Configuration getConfig() {
    return config;
  }

  @Override
  public PrometheusRegistry getRegistry() {
    return collectorRegistry;
  }
}
