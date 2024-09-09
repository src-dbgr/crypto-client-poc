# Crypto Client POC Application

This Crypto Client Proof of Concept (POC) application is designed to work in conjunction with the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project. It fetches cryptocurrency data from Coingecko.com, processes it, and sends it to the crypto-monitor-api-poc service for storage and monitoring purposes.

## Features

- Fetch current cryptocurrency data from Coingecko API
- Update historical data for multiple cryptocurrencies
- Parse data into [Coin](https://github.com/src-dbgr/crypto-monitor-api-poc/blob/main/src/main/java/com/sam/coin/model/Coin.java) format
- Post processed data to crypto-monitor-api-poc service
- Respect API rate limiting to prevent request throttling
- Robust error handling and retry mechanism
- Efficient data processing using CompletableFuture for asynchronous operations
- Logging for monitoring and debugging

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project set up and running

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

1. `updateCryptos()`: Updates all chosen and backend-enabled cryptos with current price and metadata information.

2. `fetchAllHistoricalData(int timeFrame)`: Updates historical data for all cryptocurrencies for the specified number of days, starting from today and going backwards.

3. `fetchAndUpdateHistoricalData()`: Fetches and updates historical data for all cryptocurrencies based on the last valid date from the backend.

To use the CryptoClient:

1. Ensure the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) is up and running (either in a Docker container or as a Java application).
2. Import this maven project into your IDE of choice.
3. Modify the `CRYPTO_IDS` list in the `CryptoClient` class to include the cryptocurrencies you're interested in.
4. In the `main` method of `CryptoClient`, uncomment or modify the operations you want to execute.
5. Run the `CryptoClient` as a Java application.

Example usage in `main` method:

```java
public static void main(String[] args) {
    try {
        LOG.info("Updating current crypto data...");
        updateCryptos();
        LOG.info("Current crypto data update completed.");

        LOG.info("Fetching and updating historical data...");
        // fetchAndUpdateHistoricalData();
        LOG.info("Historical data update completed.");

        LOG.info("Fetching all historical data for the last 60 days...");
        // fetchAllHistoricalData(60);
        LOG.info("All historical data fetch completed.");

    } catch (Exception e) {
        LOG.error("An error occurred", e);
    } finally {
        RATE_LIMITER.shutdown();
    }
}
```

## Configuration

The application uses several configuration constants that can be modified in the `CryptoClient` class:

- `BACKEND_URL`: The URL of your crypto-monitor-api-poc service
- `COINGECKO_API_URL`: The base URL for the Coingecko API
- `MAX_RETRIES`: Maximum number of retries for failed requests
- `RATE_LIMIT_DELAY`: Delay between requests to respect rate limiting
- `CRYPTO_IDS`: List of cryptocurrency IDs to fetch data for

## Contributing

Contributions to the CryptoClient project are welcome. Please follow these steps to contribute:

1. Fork the repository
2. Create a new branch (`git checkout -b feature/AmazingFeature`)
3. Make your changes
4. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
5. Push to the branch (`git push origin feature/AmazingFeature`)
6. Open a Pull Request

Please ensure your code adheres to the existing style conventions and includes appropriate test coverage.

## Contact

[devsam.io](https://devsam.io/contact/)

Project Link: [https://github.com/src-dbgr/crypto-client-poc](https://github.com/src-dbgr/crypto-client-poc)

## Related Project

This client application is tightly coupled with the [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) project, which stores the data in a PostgreSQL database and serves as the data source for Grafana Monitoring Dashboards.