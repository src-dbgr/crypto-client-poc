package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sam.coin.domain.model.Coin;
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
 * This class handles sending coin data to the backend and retrieving last valid dates for cryptocurrencies.
 */
public class BackendService {
	private static final Logger LOG = LoggerFactory.getLogger(BackendService.class);
	private final String backendUrl;
	private final HttpClientWrapper httpClient;
	private final JsonProcessor jsonProcessor;
	private final List<String> cryptoIds;
	private final SimpleDateFormat dateFormat;

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
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	}

	/**
	 * Sends coin data to the backend.
	 * This method serializes the coin data to JSON and sends it via a POST request.
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
			LOG.error("Error sending coin data to backend. Make sure the backend service is running and accessible.", e);
		}
	}

	/**
	 * Retrieves the last valid dates for each cryptocurrency from the backend.
	 * This method sends GET requests to the backend for each cryptocurrency ID and parses the response.
	 *
	 * @return Map of cryptocurrency IDs to their last valid dates
	 * @throws IOException if there's an error in network communication
	 * @throws InterruptedException if the operation is interrupted
	 */
	public Map<String, Date> getLastValidDatesFromBackend() throws IOException, InterruptedException {
		Map<String, Date> result = new HashMap<>();

		for (String coinId : cryptoIds) {
			Date lastValidDate = getLastValidDateForCoin(coinId);
			if (lastValidDate != null) {
				result.put(coinId, lastValidDate);
			}
		}

		return result;
	}

	/**
	 * Retrieves the last valid date for a single cryptocurrency from the backend.
	 * This method sends a GET request to the backend for the specified cryptocurrency ID and parses the response.
	 *
	 * @param coinId The ID of the cryptocurrency to fetch the last valid date for
	 * @return The last valid date for the specified cryptocurrency, or null if not found
	 * @throws IOException if there's an error in network communication
	 * @throws InterruptedException if the operation is interrupted
	 */
	public Date getLastValidDateFromBackend(String coinId) throws IOException, InterruptedException {
		return getLastValidDateForCoin(coinId);
	}

	/**
	 * Retrieves the last valid date for a single cryptocurrency from the backend.
	 * This private method encapsulates the logic for fetching and parsing the last valid date.
	 *
	 * @param coinId The ID of the cryptocurrency to fetch the last valid date for
	 * @return The last valid date for the specified cryptocurrency, or null if not found
	 * @throws IOException if there's an error in network communication
	 * @throws InterruptedException if the operation is interrupted
	 */
	private Date getLastValidDateForCoin(String coinId) throws IOException, InterruptedException {
		String url = backendUrl + "/" + coinId + "/lastValidDate";

		try {
			String response = httpClient.sendGetRequest(url);
			LOG.info("Last valid date for coin {} is {}", coinId, response);
			JsonNode rootNode = jsonProcessor.parseJson(response);
			if (rootNode != null && rootNode.has("data")) {
				String dateString = rootNode.get("data").asText();
				try {
					return dateFormat.parse(dateString);
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

		return null;
	}
}