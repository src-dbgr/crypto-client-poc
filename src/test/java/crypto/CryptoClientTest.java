package crypto;

import crypto.config.CryptoConfig;
import crypto.service.BackendService;
import crypto.service.api.CryptoDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoClientTest {

    @Mock
    private CryptoConfig config;

    @Mock
    private CryptoDataSource dataSource;

    @Mock
    private BackendService backendService;

    private CryptoClient cryptoClient;

    @BeforeEach
    void setUp() {
        cryptoClient = new CryptoClient(config, dataSource, backendService);
    }

    @Test
    @DisplayName("Should fetch and send current data for all configured cryptocurrencies")
    void updateCurrentData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        when(config.getAllCryptoIds()).thenReturn(cryptoIds);

        // Act
        cryptoClient.updateCurrentData();

        // Assert
        verify(dataSource).fetchAndSendCurrentData(eq(cryptoIds), any());
    }

    @Test
    @DisplayName("Should fetch and send historical data based on last valid dates")
    void updateHistoricalData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        Map<String, Date> lastValidDates = new HashMap<>();
        lastValidDates.put("bitcoin", new Date());
        lastValidDates.put("ethereum", new Date());

        when(config.getAllCryptoIds()).thenReturn(cryptoIds);
        when(backendService.getLastValidDatesFromBackend()).thenReturn(lastValidDates);

        // Act
        cryptoClient.updateHistoricalData();

        // Assert
        verify(dataSource).fetchAndSendHistoricalData(eq(cryptoIds), eq(lastValidDates), any());
    }

    @Test
    @DisplayName("Should fetch and send all historical data for a specified time frame")
    void fetchAllHistoricalData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        int timeFrame = 60;

        when(config.getAllCryptoIds()).thenReturn(cryptoIds);

        // Act
        cryptoClient.fetchAllHistoricalData(timeFrame);

        // Assert
        verify(dataSource).fetchAndSendAllHistoricalData(eq(cryptoIds), eq(timeFrame), any());
    }
}