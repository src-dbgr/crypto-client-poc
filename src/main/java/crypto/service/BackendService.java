package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sam.coin.model.Coin;
import crypto.processor.JsonProcessor;
import crypto.util.HttpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for interacting with the backend API.
 */
public class BackendService {
	private static final Logger LOG = LoggerFactory.getLogger(BackendService.class);
	private final String backendUrl;
	private final HttpClientWrapper httpClient;
	private final JsonProcessor jsonProcessor;
	private final List<String> cryptoIds;

	/**
	 * Constructor for BackendService.
	 *
	 * @param backendUrl URL of the backend API
	 * @param httpClient HTTP client wrapper for making API requests
	 * @param jsonProcessor Processor for JSON data
	 * @param cryptoIds List of cryptocurrency IDs
	 */
	public BackendService(String backendUrl, HttpClientWrapper httpClient, JsonProcessor jsonProcessor, List<String> cryptoIds) {
		this.backendUrl = backendUrl;
		this.httpClient = httpClient;
		this.jsonProcessor = jsonProcessor;
		this.cryptoIds = cryptoIds;
	}

	/**
	 * Sends coin data to the backend.
	 *
	 * @param coin Coin object containing the data to be sent
	 */
	public void sendCoinDataToBackend(Coin coin) {
		try {
			String jsonCoin = jsonProcessor.getObjectMapper().writeValueAsString(coin);
			String prettyJsonCoin = jsonProcessor.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(coin);
			LOG.info("Sending coin data to backend:\n{}", prettyJsonCoin);

			HttpResponse<String> response = httpClient.sendPostRequest(backendUrl, jsonCoin);
			LOG.info("Backend response: {}", response);
		} catch (Exception e) {
			LOG.error("Error sending coin data to backend", e);
		}
	}

	/**
	 * Retrieves the last valid dates for each cryptocurrency from the backend.
	 *
	 * @return Map of cryptocurrency IDs to their last valid dates
	 * @throws IOException if there's an error in network communication
	 * @throws InterruptedException if the operation is interrupted
	 */
	public Map<String, Date> getLastValidDatesFromBackend() throws IOException, InterruptedException {
		Map<String, Date> result = new HashMap<>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (String coinId : cryptoIds) {
			String url = backendUrl + "/lastValidDate?coinId=" + coinId;
			try {
				String response = httpClient.sendGetRequest(url);
				LOG.info("Last valid date for coin {} is {}", coinId, response);
				JsonNode rootNode = jsonProcessor.parseJson(response);
				if (rootNode.has(coinId)) {
					String dateString = rootNode.get(coinId).asText();
					try {
						Date date = dateFormat.parse(dateString);
						result.put(coinId, date);
					} catch (ParseException e) {
						LOG.warn("Failed to parse date for coin {}. Date string: {}", coinId, dateString, e);
					}
				} else {
					LOG.warn("No valid date found for coin {}", coinId);
				}
			} catch (IOException e) {
				LOG.error("Failed to get last valid date for coin {}. Error: {}", coinId, e.getMessage());
				throw e; // Re-throw the exception to be handled by the caller
			}
		}
		return result;
	}
}