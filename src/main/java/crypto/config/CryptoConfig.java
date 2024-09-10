package crypto.config;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for the CryptoClient.
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

	public String getBackendUrl() { return backendUrl; }
	public String getCoingeckoApiUrl() { return coingeckoApiUrl; }
	public int getMaxRetries() { return maxRetries; }
	public long getRateLimitDelay() { return rateLimitDelay; }
	public String getSourceFile() { return sourceFile; }
	public String getTargetFile() { return targetFile; }
	public List<String> getCryptoIds() { return cryptoIds; }
}