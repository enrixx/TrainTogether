package de.othr.traintogether.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.othr.traintogether.dto.ReviewDto;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CachedReviews> cache = new ConcurrentHashMap<>();

    private record CachedReviews(List<ReviewDto> reviews, Instant timestamp) {}

    @Value("${google.places.api-key:}")
    private String apiKey;

    public Optional<String> findPlaceId(String gymName, double lat, double lon) {
        if (isApiKeyMissing()) return Optional.empty();

        HttpUrl parsedUrl = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/findplacefromtext/json");
        if (parsedUrl == null) return Optional.empty();

        HttpUrl url = parsedUrl.newBuilder()
                .addQueryParameter("input", gymName)
                .addQueryParameter("inputtype", "textquery")
                .addQueryParameter("fields", "place_id")
                .addQueryParameter("locationbias", String.format(Locale.US, "circle:2000@%f,%f", lat, lon))
                .addQueryParameter("key", apiKey)
                .build();

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Google Places FindPlace request failed: {}", response);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            String status = root.path("status").asText();
            if (status.equals("OK")) {
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    return Optional.ofNullable(candidates.get(0).path("place_id").asText());
                }
            } else {
                 String errorMessage = root.path("error_message").asText();
                 log.warn("Google Places FindPlace (with loc bias) status: {}. Message: {}", status, errorMessage);
            }
        } catch (IOException e) {
            log.error("Error calling Google Places FindPlace API", e);
        }

        return Optional.empty();
    }

    public Optional<String> findPlaceId(String gymName, String city) {
        if (isApiKeyMissing()) return Optional.empty();

        HttpUrl parsedUrl = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/findplacefromtext/json");
        if (parsedUrl == null) return Optional.empty();

        HttpUrl url = parsedUrl.newBuilder()
                .addQueryParameter("input", gymName + " " + city)
                .addQueryParameter("inputtype", "textquery")
                .addQueryParameter("fields", "place_id")
                .addQueryParameter("key", apiKey)
                .build();

        Request request = new Request.Builder().url(url).build();
        return executeFindPlaceRequest(request);
    }

    private Optional<String> executeFindPlaceRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Google Places FindPlace request failed: {}", response);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            String status = root.path("status").asText();
            if (status.equals("OK")) {
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    return Optional.ofNullable(candidates.get(0).path("place_id").asText());
                }
            } else {
                 String errorMessage = root.path("error_message").asText();
                 log.warn("Google Places FindPlace status: {}. Message: {}", status, errorMessage);
            }
        } catch (IOException e) {
            log.error("Error calling Google Places FindPlace API", e);
        }

        return Optional.empty();
    }

    public List<ReviewDto> fetchReviews(String placeId) {
        if (isApiKeyMissing()) return Collections.emptyList();

        // Check cache
        CachedReviews cached = cache.get(placeId);
        if (cached != null) {
            if (cached.timestamp().plus(24, ChronoUnit.HOURS).isAfter(Instant.now())) {
                log.debug("Returning cached reviews for placeId: {}", placeId);
                return cached.reviews();
            } else {
                cache.remove(placeId); // Expired
            }
        }

        HttpUrl parsedUrl = HttpUrl.parse("https://maps.googleapis.com/maps/api/place/details/json");
        if (parsedUrl == null) return Collections.emptyList();

        HttpUrl url = parsedUrl.newBuilder()
                .addQueryParameter("place_id", placeId)
                .addQueryParameter("fields", "reviews")
                .addQueryParameter("key", apiKey)
                .addQueryParameter("language", "de")
                .build();

        Request request = new Request.Builder().url(url).build();
        List<ReviewDto> reviews = new ArrayList<>();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Google Places Details request failed: {}", response);
                return reviews;
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            String status = root.path("status").asText();
            if (status.equals("OK")) {
                JsonNode result = root.path("result");
                JsonNode reviewsNode = result.path("reviews");
                if (reviewsNode.isArray()) {
                    for (JsonNode r : reviewsNode) {
                        ReviewDto dto = new ReviewDto();
                        dto.setAuthorName(r.path("author_name").asText());
                        dto.setProfilePhotoUrl(r.path("profile_photo_url").asText());
                        dto.setRating(r.path("rating").asInt());
                        if (r.has("original_text")) {
                             dto.setText(r.path("original_text").asText());
                        } else {
                             dto.setText(r.path("text").asText());
                        }
                        dto.setRelativeTimeDescription(r.path("relative_time_description").asText());
                        reviews.add(dto);
                    }
                }

                // Cache the result if we successfully got (or didn't get) reviews
                cache.put(placeId, new CachedReviews(reviews, Instant.now()));

            } else {
                 String errorMessage = root.path("error_message").asText();
                 log.warn("Google Places Details status: {}. Message: {}", status, errorMessage);
            }
        } catch (IOException e) {
            log.error("Error calling Google Places Details API", e);
        }

        return reviews;
    }

    private boolean isApiKeyMissing() {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Places API key not configured");
            return true;
        }
        return false;
    }
}
