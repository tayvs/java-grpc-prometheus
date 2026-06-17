package dev.tayvs.grpc.prometheus;

import io.prometheus.client.CollectorRegistry;

public class Provider implements ProviderI<CollectorRegistry> {

    private final Configuration config;
    private final CollectorRegistry collectorRegistry;

    Provider(Configuration config) {
        this.config = config;
        this.collectorRegistry = CollectorRegistry.defaultRegistry;
    }

    Provider(Configuration config, CollectorRegistry collectorRegistry) {
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
    public CollectorRegistry getRegistry() {
        return collectorRegistry;
    }
}
