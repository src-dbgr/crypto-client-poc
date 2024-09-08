package crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.coin.model.Coin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client for fetching and updating cryptocurrency data from Coingecko API.
 * This class respects Coingecko's rate limiting for public API usage.
 */
public class CryptoClient {

	private static final Logger LOG = Logger.getLogger(CryptoClient.class.getName());
	private static final String BACKEND_URL = "http://localhost:8080/api/v1/coin";
	private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";
	private static final int MAX_RETRIES = 10;
	private static final int RATE_LIMIT_DELAY = 5000; // milliseconds
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private static final List<String> CRYPTO_IDS = Arrays.asList(
			"bitcoin", "ethereum", "cardano", "polkadot", "chainlink",
			"stellar", "zcash", "algorand", "bitcoin-diamond", "litecoin",
			"compound-ether", "compound-coin", "bzx-protocol", "band-protocol",
			"ampleforth", "zilliqa", "vechain", "waves", "uma", "ocean-protocol",
			"theta-token", "singularitynet", "thorchain", "kava"
	);

	private CryptoClient() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Updates the current data for all cryptocurrencies in the list.
	 *
	 * @throws IOException if there's an I/O error during the operation
	 * @throws InterruptedException if the operation is interrupted
	 */
	public static void updateCryptos() throws IOException, InterruptedException {
		updateCryptoList("coingecko.json", "portfoliocoingecko.json");

		String url = COINGECKO_API_URL + "/simple/price?ids=%s&vs_currencies=eur,btc,eth,usd&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true&include_last_updated_at=true";

		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("portfoliocoingecko.json")) {
			JsonNode coinsNode = OBJECT_MAPPER.readTree(in);
			for (JsonNode crypto : coinsNode) {
				String cryptoId = crypto.get("id").asText();
				String cryptoName = crypto.get("name").asText();
				String cryptoSymbol = crypto.get("symbol").asText();

				String formattedUrl = String.format(url, cryptoId);

				CompletableFuture.runAsync(() -> {
					try {
						processCryptoData(formattedUrl, cryptoId, cryptoName, cryptoSymbol);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Error processing crypto data for " + cryptoId, e);
					}
				}).join(); // Wait for each crypto to complete before moving to the next one

				Thread.sleep(RATE_LIMIT_DELAY);
			}
		}
	}

	private static void processCryptoData(String url, String cryptoId, String cryptoName, String cryptoSymbol) throws IOException, InterruptedException {
		int retryCount = 0;
		while (retryCount < MAX_RETRIES) {
			try {
				HttpResponse<String> response = sendGetRequest(url);
				LOG.info("Response Status Code: " + response.statusCode());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					JsonNode coinData = OBJECT_MAPPER.readTree(response.body()).get(cryptoId);
					if (coinData != null) {
						Coin coin = createCoinFromJsonNode(cryptoId, cryptoName, cryptoSymbol, coinData);
						sendCoinDataToBackend(coin);
						return;
					} else {
						LOG.warning("No data returned for " + cryptoId);
						return;
					}
				} else {
					throw new IOException("HTTP status code: " + response.statusCode() + " for " + cryptoId);
				}
			} catch (Exception e) {
				retryCount++;
				LOG.log(Level.WARNING, "Error occurred for " + cryptoId + ": " + e.getMessage(), e);
				if (retryCount >= MAX_RETRIES) {
					LOG.severe("Max retries reached for " + cryptoId + ". Moving to next coin.");
					return;
				} else {
					LOG.info("Retrying in " + (RATE_LIMIT_DELAY * retryCount) + " milliseconds...");
					Thread.sleep(RATE_LIMIT_DELAY * retryCount);  // Exponential backoff
				}
			}
		}
	}

	private static void updateCryptoList(String sourceName, String targetName) throws IOException {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceName)) {
			JsonNode jsonNode = OBJECT_MAPPER.readTree(in);
			List<JsonNode> filteredCoins = new ArrayList<>();
			for (JsonNode coin : jsonNode) {
				if (CRYPTO_IDS.contains(coin.get("id").asText())) {
					filteredCoins.add(coin);
				}
			}
			OBJECT_MAPPER.writeValue(new File("src/main/resources/" + targetName), filteredCoins);
		}
	}

	/**
	 * Fetches and updates historical data for all cryptocurrencies.
	 *
	 * @throws Exception if there's an error during the operation
	 */
	public static void fetchAndUpdateHistoricalData() throws Exception {
		Map<String, Date> lastValidDates = getLastValidDatesFromBackend();
		for (String coinId : CRYPTO_IDS) {
			Date lastValidDate = lastValidDates.get(coinId);
			LocalDate startDate = lastValidDate != null ?
					lastValidDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1) :
					LocalDate.now().minusYears(1);
			LocalDate endDate = LocalDate.now();

			while (!startDate.isAfter(endDate)) {
				String dateStr = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
				String url = String.format("%s/coins/%s/history?date=%s", COINGECKO_API_URL, coinId, dateStr);

				LocalDate finalStartDate = startDate;
				CompletableFuture.runAsync(() -> {
					try {
						processHistoricalData(url, coinId, finalStartDate);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Error processing historical data for " + coinId + " on " + dateStr, e);
					}
				}).join(); // Wait for each date to complete before moving to the next one

				startDate = startDate.plusDays(1);
				Thread.sleep(RATE_LIMIT_DELAY);
			}
		}
	}

	private static void processHistoricalData(String url, String coinId, LocalDate date) throws Exception {
		int retryCount = 0;
		while (retryCount < MAX_RETRIES) {
			try {
				HttpResponse<String> response = sendGetRequest(url);
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					Coin coin = parseCoinData(response.body(), coinId, date);
					sendCoinDataToBackend(coin);
					return;
				} else if (response.statusCode() == 401 || response.statusCode() == 403) {
					LOG.warning("Skipping due to lack of permission for " + coinId + " on " + date);
					return;
				} else {
					throw new IOException("HTTP error code: " + response.statusCode() + " for " + coinId + " on " + date);
				}
			} catch (Exception e) {
				retryCount++;
				LOG.log(Level.WARNING, "Error occurred for " + coinId + " on " + date + ": " + e.getMessage(), e);
				if (retryCount >= MAX_RETRIES) {
					LOG.severe("Max retries reached for " + coinId + " on " + date + ". Moving to next date.");
					return;
				} else {
					LOG.info("Retrying in " + (RATE_LIMIT_DELAY * retryCount) + " milliseconds...");
					Thread.sleep(RATE_LIMIT_DELAY * retryCount);  // Exponential backoff
				}
			}
		}
	}

	/**
	 * Fetches historical data for all cryptocurrencies for a specified time frame.
	 *
	 * @param timeFrame the number of days to fetch data for
	 * @throws InterruptedException if the operation is interrupted
	 */
	public static void fetchAllHistoricalData(int timeFrame) throws InterruptedException {
		for (String coinId : CRYPTO_IDS) {
			for (int i = 0; i < timeFrame; i++) {
				LocalDate date = LocalDate.now().minusDays(i);
				String dateString = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
				String url = String.format("%s/coins/%s/history?date=%s", COINGECKO_API_URL, coinId, dateString);

				CompletableFuture.runAsync(() -> {
					try {
						processHistoricalData(url, coinId, date);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Error processing historical data for " + coinId + " on " + dateString, e);
					}
				}).join(); // Wait for each date to complete before moving to the next one

				Thread.sleep(RATE_LIMIT_DELAY);
			}
		}
	}

	private static Map<String, Date> getLastValidDatesFromBackend() throws Exception {
		Map<String, Date> result = new HashMap<>();
		for (String coinId : CRYPTO_IDS) {
			String url = BACKEND_URL + "/lastValidDate?coinId=" + coinId;
			HttpResponse<String> response = sendGetRequest(url);
			if (response.statusCode() == 200) {
				JsonNode rootNode = OBJECT_MAPPER.readTree(response.body());
				if (rootNode.has(coinId)) {
					String dateString = rootNode.get(coinId).asText();
					result.put(coinId, new SimpleDateFormat("yyyy-MM-dd").parse(dateString));
				}
			}
		}
		return result;
	}

	private static Coin parseCoinData(String jsonData, String coinId, LocalDate date) throws Exception {
		JsonNode root = OBJECT_MAPPER.readTree(jsonData);
		Coin coin = new Coin();
		coin.setCoinId(coinId);
		coin.setTimestamp(Timestamp.valueOf(date.atStartOfDay()));
		coin.setSymbol(getTextSafely(root, "symbol"));
		coin.setCoinName(getTextSafely(root, "name"));

		JsonNode marketData = root.get("market_data");
		if (marketData != null) {
			setCoinPrices(coin, marketData.get("current_price"));
			setCoinMarketCaps(coin, marketData.get("market_cap"));
			setCoinVolumes(coin, marketData.get("total_volume"));
		}

		setCommunityData(coin, root.get("community_data"));
		setDeveloperData(coin, root.get("developer_data"));
		setPublicInterestStats(coin, root.get("public_interest_stats"));

		return coin;
	}

	private static void setCoinPrices(Coin coin, JsonNode priceNode) {
		if (priceNode != null) {
			coin.setPriceEur(getDecimalSafely(priceNode, "eur"));
			coin.setPriceUsd(getDecimalSafely(priceNode, "usd"));
			coin.setPriceBtc(getDecimalSafely(priceNode, "btc"));
			coin.setPriceEth(getDecimalSafely(priceNode, "eth"));
		}
	}

	private static void setCoinMarketCaps(Coin coin, JsonNode marketCapNode) {
		if (marketCapNode != null) {
			coin.setMarketCapEur(getDecimalSafely(marketCapNode, "eur"));
			coin.setMarketCapUsd(getDecimalSafely(marketCapNode, "usd"));
			coin.setMarketCapBtc(getDecimalSafely(marketCapNode, "btc"));
			coin.setMarketCapEth(getDecimalSafely(marketCapNode, "eth"));
		}
	}

	private static void setCoinVolumes(Coin coin, JsonNode volumeNode) {
		if (volumeNode != null) {
			coin.setTotalVolumeEur(getDecimalSafely(volumeNode, "eur"));
			coin.setTotalVolumeUsd(getDecimalSafely(volumeNode, "usd"));
			coin.setTotalVolumeBtc(getDecimalSafely(volumeNode, "btc"));
			coin.setTotalVolumeEth(getDecimalSafely(volumeNode, "eth"));
		}
	}

	private static void setCommunityData(Coin coin, JsonNode communityData) {
		if (communityData != null) {
			coin.setTwitterFollowers(getLongSafely(communityData, "twitter_followers", 0L));
			coin.setRedditAvgPosts48Hours(getDecimalSafely(communityData, "reddit_average_posts_48h"));
			coin.setRedditAvgComments48Hours(getDecimalSafely(communityData, "reddit_average_comments_48h"));
			coin.setRedditSubscribers(getLongSafely(communityData, "reddit_subscribers", 0L));
			coin.setRedditAccountsActive48Hours(getDecimalSafely(communityData, "reddit_accounts_active_48h"));
		}
	}

	private static void setDeveloperData(Coin coin, JsonNode developerData) {
		if (developerData != null) {
			coin.setDevForks(getLongSafely(developerData, "forks", 0L));
			coin.setDevStars(getLongSafely(developerData, "stars", 0L));
			coin.setDevTotalIssues(getLongSafely(developerData, "total_issues", 0L));
			coin.setDevClosedIssues(getLongSafely(developerData, "closed_issues", 0L));
			coin.setDevPullRequestsMerged(getLongSafely(developerData, "pull_requests_merged", 0L));
			coin.setDevPullRequestContributors(getLongSafely(developerData, "pull_request_contributors", 0L));
			coin.setDevCommitCount4Weeks(getLongSafely(developerData, "commit_count_4_weeks", 0L));

			JsonNode codeAdditionsDeletions4Weeks = developerData.get("code_additions_deletions_4_weeks");
			if (codeAdditionsDeletions4Weeks != null) {
				coin.setDevCodeAdditions4Weeks(getLongSafely(codeAdditionsDeletions4Weeks, "additions", 0L));
				coin.setDevCodeDeletions4Weeks(getLongSafely(codeAdditionsDeletions4Weeks, "deletions", 0L));
			}
		}
	}

	private static void setPublicInterestStats(Coin coin, JsonNode publicInterestStats) {
		if (publicInterestStats != null) {
			coin.setPublicAlexaRank(getLongSafely(publicInterestStats, "alexa_rank", 0L));
		}
	}

	private static HttpResponse<String> sendGetRequest(String uri) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.header("Accept", "application/json")
				.GET()
				.build();

		LOG.info("Sending request: " + request);
		return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static void sendCoinDataToBackend(Coin coin) {
		try {
			String jsonCoin = OBJECT_MAPPER.writeValueAsString(coin);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(BACKEND_URL))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonCoin))
					.build();

			LOG.info("Sending coin data to backend: " + request);
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOG.warning("Error sending data to backend. HTTP status: " + response.statusCode());
			}
			LOG.info("Backend response: " + response);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Error sending coin data to backend", e);
		}
	}

	private static Coin createCoinFromJsonNode(String cryptoId, String cryptoName, String cryptoSymbol, JsonNode coinData) {
		Coin coin = new Coin();
		coin.setCoinId(cryptoId);
		coin.setCoinName(cryptoName);
		coin.setSymbol(cryptoSymbol);
		coin.setTimestamp(new Timestamp(coinData.get("last_updated_at").asLong() * 1000));

		coin.setPriceEur(getDecimalSafely(coinData, "eur"));
		coin.setPriceUsd(getDecimalSafely(coinData, "usd"));
		coin.setPriceBtc(getDecimalSafely(coinData, "btc"));
		coin.setPriceEth(getDecimalSafely(coinData, "eth"));

		coin.setMarketCapEur(getDecimalSafely(coinData, "eur_market_cap"));
		coin.setMarketCapUsd(getDecimalSafely(coinData, "usd_market_cap"));
		coin.setMarketCapBtc(getDecimalSafely(coinData, "btc_market_cap"));
		coin.setMarketCapEth(getDecimalSafely(coinData, "eth_market_cap"));

		coin.setTotalVolumeEur(getDecimalSafely(coinData, "eur_24h_vol"));
		coin.setTotalVolumeUsd(getDecimalSafely(coinData, "usd_24h_vol"));
		coin.setTotalVolumeBtc(getDecimalSafely(coinData, "btc_24h_vol"));
		coin.setTotalVolumeEth(getDecimalSafely(coinData, "eth_24h_vol"));

		return coin;
	}

	private static String getTextSafely(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null) ? field.asText() : "";
	}

	private static BigDecimal getDecimalSafely(JsonNode node, String fieldName) {
		if (node == null) return BigDecimal.ZERO;
		JsonNode field = node.get(fieldName);
		return (field != null && field.isNumber()) ? field.decimalValue() : BigDecimal.ZERO;
	}

	private static Long getLongSafely(JsonNode node, String fieldName, Long defaultValue) {
		if (node == null) return defaultValue;
		JsonNode field = node.get(fieldName);
		return (field != null && field.isNumber()) ? field.longValue() : defaultValue;
	}

	/**
	 * Main method to demonstrate the usage of the CryptoClient.
	 *
	 * @param args command line arguments (not used)
	 */
	public static void main(String[] args) {
		try {
			LOG.info("Updating current crypto data...");
			updateCryptos();
			LOG.info("Current crypto data update completed.");

			LOG.info("Fetching and updating historical data...");
//			fetchAndUpdateHistoricalData();
			LOG.info("Historical data update completed.");

			LOG.info("Fetching all historical data for the last 60 days...");
//			fetchAllHistoricalData(60);
			LOG.info("All historical data fetch completed.");

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "An error occurred", e);
		}
	}
}