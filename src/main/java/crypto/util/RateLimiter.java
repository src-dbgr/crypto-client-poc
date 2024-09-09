package crypto.util;

/**
 * Rate limiter to control the frequency of API requests.
 */
public class RateLimiter {
	private final long delayMs;
	private long lastRequestTime = 0;

	/**
	 * Constructor for RateLimiter.
	 *
	 * @param delayMs Delay in milliseconds between requests
	 */
	public RateLimiter(long delayMs) {
		this.delayMs = delayMs;
	}

	/**
	 * Acquires a permit to make an API request, potentially waiting if necessary.
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