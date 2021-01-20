package org.opengroup.osdu.entitlements.v2.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class JwtClaimExtractorTest {

    @Test(expected = AppException.class)
    public void should_throwAppException_when_nullJwt() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        sut.extract(null);
    }

    @Test(expected = AppException.class)
    public void should_throwAppException_when_noBearerInJwt() {
        JwtClaimExtractor sut = new JwtClaimExtractor();
        sut.extract("ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICA.fefwewfwvfwef");
    }

    @Test(expected = AppException.class)
    public void should_throwAppException_when_corruptedInJwt() {
        JwtClaimExtractor sut = new JwtClaimExtractor();
        sut.extract("Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICA.fefwewfwvfwef");
    }

    @Test
    public void should_parseClaims_when_otherTypeOfToken() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        JwtClaims result = sut.extract("Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmNvbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImNsaWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6IjogIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiAiIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGFjdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUgDVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA");

        assertThat(result.getAppId()).isEqualTo("test.com");
        assertThat(result.getUserId()).isEqualTo("a@b.com");
    }

    @Test
    public void should_parseClaims_when_AADRegularUserToken() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        JwtClaims result = sut.extract("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC94eHgteHh4LyIsImlhdCI6MTYwNTA0NDgyNywiZXhwIjoxNjM2NTgwODI3LCJhdWQiOiJ4eHh4LXh4eHgiLCJzdWIiOiJhQGIuY29tIiwidXBuIjoiYUBiLmNvbSJ9.MOyfIqF6JRttyPQ4kI_0lrgXQAM7UKwI4wvVcXfzxfE");

        assertThat(result.getAppId()).isEqualTo("xxxx-xxxx");
        assertThat(result.getUserId()).isEqualTo("a@b.com");
    }

    @Test
    public void should_parseClaims_when_AADGuestUserToken() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        JwtClaims result = sut.extract("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC94eHgteHh4LyIsImlhdCI6MTYwNTA0NDgyNywiZXhwIjoxNjM2NTgwODI3LCJhdWQiOiJ4eHh4LXh4eHgiLCJzdWIiOiJhQGIuY29tIiwidW5pcXVlX25hbWUiOiJhQGIuY29tIn0.Sl5fTp7M8KOkBJC-_OAeVh1lu-qLKe6m38QdsHRCEik");

        assertThat(result.getAppId()).isEqualTo("xxxx-xxxx");
        assertThat(result.getUserId()).isEqualTo("a@b.com");
    }

    @Test
    public void should_parseClaims_when_AADServicePrincipalToken() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        JwtClaims result = sut.extract("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC94eHgteHh4LyIsImlhdCI6MTYwNTA0NDgyNywiZXhwIjoxNjM2NTgwODI3LCJhdWQiOiJ4eHh4LXh4eHgiLCJzdWIiOiJzZXJ2aWNlLXByaW5jaXBhbC1pZCIsImFwcGlkIjoic2VydmljZS1wcmluY2lwYWwtaWQifQ.TXw5jCBUumQLPVsY2D23XvFxA1w08zdFRVuUBKia7pQ");

        assertThat(result.getAppId()).isEqualTo("xxxx-xxxx");
        assertThat(result.getUserId()).isEqualTo("service-principal-id");
    }

    @Test(expected = AppException.class)
    public void should_throwAppException_given_corruptedAADToken() {
        JwtClaimExtractor sut = new JwtClaimExtractor();

        sut.extract("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC94eHgteHh4LyIsImlhdCI6MTYwNTA0NDgyNywiZXhwIjoxNjM2NTgwODI3LCJhdWQiOiJ4eHh4LXh4eHgiLCJzdWIiOiJzZXJ2aWNlLXByaW5jaXBhbC1pZCJ9.iNC6zV10UBq4QrrSmozJ3OeSRvQBsqxnzPoc-ARYJaQ");

    }
}
