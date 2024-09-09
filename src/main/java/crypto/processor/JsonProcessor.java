package crypto.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Processor for JSON data.
 */
public class JsonProcessor {
	private final ObjectMapper objectMapper;

	public JsonProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public JsonNode readJsonFromFile(String fileName) throws IOException {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
			return objectMapper.readTree(in);
		}
	}

	public void writeJsonToFile(String fileName, Object content) throws IOException {
		objectMapper.writeValue(new java.io.File("src/main/resources/" + fileName), content);
	}

	public JsonNode parseJson(String json) throws IOException {
		return objectMapper.readTree(json);
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}
