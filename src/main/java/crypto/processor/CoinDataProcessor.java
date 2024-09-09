package crypto.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.coin.model.Coin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Processor for coin data.
 */
public class CoinDataProcessor {
	/**
	 * Creates a Coin object from JsonNode data.
	 *
	 * @param cryptoId ID of the cryptocurrency
	 * @param cryptoName Name of the cryptocurrency
	 * @param cryptoSymbol Symbol of the cryptocurrency
	 * @param coinData JsonNode containing the coin data
	 * @return Coin object with the processed data
	 */
	public Coin createCoinFromJsonNode(String cryptoId, String cryptoName, String cryptoSymbol, JsonNode coinData) {
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
