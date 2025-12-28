package de.othr.traintogether.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.GymRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class GymService {

    private static final Logger logger = LoggerFactory.getLogger(GymService.class);

    private final GymRepository gymRepository;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GymService(GymRepository gymRepository) {
        this.gymRepository = gymRepository;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(readOnly = true)
    public List<Gym> findAll() {
        return gymRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Gym> findById(Long id) {
        return gymRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Gym getById(Long id) {
        return gymRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym not found with id: " + id));
    }

    @Transactional
    public Gym save(Gym gym) {
        logger.info("Saving gym: {}", gym.getName());
        return gymRepository.save(gym);
    }

    @Transactional
    public Gym create(Gym gym, User owner) {
        gym.setOwner(owner);
        geocodeGym(gym);
        logger.info("Creating gym: {} for owner: {}", gym.getName(), owner.getEmail());
        return gymRepository.save(gym);
    }

    @Transactional
    public Gym update(Long id, Gym updatedGym) {
        Gym existingGym = getById(id);

        boolean addressChanged = !existingGym.getAddress().equals(updatedGym.getAddress()) ||
                                 !existingGym.getCity().equals(updatedGym.getCity()) ||
                                 !existingGym.getPostalCode().equals(updatedGym.getPostalCode());

        existingGym.setName(updatedGym.getName());
        existingGym.setAddress(updatedGym.getAddress());
        existingGym.setCity(updatedGym.getCity());
        existingGym.setPostalCode(updatedGym.getPostalCode());
        existingGym.setPhoneNumber(updatedGym.getPhoneNumber());
        existingGym.setDescription(updatedGym.getDescription());
        existingGym.setBannerText(updatedGym.getBannerText());
        existingGym.setBannerTextColor(updatedGym.getBannerTextColor());
        existingGym.setBannerImageUrl(updatedGym.getBannerImageUrl());

        if (addressChanged) {
            geocodeGym(existingGym);
        }

        logger.info("Updating gym with id: {}", id);
        return gymRepository.save(existingGym);
    }

    @Transactional
    public void deleteById(Long id) {
        logger.info("Deleting gym with id: {}", id);
        gymRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Gym> findByOwnerId(Long ownerId) {
        return gymRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<Gym> findByOwnerEmail(String email) {
        return gymRepository.findFirstByOwnerEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Gym> findByCity(String city) {
        return gymRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Gym> searchByCity(String city) {
        return gymRepository.findByCityContainingIgnoreCase(city);
    }

    @Transactional(readOnly = true)
    public List<Gym> searchByName(String name) {
        return gymRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long gymId, Long userId) {
        return gymRepository.findById(gymId)
                .map(gym -> gym.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private void geocodeGym(Gym gym) {
        try {
            String query = gym.getAddress() + ", " + gym.getPostalCode() + " " + gym.getCity();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedQuery + "&limit=1";

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "TrainTogetherApp/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    if (rootNode.isArray() && !rootNode.isEmpty()) {
                        JsonNode firstResult = rootNode.get(0);
                        double lat = firstResult.get("lat").asDouble();
                        double lon = firstResult.get("lon").asDouble();
                        gym.setLat(lat);
                        gym.setLon(lon);
                        logger.info("Geocoded gym address to: {}, {}", lat, lon);
                    } else {
                        logger.warn("No geocoding results found for address: {}", query);
                    }
                } else {
                    logger.error("Geocoding failed with status: {}", response.code());
                }
            }
        } catch (IOException e) {
            logger.error("Error during geocoding", e);
        }
    }
}
