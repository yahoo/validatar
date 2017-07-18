/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;
import static com.yahoo.validatar.TestHelpers.getQueryFrom;
import static org.mockito.Mockito.mock;

public class JSONTest {
    private JSON json;

    public static final int WIRE_MOCK_PORT = 13412;

    private WireMockServer startWireMockServer(int port) {
        WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
        server.start();
        WireMock.configureFor(port);
        return server;
    }

    @BeforeMethod
    public void setup() {
        json = new JSON();
        json.setup(new String[0]);
    }

    @Test
    public void testDefaults() {
        Assert.assertTrue(json.setup(new String[0]));
        Assert.assertEquals(json.getName(), json.ENGINE_NAME);
        runWithoutOutput(json::printHelp);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullMetadata() {
        Query query = new Query();
        runWithoutOutput(() -> json.execute(query));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*url must be provided.*")
    public void testNullURL() {
        Query query = new Query();
        query.metadata = new ArrayList<>();
        runWithoutOutput(() -> json.execute(query));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*url must be provided.*")
    public void testEmptyURL() {
        Query query = new Query();
        Metadata meta = new Metadata();
        meta.key = JSON.URL_KEY;
        meta.value = "";
        query.metadata = Arrays.asList(meta);
        runWithoutOutput(() -> json.execute(query));
    }

    @Test
    public void testJSONMapConversionNull() {
        Query query = new Query();
        Map<String, List<TypedObject>> actual = json.convertToMap(null, query);
        Assert.assertFalse(query.failed());
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void testJSONMapConversionEmpty() {
        Query query = new Query();
        Map<String, List<TypedObject>> actual = json.convertToMap("", query);
        Assert.assertFalse(query.failed());
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void testJSONMapTypeConversions() {
        Query query = new Query();
        String jsonData = "{'users' : ['user1', 'user2'], 'count': [10], 'ratio': [0.14], 'numbers': [123123123123, 1.23143]," +
                          " 'booleans': [true, false], 'mixed': [[1, 2], {'a': 1}, null, 2]}";
        Map<String, List<TypedObject>> actual = json.convertToMap(jsonData, query);

        Assert.assertEquals(actual.size(), 6);

        List<TypedObject> users = actual.get("users");
        Assert.assertEquals(users.get(0).type, TypeSystem.Type.STRING);
        Assert.assertEquals(users.get(0).data, "user1");
        Assert.assertEquals(users.get(1).type, TypeSystem.Type.STRING);
        Assert.assertEquals(users.get(1).data, "user2");

        // Internally should be an Integer when converting
        List<TypedObject> counts = actual.get("count");
        Assert.assertEquals(counts.get(0).type, TypeSystem.Type.LONG);
        Assert.assertEquals(counts.get(0).data, 10L);

        List<TypedObject> ratios = actual.get("ratio");
        Assert.assertEquals(ratios.get(0).type, TypeSystem.Type.DOUBLE);
        Assert.assertEquals(ratios.get(0).data, 0.14);

        List<TypedObject> longs = actual.get("numbers");
        Assert.assertEquals(longs.get(0).type, TypeSystem.Type.DOUBLE);
        Assert.assertEquals(longs.get(0).data, 123123123123.0);
        Assert.assertEquals(longs.get(1).type, TypeSystem.Type.DOUBLE);
        Assert.assertEquals(longs.get(1).data, 1.23143);

        List<TypedObject> booleans = actual.get("booleans");
        Assert.assertEquals(booleans.get(0).type, TypeSystem.Type.BOOLEAN);
        Assert.assertEquals(booleans.get(0).data, true);
        Assert.assertEquals(booleans.get(1).type, TypeSystem.Type.BOOLEAN);
        Assert.assertEquals(booleans.get(1).data, false);

        List<TypedObject> mixeds = actual.get("mixed");
        Assert.assertNull(mixeds.get(0));
        Assert.assertNull(mixeds.get(1));
        Assert.assertNull(mixeds.get(2));
        Assert.assertEquals(mixeds.get(3).type, TypeSystem.Type.LONG);
        Assert.assertEquals(mixeds.get(3).data, 2L);
    }

    @Test
    public void testJSONMapConversionBadJSON() {
        Query query = new Query();
        String jsonData = "{'a' : ";
        Map<String, List<TypedObject>> actual = json.convertToMap(jsonData, query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("Invalid JSON")));
    }

    @Test
    public void testJSONMapConversionWrongFormat() {
        Query query;
        String jsonData;
        Map<String, List<TypedObject>> actual;

        query = new Query();
        jsonData = "'a'";
        actual = json.convertToMap(jsonData, query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("JSON is not in a map of columns")));

        query = new Query();
        jsonData = "[1, 2, 3]";
        actual = json.convertToMap(jsonData, query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("JSON is not in a map of columns")));

        query = new Query();
        jsonData = "{'a': 1}";
        actual = json.convertToMap(jsonData, query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("JSON is not in a map of columns")));
    }

    @Test
    public void testJSONExtraction() {
        String function = "function test(input) { " +
                          "   var parsed = JSON.parse(input); " +
                          "   var result = {'users': []}; " +
                          "   for (var k in parsed) { " +
                          "      result['users'].push(parsed[k]); " +
                          "   }" +
                          "   return JSON.stringify(result);" +
                          "}";
        Query query = new Query();
        query.value = function;
        String httpResponse = "[\"user1\", \"user2\", \"user3\"]";
        String actual = json.convertToColumnarJSON(httpResponse, "test", query);
        String expected = "{\"users\":[\"user1\",\"user2\",\"user3\"]}";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testBadJavascript() {
        String function = "function test(input) {";
        Query query = new Query();
        query.value = function;
        String httpResponse = "[\"user1\", \"user2\", \"user3\"]";
        String actual = json.convertToColumnarJSON(httpResponse, "test", query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("ScriptException")));
    }

    @Test
    public void testUnknownFunction() {
        String function = "function identity(input) {return input;}";
        Query query = new Query();
        query.value = function;
        String httpResponse = "[\"user1\", \"user2\", \"user3\"]";
        String actual = json.convertToColumnarJSON(httpResponse, "test", query);
        Assert.assertNull(actual);
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("NoSuchMethodException")));
    }

    @Test
    public void testFailingRequest() throws IOException {
        HttpClient client = mock(HttpClient.class);
        Mockito.doThrow(new IOException("Testing failure")).when(client).execute(Mockito.any(HttpUriRequest.class));

        HttpUriRequest request = RequestBuilder.get("fake").build();
        Query query = new Query();

        json.makeRequest(client, request, query);

        Assert.assertTrue(query.failed());
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("Testing failure")));
    }

    @Test
    public void testNoResponse() throws IOException {
        HttpClient client = mock(HttpClient.class);
        Mockito.doReturn(null).when(client).execute(Mockito.any(HttpUriRequest.class));

        HttpUriRequest request = RequestBuilder.get("fake").build();
        Query query = new Query();

        json.makeRequest(client, request, query);

        Assert.assertTrue(query.failed());
        Assert.assertTrue(query.getMessages().stream().anyMatch(s -> s.contains("NullPointerException")));
    }

    @Test
    public void testPost() throws Exception {
        WireMockServer server = startWireMockServer(WIRE_MOCK_PORT);

        String userJSON = "[{\"name\": \"foo\", \"count\": 4},{\"name\": \"bar\", \"count\": 1}]";
        stubFor(post(urlEqualTo("/api/users")).willReturn(aResponse().withBody(userJSON)));

        Query query = getQueryFrom("rest-tests/sample.yaml", "Query1");
        json.execute(query);

        Assert.assertFalse(query.failed());
        Assert.assertEquals(query.getResult().getColumns().size(), 2);

        List<TypedObject> users = query.getResult().getColumn("users").getValues();
        Assert.assertEquals(users.size(), 2);
        Assert.assertEquals(users.get(0).data, "foo");
        Assert.assertEquals(users.get(0).type, TypeSystem.Type.STRING);
        Assert.assertEquals(users.get(1).data, "bar");
        Assert.assertEquals(users.get(1).type, TypeSystem.Type.STRING);

        List<TypedObject> counts = query.getResult().getColumn("counts").getValues();
        Assert.assertEquals(counts.size(), 2);
        Assert.assertEquals(counts.get(0).data, 4L);
        Assert.assertEquals(counts.get(0).type, TypeSystem.Type.LONG);
        Assert.assertEquals(counts.get(1).data, 1L);
        Assert.assertEquals(counts.get(1).type, TypeSystem.Type.LONG);

        server.stop();
    }

    @Test
    public void testGet() throws Exception {
        WireMockServer server = startWireMockServer(WIRE_MOCK_PORT);

        String maxJSON = "\"14\"";
        stubFor(get(urlEqualTo("/api/visits/max")).willReturn(aResponse().withBody(maxJSON)));

        Query query = getQueryFrom("rest-tests/sample.yaml", "Query2");
        json.execute(query);

        Assert.assertFalse(query.failed());
        Assert.assertEquals(query.getResult().getColumns().size(), 1);
        List<TypedObject> max = query.getResult().getColumn("max").getValues();
        Assert.assertEquals(max.size(), 1);
        Assert.assertEquals(max.get(0).data, 14L);
        Assert.assertEquals(max.get(0).type, TypeSystem.Type.LONG);

        server.stop();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class, expectedExceptionsMessageRegExp = ".*PUT.*")
    public void testUnknownVerb() throws Exception {
        Query query = getQueryFrom("rest-tests/sample.yaml", "Failer");
        json.execute(query);
    }
}
