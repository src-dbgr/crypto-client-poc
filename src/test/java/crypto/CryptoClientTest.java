package crypto;

import crypto.config.CryptoConfig;
import crypto.service.BackendService;
import crypto.service.api.CryptoDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    void updateCurrentData_shouldFetchAndSendCurrentData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        when(config.getCryptoIds()).thenReturn(cryptoIds);

        // Act
        cryptoClient.updateCurrentData();

        // Assert
        verify(dataSource).fetchAndSendCurrentData(eq(cryptoIds), any());
    }

    @Test
    void updateHistoricalData_shouldFetchAndSendHistoricalData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        Map<String, Date> lastValidDates = new HashMap<>();
        lastValidDates.put("bitcoin", new Date());
        lastValidDates.put("ethereum", new Date());

        when(config.getCryptoIds()).thenReturn(cryptoIds);
        when(backendService.getLastValidDatesFromBackend()).thenReturn(lastValidDates);

        // Act
        cryptoClient.updateHistoricalData();

        // Assert
        verify(dataSource).fetchAndSendHistoricalData(eq(cryptoIds), eq(lastValidDates), any());
    }

    @Test
    void fetchAllHistoricalData_shouldFetchAndSendAllHistoricalData() throws Exception {
        // Arrange
        List<String> cryptoIds = Arrays.asList("bitcoin", "ethereum");
        int timeFrame = 60;

        when(config.getCryptoIds()).thenReturn(cryptoIds);

        // Act
        cryptoClient.fetchAllHistoricalData(timeFrame);

        // Assert
        verify(dataSource).fetchAndSendAllHistoricalData(eq(cryptoIds), eq(timeFrame), any());
    }
}
