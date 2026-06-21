package dev.tayvs.grpc.prometheus;

import io.prometheus.client.CollectorRegistry;

import java.util.function.Function;

public class Provider implements ProviderI<CollectorRegistry> {

  public static Provider cheapMetricsOnly() {
    return new Provider(Configuration.cheapMetricsOnly());
  }

  public static Provider allMetrics() {
    return new Provider(Configuration.allMetrics());
  }

  private final Configuration config;
  private final CollectorRegistry collectorRegistry;
  private final ClientMetricsI.FactoryI clientMetricsFactoryProvider;
  private final ServerMetricsI.FactoryI serverMetricsFactoryProvider;

  public Provider(Configuration config) {
    this(config, CollectorRegistry.defaultRegistry);
  }

  public Provider(Configuration config, CollectorRegistry collectorRegistry) {
    this(config, collectorRegistry, ClientMetrics.Factory::new, ServerMetrics.Factory::new);
  }

  public Provider(
      Configuration config,
      CollectorRegistry collectorRegistry,
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
  public CollectorRegistry getRegistry() {
    return collectorRegistry;
  }
}
