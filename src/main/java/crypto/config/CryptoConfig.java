package crypto.config;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for the CryptoClient.
 * This class holds various configuration parameters used throughout the application.
 */
public class CryptoConfig {
	private final String backendUrl = "http://localhost:8080/api/v1/coins";
	private final String coingeckoApiUrl = "https://api.coingecko.com/api/v3";
	private final int maxRetries = 10;
	private final long rateLimitDelay = 5000;
	private final String sourceFile = "coingecko.json";
	private final String targetFile = "portfoliocoingecko.json";
	private final List<String> cryptoIds = Arrays.asList(
			"bitcoin", "ethereum", "cardano", "polkadot", "chainlink",
			"stellar", "zcash", "algorand", "bitcoin-diamond", "litecoin",
			"compound-ether", "compound-coin", "bzx-protocol", "band-protocol",
			"ampleforth", "zilliqa", "vechain", "waves", "uma", "ocean-protocol",
			"theta-token", "singularitynet", "thorchain", "kava"
	);

	/**
	 * Gets the URL of the backend API.
	 * @return The backend URL
	 */
	public String getBackendUrl() { return backendUrl; }

	/**
	 * Gets the URL of the CoinGecko API.
	 * @return The CoinGecko API URL
	 */
	public String getCoingeckoApiUrl() { return coingeckoApiUrl; }

	/**
	 * Gets the maximum number of retries for API requests.
	 * @return The maximum number of retries
	 */
	public int getMaxRetries() { return maxRetries; }

	/**
	 * Gets the delay between rate-limited requests in milliseconds.
	 * @return The rate limit delay
	 */
	public long getRateLimitDelay() { return rateLimitDelay; }

	/**
	 * Gets the name of the source file for JSON data.
	 * @return The source file name
	 */
	public String getSourceFile() { return sourceFile; }

	/**
	 * Gets the name of the target file for processed JSON data.
	 * @return The target file name
	 */
	public String getTargetFile() { return targetFile; }

	/**
	 * Gets the list of cryptocurrency IDs to be processed.
	 * @return The list of cryptocurrency IDs
	 */
	public List<String> getCryptoIds() { return cryptoIds; }
}