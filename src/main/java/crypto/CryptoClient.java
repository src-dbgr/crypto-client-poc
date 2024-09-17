package crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.config.CryptoConfig;
import crypto.config.CryptoId;
import crypto.processor.CoinDataProcessor;
import crypto.processor.JsonProcessor;
import crypto.service.BackendService;
import crypto.service.CoinGeckoService;
import crypto.service.api.CryptoDataSource;
import crypto.util.HttpClientWrapper;
import crypto.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Date;

/**
 * Main class for orchestrating cryptocurrency data updates and processing.
 */
public class CryptoClient {
	private static final Logger LOG = LoggerFactory.getLogger(CryptoClient.class);
	private final CryptoConfig config;
	private final CryptoDataSource dataSource;
	private final BackendService backendService;

	/**
	 * Constructor for CryptoClient.
	 *
	 * @param config Configuration for the client
	 * @param dataSource Source for cryptocurrency data
	 * @param backendService Service for interacting with the backend
	 */
	public CryptoClient(CryptoConfig config, CryptoDataSource dataSource, BackendService backendService) {
		this.config = config;
		this.dataSource = dataSource;
		this.backendService = backendService;
	}

	/**
	 * Updates current data for all cryptocurrencies in the configured list.
	 *
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void updateCurrentData() throws Exception {
		LOG.info("Starting to update current crypto data for all supported cryptocurrencies");
		dataSource.fetchAndSendCurrentData(config.getAllCryptoIds(), backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated current crypto data for all supported cryptocurrencies");
	}

	/**
	 * Updates current data for a single cryptocurrency.
	 *
	 * @param cryptoId The cryptocurrency ID to update
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void updateCurrentData(CryptoId cryptoId) throws Exception {
		LOG.info("Starting to update current crypto data for {}", cryptoId);
		dataSource.fetchAndSendCurrentData(cryptoId, backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated current crypto data for {}", cryptoId);
	}

	/**
	 * Fetches and updates historical data for all cryptocurrencies.
	 *
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void updateHistoricalData() throws Exception {
		LOG.info("Starting to update historical crypto data for all supported cryptocurrencies");
		dataSource.fetchAndSendHistoricalData(config.getAllCryptoIds(), backendService.getLastValidDatesFromBackend(), backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated historical crypto data for all supported cryptocurrencies");
	}

	/**
	 * Fetches and updates historical data for a single cryptocurrency.
	 *
	 * @param cryptoId The cryptocurrency ID to update
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void updateHistoricalData(CryptoId cryptoId) throws Exception {
		LOG.info("Starting to update historical crypto data for {}", cryptoId);
		Date lastValidDate = backendService.getLastValidDateFromBackend(cryptoId.getId());
		dataSource.fetchAndSendHistoricalData(cryptoId, lastValidDate, backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated historical crypto data for {}", cryptoId);
	}

	/**
	 * Fetches all historical data for all cryptocurrencies for a specific time frame.
	 *
	 * @param timeFrame the number of days to fetch data for
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void fetchAllHistoricalData(int timeFrame) throws Exception {
		LOG.info("Starting to fetch all historical data for the last {} days for all supported cryptocurrencies", timeFrame);
		dataSource.fetchAndSendAllHistoricalData(config.getAllCryptoIds(), timeFrame, backendService::sendCoinDataToBackend);
		LOG.info("Successfully fetched all historical data for all supported cryptocurrencies");
	}

	/**
	 * Fetches all historical data for a single cryptocurrency for a specific time frame.
	 *
	 * @param cryptoId The cryptocurrency ID to fetch data for
	 * @param timeFrame the number of days to fetch data for
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void fetchAllHistoricalData(CryptoId cryptoId, int timeFrame) throws Exception {
		LOG.info("Starting to fetch all historical data for the last {} days for {}", timeFrame, cryptoId);
		dataSource.fetchAndSendAllHistoricalData(cryptoId, timeFrame, backendService::sendCoinDataToBackend);
		LOG.info("Successfully fetched all historical data for {}", cryptoId);
	}

	/**
	 * Main method to run the CryptoClient.
	 *
	 * @param args Command line arguments (not used)
	 */
	public static void main(String[] args) {
		CryptoConfig config = new CryptoConfig();
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpClientWrapper httpClientWrapper = new HttpClientWrapper(httpClient);
		JsonProcessor jsonProcessor = new JsonProcessor(new ObjectMapper());
		CoinDataProcessor coinDataProcessor = new CoinDataProcessor();
		RateLimiter rateLimiter = new RateLimiter(config.getRateLimitDelay());

		CryptoDataSource dataSource = new CoinGeckoService(config, httpClientWrapper, jsonProcessor, coinDataProcessor, rateLimiter);
		BackendService backendService = new BackendService(config.getBackendUrl(), httpClientWrapper, jsonProcessor, config.getAllCryptoIds());

		CryptoClient client = new CryptoClient(config, dataSource, backendService);

		try {
			LOG.info("Updating current crypto data for all cryptocurrencies...");
			client.updateCurrentData();
			LOG.info("Current crypto data update completed for all cryptocurrencies.");

//			LOG.info("Updating current crypto data for Bitcoin...");
//			client.updateCurrentData(CryptoId.BITCOIN);
//			LOG.info("Current crypto data update completed for Bitcoin.");
//
//			LOG.info("Fetching and updating historical data for all cryptocurrencies...");
//			client.updateHistoricalData();
//			LOG.info("Historical data update completed for all cryptocurrencies.");
//
//			LOG.info("Fetching and updating historical data for Ethereum...");
//			client.updateHistoricalData(CryptoId.ETHEREUM);
//			LOG.info("Historical data update completed for Ethereum.");
//
//			LOG.info("Fetching all historical data for the last 60 days for all cryptocurrencies...");
//			client.fetchAllHistoricalData(60);
//			LOG.info("All historical data fetch completed for all cryptocurrencies.");
//
//			LOG.info("Fetching all historical data for the last 30 days for Cardano...");
//			client.fetchAllHistoricalData(CryptoId.CARDANO, 30);
//			LOG.info("All historical data fetch completed for Cardano.");

		} catch (Exception e) {
			LOG.error("An error occurred", e);
		}
	}
}