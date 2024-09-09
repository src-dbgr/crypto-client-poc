package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sam.coin.model.Coin;
import crypto.processor.JsonProcessor;
import crypto.util.HttpClientWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackendServiceTest {

    @Mock
    private HttpClientWrapper httpClient;

    @Mock
    private JsonProcessor jsonProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    private BackendService backendService;

    @BeforeEach
    void setUp() {
        backendService = new BackendService("http://localhost:8080/api/v1/coin", httpClient, jsonProcessor, Arrays.asList("bitcoin", "ethereum"));
    }

    /**
     * Test case to verify that coin data is successfully sent to the backend.
     * It checks if the appropriate methods are called with the correct parameters
     * and if the response is properly handled.
     */
    @Test
    void sendCoinDataToBackend_shouldSendDataSuccessfully() throws Exception {
        // Arrange
        Coin coin = new Coin();
        coin.setCoinId("bitcoin");
        coin.setTimestamp(new Timestamp(System.currentTimeMillis()));
        coin.setPriceUsd(new BigDecimal("50000"));

        String jsonCoin = "{\"id\":\"bitcoin\"}";
        String prettyJsonCoin = "{\n  \"id\": \"bitcoin\"\n}";
        HttpResponse<String> response = mock(HttpResponse.class);

        when(jsonProcessor.getObjectMapper()).thenReturn(objectMapper);
        when(objectMapper.writeValueAsString(coin)).thenReturn(jsonCoin);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(coin)).thenReturn(prettyJsonCoin);
        when(httpClient.sendPostRequest(anyString(), eq(jsonCoin))).thenReturn(response);

        // Act
        backendService.sendCoinDataToBackend(coin);

        // Assert
        verify(httpClient).sendPostRequest(anyString(), eq(jsonCoin));
        verify(objectMapper).writeValueAsString(coin);
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(coin);
    }

    /**
     * Test case to verify that the last valid dates are correctly retrieved from the backend.
     * It checks if the response is properly parsed and if the returned map contains
     * the expected data for each cryptocurrency.
     */
    @Test
    void getLastValidDatesFromBackend_shouldRetrieveDates() throws Exception {
        // Arrange
        String response = "{\"bitcoin\":\"2023-05-01\",\"ethereum\":\"2023-05-02\"}";
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode bitcoinNode = mock(JsonNode.class);
        JsonNode ethereumNode = mock(JsonNode.class);

        when(httpClient.sendGetRequest(anyString())).thenReturn(response);
        when(jsonProcessor.parseJson(response)).thenReturn(rootNode);
        when(rootNode.has("bitcoin")).thenReturn(true);
        when(rootNode.has("ethereum")).thenReturn(true);
        when(rootNode.get("bitcoin")).thenReturn(bitcoinNode);
        when(rootNode.get("ethereum")).thenReturn(ethereumNode);
        when(bitcoinNode.asText()).thenReturn("2023-05-01");
        when(ethereumNode.asText()).thenReturn("2023-05-02");

        // Act
        Map<String, Date> result = backendService.getLastValidDatesFromBackend();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("bitcoin"));
        assertTrue(result.containsKey("ethereum"));
    }
}