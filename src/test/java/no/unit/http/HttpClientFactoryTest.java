package no.unit.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class HttpClientFactoryTest {

    @Test
    @SuppressWarnings("resource")
    void shouldOnlyCreateOneInstanceFromEnumSingleton() {
        HttpClientFactory singletonFactoryOne = new SingularHttpClientFactory();
        HttpClientFactory singletonFactoryTwo = new SingularHttpClientFactory();
        HttpClientFactory singletonFactoryThree = new SingularHttpClientFactory();

        var clientOne = singletonFactoryOne.create();
        var clientTwo = singletonFactoryTwo.create();
        var clientThree = singletonFactoryThree.create();

        assertEquals(clientOne.hashCode(), clientTwo.hashCode());
        assertEquals(clientOne.hashCode(), clientThree.hashCode());
        assertEquals(clientTwo.hashCode(), clientThree.hashCode());
    }

    @Test
    @SuppressWarnings("resource")
    void shouldCreateSeparateInstancesWhenUsingDefaultFactory() {
        HttpClientFactory singletonFactoryOne = new DefaultHttpClientFactory();
        HttpClientFactory singletonFactoryTwo = new DefaultHttpClientFactory();
        HttpClientFactory singletonFactoryThree = new DefaultHttpClientFactory();

        var clientOne = singletonFactoryOne.create();
        var clientTwo = singletonFactoryTwo.create();
        var clientThree = singletonFactoryThree.create();

        assertNotEquals(clientOne.hashCode(), clientTwo.hashCode());
        assertNotEquals(clientOne.hashCode(), clientThree.hashCode());
        assertNotEquals(clientTwo.hashCode(), clientThree.hashCode());
    }

}
