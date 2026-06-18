package dev.tayvs.grpc.prometheus;

import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class Provider implements ProviderI<PrometheusRegistry> {

  private final Configuration config;
  private final PrometheusRegistry collectorRegistry;

  public Provider(Configuration config) {
    this.config = config;
    this.collectorRegistry = PrometheusRegistry.defaultRegistry;
  }

  public Provider(Configuration config, PrometheusRegistry collectorRegistry) {
    this.config = config;
    this.collectorRegistry = collectorRegistry;
  }

  @Override
  public ClientMetricsI.FactoryI clientMetricsFactory() {
    return new ClientMetrics.Factory(this);
  }

  @Override
  public ServerMetricsI.FactoryI serverMetricsFactory() {
    return new ServerMetrics.Factory(this);
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
