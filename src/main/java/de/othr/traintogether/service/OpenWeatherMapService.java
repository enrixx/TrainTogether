package de.othr.traintogether.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.othr.traintogether.dto.WeatherDto;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Service
public class OpenWeatherMapService {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherMapService.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openweathermap.api-key:}")
    private String apiKey;

    // time windows
    private static final long PAST_SECONDS = 2L * 24 * 3600; // 2 days in the past allowed
    private static final long FUTURE_SECONDS = 5L * 24 * 3600; // forecast limit: ~5 days

    // Fetch weather near given lat/lon for a specific course time.
    // Returns Optional.empty() when API key missing, timestamp implausible or request fails.
    public Optional<WeatherDto> fetchWeather(double lat, double lon, long courseEpochSeconds) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenWeatherMap API key not configured");
            return Optional.empty();
        }

        if (isTimestampOutOfRange(courseEpochSeconds)) {
            log.debug("Course timestamp {} not plausible for weather lookup", courseEpochSeconds);
            return Optional.empty();
        }

        long now = Instant.now().getEpochSecond();
        long diff = Math.abs(courseEpochSeconds - now);

        try {
            if (diff <= 3600) {
                Optional<WeatherDto> cur = fetchCurrent(lat, lon);
                if (cur.isPresent()) {
                    return cur;
                }
            }
            return fetchForecast(lat, lon, courseEpochSeconds);
        } catch (IOException e) {
            log.debug("Error fetching weather: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // --- internal helpers ---
    private Optional<WeatherDto> fetchCurrent(double lat, double lon) throws IOException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.openweathermap.org")
                .addPathSegment("data")
                .addPathSegment("2.5")
                .addPathSegment("weather")
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .addQueryParameter("units", "metric")
                .addQueryParameter("appid", apiKey)
                .build();

        Request req = new Request.Builder().url(url).get().build();
        try (Response res = client.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                log.debug("Current weather request failed: {}", res.code());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(res.body().byteStream());
            return Optional.ofNullable(parseWeatherNode(root, false));
        }
    }

    private Optional<WeatherDto> fetchForecast(double lat, double lon, long targetEpochSeconds) throws IOException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.openweathermap.org")
                .addPathSegment("data")
                .addPathSegment("2.5")
                .addPathSegment("forecast")
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .addQueryParameter("units", "metric")
                .addQueryParameter("appid", apiKey)
                .build();

        Request req = new Request.Builder().url(url).get().build();
        try (Response res = client.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                log.debug("Forecast request failed: {}", res.code());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(res.body().byteStream());
            JsonNode list = root.path("list");
            if (!list.isArray()) return Optional.empty();

            JsonNode best = null;
            long bestDiff = Long.MAX_VALUE;
            for (JsonNode item : list) {
                long ts = item.path("dt").asLong();
                long d = Math.abs(ts - targetEpochSeconds);
                if (d < bestDiff) {
                    bestDiff = d;
                    best = item;
                }
            }
            if (best == null) return Optional.empty();

            WeatherDto w = parseWeatherNode(best, true);
            return Optional.ofNullable(w);
        }
    }

    /**
     * Parse weather JSON node (either `/weather` root or an item from `/forecast` list) into WeatherDto.
     * If node does not contain expected fields, returns null.
     */
    private WeatherDto parseWeatherNode(JsonNode node, boolean isForecastItem) {
        if (node == null || node.isMissingNode()) return null;

        JsonNode main = node.path("main");
        if (main.isMissingNode()) return null;

        WeatherDto w = new WeatherDto();
        w.setTemperatureC(main.path("temp").isNumber() ? main.path("temp").asDouble() : null);
        w.setHumidity(main.path("humidity").isInt() ? main.path("humidity").asInt() : null);

        JsonNode weatherArr = node.path("weather");
        if (weatherArr.isArray() && !weatherArr.isEmpty()) {
            JsonNode w0 = weatherArr.get(0);
            w.setDescription(w0.path("description").asText(null));
            w.setIcon(w0.path("icon").asText(null));
        }

        w.setWindSpeed(node.path("wind").path("speed").isNumber() ? node.path("wind").path("speed").asDouble() : null);
        w.setTimestamp(node.path("dt").isNumber() ? Instant.ofEpochSecond(node.path("dt").asLong()) : null);
        w.setForecast(isForecastItem);
        return w;
    }

    private boolean isTimestampOutOfRange(long epochSeconds) {
        Instant now = Instant.now();
        long nowSec = now.getEpochSecond();
        if (epochSeconds < nowSec - PAST_SECONDS) return true;
        return epochSeconds > nowSec + FUTURE_SECONDS;
    }
}
