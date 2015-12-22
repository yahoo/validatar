package com.yahoo.validatar.execution.rest;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.singletonList;

@Slf4j
public class JSON implements Engine {
    public static final String ENGINE_NAME = "rest";

    public static final String JAVASCRIPT_ENGINE = "nashorn";

    public static final String RETRY_KEY = "retry";
    public static final Integer DEFAULT_RETRIES = 3;

    public static final String TIMEOUT_KEY = "rest-timeout";
    public static final Integer DEFAULT_TIMEOUT_MS = 60000;

    public static final String VERB_KEY = "method";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String DEFAULT_VERB = GET;

    public static final String URL_KEY = "url";
    public static final String BODY_KEY = "body";
    public static final String EMPTY_BODY = "";

    public static final HashSet<String> KNOWN_KEYS = new HashSet<>(Arrays.asList(RETRY_KEY, VERB_KEY, URL_KEY, BODY_KEY));

    private int defaultTimeout = DEFAULT_TIMEOUT_MS;
    private int defaultRetries = DEFAULT_RETRIES;

    private ScriptEngine evaluator;

    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(singletonList(TIMEOUT_KEY), "The default time to wait for each HTTP request")
                .withRequiredArg()
                .describedAs("REST Query timeout")
                .ofType(Integer.class)
                .defaultsTo(DEFAULT_TIMEOUT_MS);
            acceptsAll(singletonList(RETRY_KEY), "The default number of times to retry each HTTP request")
             .withRequiredArg()
             .describedAs("REST Query retry limit")
             .ofType(Integer.class)
             .defaultsTo(DEFAULT_RETRIES);
            allowsUnrecognizedOptions();
        }
    };

    @Override
    public boolean setup(String[] arguments) {
        evaluator = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
        OptionSet options = parser.parse(arguments);
        defaultTimeout = (Integer) options.valueOf(TIMEOUT_KEY);
        defaultRetries = (Integer) options.valueOf(RETRY_KEY);
        return true;
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("REST Engine options", parser);
    }

    @Override
    public void execute(Query query) {
        Map<String, String> metadata = query.getMetadata();
        if (metadata == null) {
            throw new NullPointerException("This query does not have at least a url to make the request to");
        }

        HttpClient client = createClient(metadata);
        HttpUriRequest request = createRequest(metadata);
        try {
            HttpResponse response = client.execute(request);
        } catch (IOException ioe) {
            log.error("Could not execute request with params {}", metadata.toString());
        }
    }

    /**
     * Creates a HttpClient to use for making requests.
     *
     * @param metadata The map containing the configuration for this client.
     * @return The created HttpClient object.
     */
    HttpClient createClient(Map<String, String> metadata) {
        int timeout = Integer.valueOf(metadata.getOrDefault(TIMEOUT_KEY, String.valueOf(defaultTimeout)));
        int retries = Integer.valueOf(metadata.getOrDefault(RETRY_KEY, String.valueOf(defaultRetries)));
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout)
                                                     .setConnectionRequestTimeout(timeout)
                                                     .setSocketTimeout(timeout).build();
        return HttpClientBuilder.create()
                                .setDefaultRequestConfig(config)
                                .setRetryHandler(new DefaultHttpRequestRetryHandler(retries, false))
                                .build();
    }

    /**
     * Creates a HttpUriRequest based on the metadata configuration.
     * @param metadata The metadata configuration.
     * @return A configured request object.
     */
    HttpUriRequest createRequest(Map<String, String> metadata) {
        String verb = metadata.getOrDefault(VERB_KEY, DEFAULT_VERB);
        String url = metadata.get(URL_KEY);
        RequestBuilder builder;
        if (GET.equals(verb)) {
            builder = RequestBuilder.get(url);
        } else if (POST.equals(verb)) {
            builder = RequestBuilder.post(url);
            String body = metadata.getOrDefault(BODY_KEY, EMPTY_BODY);
            builder.setEntity(new StringEntity(body, Charset.defaultCharset()));
        } else {
            throw new UnsupportedOperationException("This HTTP method is not currently supported: " + verb);
        }
        metadata.entrySet().stream().filter(entry -> KNOWN_KEYS.contains(entry.getKey()))
                                    .forEach(entry -> builder.addHeader(entry.getKey(), entry.getValue()));
        return builder.build();
    }
}
