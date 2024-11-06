package com.nathcat.AuthCat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.*;

public class AuthCatTest {

    @org.junit.Test
    public void tryLogin() throws IOException, InterruptedException, ParseException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://data.nathcat.net/sso/get-session.php"))
                .setHeader("Content-Type", "application/json")
                .setHeader("Cookie", "AuthCat-SSO=jqp1c4vs7i8310unva4e4lt18q")
                .build();

        HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString());
        String s = r.body();
        System.out.println(s);
        JSONObject d = (JSONObject) new JSONParser().parse(s);

        assertEquals("Nathcat", ((JSONObject) d.get("user")).get("username"));
    }
}