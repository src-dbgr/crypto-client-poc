package crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sam.coin.model.Coin;
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
import java.util.function.Consumer;

public class CoinGeckoService  implements CryptoDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(CoinGeckoService.class);
    private final CryptoConfig config;
    private final HttpClientWrapper httpClient;
    private final JsonProcessor jsonProcessor;
    private final CoinDataProcessor coinDataProcessor;
    private final RateLimiter rateLimiter;

    public CoinGeckoService(CryptoConfig config, HttpClientWrapper httpClient, JsonProcessor jsonProcessor,
                            CoinDataProcessor coinDataProcessor, RateLimiter rateLimiter) {
        this.config = config;
        this.httpClient = httpClient;
        this.jsonProcessor = jsonProcessor;
        this.coinDataProcessor = coinDataProcessor;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void fetchAndSendCurrentData(List<String> cryptoIds, Consumer<Coin> sendToBackend) throws IOException, InterruptedException {
        String url = config.getCoingeckoApiUrl() + "/simple/price?ids=%s&vs_currencies=eur,btc,eth,usd&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true&include_last_updated_at=true";

        for (String cryptoId : cryptoIds) {
            String formattedUrl = String.format(url, cryptoId);
            processCryptoData(formattedUrl, cryptoId, sendToBackend);
            rateLimiter.acquire();
        }
    }

    private void processCryptoData(String url, String cryptoId, Consumer<Coin> sendToBackend) throws InterruptedException {
        LOG.info("Process crypto data for Crypto {}", cryptoId);
        for (int retryCount = 0; retryCount < config.getMaxRetries(); retryCount++) {
            try {
                String response = httpClient.sendGetRequest(url);
                JsonNode rootNode = jsonProcessor.parseJson(response);
                JsonNode coinData = rootNode.get(cryptoId);
                if (coinData != null) {
                    String coinInfoUrl = String.format("%s/coins/%s", config.getCoingeckoApiUrl(), cryptoId);
                    String coinInfoResponse = httpClient.sendGetRequest(coinInfoUrl);
                    JsonNode coinInfo = jsonProcessor.parseJson(coinInfoResponse);
                    String cryptoName = coinInfo.path("name").asText("");
                    String cryptoSymbol = coinInfo.path("symbol").asText("");

                    Coin coin = coinDataProcessor.createCoinFromJsonNode(cryptoId, cryptoName, cryptoSymbol, coinData);
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
                    Thread.sleep(delay);  // Exponential backoff
                }
            }
        }
    }

    @Override
    public void fetchAndSendHistoricalData(List<String> cryptoIds, Map<String, Date> lastValidDates, Consumer<Coin> sendToBackend) throws Exception {
        for (String coinId : cryptoIds) {
            Date lastValidDate = lastValidDates.get(coinId);
            LocalDate startDate = lastValidDate != null ?
                    lastValidDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1) :
                    LocalDate.now().minusYears(1);
            LocalDate endDate = LocalDate.now();

            while (!startDate.isAfter(endDate)) {
                String dateStr = startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String url = String.format("%s/coins/%s/history?date=%s", config.getCoingeckoApiUrl(), coinId, dateStr);

                String response = httpClient.sendGetRequest(url);
                Coin coin = coinDataProcessor.parseCoinData(response, coinId, startDate);
                sendToBackend.accept(coin);

                startDate = startDate.plusDays(1);
                rateLimiter.acquire();
            }
        }
    }

    private void processHistoricalData(String url, String coinId, LocalDate date, Consumer<Coin> sendToBackend) throws Exception {
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
                    Thread.sleep(delay);  // Exponential backoff
                }
            }
        }
    }

    @Override
    public void fetchAndSendAllHistoricalData(List<String> cryptoIds, int timeFrame, Consumer<Coin> sendToBackend) throws Exception {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(timeFrame - 1);

        for (String coinId : cryptoIds) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateString = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                String url = String.format("%s/coins/%s/history?date=%s", config.getCoingeckoApiUrl(), coinId, dateString);

                String response = httpClient.sendGetRequest(url);
                Coin coin = coinDataProcessor.parseCoinData(response, coinId, date);
                sendToBackend.accept(coin);

                rateLimiter.acquire();
            }
        }
    }

}