package crypto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sam.coin.domain.model.Coin;
import crypto.processor.JsonProcessor;
import crypto.util.HttpClientWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackendServiceTest {

    @Mock
    private HttpClientWrapper httpClientMock;

    @Mock
    private JsonProcessor jsonProcessorMock;

    private BackendService backendService;

    private static final String BASE_URL = "http://localhost:8080/api/v1/coin";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        backendService = new BackendService(BASE_URL, httpClientMock, jsonProcessorMock, Arrays.asList("bitcoin", "ethereum"));
    }

    @Test
    @DisplayName("Verify successful POST request of coin data to backend API")
    void sendCoinDataToBackend_shouldSendDataSuccessfully() throws Exception {
        // Arrange
        Coin coin = createTestCoin("bitcoin", "50000");
        String jsonCoin = objectMapper.writeValueAsString(coin);

        when(jsonProcessorMock.getObjectMapper()).thenReturn(objectMapper);

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClientMock.sendPostRequest(eq(BASE_URL), anyString())).thenReturn(mockResponse);

        // Act
        backendService.sendCoinDataToBackend(coin);

        // Assert
        verify(httpClientMock).sendPostRequest(eq(BASE_URL), eq(jsonCoin));
    }

    @Test
    @DisplayName("Ensure graceful handling of network errors during coin data transmission")
    void sendCoinDataToBackend_shouldHandleNetworkError() throws Exception {
        // Arrange
        Coin coin = createTestCoin("ethereum", "2000");
        when(jsonProcessorMock.getObjectMapper()).thenReturn(objectMapper);
        when(httpClientMock.sendPostRequest(anyString(), anyString())).thenThrow(new IOException("Network error"));

        // Act & Assert
        assertDoesNotThrow(() -> backendService.sendCoinDataToBackend(coin));
        verify(httpClientMock).sendPostRequest(eq(BASE_URL), anyString());
    }

    @ParameterizedTest(name = "{index} => coinId={0}, dateString={1}")
    @DisplayName("Validate retrieval and parsing of last valid dates for {0}")
    @CsvSource({
            "bitcoin,2023-09-17T00:00:00.000+00:00",
            "ethereum,2023-09-16T00:00:00.000+00:00"
    })
    void getLastValidDatesFromBackend_shouldRetrieveDates(String coinId, String dateString) throws Exception {
        // Arrange
        backendService = new BackendService(BASE_URL, httpClientMock, jsonProcessorMock, Collections.singletonList(coinId));

        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("success", true);
        responseNode.put("data", dateString);
        String responseJson = objectMapper.writeValueAsString(responseNode);

        when(httpClientMock.sendGetRequest(BASE_URL + "/" + coinId + "/lastValidDate")).thenReturn(responseJson);
        when(jsonProcessorMock.parseJson(responseJson)).thenReturn(objectMapper.readTree(responseJson));

        // Act
        Map<String, Date> result = backendService.getLastValidDatesFromBackend();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertTrue(result.containsKey(coinId));
        assertEquals(parseDate(dateString), result.get(coinId));

        verify(httpClientMock).sendGetRequest(BASE_URL + "/" + coinId + "/lastValidDate");
        verify(jsonProcessorMock).parseJson(responseJson);
    }

    @Test
    @DisplayName("Confirm robust handling of API responses with invalid date formats")
    void getLastValidDatesFromBackend_shouldHandleInvalidDateFormat() throws Exception {
        // Arrange
        String invalidDateString = "invalid-date";
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("success", true);
        responseNode.put("data", invalidDateString);
        String responseJson = objectMapper.writeValueAsString(responseNode);

        when(httpClientMock.sendGetRequest(anyString())).thenReturn(responseJson);
        when(jsonProcessorMock.parseJson(responseJson)).thenReturn(objectMapper.readTree(responseJson));

        // Act
        Map<String, Date> result = backendService.getLastValidDatesFromBackend();

        // Assert
        assertTrue(result.isEmpty());
        verify(httpClientMock, times(2)).sendGetRequest(anyString());
        verify(jsonProcessorMock, times(2)).parseJson(responseJson);
    }

    @Test
    @DisplayName("Assess error propagation for network failures during date retrieval")
    void getLastValidDatesFromBackend_shouldHandleNetworkError() throws Exception {
        // Arrange
        when(httpClientMock.sendGetRequest(anyString())).thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThrows(IOException.class, () -> backendService.getLastValidDatesFromBackend());
        verify(httpClientMock, times(1)).sendGetRequest(anyString());
    }

    private Coin createTestCoin(String crypto, String price) {
        Coin coin = new Coin();
        coin.setCoinId(crypto);
        coin.setTimestamp(new Timestamp(System.currentTimeMillis()));
        coin.setPriceUsd(new BigDecimal(price));
        return coin;
    }

    private Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateString);
    }
}