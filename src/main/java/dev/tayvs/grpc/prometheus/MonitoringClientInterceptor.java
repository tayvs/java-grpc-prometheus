package dev.tayvs.grpc.prometheus;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.time.Clock;

/** A {@link ClientInterceptor} which sends stats about incoming grpc calls to Prometheus. */
public class MonitoringClientInterceptor implements ClientInterceptor {
  private final Clock clock;
  private final Configuration configuration;
  private final ClientMetricsI.FactoryI clientMetricsFactory;

  public static MonitoringClientInterceptor create(ProviderI<?> provider) {
    return new MonitoringClientInterceptor(Clock.systemDefaultZone(), provider);
  }

  private MonitoringClientInterceptor(Clock clock, ProviderI<?> providerI) {
    this.clock = clock;
    this.configuration = providerI.getConfig();
    this.clientMetricsFactory = providerI.clientMetricsFactory();
  }

  @Override
  public <R, S> ClientCall<R, S> interceptCall(
      MethodDescriptor<R, S> methodDescriptor, CallOptions callOptions, Channel channel) {
    GrpcMethod grpcMethod = GrpcMethod.of(methodDescriptor);
    ClientMetricsI metrics = clientMetricsFactory.createMetricsForMethod(grpcMethod);
    return new MonitoringClientCall<>(
        channel.newCall(methodDescriptor, callOptions), metrics, grpcMethod, configuration, clock);
  }
}
