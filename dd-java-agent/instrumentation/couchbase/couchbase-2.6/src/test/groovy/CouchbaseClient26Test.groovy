import datadog.trace.agent.test.asserts.TraceAssert
import datadog.trace.test.util.Flaky
import spock.lang.Retry

@Flaky
@Retry
class CouchbaseClient26Test extends CouchbaseClientTest {
  @Override
  void assertCouchbaseCall(TraceAssert trace, String name, String bucketName = null, Object parentSpan = null) {
    CouchbaseSpanUtil.assertCouchbaseCall(trace, name, bucketName, parentSpan)
  }
}
