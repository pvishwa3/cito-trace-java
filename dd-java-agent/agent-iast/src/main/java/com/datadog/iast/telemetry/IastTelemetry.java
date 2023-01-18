package com.datadog.iast.telemetry;

import com.datadog.iast.taint.TaintedObjects;
import datadog.trace.api.Config;
import javax.annotation.Nonnull;

public interface IastTelemetry {

  TaintedObjects withTelemetry(@Nonnull TaintedObjects taintedObjects);

  static IastTelemetry build(@Nonnull final Config config) {
    if (!config.isTelemetryEnabled()) {
      return new NoOpTelemetry();
    }
    return new IastTelemetryImpl(config.getIastTelemetryVerbosity());
  }
}
