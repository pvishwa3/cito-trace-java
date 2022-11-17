import datadog.trace.agent.test.utils.TraceUtils
import datadog.trace.bootstrap.instrumentation.api.AgentTracer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http.HttpMethod

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import static java.util.concurrent.TimeUnit.SECONDS

class OkHttp3AsyncTest extends OkHttp3Test {
  @Override
  int doRequest(String method, URI uri, Map<String, String> headers, String body, Closure callback) {
    def reqBody = HttpMethod.requiresRequestBody(method) ? RequestBody.create(MediaType.parse("text/plain"), body) : null
    def request = new Request.Builder()
      .url(uri.toURL())
      .method(method, reqBody)
      .headers(Headers.of(headers))
      .build()

    AtomicReference<Response> responseRef = new AtomicReference()
    AtomicReference<Exception> exRef = new AtomicReference()
    def latch = new CountDownLatch(1)

    client.newCall(request).enqueue(new Callback() {
        void onResponse(Call call, Response response) {
          responseRef.set(response)
          callback?.call()
          latch.countDown()
        }

        void onFailure(Call call, IOException e) {
          exRef.set(e)
          callback?.call()
          latch.countDown()
        }
      })
    latch.await(10, SECONDS)
    if (exRef.get() != null) {
      throw exRef.get()
    }
    return responseRef.get().code()
  }

  def "callbacks should carry context" () {

    when:
    def capturedSpanName = new AtomicReference<>("missing")
    try {
      TraceUtils.runUnderTrace("parent", {
        doRequest(method, url, ["Datadog-Meta-Lang": "java"], "", {
          capturedSpanName.set(AgentTracer.activeSpan().getOperationName().toString())
        })
      })
    } catch (Exception e) {
      assert error == true
    }

    then:
    "parent" == capturedSpanName.get()

    where:
    url                                 | error
    server.address.resolve("/success")  | false
    new URI("http://240.0.0.1")         | true

    method = "GET"
  }
}
