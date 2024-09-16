package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.coin.domain.model.Coin;
import crypto.config.CryptoConfig;
import crypto.processor.CoinDataProcessor;
import crypto.processor.JsonProcessor;
import crypto.util.HttpClientWrapper;
import crypto.util.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinGeckoServiceTest {

    @Mock
    private CryptoConfig config;

    @Mock
    private HttpClientWrapper httpClient;

    @Mock
    private JsonProcessor jsonProcessor;

    @Mock
    private CoinDataProcessor coinDataProcessor;

    @Mock
    private RateLimiter rateLimiter;

    private CoinGeckoService coinGeckoService;

    @BeforeEach
    void setUp() {
        coinGeckoService = new CoinGeckoService(config, httpClient, jsonProcessor, coinDataProcessor, rateLimiter);
        when(config.getCoingeckoApiUrl()).thenReturn("https://api.coingecko.com/api/v3");
        when(config.getMaxRetries()).thenReturn(3);
    }

    /**
     * Tests the fetchAndSendCurrentData method when data is available for all requested cryptocurrencies.
     * Verifies that the method correctly processes and sends data for multiple cryptocurrencies.
     */
    @Test
    void fetchAndSendCurrentData_allDataAvailable_shouldProcessAndSendAllData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        String apiUrl = "https://api.coingecko.com/api/v3";
        String bitcoinResponse = "{\"id\":\"bitcoin\",\"symbol\":\"btc\",\"name\":\"Bitcoin\",\"market_data\":{\"current_price\":{\"usd\":50000,\"eur\":42000,\"btc\":1,\"eth\":15},\"market_cap\":{\"usd\":1000000000,\"eur\":840000000,\"btc\":20000,\"eth\":300000},\"total_volume\":{\"usd\":50000000,\"eur\":42000000,\"btc\":1000,\"eth\":15000},\"last_updated\":\"2021-09-10T19:54:06.165Z\"}}";
        String ethereumResponse = "{\"id\":\"ethereum\",\"symbol\":\"eth\",\"name\":\"Ethereum\",\"market_data\":{\"current_price\":{\"usd\":3000,\"eur\":2520,\"btc\":0.06,\"eth\":1},\"market_cap\":{\"usd\":400000000,\"eur\":336000000,\"btc\":8000,\"eth\":133333},\"total_volume\":{\"usd\":20000000,\"eur\":16800000,\"btc\":400,\"eth\":6666},\"last_updated\":\"2021-09-10T19:54:06.165Z\"}}";

        setupMocks(apiUrl, bitcoinResponse, ethereumResponse);

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(2)).sendGetRequest(contains("/coins/"));
        verify(coinDataProcessor, times(2)).createCoinFromJsonNode(anyString(), any(JsonNode.class));
        verify(rateLimiter, times(2)).acquire();
    }

    /**
     * Tests the fetchAndSendCurrentData method when data is not available for some cryptocurrencies.
     * Verifies that the method handles missing data gracefully and processes only available data.
     */
    @Test
    void fetchAndSendCurrentData_partialDataAvailable_shouldProcessAvailableData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        String apiUrl = "https://api.coingecko.com/api/v3";
        String bitcoinResponse = "{\"id\":\"bitcoin\",\"symbol\":\"btc\",\"name\":\"Bitcoin\",\"market_data\":{\"current_price\":{\"usd\":50000}}}";
        String ethereumResponse = "{}";  // No data for Ethereum

        setupMocks(apiUrl, bitcoinResponse, ethereumResponse);

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(2)).sendGetRequest(contains("/coins/"));
        verify(coinDataProcessor, times(1)).createCoinFromJsonNode(eq("bitcoin"), any(JsonNode.class));
        verify(rateLimiter, times(2)).acquire();
    }

    /**
     * Tests the fetchAndSendHistoricalData method for multiple cryptocurrencies.
     * Ensures that historical data is correctly fetched and processed for a given date range.
     */
    @Test
    void fetchAndSendHistoricalData_multipleCryptos_shouldFetchAndProcessData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        Map<String, Date> lastValidDates = new HashMap<>();
        lastValidDates.put("bitcoin", Date.from(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        lastValidDates.put("ethereum", Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        when(config.getMaxRetries()).thenReturn(3);
//        when(config.getRateLimitDelay()).thenReturn(1000L);
        when(httpClient.sendGetRequest(anyString())).thenReturn("{'market_data': {'current_price': {'usd': 50000}}}");
        when(coinDataProcessor.parseCoinData(anyString(), anyString(), any(LocalDate.class))).thenReturn(new Coin());

        // Act
        coinGeckoService.fetchAndSendHistoricalData(cryptoIds, lastValidDates, coin -> {});

        // Assert
        verify(httpClient, atLeast(2)).sendGetRequest(contains("bitcoin"));
        verify(httpClient, atLeast(3)).sendGetRequest(contains("ethereum"));
        verify(coinDataProcessor, atLeast(2)).parseCoinData(anyString(), eq("bitcoin"), any(LocalDate.class));
        verify(coinDataProcessor, atLeast(3)).parseCoinData(anyString(), eq("ethereum"), any(LocalDate.class));
        verify(rateLimiter, atLeast(5)).acquire();
    }

    /**
     * Tests the fetchAndSendAllHistoricalData method for a specific time frame.
     * Verifies that the method correctly fetches and processes historical data for all specified days.
     */
    @Test
    void fetchAndSendAllHistoricalData_specificTimeFrame_shouldFetchDataForAllDays() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        int timeFrame = 30;  // 30 days

        String historicalDataResponse = "{\"id\":\"bitcoin\",\"symbol\":\"btc\",\"name\":\"Bitcoin\",\"market_data\":{\"current_price\":{\"usd\":45000}}}";
        when(httpClient.sendGetRequest(anyString())).thenReturn(historicalDataResponse);
        when(coinDataProcessor.parseCoinData(anyString(), anyString(), any(LocalDate.class))).thenReturn(new Coin());

        // Act
        coinGeckoService.fetchAndSendAllHistoricalData(cryptoIds, timeFrame, coin -> {});

        // Assert
        verify(httpClient, times(timeFrame * cryptoIds.size())).sendGetRequest(anyString());
        verify(coinDataProcessor, times(timeFrame * cryptoIds.size())).parseCoinData(anyString(), anyString(), any(LocalDate.class));
        verify(rateLimiter, times(timeFrame * cryptoIds.size())).acquire();
    }
    /**
     * Tests the error handling in fetchAndSendCurrentData method when API calls fail.
     * Ensures that the method handles API failures gracefully and attempts retries.
     */
    @Test
    void fetchAndSendCurrentData_apiFailure_shouldHandleErrorAndRetry() throws Exception {
        // Arrange
        List<String> cryptoIds = Collections.singletonList("bitcoin");
        when(config.getCoingeckoApiUrl()).thenReturn("https://api.coingecko.com/api/v3");
        when(config.getMaxRetries()).thenReturn(3);
        when(config.getRateLimitDelay()).thenReturn(1000L);
        when(httpClient.sendGetRequest(anyString()))
                .thenThrow(new IOException("API Error"))
                .thenThrow(new IOException("API Error"))
                .thenReturn("{\"id\":\"bitcoin\",\"symbol\":\"btc\",\"name\":\"Bitcoin\",\"market_data\":{\"current_price\":{\"usd\":50000}}}");

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(3)).sendGetRequest(contains("/coins/"));
        verify(rateLimiter, times(1)).acquire();
    }

    private void setupMocks(String apiUrl, String bitcoinResponse, String ethereumResponse) throws Exception {
        when(config.getCoingeckoApiUrl()).thenReturn(apiUrl);
        when(config.getMaxRetries()).thenReturn(3);
        when(httpClient.sendGetRequest(contains("/coins/bitcoin"))).thenReturn(bitcoinResponse);
        when(httpClient.sendGetRequest(contains("/coins/ethereum"))).thenReturn(ethereumResponse);
        when(jsonProcessor.parseJson(anyString())).thenAnswer(invocation -> {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree((String) invocation.getArgument(0));
        });
        when(coinDataProcessor.createCoinFromJsonNode(anyString(), any(JsonNode.class))).thenReturn(new Coin());
    }
}