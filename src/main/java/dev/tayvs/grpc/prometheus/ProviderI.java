package dev.tayvs.grpc.prometheus;

public interface ProviderI<REGISTRY> {
//  ClientMetricsI.FactoryI clientMetricsFactory(Configuration configuration);
  ClientMetricsI.FactoryI clientMetricsFactory();
  ServerMetricsI.FactoryI serverMetricsFactory();
  Configuration getConfig();

  REGISTRY getRegistry();
}
