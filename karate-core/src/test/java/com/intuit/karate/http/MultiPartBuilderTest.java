package com.intuit.karate.http;

import com.intuit.karate.FileUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
class MultiPartBuilderTest {

    static final Logger logger = LoggerFactory.getLogger(MultiPartBuilderTest.class);

    String join(String... lines) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = Arrays.asList(lines).iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next()).append('\r').append('\n');
        }
        return sb.toString();
    }

    @Test
    void testMultiPart() {
        MultiPartBuilder builder = new MultiPartBuilder(true, null);
        builder.part("bar", "hello world");
        byte[] bytes = builder.build();
        String boundary = builder.getBoundary();
        String actual = FileUtils.toString(bytes);
        String expected = join(
                "--" + boundary,
                "content-disposition: form-data; name=\"bar\"",
                "content-length: 11",
                "content-type: text/plain",
                "",
                "hello world",
                "--" + boundary + "--"
        );
        assertEquals(expected, actual);
    }

    @Test
    void testUrlEncoded() {
        MultiPartBuilder builder = new MultiPartBuilder(false, null);
        builder.part("bar", "hello world");
        byte[] bytes = builder.build();
        assertEquals("application/x-www-form-urlencoded", builder.getContentTypeHeader());
        String actual = FileUtils.toString(bytes);
        assertEquals("bar=hello+world", actual);
    }

	@Test
	void testList() {
		MultiPartBuilder builder = new MultiPartBuilder(true, null);
		Map<String, Object> input = new HashMap<String, Object>();
		input.put("name", "bar");
		input.put("value", Arrays.asList("one", "two", "three"));
		builder.part(input);
		byte[] bytes = builder.build();
		String boundary = builder.getBoundary();
		String actual = FileUtils.toString(bytes);
		String expected = join(
			"--" + boundary,
			"content-disposition: form-data; name=\"bar\"",
			"content-length: 3",
			"content-type: text/plain",
			"",
			"one",
			"--" + boundary,
			"content-disposition: form-data; name=\"bar\"",
			"content-length: 3",
			"content-type: text/plain",
			"",
			"two",
			"--" + boundary,
			"content-disposition: form-data; name=\"bar\"",
			"content-length: 5",
			"content-type: text/plain",
			"",
			"three",
			"--" + boundary + "--"
		);
		assertEquals("multipart/form-data; boundary=" + boundary, builder.getContentTypeHeader());
		assertEquals(expected, actual);
	}

	@Test
	void testNullValue() {
		MultiPartBuilder builder = new MultiPartBuilder(true, null);
		builder.part("name", null);
		byte[] bytes = builder.build();
        String actual = FileUtils.toString(bytes);
		String boundary = builder.getBoundary();
		String expected = join(
			"--" + boundary,
			"content-disposition: form-data; name=\"name\"",
			"content-length: 0",
			"content-type: application/octet-stream",
			"--" + boundary + "--"
		);
		assertEquals(expected, actual);
	}
} 