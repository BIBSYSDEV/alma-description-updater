package no.unit.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class HttpUtilsTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandle200OkHttpResponseCorrectly() {
        var httpResponse = mock(HttpResponse.class);
        doReturn(200).when(httpResponse).statusCode();

        assertThat(HttpUtils.isSuccessful(httpResponse), equalTo(true));
        assertThat(HttpUtils.nonSuccessful(httpResponse), equalTo(false));
    }

    @Test
    void shouldHandleNullHttpResponseCorrectly() {
        assertThat(HttpUtils.isSuccessful(null), equalTo(false));
        assertThat(HttpUtils.nonSuccessful(null), equalTo(true));
    }

}
