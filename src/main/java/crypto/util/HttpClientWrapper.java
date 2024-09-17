package crypto.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Wrapper class for HttpClient to simplify HTTP requests.
 * This class provides methods for sending HTTP requests with error handling.
 */
public class HttpClientWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientWrapper.class);
    private final HttpClient httpClient;

    /**
     * Constructs a new HttpClientWrapper with the specified HttpClient.
     *
     * @param httpClient The HttpClient to be used for requests
     */
    public HttpClientWrapper(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sends a GET request to the specified URL.
     *
     * @param url The URL to send the GET request to
     * @return The response body as a String
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public String sendGetRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            LOG.error("HTTP request failed with status code: {}", response.statusCode());
            LOG.error("HTTP request failed with body: {}", response.body());
            throw new IOException("HTTP request failed with status code: " + response.statusCode());
        }
    }

    /**
     * Sends a POST request to the specified URL with the given body.
     *
     * @param url The URL to send the POST request to
     * @param body The body of the POST request
     * @return The HttpResponse object
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public HttpResponse<String> sendPostRequest(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response;
        } else {
            LOG.error("HTTP request failed with response: {}", response.body());
            throw new IOException("HTTP request failed with status code: " + response.statusCode());
        }
    }
}