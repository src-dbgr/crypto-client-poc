package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sam.coin.domain.model.Coin;
import crypto.config.CryptoConfig;
import crypto.processor.CoinDataProcessor;
import crypto.processor.JsonProcessor;
import crypto.service.api.CryptoDataSource;
import crypto.util.HttpClientWrapper;
import crypto.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of CryptoDataSource that fetches data from the CoinGecko API.
 * This class handles fetching both current and historical cryptocurrency data.
 */
public class CoinGeckoService implements CryptoDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(CoinGeckoService.class);
    public static final int COIN_GECKO_MAX_PAST_DAYS = 365;
    private final CryptoConfig config;
    private final HttpClientWrapper httpClient;
    private final JsonProcessor jsonProcessor;
    private final CoinDataProcessor coinDataProcessor;
    private final RateLimiter rateLimiter;

    /**
     * Constructs a new CoinGeckoService with the specified dependencies.
     *
     * @param config Configuration for the service
     * @param httpClient HTTP client wrapper for making API requests
     * @param jsonProcessor Processor for JSON data
     * @param coinDataProcessor Processor for coin data
     * @param rateLimiter Rate limiter to control API request frequency
     */
    public CoinGeckoService(CryptoConfig config, HttpClientWrapper httpClient, JsonProcessor jsonProcessor,
                            CoinDataProcessor coinDataProcessor, RateLimiter rateLimiter) {
        this.config = config;
        this.httpClient = httpClient;
        this.jsonProcessor = jsonProcessor;
        this.coinDataProcessor = coinDataProcessor;
        this.rateLimiter = rateLimiter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchAndSendCurrentData(List<String> cryptoIds, Consumer<Coin> sendToBackend) throws IOException, InterruptedException {
        for (String cryptoId : cryptoIds) {
            String url = String.format("%s/coins/%s", config.getCoingeckoApiUrl(), cryptoId);
            processCryptoData(url, cryptoId, sendToBackend);
            rateLimiter.acquire();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchAndSendHistoricalData(List<String> cryptoIds, Map<String, Date> lastValidDates, Consumer<Coin> sendToBackend) throws Exception {
        for (String coinId : cryptoIds) {
            Date lastValidDate = lastValidDates.get(coinId);
            LocalDate startDate = determineStartDate(lastValidDate, coinId);
            LocalDate endDate = LocalDate.now();

            while (!startDate.isAfter(endDate)) {
                String dateStr = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String url = String.format("%s/coins/%s/history?date=%s", config.getCoingeckoApiUrl(), coinId, dateStr);

                processHistoricalData(url, coinId, startDate, sendToBackend);

                startDate = startDate.plusDays(1);
                rateLimiter.acquire();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchAndSendAllHistoricalData(List<String> cryptoIds, int timeFrame, Consumer<Coin> sendToBackend) throws Exception {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(timeFrame - 1);

        for (String coinId : cryptoIds) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateString = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String url = String.format("%s/coins/%s/history?date=%s", config.getCoingeckoApiUrl(), coinId, dateString);

                processHistoricalData(url, coinId, date, sendToBackend);

                rateLimiter.acquire();
            }
        }
    }

    /**
     * Determines the start date for fetching historical data based on the last valid date.
     * If the last valid date is more than 365 days in the past, it adjusts the start date
     * to comply with CoinGecko's limitation of retrieving data for only up to 365 days in the past.
     *
     * @param lastValidDate The last valid date for the given coin
     * @param coinId The ID of the coin for logging purposes
     * @return The start date for historical data retrieval
     */
    LocalDate determineStartDate(Date lastValidDate, String coinId) {
        LocalDate maxPastDate = LocalDate.now().minusDays(COIN_GECKO_MAX_PAST_DAYS);

        if (lastValidDate != null) {
            LocalDate lastValidLocalDate = lastValidDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastValidLocalDate.isBefore(maxPastDate)) {
                LOG.info(String.format("The last valid date for coin %s is more than 365 days in the past. Adjusting start date to %s.",
                        coinId, maxPastDate));
                return maxPastDate.plusDays(1);
            } else {
                return lastValidLocalDate.plusDays(1);
            }
        } else {
            return maxPastDate;
        }
    }

    /**
     * Processes cryptocurrency data from the given URL and sends it to the backend.
     *
     * @param url The URL to fetch the cryptocurrency data from
     * @param cryptoId The ID of the cryptocurrency
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws InterruptedException if the thread is interrupted while waiting between retries
     */
    private void processCryptoData(String url, String cryptoId, Consumer<Coin> sendToBackend) throws InterruptedException {
        LOG.info("Process crypto data for Crypto {}", cryptoId);
        for (int retryCount = 0; retryCount < config.getMaxRetries(); retryCount++) {
            try {
                String response = httpClient.sendGetRequest(url);
                JsonNode rootNode = jsonProcessor.parseJson(response);
                if (rootNode != null) {
                    Coin coin = coinDataProcessor.createCoinFromJsonNode(cryptoId, rootNode);
                    sendToBackend.accept(coin);
                } else {
                    LOG.warn("No data returned for {}", cryptoId);
                }
                return;
            } catch (Exception e) {
                LOG.warn("Issue occurred for {}: {}", cryptoId, e.getMessage(), e);
                if (retryCount == config.getMaxRetries() - 1) {
                    LOG.error("Max retries reached for {}. Moving to next coin.", cryptoId);
                    return;
                } else {
                    long delay = config.getRateLimitDelay() * (long) (retryCount + 1);
                    LOG.info("Retrying in {} milliseconds...", delay);
                    TimeUnit.MILLISECONDS.sleep(delay);  // Backoff
                }
            }
        }
    }

    /**
     * Processes historical cryptocurrency data from the given URL and sends it to the backend.
     *
     * @param url The URL to fetch the historical cryptocurrency data from
     * @param coinId The ID of the cryptocurrency
     * @param date The date for which to fetch historical data
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws InterruptedException if the thread is interrupted while waiting between retries
     */
    private void processHistoricalData(String url, String coinId, LocalDate date, Consumer<Coin> sendToBackend) throws InterruptedException {
        LOG.info("Process historical Coin Data for Crypto: {} and Date: {}", coinId, date);
        for (int retryCount = 0; retryCount < config.getMaxRetries(); retryCount++) {
            try {
                String response = httpClient.sendGetRequest(url);
                Coin coin = coinDataProcessor.parseCoinData(response, coinId, date);
                sendToBackend.accept(coin);
                return;
            } catch (Exception e) {
                LOG.warn("Error occurred for {} on {}: {}", coinId, date, e.getMessage(), e);
                if (retryCount == config.getMaxRetries() - 1) {
                    LOG.error("Max retries reached for {} on {}. Moving to next date.", coinId, date);
                    return;
                } else {
                    long delay = config.getRateLimitDelay() * (long) (retryCount + 1);
                    LOG.info("Rate Limiting hit. Retrying in {} milliseconds...", delay);
                    TimeUnit.MILLISECONDS.sleep(delay);  // Backoff
                }
            }
        }
    }
}