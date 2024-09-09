package crypto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.coin.model.Coin;
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
        String bitcoinPriceResponse = "{\"bitcoin\":{\"usd\":50000}}";
        String ethereumPriceResponse = "{\"ethereum\":{\"usd\":3000}}";
        String bitcoinInfoResponse = "{\"name\":\"Bitcoin\",\"symbol\":\"BTC\"}";
        String ethereumInfoResponse = "{\"name\":\"Ethereum\",\"symbol\":\"ETH\"}";

        setupMocks(apiUrl, bitcoinPriceResponse, ethereumPriceResponse, bitcoinInfoResponse, ethereumInfoResponse);

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(2)).sendGetRequest(contains("/simple/price"));
        verify(httpClient, times(2)).sendGetRequest(contains("/coins/"));
        verify(coinDataProcessor, times(2)).createCoinFromJsonNode(anyString(), anyString(), anyString(), any());
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
        String bitcoinPriceResponse = "{\"bitcoin\":{\"usd\":50000}}";
        String ethereumPriceResponse = "{}";  // No data for Ethereum
        String bitcoinInfoResponse = "{\"name\":\"Bitcoin\",\"symbol\":\"BTC\"}";

        setupMocks(apiUrl, bitcoinPriceResponse, ethereumPriceResponse, bitcoinInfoResponse, null);

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(2)).sendGetRequest(contains("/simple/price"));
        verify(httpClient, times(1)).sendGetRequest(contains("/coins/bitcoin"));
        verify(httpClient, never()).sendGetRequest(contains("/coins/ethereum"));
        verify(coinDataProcessor, times(1)).createCoinFromJsonNode(eq("bitcoin"), anyString(), anyString(), any());
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
        lastValidDates.put("bitcoin", Date.from(LocalDate.now().minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        lastValidDates.put("ethereum", Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        String historicalDataResponse = "{\"market_data\":{\"current_price\":{\"usd\":45000}}}";
        when(httpClient.sendGetRequest(anyString())).thenReturn(historicalDataResponse);
        when(coinDataProcessor.parseCoinData(anyString(), anyString(), any(LocalDate.class))).thenReturn(new Coin());

        // Act
        coinGeckoService.fetchAndSendHistoricalData(cryptoIds, lastValidDates, coin -> {});

        // Assert
        verify(httpClient, atLeast(8)).sendGetRequest(anyString());
        verify(coinDataProcessor, atLeast(8)).parseCoinData(anyString(), anyString(), any(LocalDate.class));
        verify(rateLimiter, atLeast(8)).acquire();
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

        String historicalDataResponse = "{\"market_data\":{\"current_price\":{\"usd\":45000}}}";
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
                .thenReturn("{\"bitcoin\":{\"usd\":50000}}");

        // Act
        coinGeckoService.fetchAndSendCurrentData(cryptoIds, coin -> {});

        // Assert
        verify(httpClient, times(3)).sendGetRequest(contains("/simple/price"));
        verify(rateLimiter, times(1)).acquire();
    }

    private void setupMocks(String apiUrl, String bitcoinPriceResponse, String ethereumPriceResponse,
                            String bitcoinInfoResponse, String ethereumInfoResponse) throws Exception {
        when(config.getCoingeckoApiUrl()).thenReturn(apiUrl);
        when(config.getMaxRetries()).thenReturn(3);
        when(httpClient.sendGetRequest(contains("/simple/price?ids=bitcoin"))).thenReturn(bitcoinPriceResponse);
        when(httpClient.sendGetRequest(contains("/simple/price?ids=ethereum"))).thenReturn(ethereumPriceResponse);
        when(httpClient.sendGetRequest(contains("/coins/bitcoin"))).thenReturn(bitcoinInfoResponse);
        if (ethereumInfoResponse != null) {
            when(httpClient.sendGetRequest(contains("/coins/ethereum"))).thenReturn(ethereumInfoResponse);
        }
        when(jsonProcessor.parseJson(anyString())).thenAnswer(invocation -> {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree((String) invocation.getArgument(0));
        });
        when(coinDataProcessor.createCoinFromJsonNode(anyString(), anyString(), anyString(), any())).thenReturn(new Coin());
    }
}