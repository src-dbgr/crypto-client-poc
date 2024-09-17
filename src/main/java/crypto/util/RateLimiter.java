package crypto.util;

/**
 * Rate limiter to control the frequency of API requests.
 * This class ensures that requests are not sent more frequently than the specified delay.
 */
public class RateLimiter {
	private final long delayMs;
	private long lastRequestTime = 0;

	/**
	 * Constructs a new RateLimiter with the specified delay.
	 *
	 * @param delayMs The minimum delay between requests in milliseconds
	 */
	public RateLimiter(long delayMs) {
		this.delayMs = delayMs;
	}

	/**
	 * Acquires a permit to make an API request, potentially waiting if necessary.
	 * This method ensures that the specified delay has passed since the last request.
	 *
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	public synchronized void acquire() throws InterruptedException {
		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - lastRequestTime;
		if (elapsedTime < delayMs) {
			long sleepTime = delayMs - elapsedTime;
			Thread.sleep(sleepTime);
		}
		lastRequestTime = System.currentTimeMillis();
	}
}