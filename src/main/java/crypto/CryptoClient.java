package crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import crypto.config.CryptoConfig;
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
		LOG.info("Starting to update current crypto data");
		dataSource.fetchAndSendCurrentData(config.getCryptoIds(), backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated current crypto data");
	}

	/**
	 * Fetches and updates historical data for all cryptocurrencies.
	 *
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void updateHistoricalData() throws Exception {
		LOG.info("Starting to update historical crypto data");
		dataSource.fetchAndSendHistoricalData(config.getCryptoIds(), backendService.getLastValidDatesFromBackend(), backendService::sendCoinDataToBackend);
		LOG.info("Successfully updated historical crypto data");
	}

	/**
	 * Fetches all historical data for a specific time frame.
	 *
	 * @param timeFrame the number of days to fetch data for
	 * @throws Exception if there's an error in API communication or data processing
	 */
	public void fetchAllHistoricalData(int timeFrame) throws Exception {
		LOG.info("Starting to fetch all historical data for the last {} days", timeFrame);
		dataSource.fetchAndSendAllHistoricalData(config.getCryptoIds(), timeFrame, backendService::sendCoinDataToBackend);
		LOG.info("Successfully fetched all historical data");
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

		// Create the CoinGeckoService as an implementation of CryptoDataSource
		CryptoDataSource dataSource = new CoinGeckoService(config, httpClientWrapper, jsonProcessor, coinDataProcessor, rateLimiter);
		BackendService backendService = new BackendService(config.getBackendUrl(), httpClientWrapper, jsonProcessor, config.getCryptoIds());

		CryptoClient client = new CryptoClient(config, dataSource, backendService);

		try {
			LOG.info("Updating current crypto data...");
//            client.updateCurrentData();
			LOG.info("Current crypto data update completed.");

			LOG.info("Fetching and updating historical data...");
			client.updateHistoricalData();
			LOG.info("Historical data update completed.");

			LOG.info("Fetching all historical data for the last 60 days...");
//            client.fetchAllHistoricalData(60);
			LOG.info("All historical data fetch completed.");

		} catch (Exception e) {
			LOG.error("An error occurred", e);
		}
	}
}