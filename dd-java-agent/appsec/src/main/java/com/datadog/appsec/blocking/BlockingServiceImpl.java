package com.datadog.appsec.blocking;

import com.datadog.appsec.event.EventProducerService;
import com.datadog.appsec.event.ExpiredSubscriberInfoException;
import com.datadog.appsec.event.data.KnownAddresses;
import com.datadog.appsec.event.data.SingletonDataBundle;
import com.datadog.appsec.gateway.AppSecRequestContext;
import datadog.appsec.api.blocking.BlockingContentType;
import datadog.appsec.api.blocking.BlockingDetails;
import datadog.appsec.api.blocking.BlockingService;
import datadog.trace.api.gateway.BlockResponseFunction;
import datadog.trace.api.gateway.Flow;
import datadog.trace.api.gateway.RequestContext;
import datadog.trace.api.gateway.RequestContextSlot;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockingServiceImpl implements BlockingService {
  private static final Logger log = LoggerFactory.getLogger(BlockingServiceImpl.class);

  private final EventProducerService eventProducer;
  private volatile EventProducerService.DataSubscriberInfo subInfo;

  public BlockingServiceImpl(EventProducerService eventProducer) {
    this.eventProducer = eventProducer;
  }

  @Override
  public BlockingDetails shouldBlockUser(@Nonnull String userId) {
    AppSecRequestContext reqCtx = getAppSecRequestContext();
    if (reqCtx == null) {
      return null;
    }

    Flow<Void> flow;
    while (true) {
      if (subInfo == null) {
        subInfo = eventProducer.getDataSubscribers(KnownAddresses.USER_ID);
      }
      SingletonDataBundle<String> db = new SingletonDataBundle<>(KnownAddresses.USER_ID, userId);
      try {
        flow = eventProducer.publishDataEvent(subInfo, reqCtx, db, true);
        break;
      } catch (ExpiredSubscriberInfoException e) {
        subInfo = null;
      }
    }

    Flow.Action action = flow.getAction();
    if (action instanceof Flow.Action.RequestBlockingAction) {
      Flow.Action.RequestBlockingAction rba = (Flow.Action.RequestBlockingAction) action;
      return new BlockingDetails(rba.getStatusCode(), rba.getBlockingContentType());
    }
    return null;
  }

  @Override
  public boolean tryCommitBlockingResponse(int statusCode, BlockingContentType templateType) {
    RequestContext reqCtx = getRequestContext();
    if (reqCtx == null) {
      return false;
    }

    BlockResponseFunction blockResponseFunction = reqCtx.getBlockResponseFunction();
    if (blockResponseFunction == null) {
      log.warn("Do not know how to commit blocking response for this server");
      return false;
    }
    return blockResponseFunction.tryCommitBlockingResponse(statusCode, templateType);
  }

  private static RequestContext getRequestContext() {
    AgentSpan agentSpan = AgentTracer.get().activeSpan();
    if (agentSpan == null) {
      log.warn("Cannot block (no active span)");
      return null;
    }

    return agentSpan.getRequestContext();
  }

  private AppSecRequestContext getAppSecRequestContext() {
    AppSecRequestContext ctx = null;
    RequestContext reqCtx = getRequestContext();
    if (reqCtx != null) {
      ctx = reqCtx.getData(RequestContextSlot.APPSEC);
    }

    if (ctx == null) {
      log.warn("No AppSec request context. Not an http request or AppSec inactive?");
      return null;
    }
    return ctx;
  }
}
