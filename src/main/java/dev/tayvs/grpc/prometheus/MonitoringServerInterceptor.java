package dev.tayvs.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.time.Clock;

/** A {@link ServerInterceptor} which sends stats about incoming grpc calls to Prometheus. */
public class MonitoringServerInterceptor implements ServerInterceptor {
  private final Clock clock;
  private final Configuration configuration;
  private final ServerMetricsI.FactoryI serverMetricsFactory;

  public static MonitoringServerInterceptor create(ProviderI<?> provider) {
    return new MonitoringServerInterceptor(Clock.systemDefaultZone(), provider);
  }

  private MonitoringServerInterceptor(Clock clock, ProviderI<?> providerI) {
    this.clock = clock;
    this.configuration = providerI.getConfig();
    this.serverMetricsFactory = providerI.serverMetricsFactory();
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      ServerCall<R, S> call, Metadata requestMetadata, ServerCallHandler<R, S> next) {
    MethodDescriptor<R, S> methodDescriptor = call.getMethodDescriptor();
    GrpcMethod grpcMethod = GrpcMethod.of(methodDescriptor);
    ServerMetricsI metrics = serverMetricsFactory.createMetricsForMethod(grpcMethod);
    ServerCall<R, S> monitoringCall =
        new MonitoringServerCall<>(call, clock, grpcMethod, metrics, configuration, requestMetadata);
    return new MonitoringServerCallListener<>(
        next.startCall(monitoringCall, requestMetadata), metrics, grpcMethod, requestMetadata);
  }
}
