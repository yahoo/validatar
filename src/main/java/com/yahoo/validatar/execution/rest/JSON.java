package com.yahoo.validatar.execution.rest;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Slf4j
public class JSON implements Engine {
    public static final String ENGINE_NAME = "rest";

    public static final String JAVASCRIPT_ENGINE = "nashorn";

    public static final String FUNCTION_NAME_KEY = "rest-function";
    public static final String DEFAULT_FUNCTION_NAME = "process";

    public static final String RETRY_KEY = "rest-retry";
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

    public static final HashSet<String> KNOWN_KEYS = new HashSet<>(asList(RETRY_KEY, VERB_KEY, URL_KEY,
                                                                          BODY_KEY, FUNCTION_NAME_KEY));

    private int defaultTimeout = DEFAULT_TIMEOUT_MS;
    private int defaultRetries = DEFAULT_RETRIES;
    private String defaultFunction = DEFAULT_FUNCTION_NAME;

    private ScriptEngine evaluator;

    private final OptionParser parser = new OptionParser() {
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
            acceptsAll(singletonList(FUNCTION_NAME_KEY), "The name of the Javascript function used in all queries")
             .withRequiredArg()
             .describedAs("REST Javascript method name")
             .defaultsTo(DEFAULT_FUNCTION_NAME);
            allowsUnrecognizedOptions();
        }
    };

    @Override
    public boolean setup(String[] arguments) {
        evaluator = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
        OptionSet options = parser.parse(arguments);
        defaultTimeout = (Integer) options.valueOf(TIMEOUT_KEY);
        defaultRetries = (Integer) options.valueOf(RETRY_KEY);
        defaultFunction = (String) options.valueOf(FUNCTION_NAME_KEY);
        return true;
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("REST Engine options", parser);
        System.out.println("This REST Engine works with JSON input (if any) and JSON output.");
        System.out.println("The query part of the engine is a JavaScript function that takes your response from your");
        System.out.println("request and transforms it columnnar. Use Javascript to iterate over your output and pull");
        System.out.println("out your columns and store it as a JSON object. For example, suppose you want to extract");
        System.out.println("columns called 'a' and 'b', you would create and return the following JSON string :");
        System.out.println("{\"a\": [a1, a2, ... an], \"b\": [b1, b2, ... bn]}");
        System.out.println("This engine will inspect these elements and convert them to the proper typed objects.");
        System.out.println("The metadata part of the query contains the required key/value pairs for making the REST");
        System.out.println("call. The url to make the request to can be set using the " + URL_KEY + ". You can add a");
        System.out.println("timeout in ms for the REST call using " + TIMEOUT_KEY + ". The HTTP method to use can be");
        System.out.println("set using the " + VERB_KEY + " - currently supports " + GET + " and " + POST + " methods");
        System.out.println("The String body for the " + POST + " can be set using the " + BODY_KEY + ". The number of");
        System.out.println("times to retry can be set using " + RETRY_KEY + ". If you wish to change the name of the");
        System.out.println("Javascript function you are using, use the " + FUNCTION_NAME_KEY + ". Default name is");
        System.out.println(DEFAULT_FUNCTION_NAME + ". Any other key/value pair is added as headers to the REST call.");
    }

    @Override
    public void execute(Query query) {
        Map<String, String> metadata = query.getMetadata();
        validate(metadata);
        String data = makeRequest(createClient(metadata), createRequest(metadata), query);
        log.info("Received response as string {}", data);
        String function = metadata.getOrDefault(FUNCTION_NAME_KEY, String.valueOf(defaultFunction));
        String columnarData = convertToColumnarJSON(data, function, query);
        log.info("Processed response using query into JSON: {}", columnarData);
        Map<String, List<TypedObject>> typedData = convertToTypedObject(columnarData);
        log.info("Processed JSON into {}", typedData);
    }

    Map<String, List<TypedObject>> convertToTypedObject(String columnarData) {
        return null;
    }

    String convertToColumnarJSON(String data, String function, Query query) {
        try {
            log.info("Evaluating query as Javascript {}", query.value);
            evaluator.eval(query.value);
            Invocable invocable = (Invocable) evaluator;
            String json = (String) invocable.invokeFunction(function, data);
        } catch (ScriptException se) {
            log.error("Exception while processing input Javascript", se);
            query.setFailure(se.toString());
        } catch (NoSuchMethodException nsme) {
            log.error("Method {} not found in {}\n{}", function, query.value, nsme);
            query.setFailure(nsme.toString());
        }
        return null;
    }

    String makeRequest(HttpClient client, HttpUriRequest request, Query query) {
        try {
            log.info("{}ing {} with headers \n{}", request.getMethod(), request.getURI(), request.getAllHeaders());
            HttpResponse response = client.execute(request);
            StatusLine line = response.getStatusLine();
            log.info("Received {}: {} \n{}", line.getStatusCode(), line.getReasonPhrase(), response.getAllHeaders());
            return EntityUtils.toString(response.getEntity());
        } catch (IOException ioe) {
            log.error("Could not execute request", ioe);
            query.setFailure(ioe.toString());
        } catch (NullPointerException npe) {
            log.error("Received no response", npe);
            query.setFailure(npe.toString());
        }
        return null;
    }

    /**
     * Creates a HttpClient to use for making requests.
     *
     * @param metadata The map containing the configuration for this client.
     * @return The created HttpClient object.
     */
    private HttpClient createClient(Map<String, String> metadata) {
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
    private HttpUriRequest createRequest(Map<String, String> metadata) {
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
        // Everything else is assumed to be a header
        metadata.entrySet().stream().filter(entry -> KNOWN_KEYS.contains(entry.getKey()))
                                    .forEach(entry -> builder.addHeader(entry.getKey(), entry.getValue()));
        return builder.build();
    }

    private void validate(Map<String, String> metadata) {
        Objects.requireNonNull(metadata);
        String url = metadata.get(URL_KEY);
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("The " + URL_KEY + " must be provided and contain a valid url.");
        }
    }
}
