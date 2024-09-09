# Crypto Client POC Application

This Crypto Client Proof of Concept (POC) application is designed to work in conjunction with the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project. It fetches cryptocurrency data from Coingecko.com, processes it, and sends it to the crypto-monitor-api-poc service for storage and monitoring purposes.

## Features

- Fetch current cryptocurrency data from Coingecko API
- Update historical data for multiple cryptocurrencies
- Parse data into [Coin](https://github.com/src-dbgr/crypto-monitor-api-poc/blob/main/src/main/java/com/sam/coin/model/Coin.java) format
- Post processed data to crypto-monitor-api-poc service
- Respect API rate limiting to prevent request throttling
- Robust error handling and retry mechanism
- Efficient data processing
- Comprehensive logging for monitoring and debugging

## Coin Object

The Coin object is a crucial component of this client application. It serves as the data model for cryptocurrency information and is used for communication with the crypto-monitor-api-poc service.

The Coin class is defined in the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project and is included as a dependency in this client. The client application populates Coin objects with data fetched from Coingecko and sends these objects to the server, which expects this specific format.

Key fields of the Coin object include:
- `coinId`: Unique identifier for the cryptocurrency
- `coinName`: Name of the cryptocurrency
- `symbol`: Symbol of the cryptocurrency
- `priceUsd`, `priceEur`, `priceBtc`, `priceEth`: Current prices in various currencies
- `marketCapUsd`, `marketCapEur`, `marketCapBtc`, `marketCapEth`: Market capitalization data
- `timestamp`: Time of data retrieval

For a complete list of fields and methods, refer to the [Coin.java](https://github.com/src-dbgr/crypto-monitor-api-poc/blob/main/src/main/java/com/sam/coin/model/Coin.java) file in the crypto-monitor-api-poc project.

## Prerequisites

Before setting up and running the Crypto Client POC, ensure you have the following:

- Java 11 or higher
- Maven 3.6 or higher
- [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project set up and built:
   - Clone the crypto-monitor-api-poc repository
   - Navigate to the crypto-monitor-api-poc directory
   - Run `mvn clean install` to build the project and install its artifacts in your local Maven repository
      - This step is crucial as the Crypto Client POC depends on classes (such as the Coin object) from the crypto-monitor-api-poc project
- The crypto-monitor-api-poc service should be running and accessible:
   - Start the crypto-monitor-api-poc service (either in a Docker container or as a standalone Java application)
   - Ensure the service is accessible at `http://localhost:8080`
      - This is the default URL the Crypto Client POC will use to communicate with the backend service
   - If you need to use a different URL, update the `backendUrl` in the `CryptoConfig` class accordingly

Ensuring these prerequisites are met will allow for smooth compilation, execution, and proper functionality of the Crypto Client POC application.

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/src-dbgr/crypto-client-poc.git
   ```

2. Navigate to the project directory:
   ```
   cd crypto-client-poc
   ```

3. Build the project using Maven:
   ```
   mvn clean install
   ```

## Usage

The CryptoClient offers the following main operations:

1. `updateCurrentData()`: Updates all chosen and backend-enabled cryptos with current price and metadata information.

2. `updateHistoricalData()`: Fetches and updates historical data for all cryptocurrencies based on the last valid date from the backend.

3. `fetchAllHistoricalData(int timeFrame)`: Updates historical data for all cryptocurrencies for the specified number of days, starting from today and going backwards.

To use the CryptoClient:

1. Ensure the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) is up and running.
2. Modify the `cryptoIds` list in the `CryptoConfig` class to include the cryptocurrencies you're interested in.
3. In the `main` method of `CryptoClient`, uncomment or modify the operations you want to execute.
   > the major methods are:
   > * client.updateCurrentData()
   > * client.updateHistoricalData();
   > * client.fetchAllHistoricalData({time frame});
4. Run the `CryptoClient` as a Java application.

Example usage in `main` method:

```java
public static void main(String[] args) {
    // ... initialization code ...

    CryptoClient client = new CryptoClient(config, dataSource, backendService);

    try {
        LOG.info("Updating current crypto data...");
        client.updateCurrentData();
        LOG.info("Current crypto data update completed.");

        LOG.info("Fetching and updating historical data...");
        // client.updateHistoricalData();
        LOG.info("Historical data update completed.");

        LOG.info("Fetching all historical data for the last 60 days...");
        // client.fetchAllHistoricalData(60);
        LOG.info("All historical data fetch completed.");

    } catch (Exception e) {
        LOG.error("An error occurred", e);
    }
}
```

## Configuration

The application configuration is managed through the `CryptoConfig` class. This class contains several important settings:

- `backendUrl`: The URL of your crypto-monitor-api-poc service
- `coingeckoApiUrl`: The base URL for the Coingecko API
- `maxRetries`: Maximum number of retries for failed requests
- `rateLimitDelay`: Delay between requests to respect rate limiting
- `cryptoIds`: List of cryptocurrency IDs to fetch data for

### Cryptocurrency IDs

The `cryptoIds` list in `CryptoConfig` specifies which cryptocurrencies the application will track. It's crucial that these IDs match exactly with the identifiers used by the Coingecko API. For example:

```java
private final List<String> cryptoIds = Arrays.asList(
    "bitcoin", "ethereum", "cardano", "polkadot", "chainlink",
    "stellar", "zcash", "algorand", "bitcoin-diamond", "litecoin",
    // ... other cryptocurrencies ...
);
```

To add or remove cryptocurrencies from tracking, modify this list in the `CryptoConfig` class.

## API Limitations and Rate Limiting

Please note that Coingecko has recently imposed significant restrictions on their public API. These limitations may result in rate limiting issues when fetching data. To mitigate this, the application implements a `RateLimiter` class that enforces delays between API requests:

```java
public class RateLimiter {
    private final long delayMs;
    // ... implementation ...
}
```

The delay between requests is configurable via the `rateLimitDelay` parameter in `CryptoConfig`. Adjust this value if you encounter rate limiting issues:

```java
private final long rateLimitDelay = 5000; // 5 seconds delay
```
A retry mechanism with growing delays is in place. Despite these precautions, you may still experience limitations when using the Coingecko public API extensively. Consider using their pro services for more reliable and extensive data fetching capabilities in a production environment.

## Configuration

The application uses several configuration constants that can be modified in the `CryptoConfig` class:

- `backendUrl`: The URL of your crypto-monitor-api-poc service
- `coingeckoApiUrl`: The base URL for the Coingecko API
- `maxRetries`: Maximum number of retries for failed requests
- `rateLimitDelay`: Delay between requests to respect rate limiting
- `cryptoIds`: List of cryptocurrency IDs to fetch data for

## Contact

[devsam.io](https://devsam.io/contact/)

Project Link: [https://github.com/src-dbgr/crypto-client-poc](https://github.com/src-dbgr/crypto-client-poc)

## Related Project

This client application is tightly coupled with the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project, which stores the data in a PostgreSQL database and serves as the data source for Grafana Monitoring Dashboards.