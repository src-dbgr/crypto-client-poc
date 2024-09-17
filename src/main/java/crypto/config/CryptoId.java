package crypto.config;

/**
 * Enumeration of supported cryptocurrency IDs.
 * This enum provides a centralized list of all supported cryptocurrencies
 * and their corresponding IDs used in the CoinGecko API.
 */
public enum CryptoId {
    BITCOIN("bitcoin"),
    ETHEREUM("ethereum"),
    CARDANO("cardano"),
    POLKADOT("polkadot"),
    CHAINLINK("chainlink"),
    STELLAR("stellar"),
    ZCASH("zcash"),
    ALGORAND("algorand"),
    BITCOIN_DIAMOND("bitcoin-diamond"),
    LITECOIN("litecoin"),
    COMPOUND_ETHER("compound-ether"),
    COMPOUND_COIN("compound-coin"),
    BZX_PROTOCOL("bzx-protocol"),
    BAND_PROTOCOL("band-protocol"),
    AMPLEFORTH("ampleforth"),
    ZILLIQA("zilliqa"),
    VECHAIN("vechain"),
    WAVES("waves"),
    UMA("uma"),
    OCEAN_PROTOCOL("ocean-protocol"),
    THETA_TOKEN("theta-token"),
    SINGULARITYNET("singularitynet"),
    THORCHAIN("thorchain"),
    KAVA("kava");

    private final String id;

    CryptoId(String id) {
        this.id = id;
    }

    /**
     * Gets the CoinGecko API ID for this cryptocurrency.
     * @return The CoinGecko API ID
     */
    public String getId() {
        return id;
    }
}