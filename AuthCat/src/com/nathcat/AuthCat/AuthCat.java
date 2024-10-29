package com.nathcat.AuthCat;

import com.nathcat.AuthCat.Exceptions.InvalidResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * <p>An integration for making requests to the AuthCat service.</p>
 * <p>
 *     This class contains methods which will allow you to easily make requests to the AuthCat service for better
 *     integration with your applications.
 * </p>
 * @version 1.0.0
 * @author Nathan Baines
 * @see <a href="https://data.nathcat.net/sso">AuthCat</a>
 */
public class AuthCat {

    private static HttpClient makeClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .authenticator(null)
                .build();
    }

    private static HttpResponse<String> sendRequest(String uri, JSONObject body) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Call the authentication service from AuthCat.
     * @param authEntry The supplied authentication data, should contain a username and password field
     * @return The JSON data returned by the service
     * @throws IOException Thrown if an I/O error occurs when communicating with the service
     * @throws InterruptedException Thrown if the connection with the service is interrupted
     * @throws InvalidResponse Thrown if the service responds with an unexpected or invalid response code
     */
    public static JSONObject tryLogin(JSONObject authEntry) throws IOException, InterruptedException, InvalidResponse {
        HttpResponse<String> response = sendRequest("https://data.nathcat.net/sso/try-login.php", authEntry);

        if (response.statusCode() == 200) {
            try {
                return (JSONObject) new JSONParser().parse(response.body());
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new InvalidResponse(response.statusCode());
        }
    }

    /**
     * Call the user search service
     * @param searchData The JSON search data, must contain either a username field, or a fullName field, or both
     * @return The JSON response from the server
     * @throws IOException Thrown if an I/O error occurs when communicating with the service
     * @throws InterruptedException Thrown if the connection with the service is interrupted
     * @throws InvalidResponse Thrown if the service responds with an unexpected or invalid response
     */
    public static JSONObject userSearch(JSONObject searchData) throws IOException, InterruptedException, InvalidResponse {
        HttpResponse<String> response = sendRequest("https://data.nathcat.net/sso/user-search.php", searchData);

        if (response.statusCode() == 200) {
            try {
                return (JSONObject) new JSONParser().parse(response.body());
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new InvalidResponse(response.statusCode());
        }
    }
}
