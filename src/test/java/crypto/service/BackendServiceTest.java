package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sam.coin.domain.model.Coin;
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
import java.text.SimpleDateFormat;
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
        String bitcoinResponse = "{\"success\":true,\"data\":\"2023-05-01\",\"message\":\"Last valid date retrieved successfully\"}";
        String ethereumResponse = "{\"success\":true,\"data\":\"2023-05-02\",\"message\":\"Last valid date retrieved successfully\"}";

        JsonNode bitcoinRootNode = mock(JsonNode.class);
        JsonNode ethereumRootNode = mock(JsonNode.class);
        JsonNode bitcoinDataNode = mock(JsonNode.class);
        JsonNode ethereumDataNode = mock(JsonNode.class);

        when(httpClient.sendGetRequest("http://localhost:8080/api/v1/coin/bitcoin/lastValidDate")).thenReturn(bitcoinResponse);
        when(httpClient.sendGetRequest("http://localhost:8080/api/v1/coin/ethereum/lastValidDate")).thenReturn(ethereumResponse);

        when(jsonProcessor.parseJson(bitcoinResponse)).thenReturn(bitcoinRootNode);
        when(jsonProcessor.parseJson(ethereumResponse)).thenReturn(ethereumRootNode);

        when(bitcoinRootNode.has("data")).thenReturn(true);
        when(ethereumRootNode.has("data")).thenReturn(true);
        when(bitcoinRootNode.get("data")).thenReturn(bitcoinDataNode);
        when(ethereumRootNode.get("data")).thenReturn(ethereumDataNode);
        when(bitcoinDataNode.asText()).thenReturn("2023-05-01");
        when(ethereumDataNode.asText()).thenReturn("2023-05-02");

        // Act
        Map<String, Date> result = backendService.getLastValidDatesFromBackend();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("bitcoin"));
        assertTrue(result.containsKey("ethereum"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(dateFormat.parse("2023-05-01"), result.get("bitcoin"));
        assertEquals(dateFormat.parse("2023-05-02"), result.get("ethereum"));

        // Verify that the correct URLs were called
        verify(httpClient).sendGetRequest("http://localhost:8080/api/v1/coin/bitcoin/lastValidDate");
        verify(httpClient).sendGetRequest("http://localhost:8080/api/v1/coin/ethereum/lastValidDate");
    }
}