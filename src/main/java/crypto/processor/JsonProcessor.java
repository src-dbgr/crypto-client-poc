package crypto.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Processor for JSON data operations.
 * This class provides methods for reading, writing, and parsing JSON data using Jackson ObjectMapper.
 */
public class JsonProcessor {
	private final ObjectMapper objectMapper;

	/**
	 * Constructs a new JsonProcessor with the specified ObjectMapper.
	 *
	 * @param objectMapper The ObjectMapper to be used for JSON operations
	 */
	public JsonProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Reads a JSON file from the classpath and returns it as a JsonNode.
	 *
	 * @param fileName The name of the file to read
	 * @return JsonNode representation of the file contents
	 * @throws IOException if an I/O error occurs reading from the file
	 */
	public JsonNode readJsonFromFile(String fileName) throws IOException {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
			return objectMapper.readTree(in);
		}
	}

	/**
	 * Writes an object as JSON to a file in the resources directory.
	 *
	 * @param fileName The name of the file to write to
	 * @param content The object to be written as JSON
	 * @throws IOException if an I/O error occurs writing to the file
	 */
	public void writeJsonToFile(String fileName, Object content) throws IOException {
		objectMapper.writeValue(new java.io.File("src/main/resources/" + fileName), content);
	}

	/**
	 * Parses a JSON string into a JsonNode.
	 *
	 * @param json The JSON string to parse
	 * @return JsonNode representation of the parsed JSON
	 * @throws IOException if the input is not valid JSON
	 */
	public JsonNode parseJson(String json) throws IOException {
		return objectMapper.readTree(json);
	}

	/**
	 * Gets the ObjectMapper instance used by this processor.
	 *
	 * @return The ObjectMapper instance
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}