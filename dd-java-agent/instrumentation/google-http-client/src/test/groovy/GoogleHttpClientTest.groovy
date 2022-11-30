import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpResponse
import datadog.trace.test.util.Flaky
import spock.lang.Retry
import spock.lang.Timeout

@Flaky
@Retry(condition = {
  !invocation.method.name.contains('circular redirects')
})
@Timeout(5)
class GoogleHttpClientTest extends AbstractGoogleHttpClientTest {

  @Override
  HttpResponse executeRequest(HttpRequest request) {
    return request.execute()
  }
}
