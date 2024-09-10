package crypto.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.coin.model.Coin;
import crypto.service.CoinGeckoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Processor for coin data.
 */
public class CoinDataProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(CoinDataProcessor.class);

	/**
	 * Creates a Coin object from JsonNode data.
	 *
	 * @param cryptoId ID of the cryptocurrency
	 * @param rootNode JsonNode containing the coin data
	 * @return Coin object with the processed data
	 */
	public Coin createCoinFromJsonNode(String cryptoId, JsonNode rootNode) {
		Coin coin = new Coin();
		coin.setCoinId(cryptoId);
		coin.setCoinName(getTextSafely(rootNode, "name"));
		coin.setSymbol(getTextSafely(rootNode, "symbol"));

		JsonNode marketData = rootNode.path("market_data");
		if (!marketData.isMissingNode()) {
			String lastUpdatedStr = getTextSafely(marketData, "last_updated");
			coin.setTimestamp(parseTimestamp(lastUpdatedStr));

			JsonNode currentPrice = marketData.path("current_price");
			coin.setPriceEur(getDecimalSafely(currentPrice, "eur"));
			coin.setPriceUsd(getDecimalSafely(currentPrice, "usd"));
			coin.setPriceBtc(getDecimalSafely(currentPrice, "btc"));
			coin.setPriceEth(getDecimalSafely(currentPrice, "eth"));

			JsonNode marketCap = marketData.path("market_cap");
			coin.setMarketCapEur(getDecimalSafely(marketCap, "eur"));
			coin.setMarketCapUsd(getDecimalSafely(marketCap, "usd"));
			coin.setMarketCapBtc(getDecimalSafely(marketCap, "btc"));
			coin.setMarketCapEth(getDecimalSafely(marketCap, "eth"));

			JsonNode totalVolume = marketData.path("total_volume");
			coin.setTotalVolumeEur(getDecimalSafely(totalVolume, "eur"));
			coin.setTotalVolumeUsd(getDecimalSafely(totalVolume, "usd"));
			coin.setTotalVolumeBtc(getDecimalSafely(totalVolume, "btc"));
			coin.setTotalVolumeEth(getDecimalSafely(totalVolume, "eth"));
		}

		return coin;
	}

	private Timestamp parseTimestamp(String dateTimeStr) {
		try {
			Instant instant = Instant.parse(dateTimeStr);
			return new Timestamp(instant.toEpochMilli());
		} catch (DateTimeParseException e) {
			LOG.error("Failed to parse timestamp: {}. Using current time as fallback.", dateTimeStr, e);
			return new Timestamp(System.currentTimeMillis());
		}
	}

	/**
	 * Parses coin data from JSON string.
	 *
	 * @param jsonData JSON string containing coin data
	 * @param coinId ID of the cryptocurrency
	 * @param date Date of the historical data
	 * @return Coin object with the parsed data
	 * @throws IOException if there's an error parsing the JSON data
	 */
	public Coin parseCoinData(String jsonData, String coinId, LocalDate date) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(jsonData);
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

	private void setCoinPrices(Coin coin, JsonNode priceNode) {
		if (priceNode != null) {
			coin.setPriceEur(getDecimalSafely(priceNode, "eur"));
			coin.setPriceUsd(getDecimalSafely(priceNode, "usd"));
			coin.setPriceBtc(getDecimalSafely(priceNode, "btc"));
			coin.setPriceEth(getDecimalSafely(priceNode, "eth"));
		}
	}

	private void setCoinMarketCaps(Coin coin, JsonNode marketCapNode) {
		if (marketCapNode != null) {
			coin.setMarketCapEur(getDecimalSafely(marketCapNode, "eur"));
			coin.setMarketCapUsd(getDecimalSafely(marketCapNode, "usd"));
			coin.setMarketCapBtc(getDecimalSafely(marketCapNode, "btc"));
			coin.setMarketCapEth(getDecimalSafely(marketCapNode, "eth"));
		}
	}

	private void setCoinVolumes(Coin coin, JsonNode volumeNode) {
		if (volumeNode != null) {
			coin.setTotalVolumeEur(getDecimalSafely(volumeNode, "eur"));
			coin.setTotalVolumeUsd(getDecimalSafely(volumeNode, "usd"));
			coin.setTotalVolumeBtc(getDecimalSafely(volumeNode, "btc"));
			coin.setTotalVolumeEth(getDecimalSafely(volumeNode, "eth"));
		}
	}

	private void setCommunityData(Coin coin, JsonNode communityData) {
		if (communityData != null) {
			coin.setTwitterFollowers(getLongSafely(communityData, "twitter_followers"));
			coin.setRedditAvgPosts48Hours(getDecimalSafely(communityData, "reddit_average_posts_48h"));
			coin.setRedditAvgComments48Hours(getDecimalSafely(communityData, "reddit_average_comments_48h"));
			coin.setRedditSubscribers(getLongSafely(communityData, "reddit_subscribers"));
			coin.setRedditAccountsActive48Hours(getDecimalSafely(communityData, "reddit_accounts_active_48h"));
		}
	}

	private void setDeveloperData(Coin coin, JsonNode developerData) {
		if (developerData != null) {
			coin.setDevForks(getLongSafely(developerData, "forks"));
			coin.setDevStars(getLongSafely(developerData, "stars"));
			coin.setDevTotalIssues(getLongSafely(developerData, "total_issues"));
			coin.setDevClosedIssues(getLongSafely(developerData, "closed_issues"));
			coin.setDevPullRequestsMerged(getLongSafely(developerData, "pull_requests_merged"));
			coin.setDevPullRequestContributors(getLongSafely(developerData, "pull_request_contributors"));
			coin.setDevCommitCount4Weeks(getLongSafely(developerData, "commit_count_4_weeks"));

			JsonNode codeAdditionsDeletions4Weeks = developerData.get("code_additions_deletions_4_weeks");
			if (codeAdditionsDeletions4Weeks != null) {
				coin.setDevCodeAdditions4Weeks(getLongSafely(codeAdditionsDeletions4Weeks, "additions"));
				coin.setDevCodeDeletions4Weeks(getLongSafely(codeAdditionsDeletions4Weeks, "deletions"));
			}
		}
	}

	private void setPublicInterestStats(Coin coin, JsonNode publicInterestStats) {
		if (publicInterestStats != null) {
			coin.setPublicAlexaRank(getLongSafely(publicInterestStats, "alexa_rank"));
		}
	}

	private BigDecimal getDecimalSafely(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null && field.isNumber()) ? field.decimalValue() : BigDecimal.ZERO;
	}

	private Long getLongSafely(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null && field.isNumber()) ? field.longValue() : 0L;
	}

	private String getTextSafely(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null) ? field.asText() : "";
	}
}
