package com.intuit.karate.http;

import java.util.*;
import com.intuit.karate.core.Config;
import com.intuit.karate.core.ScenarioEngine;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
class HttpRequestBuilderTest {

    static final Logger logger = LoggerFactory.getLogger(HttpRequestBuilderTest.class);

    HttpRequestBuilder http;

    @BeforeEach
    void beforeEach() {
        ScenarioEngine se = ScenarioEngine.forTempUse(HttpClientFactory.DEFAULT);
        http = new HttpRequestBuilder(HttpClientFactory.DEFAULT.create(se));
    }

    @Test
    void testUrlAndPath() {
        http.url("http://host/foo");
        assertEquals("http://host/foo", http.getUri());
        http.path("/bar");
        assertEquals("http://host/foo/bar", http.getUri());
    }

    @Test
    void testUrlAndPathWithSlash() {
        http.url("http://host/foo/");
        assertEquals("http://host/foo/", http.getUri());
        http.path("/bar/");
        assertEquals("http://host/foo/bar", http.getUri());
    }
    
    @Test
    void testUrlAndPathWithTrailingSlash() {
        http.url("http://host/foo");
        assertEquals("http://host/foo", http.getUri());
        http.path("bar");
        http.path("/");
        assertEquals("http://host/foo/bar/", http.getUri());
    }    
    
    @Test
    void testUrlAndPathWithEncodedSlash() {
        http.url("http://host");
        assertEquals("http://host", http.getUri());
        http.path("foo\\/bar");
        assertEquals("http://host/foo%2Fbar", http.getUri());
    }     

    @Test
    void panicWithEmptyUrl() {
        assertThrows(
            RuntimeException.class,
            () -> http.build()
        );
    }

    @Test
    void fallbackOnClientURLWhenEmptyURL() {
        ScenarioEngine se = ScenarioEngine.forTempUse(HttpClientFactory.DEFAULT);
        var client = HttpClientFactory.DEFAULT.create(se);

        Config config = client.getConfig();
        config.setUrl("http://test");
        client.setConfig(config);

        http = new HttpRequestBuilder(client);
        http.build();

        assertEquals("http://test", http.getUri());
    }

    @Test
    void usesPOSTOnMultipartBody() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "field");
        map.put("value", "value");

        http.url("http://test");
        http.multiPart(map);

        HttpRequest request = http.build();

        assertEquals("POST", request.getMethod());
    }

    @Test
    void buildsBoundaryOnUserContentType() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "field");
        map.put("value", "value");

        http.url("http://test");
        http.multiPart(map);
        http.contentType("custom/type");

        HttpRequest request = http.build();

        // The actual value of the boundary field seems to be run dependent.
        // Since it is a web parameter, I would guess it depends on the
        // current time. We are not testing its value here, but rather
        // the behavior of buildInternal that creates this.
        assert(request.getContentType().contains("custom/type; boundary=")); 
    }
}
