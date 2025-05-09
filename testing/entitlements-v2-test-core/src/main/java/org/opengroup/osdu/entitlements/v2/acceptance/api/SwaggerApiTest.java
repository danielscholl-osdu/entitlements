package org.opengroup.osdu.entitlements.v2.acceptance.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.AcceptanceBaseTest;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

import static org.junit.Assert.assertEquals;

@Slf4j
public abstract class SwaggerApiTest extends AcceptanceBaseTest {

  private static final String HTTP_GET = "GET";
  protected static final String SWAGGER_API_PATH = "swagger";
  protected static final String SWAGGER_API_DOCS_PATH = "api-docs";

  protected SwaggerApiTest(ConfigurationService configurationService, TokenService tokenService) {
    super(configurationService, tokenService);
  }

  @Test
  public void shouldReturn200_whenSwaggerApiIsCalled() throws Exception {
    RequestData request =
            RequestData.builder()
                    .relativePath(SWAGGER_API_PATH)
                    .method(HTTP_GET)
                    .body(null)
                    .token(tokenService.getToken().getValue())
                    .build();

    CloseableHttpResponse response = httpClientService.send(request);

    assertEquals(HttpStatus.SC_OK, response.getCode());
  }

  @Test
  public void shouldReturn200_whenSwaggerApiDocsIsCalled() throws Exception {
    RequestData request =
            RequestData.builder()
                    .relativePath(SWAGGER_API_DOCS_PATH)
                    .method(HTTP_GET)
                    .body(null)
                    .token(tokenService.getToken().getValue())
                    .build();

    CloseableHttpResponse response = httpClientService.send(request);
    
    assertEquals(HttpStatus.SC_OK, response.getCode());
  }

  @Override
  public void shouldReturn401WhenMakingHttpRequestWithoutToken() {
    // not actual for this case
  }

  @Override
  protected RequestData getRequestDataForNoTokenTest() {
    return null;
  }
}
