package dev.tayvs.grpc.prometheus;

import io.grpc.Metadata;
import io.grpc.Status;

public interface ServerMetricsI {
  void recordCallStarted(Metadata metadata);
  void recordServerHandled(Status.Code code, Metadata metadata);
  void recordStreamMessageSent(Metadata metadata);
  void recordStreamMessageReceived(Metadata metadata);
  void recordLatency(double latencySec, Metadata metadata, Status.Code code);

  interface FactoryI {
    ServerMetricsI createMetricsForMethod(GrpcMethod grpcMethod);
  }
}
