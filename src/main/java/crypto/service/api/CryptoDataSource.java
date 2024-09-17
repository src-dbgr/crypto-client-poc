package crypto.service.api;

import com.sam.coin.domain.model.Coin;
import crypto.config.CryptoId;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Defines the contract for cryptocurrency data sources.
 * Implementations of this interface are responsible for fetching and sending
 * both current and historical cryptocurrency data.
 */
public interface CryptoDataSource {

    /**
     * Fetches and sends current data for specified cryptocurrencies.
     *
     * @param cryptoIds List of cryptocurrency IDs to fetch data for
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendCurrentData(List<String> cryptoIds, Consumer<Coin> sendToBackend) throws Exception;

    /**
     * Fetches and sends current data for a single cryptocurrency.
     *
     * @param cryptoId The cryptocurrency ID to fetch data for
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendCurrentData(CryptoId cryptoId, Consumer<Coin> sendToBackend) throws Exception;

    /**
     * Fetches and sends historical data for specified cryptocurrencies from their last valid dates.
     *
     * @param cryptoIds List of cryptocurrency IDs to fetch historical data for
     * @param lastValidDates Map of cryptocurrency IDs to their last valid dates
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendHistoricalData(List<String> cryptoIds, Map<String, Date> lastValidDates, Consumer<Coin> sendToBackend) throws Exception;

    /**
     * Fetches and sends historical data for a single cryptocurrency from its last valid date.
     *
     * @param cryptoId The cryptocurrency ID to fetch historical data for
     * @param lastValidDate The last valid date for the cryptocurrency
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendHistoricalData(CryptoId cryptoId, Date lastValidDate, Consumer<Coin> sendToBackend) throws Exception;

    /**
     * Fetches and sends all historical data for specified cryptocurrencies within a given time frame.
     *
     * @param cryptoIds List of cryptocurrency IDs to fetch all historical data for
     * @param timeFrame Number of days in the past to fetch data for
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendAllHistoricalData(List<String> cryptoIds, int timeFrame, Consumer<Coin> sendToBackend) throws Exception;

    /**
     * Fetches and sends all historical data for a single cryptocurrency within a given time frame.
     *
     * @param cryptoId The cryptocurrency ID to fetch all historical data for
     * @param timeFrame Number of days in the past to fetch data for
     * @param sendToBackend Consumer function to send processed data to the backend
     * @throws Exception if an error occurs during data fetching or sending
     */
    void fetchAndSendAllHistoricalData(CryptoId cryptoId, int timeFrame, Consumer<Coin> sendToBackend) throws Exception;
}