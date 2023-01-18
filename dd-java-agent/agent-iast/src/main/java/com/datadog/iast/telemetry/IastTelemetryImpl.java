package com.datadog.iast.telemetry;

import com.datadog.iast.taint.TaintedObjects;
import com.datadog.iast.telemetry.taint.TaintedObjectsWithTelemetry;
import datadog.trace.api.iast.telemetry.Verbosity;
import javax.annotation.Nonnull;

public class IastTelemetryImpl implements IastTelemetry {

  private final Verbosity verbosity;

  public IastTelemetryImpl(final Verbosity verbosity) {
    this.verbosity = verbosity;
  }

  @Override
  public TaintedObjects withTelemetry(@Nonnull final TaintedObjects taintedObjects) {
    return TaintedObjectsWithTelemetry.withTelemetry(verbosity, taintedObjects);
  }
}
