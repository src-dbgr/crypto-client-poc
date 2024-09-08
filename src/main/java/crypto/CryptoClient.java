package crypto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sam.coin.model.Coin;

public class CryptoClient {

	private static final String BACKEND_URL = "http://localhost:8080/api/v1/coin";
	private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";

	private static final int MAX_RETRIES = 10;
	private static final int INITIAL_WAIT_TIME = 5000; // 5 Sekunden

	static String[] coins = new String[] { "ethereum", "stellar", "cardano", "zcash", "algorand", "bitcoin",
			"chainlink", "Polkadot", "THETA" };

	static String[] cryptoIds = new String[] { "bitcoin-diamond", "litecoin", "compound-ether", "compound-coin",
			"bzx-protocol", "band-protocol", "algorand", "bitcoin", "cardano", "chainlink", "ethereum", "polkadot",
			"stellar", "zcash", "ampleforth", "zilliqa", "vechain", "waves", "uma", "ocean-protocol", "theta-token",
			"singularitynet", "thorchain", "kava" };

	String[] neueCoins = new String[] { "ocean-protocol", "theta-token", "singularitynet", "thorchain", "kava" };

	static ArrayList<JsonNode> list = new ArrayList<>();

	// parses out coin details from grand list of coins for each coinId in cryptoIds
	// Array.
	// writes out a new file "targetname" with extracted information
	// assumes JSON source contains a data parent element
	public static void parse(String sourceName, String targetName) {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceName)) {
			// pass InputStream to JSON-Library, e.g. using Jackson
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readValue(in, JsonNode.class);
			JsonNode coins = jsonNode.get("data");
			for (JsonNode coin : coins) {
				for (int i = 0; i < cryptoIds.length; i++) {
					if (coin.get("id").toString().equalsIgnoreCase("\"" + cryptoIds[i] + "\"")) {
						System.out.println(coin.toPrettyString());
						list.add(coin);
					}
				}
			}
			ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
			writer.writeValue(new File("src/main/resources/" + targetName), list);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// parses out coin details from grand list of coins for each coinId in cryptoIds
	// Array.
	// writes out a new file "targetname" with extracted information
	// assumes JSON source contains a data parent element
	public static void parseCoingecko(String sourceName, String targetName) {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceName)) {
			// pass InputStream to JSON-Library, e.g. using Jackson
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readValue(in, JsonNode.class);
			System.out.println("COINS TO BE UPDATED\n");
			for (JsonNode jsonNode2 : jsonNode) {
				for (int i = 0; i < cryptoIds.length; i++) {
					if (jsonNode2.get("id").toString().equalsIgnoreCase("\"" + cryptoIds[i] + "\"")) {
						System.out.println(jsonNode2.toPrettyString());
						list.add(jsonNode2);
					}
				}
			}
			System.out.println("\n==========================================================================\n");
			ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
			writer.writeValue(new File("src/main/resources/" + targetName), list);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void parseCoingeckoUpdatable(String sourceName, String targetName, boolean updateSource) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode updatedCoins = null;
		if (updateSource) {
			try {
				ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
				updatedCoins = mapper.readValue(getCoingecko("https://api.coingecko.com/api/v3/coins/list"),
						JsonNode.class);
				writer.writeValue(new File("src/main/resources/" + sourceName), updatedCoins);
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		try {
			InputStream in = updatedCoins == null
					? Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceName)
					: null;
			JsonNode coins = in != null ? mapper.readValue(in, JsonNode.class) : updatedCoins;
			for (JsonNode coin : coins) {
				for (int i = 0; i < cryptoIds.length; i++) {
					if (coin.get("id").toString().equalsIgnoreCase("\"" + cryptoIds[i] + "\"")) {
						System.out.println(coin.toPrettyString());
						list.add(coin);
					}
				}
			}
			ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
			writer.writeValue(new File("src/main/resources/" + targetName), list);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getCoinMarketCap(String uri) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
				.headers("X-CMC_PRO_API_KEY", "ea35e4c1-e48e-4892-a95b-159c273fd8d7")
				.header("Accept", "application/json").build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return response.body().toString();
	}

	public static String getCoingecko(String uri) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.header("Accept", "application/json")
				.build();
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		System.out.println("Status: " + response.statusCode());
		return response.body();
	}

	public static void post(String uri, String data) throws Exception {
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).header("content-type", "application/json")
				.POST(BodyPublishers.ofString(data)).build();

		HttpResponse<?> response = client.send(request, BodyHandlers.discarding());
		System.out.println(response.body());
		System.out.println(response.toString());
		System.out.println(response.statusCode());
	}

	public static String getFormattedDate(LocalDate date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		return date.format(formatter);
	}

	// https://www.coingecko.com/api/documentations/v3#/coins/get_coins__id__history
	public static String getUrlForHistoricalData(String coinId, String date) {
		return String.format("https://api.coingecko.com/api/v3/coins/%s/history?date=%s", coinId, date);
	}

	public static void fetchAllHistoricalData(String[] cryptos, int timeFrame) throws InterruptedException {
		int totalFailCounter = 0;

		for (String cryptocoin : cryptos) {

			int failCounter = 0;
			for (int i = 0; i < timeFrame; i++) {
				try {
					// coingecko does not allow too many requests
					// sleep in beetween new request
					Thread.sleep(600);
					LocalDate date = LocalDate.now().minusDays(i);
					System.out.println("DATE: " + date);
					String dateString = getFormattedDate(date);
					Timestamp timestamp = null;
					timestamp = Timestamp.valueOf(date.atStartOfDay());
					System.out.println(timestamp);
					String string = getCoingecko(getUrlForHistoricalData(cryptocoin, dateString));

					System.out.println(string);
					String[] currencies = new String[] { "eur", "usd", "btc", "eth" };

					ObjectMapper mapper = new ObjectMapper();
					JsonNode readValue = mapper.readValue(string, JsonNode.class);
					JsonNode market_data = readValue.get("market_data");
					JsonNode current_price = market_data.get("current_price");
					JsonNode market_cap = market_data.get("market_cap");
					JsonNode total_volume = market_data.get("total_volume");

					JsonNode community_data = readValue.get("community_data");
					JsonNode developer_data = readValue.get("developer_data");
					JsonNode public_interest_stats = readValue.get("public_interest_stats");

//		for (String currency : currencies) {
//			System.out.printf("Current Price %s: %f\n", currency, current_price.get(currency).decimalValue());
//			System.out.printf("Market Cap %s: %f\n", currency, market_cap.get(currency).decimalValue());
//			System.out.printf("Total Volume %s: %f\n", currency, total_volume.get(currency).decimalValue());
//		}
//		
//		System.out.printf("\n\nTwitter Followers: %s\n", community_data.get("twitter_followers").bigIntegerValue());
//		System.out.printf("Reddit Average Posts 48h: %s\n", community_data.get("reddit_average_posts_48h").decimalValue());
//		System.out.printf("Reddit Average Comments 48h: %s\n", community_data.get("reddit_average_comments_48h").decimalValue());
//		System.out.printf("Reddit Subscribers: %s\n", community_data.get("reddit_subscribers").bigIntegerValue());
//		System.out.printf("Reddit Accounts Active 48h: %s\n", community_data.get("reddit_accounts_active_48h").decimalValue());
//		
//
//		System.out.printf("\n\nDev Forks: %s\n", developer_data.get("forks").bigIntegerValue());
//		System.out.printf("Dev Stars: %s\n", developer_data.get("stars").bigIntegerValue());
//		System.out.printf("Dev Subscribers: %s\n", developer_data.get("subscribers").bigIntegerValue());
//		System.out.printf("Dev Total Issues: %s\n", developer_data.get("total_issues").bigIntegerValue());
//		System.out.printf("Dev Closed Issues: %s\n", developer_data.get("closed_issues").bigIntegerValue());
//		System.out.printf("Dev Pull Requests Merged: %s\n", developer_data.get("pull_requests_merged").bigIntegerValue());
//		System.out.printf("Dev Pull Requests Contributors: %s\n", developer_data.get("pull_request_contributors").bigIntegerValue());
//		System.out.printf("Dev Commit Coutn 4 Weeks: %s\n", developer_data.get("commit_count_4_weeks").bigIntegerValue());
//		System.out.printf("Dev Code Additions 4 Weeks: %s\n", developer_data.get("code_additions_deletions_4_weeks").get("additions").bigIntegerValue());
//		System.out.printf("Dev Code Deletions 4 Weeks: %s\n", developer_data.get("code_additions_deletions_4_weeks").get("deletions").bigIntegerValue());
//		
//		System.out.printf("Public Interest Stats Alexa Rank: %s\n\n\n\n",public_interest_stats.get("alexa_rank").bigIntegerValue());

					Coin coin = new Coin();
					coin.setCoinId(readValue.get("id").asText());
					coin.setCoinName(readValue.get("name").asText());
					coin.setSymbol(readValue.get("symbol").asText());
					coin.setTimestamp(timestamp);
					coin.setPriceEur(current_price.get("eur").decimalValue());
					coin.setPriceUsd(current_price.get("usd").decimalValue());
					coin.setPriceBtc(current_price.get("btc").decimalValue());
					coin.setPriceEth(current_price.get("eth").decimalValue());
					coin.setMarketCapEur(market_cap.get("eur").decimalValue());
					coin.setMarketCapUsd(market_cap.get("usd").decimalValue());
					coin.setMarketCapBtc(market_cap.get("btc").decimalValue());
					coin.setMarketCapEth(market_cap.get("eth").decimalValue());
					coin.setTotalVolumeEur(total_volume.get("eur").decimalValue());
					coin.setTotalVolumeUsd(total_volume.get("usd").decimalValue());
					coin.setTotalVolumeBtc(total_volume.get("btc").decimalValue());
					coin.setTotalVolumeEth(total_volume.get("eth").decimalValue());
					coin.setTwitterFollowers(community_data.get("twitter_followers").longValue());
					coin.setRedditAvgPosts48Hours(community_data.get("reddit_average_posts_48h").decimalValue());
					coin.setRedditAvgComments48Hours(community_data.get("reddit_average_comments_48h").decimalValue());
					coin.setRedditAccountsActive48Hours(
							community_data.get("reddit_accounts_active_48h").decimalValue());
					coin.setRedditSubscribers(community_data.get("reddit_subscribers").longValue());
					coin.setDevForks(developer_data.get("forks").longValue());
					coin.setDevStars(developer_data.get("stars").longValue());
					coin.setDevTotalIssues(developer_data.get("total_issues").longValue());
					coin.setDevClosedIssues(developer_data.get("closed_issues").longValue());
					coin.setDevPullRequestsMerged(developer_data.get("pull_requests_merged").longValue());
					coin.setDevPullRequestContributors(developer_data.get("pull_request_contributors").longValue());
					coin.setDevCommitCount4Weeks(developer_data.get("commit_count_4_weeks").longValue());
					coin.setDevCodeAdditions4Weeks(
							developer_data.get("code_additions_deletions_4_weeks").get("additions").longValue());
					coin.setDevCodeDeletions4Weeks(
							developer_data.get("code_additions_deletions_4_weeks").get("deletions").longValue());
					coin.setPublicAlexaRank(public_interest_stats.get("alexa_rank").longValue());

					String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(coin);

					System.out.println(jsonString);

					post("http://localhost:8080/api/v1/coin", jsonString);
					failCounter = 0;
				} catch (Exception e) {
					++failCounter;
					System.out.println("Error Occured: ");
					e.printStackTrace();
					Thread.sleep(2000);
					if (totalFailCounter >= 7) {
						totalFailCounter = 0;
						break;
					}
					if (failCounter >= 7) {
						++totalFailCounter;
						continue;
					} else {
						--i;
						continue;
					}
				}

			}
		}
		// System.out.println(readValue.toPrettyString());

//		JsonNode status = readValue.get("status");
//		System.out.println(status.toPrettyString());
//		
//		System.out.println("\n\n");
//		
//		JsonNode data = readValue.get("data");
//		for (JsonNode jsonNode : data) {
//			System.out.println(jsonNode.toPrettyString());
//		}
//		
//		String writeValueAsString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.body());
	}

	public static void fetchAndUpdateHistoricalData() throws Exception {
		Map<String, Date> lastValidDates = getLastValidDatesFromBackend();

		for (String coinId : cryptoIds) {
			Date lastValidDate = lastValidDates.get(coinId);
			System.out.println("-- last valid Date for: " + coinId + " is: " + lastValidDate);
			LocalDate startDate = lastValidDate != null ?
					lastValidDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1) :
					LocalDate.now().minusYears(1);  // Wenn kein Datum, starte von vor einem Jahr

			LocalDate endDate = LocalDate.now();
			System.out.println("-- end Date for: " + coinId + " is: " + endDate);

			while (!startDate.isAfter(endDate)) {
				String dateStr = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
				String url = String.format("%s/coins/%s/history?date=%s", COINGECKO_API_URL, coinId, dateStr);

				System.out.println("Calling URL: " + url);

				String response = getCoingeckoWithRetry(url);
				if (response != null) {
					Coin coin = parseCoinData(response, coinId, startDate);
					sendCoinDataToBackend(coin);
					startDate = startDate.plusDays(1);
				} else {
					System.out.println("Failed to fetch data for " + coinId + " on " + dateStr + " after multiple retries. Moving to next coin.");
					break;
				}

				Thread.sleep(1200);  // Respektiere Coingecko's Rate-Limit
			}
		}
	}

	private static void sendCoinDataToBackend(Coin coin) throws Exception {
		System.out.println(coin);

		ObjectMapper mapper = new ObjectMapper();
		String jsonCoin = mapper.writeValueAsString(coin);

		System.out.println("json coin:");
		System.out.println(jsonCoin);


		post(BACKEND_URL, jsonCoin);  // Wiederverwendung der bestehenden post-Methode
	}

	private static String getCoingeckoWithRetry(String uri) throws Exception {
		int retries = 0;
		int waitTime = INITIAL_WAIT_TIME;

		while (retries < MAX_RETRIES) {
			try {
				HttpClient client = HttpClient.newHttpClient();
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(uri))
						.header("Accept", "application/json")
						.build();

				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				int statusCode = response.statusCode();

				System.out.println("Status: " + statusCode);

				if (statusCode >= 200 && statusCode < 300) {
					return response.body();
				} else {
					throw new RuntimeException("HTTP error code: " + statusCode);
				}
			} catch (Exception e) {
				System.out.println("Error fetching data: " + e.getMessage() + ". Retrying in " + waitTime/1000 + " seconds...");
				Thread.sleep(waitTime);
				retries++;
				waitTime *= 2; // Exponential backoff
			}
		}

		System.out.println("Failed to fetch data after " + MAX_RETRIES + " retries.");
		return null;
	}

	private static Map<String, Date> getLastValidDatesFromBackend() throws Exception {
		Map<String, Date> result = new HashMap<>();
		for (String coinId : cryptoIds) {
			String url = BACKEND_URL + "/lastValidDate?coinId=" + coinId;
			String response = getCoingecko(url);  // Wiederverwendung der bestehenden Methode

			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(response);
			if (rootNode.has(coinId)) {
				String dateString = rootNode.get(coinId).asText();
				result.put(coinId, new SimpleDateFormat("yyyy-MM-dd").parse(dateString));
			}
		}
		return result;
	}

	private static Coin parseCoinData(String jsonData, String coinId, LocalDate date) throws Exception {
		if (jsonData == null || jsonData.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty or null JSON data");
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;
		try {
			root = mapper.readTree(jsonData);
		} catch (JsonParseException e) {
			System.out.println("Failed to parse JSON: " + jsonData);
			throw e;
		}

		Coin coin = new Coin();
		coin.setCoinId(coinId);
		coin.setTimestamp(Timestamp.valueOf(date.atStartOfDay()));

		coin.setSymbol(getTextSafely(root, "symbol"));
		coin.setCoinName(getTextSafely(root, "name"));

		JsonNode marketData = root.get("market_data");
		if (marketData != null) {
			JsonNode currentPrice = marketData.get("current_price");
			JsonNode marketCap = marketData.get("market_cap");
			JsonNode totalVolume = marketData.get("total_volume");

			coin.setPriceEur(getDecimalSafely(currentPrice, "eur"));
			coin.setPriceUsd(getDecimalSafely(currentPrice, "usd"));
			coin.setPriceBtc(getDecimalSafely(currentPrice, "btc"));
			coin.setPriceEth(getDecimalSafely(currentPrice, "eth"));

			coin.setMarketCapEur(getDecimalSafely(marketCap, "eur"));
			coin.setMarketCapUsd(getDecimalSafely(marketCap, "usd"));
			coin.setMarketCapBtc(getDecimalSafely(marketCap, "btc"));
			coin.setMarketCapEth(getDecimalSafely(marketCap, "eth"));

			coin.setTotalVolumeEur(getDecimalSafely(totalVolume, "eur"));
			coin.setTotalVolumeUsd(getDecimalSafely(totalVolume, "usd"));
			coin.setTotalVolumeBtc(getDecimalSafely(totalVolume, "btc"));
			coin.setTotalVolumeEth(getDecimalSafely(totalVolume, "eth"));
		}

		JsonNode communityData = root.get("community_data");
		if (communityData != null) {
			coin.setTwitterFollowers(getLongSafely(communityData, "twitter_followers", 0L));
			coin.setRedditAvgPosts48Hours(getDecimalSafely(communityData, "reddit_average_posts_48h"));
			coin.setRedditAvgComments48Hours(getDecimalSafely(communityData, "reddit_average_comments_48h"));
			coin.setRedditSubscribers(getLongSafely(communityData, "reddit_subscribers", 0L));
			coin.setRedditAccountsActive48Hours(getDecimalSafely(communityData, "reddit_accounts_active_48h"));
		}

		JsonNode developerData = root.get("developer_data");
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

		JsonNode publicInterestStats = root.get("public_interest_stats");
		if (publicInterestStats != null) {
			coin.setPublicAlexaRank(getLongSafely(publicInterestStats, "alexa_rank", 0L));
		}

		return coin;
	}

	// Hilfsmethoden für sicheres Abrufen von Werten
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

	public static int scounter = 0;

	public static void updateCryptos() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		// Aktualisiere die Kryptoliste
		parseCoingecko("coingecko.json", "portfoliocoingecko.json");

		String url = "https://api.coingecko.com/api/v3/simple/price?ids=%crypto%&vs_currencies=eur%2Cbtc%2Ceth%2Cusd&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true&include_last_updated_at=true";

		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("portfoliocoingecko.json")) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode coinsNode = mapper.readValue(in, JsonNode.class);

			for (JsonNode crypto : coinsNode) {
				String cryptoCoinId = crypto.get("id").asText();
				String cryptoCoinName = crypto.get("name").asText();
				String cryptoCoinSymbol = crypto.get("symbol").asText();

				int retries = 0;
				int waitTime = INITIAL_WAIT_TIME;

				while (retries < MAX_RETRIES) {
					try {
						Thread.sleep(waitTime);
						HttpResponse<String> response = getCoingeckoResponse(url.replace("%crypto%", cryptoCoinId));

						int statusCode = response.statusCode();
						if (statusCode >= 200 && statusCode < 300) {
							String currentCoinDetails = response.body();
							JsonNode currentCoinDetailsJsonNode = mapper.readValue(currentCoinDetails, JsonNode.class);
							JsonNode currentCoinJsonNodeDetails = currentCoinDetailsJsonNode.get(cryptoCoinId);

							if (currentCoinJsonNodeDetails == null) {
								throw new RuntimeException("Keine Daten zurückgegeben für " + cryptoCoinId);
							}

							long longdate = currentCoinJsonNodeDetails.get("last_updated_at").asLong();
							Timestamp timestmp = new Timestamp(longdate * 1000);

							Coin coin = new Coin();
							coin.setCoinId(cryptoCoinId);
							coin.setCoinName(cryptoCoinName);
							coin.setSymbol(cryptoCoinSymbol);
							coin.setTimestamp(timestmp);
							coin.setPriceEur(currentCoinJsonNodeDetails.get("eur").decimalValue());
							coin.setPriceUsd(currentCoinJsonNodeDetails.get("usd").decimalValue());
							coin.setPriceBtc(currentCoinJsonNodeDetails.get("btc").decimalValue());
							coin.setPriceEth(currentCoinJsonNodeDetails.get("eth").decimalValue());
							coin.setMarketCapEur(currentCoinJsonNodeDetails.get("eur_market_cap").decimalValue());
							coin.setMarketCapUsd(currentCoinJsonNodeDetails.get("usd_market_cap").decimalValue());
							coin.setMarketCapBtc(currentCoinJsonNodeDetails.get("btc_market_cap").decimalValue());
							coin.setMarketCapEth(currentCoinJsonNodeDetails.get("eth_market_cap").decimalValue());
							coin.setTotalVolumeEur(currentCoinJsonNodeDetails.get("eur_24h_vol").decimalValue());
							coin.setTotalVolumeUsd(currentCoinJsonNodeDetails.get("usd_24h_vol").decimalValue());
							coin.setTotalVolumeBtc(currentCoinJsonNodeDetails.get("btc_24h_vol").decimalValue());
							coin.setTotalVolumeEth(currentCoinJsonNodeDetails.get("eth_24h_vol").decimalValue());

							String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(coin);
							System.out.println(jsonString);

							post("http://localhost:8080/api/v1/coin", jsonString);
							break; // Erfolg, Schleife beenden
						} else {
							throw new RuntimeException("HTTP-Fehlercode: " + statusCode);
						}
					} catch (Exception e) {
						System.out.println("Fehler aufgetreten für " + cryptoCoinId + ": " + e.getMessage());
						retries++;
						if (retries >= MAX_RETRIES) {
							System.out.println("Maximale Anzahl von Versuchen erreicht für " + cryptoCoinId + ". Gehe zur nächsten Münze über.");
							break;
						}
						waitTime *= 2; // Exponentieller Rückzug
						System.out.println("Erneuter Versuch in " + waitTime / 1000 + " Sekunden...");
					}
				}
			}
		}
	}

	private static HttpResponse<String> getCoingeckoResponse(String uri) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(uri))
				.header("Accept", "application/json")
				.build();

		return client.send(request, BodyHandlers.ofString());
	}

	public static void main(String[] args) throws Exception {
////		parseCoingeckoUpdatable("testsource.json", "testtarget.json", true); // fetches the latest coin list
////		parseCoingecko("coingecko.json", "portfoliocoingecko.json"); // assumes the a coin list is in place
		updateCryptos();

		// fetchAndUpdateHistoricalData();
		//fetchAllHistoricalData(cryptoIds, 61);
	}

}
