package dev.tayvs.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.Status;

public interface ClientMetricsI {
  void recordCallStarted(Metadata metadata);

  void recordClientHandled(Status.Code code, Metadata metadata);

  void recordStreamMessageSent(Metadata metadata);

  void recordStreamMessageReceived(Metadata metadata);

  void recordLatency(double latencySec, Metadata metadata);

  interface FactoryI {
    ClientMetricsI createMetricsForMethod(GrpcMethod grpcMethod);
  }
}
