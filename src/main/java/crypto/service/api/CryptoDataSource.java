package crypto.service.api;

import com.sam.coin.domain.model.Coin;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface CryptoDataSource {
    void fetchAndSendCurrentData(List<String> cryptoIds, Consumer<Coin> sendToBackend) throws Exception;
    void fetchAndSendHistoricalData(List<String> cryptoIds, Map<String, Date> lastValidDates, Consumer<Coin> sendToBackend) throws Exception;
    void fetchAndSendAllHistoricalData(List<String> cryptoIds, int timeFrame, Consumer<Coin> sendToBackend) throws Exception;
}
