/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution.rest;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TypeSystem;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

@Slf4j
public class JSON implements Engine {
    public static final String ENGINE_NAME = "rest";

    public static final String JAVASCRIPT_ENGINE = "nashorn";

    public static final String FUNCTION_NAME_KEY = "rest-function";
    public static final String METADATA_FUNCTION_NAME_KEY = "function";
    public static final String DEFAULT_FUNCTION_NAME = "process";

    public static final String RETRY_KEY = "rest-retry";
    public static final String METADATA_RETRY_KEY = "retry";
    public static final Integer DEFAULT_RETRIES = 3;

    public static final String TIMEOUT_KEY = "rest-timeout";
    public static final String METADATA_TIMEOUT_KEY = "timeout";
    public static final Integer DEFAULT_TIMEOUT_MS = 60000;

    public static final String VERB_KEY = "method";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String DEFAULT_VERB = GET;

    public static final String URL_KEY = "url";
    public static final String BODY_KEY = "body";
    public static final String EMPTY_BODY = "";

    public static final HashSet<String> KNOWN_KEYS = new HashSet<>(asList(VERB_KEY, URL_KEY, BODY_KEY,
                                                                          METADATA_RETRY_KEY, METADATA_FUNCTION_NAME_KEY,
                                                                          METADATA_TIMEOUT_KEY));

    private int defaultTimeout = DEFAULT_TIMEOUT_MS;
    private int defaultRetries = DEFAULT_RETRIES;
    private String defaultFunction = DEFAULT_FUNCTION_NAME;

    private ScriptEngine evaluator;

    private static final String JSON_TO_MAP_FORMAT = "Java.asJSONCompatible(%s)";

    private final OptionParser parser = new OptionParser() {
        {
            accepts(TIMEOUT_KEY, "The default time to wait for each HTTP request")
                .withRequiredArg()
                .describedAs("REST Query timeout")
                .ofType(Integer.class)
                .defaultsTo(DEFAULT_TIMEOUT_MS);
            accepts(RETRY_KEY, "The default number of times to retry each HTTP request")
                .withRequiredArg()
                .describedAs("REST Query retry limit")
                .ofType(Integer.class)
                .defaultsTo(DEFAULT_RETRIES);
            accepts(FUNCTION_NAME_KEY, "The name of the Javascript function used in all queries")
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
        System.out.println("This REST Engine works by making a HTTP GET or POST, parsing the response (JSON is best)");
        System.out.println("using your provided native JavaScript into a common format.");
        System.out.println("The query part of the engine is a JavaScript function that takes your response from your");
        System.out.println("request and transforms it to a columnar JSON object with the columns as keys and values");
        System.out.println("as arrays of values. You may need to iterate over your output and pull out your columns");
        System.out.println("and return it as a JSON string using JSON stringify. Example: Suppose you extracted");
        System.out.println("columns called 'a' and 'b', you would create and return the following JSON string :");
        System.out.println("{\"a\": [a1, a2, ... an], \"b\": [b1, b2, ... bn]}");
        System.out.println("This engine will inspect these elements and convert them to the proper typed objects.");
        System.out.println("The metadata part of the query contains the required key/value pairs for making the REST");
        System.out.println("call. The url to make the request to can be set using the " + URL_KEY + ". You can use a");
        System.out.println("custom timeout in ms for the call using " + TIMEOUT_KEY + ". The HTTP method can be set");
        System.out.println("using the " + VERB_KEY + " - currently support " + GET + " and " + POST);
        System.out.println("The string body for the " + POST + " can be set using the " + BODY_KEY + ". The number of");
        System.out.println("times to retry can be set using " + RETRY_KEY + ". If you wish to change the name of the");
        System.out.println("Javascript function you are using, use the " + FUNCTION_NAME_KEY + ". Default name is");
        System.out.println(DEFAULT_FUNCTION_NAME + ". Any other key/value pair is added as headers to the REST call,");
        System.out.println("with the key being the header name and the value, its value.");
    }

    @Override
    public void execute(Query query) {
        Map<String, String> metadata = query.getMetadata();
        Objects.requireNonNull(metadata);
        String data = makeRequest(createClient(metadata), createRequest(metadata), query);
        String function = metadata.getOrDefault(METADATA_FUNCTION_NAME_KEY, String.valueOf(defaultFunction));
        String columnarData = convertToColumnarJSON(data, function, query);
        Map<String, List<TypedObject>> typedData = convertToMap(columnarData, query);
        query.createResults().addColumns(typedData);
    }

    /**
     * Converts the String columnar JSON data into a Map of column names to List of column values. Internal use only.
     *
     * @param columnarData The String columnar JSON data.
     * @param query The Query object being run.
     * @return The Map version of the JSON data, null if exception (query is failed).
     */
    Map<String, List<TypedObject>> convertToMap(String columnarData, Query query) {
        try {
            log.info("Converting processed JSON into a map...");
            // Type erasure will make this not enough if the JSON parses into a map but with the wrong keys, values.
            Map<String, List<Object>> result = (Map<String, List<Object>>) evaluator.eval(String.format(JSON_TO_MAP_FORMAT, columnarData));
            // But this will catch it.
            Map<String, List<TypedObject>> typed = type(result);
            log.info("Conversion complete!");
            return typed;
        } catch (ScriptException se) {
            log.error("Could not convert the processed JSON to the required format", se);
            query.setFailure("Invalid JSON (try a linter): " + columnarData);
            query.addMessage(se.toString());
        } catch (ClassCastException cce) {
            log.error("The returned JSON is not in the map of columns format", cce);
            query.setFailure("Your extracted JSON is not in a map of columns format: " + columnarData);
            query.addMessage(cce.toString());
        }
        return null;
    }

    /**
     * Uses the user provided query to process and return a JSON columnar format of the data.
     *
     * @param data The data from the REST call.
     * @param function The function name to invoke.
     * @param query The Query object being run.
     * @return The String JSON response of the call, null if exception (query is failed).
     */
    String convertToColumnarJSON(String data, String function, Query query) {
        try {
            log.info("Evaluating query using Javascript function: {}\n{}", function, query.value);
            evaluator.eval(query.value);
            Invocable invocable = (Invocable) evaluator;
            String columnarJSON = (String) invocable.invokeFunction(function, data);
            log.info("Processed response using query into JSON: {}", columnarJSON);
            return columnarJSON;
        } catch (ScriptException se) {
            log.error("Exception while processing input Javascript", se);
            query.setFailure(se.toString());
            query.addMessage(se.toString());
        } catch (NoSuchMethodException nsme) {
            log.error("Method {} not found in {}\n{}", function, query.value, nsme);
            query.setFailure(nsme.toString());
        }
        return null;
    }

    /**
     * Makes the request and returns the String response using the given client, request and query.
     *
     * @param client The HttpClient to use.
     * @param request The HttpUriRequest to make.
     * @param query The Query object being run.
     * @return The String response of the call, null if exception (query is failed).
     */
    String makeRequest(HttpClient client, HttpUriRequest request, Query query) {
        try {
            log.info("{}ing to {} with headers {}", request.getMethod(), request.getURI(), request.getAllHeaders());
            HttpResponse response = client.execute(request);
            StatusLine line = response.getStatusLine();
            log.info("Received {}: {} with headers {}", line.getStatusCode(), line.getReasonPhrase(), response.getAllHeaders());
            String data = EntityUtils.toString(response.getEntity());
            log.info("Received response as string {}", data);
            return data;
        } catch (IOException ioe) {
            log.error("Could not execute request", ioe);
            query.setFailure("Could not execute request");
            query.addMessage(ioe.toString());
        } catch (NullPointerException npe) {
            log.error("Received no response", npe);
            query.setFailure("Received no response");
            query.addMessage(npe.toString());
        }
        return null;
    }

    private static Map<String, List<TypedObject>> type(Map<String, List<Object>> untyped) {
        Map<String, List<TypedObject>> typedData = new HashMap<>();
        if (untyped == null) {
            return typedData;
        }
        for (Map.Entry<String, List<Object>> e : untyped.entrySet()) {
            String column = e.getKey();
            log.info("Column: {}", column);
            List<TypedObject> typedValues = new ArrayList<>();
            typedData.put(column, typedValues);
            List<Object> values = e.getValue();
            if (values != null) {
                values.stream().map(JSON::type).forEach(typedValues::add);
            }
        }
        return typedData;
    }

    private static TypedObject type(Object object) {
        if (object == null) {
            log.info("Value: null");
            return null;
        }
        TypedObject typed;
        if (object instanceof String) {
            typed =  new TypedObject((String) object, TypeSystem.Type.STRING);
        } else if (object instanceof Integer) {
            typed =  new TypedObject(((Integer) object).longValue(), TypeSystem.Type.LONG);
        } else if (object instanceof Number) {
            // Can't recognize Longs and Doubles since that seems to change per JDK. Safer to use Number and use DOUBLE
            typed =  new TypedObject(((Number) object).doubleValue(), TypeSystem.Type.DOUBLE);
        } else if (object instanceof Boolean) {
            typed =  new TypedObject((Boolean) object, TypeSystem.Type.BOOLEAN);
        } else {
            // We can support custom formats for BigDecimals, Timestamps etc as JS objects if need be.
            log.info("Object {} has an unsupported type {}. Nulling...", object, object.getClass().getCanonicalName());
            return null;
        }
        log.info("Value: {}\tType: {}", typed.data, typed.type);
        return typed;
    }

    /**
     * Creates a HttpClient to use for making requests.
     *
     * @param metadata The map containing the configuration for this client.
     * @return The created HttpClient object.
     */
    private HttpClient createClient(Map<String, String> metadata) {
        int timeout = Integer.valueOf(metadata.getOrDefault(METADATA_TIMEOUT_KEY, String.valueOf(defaultTimeout)));
        int retries = Integer.valueOf(metadata.getOrDefault(METADATA_RETRY_KEY, String.valueOf(defaultRetries)));
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
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("The " + URL_KEY + " must be provided and contain a valid url.");
        }
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
        metadata.entrySet().stream().filter(entry -> !KNOWN_KEYS.contains(entry.getKey()))
                                    .forEach(entry -> builder.addHeader(entry.getKey(), entry.getValue()));
        return builder.build();
    }
}
