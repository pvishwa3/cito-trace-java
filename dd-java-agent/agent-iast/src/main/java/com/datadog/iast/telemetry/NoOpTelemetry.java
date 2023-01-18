package com.datadog.iast.telemetry;

import com.datadog.iast.taint.TaintedObjects;
import javax.annotation.Nonnull;

public class NoOpTelemetry implements IastTelemetry {

  @Override
  public TaintedObjects withTelemetry(@Nonnull final TaintedObjects taintedObjects) {
    return taintedObjects;
  }
}
