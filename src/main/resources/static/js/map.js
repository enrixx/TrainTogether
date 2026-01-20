// =======================
// Karte initialisieren
// =======================
const defaultLat = 49.0139;
const defaultLon = 12.1016;
const map = L.map("map").setView([defaultLat, defaultLon], 13); // Default: Regensburg

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

// =======================
// Layer
// =======================
const gymsLayer = L.layerGroup().addTo(map);
let searchAreaCircle = null; // To visualize the search radius

// =======================
// Icons
// =======================
function blueIcon() {
    return new L.Icon({
        iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
        shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34]
    });
}

function redIcon() {
    return new L.Icon({
        iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
        shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34]
    });
}

// =======================
// Gyms laden (Overpass API + Internal DB)
// =======================
let internalGyms = [];
let externalGymsCache = new Map(); // Cache for external gyms (ID -> Gym Object)
let onlyInternal = false;
let activeSearch = null; // To store the current search parameters {lat, lon, radiusMeters}
let debounceTimer;
const MIN_ZOOM_FOR_OVERPASS = 12; // Don't fetch external gyms if zoomed out further than this

// Fetch internal gyms once on load
async function loadInternalGyms() {
    try {
        const response = await fetch("/gym/api/internal-gyms");
        if (response.ok) {
            internalGyms = await response.json();
        }
    } catch (error) {
        console.error("Failed to load internal gyms:", error);
    }
}

async function fetchGymsFromOverpass() {
    if (!map.hasLayer(gymsLayer)) return;

    // If zoomed out too far and NOT in search mode, only render internal gyms to save performance
    if (!activeSearch && map.getZoom() < MIN_ZOOM_FOR_OVERPASS) {
        renderAllGyms();
        return;
    }

    document.getElementById("map").style.cursor = "wait";

    let query;
    if (activeSearch) {
        // Search within a specific radius
        query = `
            [out:json][timeout:60];
            (
              node["leisure"="fitness_centre"](around:${activeSearch.radiusMeters},${activeSearch.lat},${activeSearch.lon});
              way["leisure"="fitness_centre"](around:${activeSearch.radiusMeters},${activeSearch.lat},${activeSearch.lon});
              node["sport"="fitness"](around:${activeSearch.radiusMeters},${activeSearch.lat},${activeSearch.lon});
            );
            out center;
        `;
    } else {
        // Default: search within the visible map bounds
        const bounds = map.getBounds();
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;
        query = `
            [out:json][timeout:60];
            (
              node["leisure"="fitness_centre"](${bbox});
              way["leisure"="fitness_centre"](${bbox});
              node["sport"="fitness"](${bbox});
            );
            out center;
        `;
    }

    try {
        // Using Kumi Systems mirror which is often faster/less loaded than the main instance
        const response = await fetch("https://overpass.kumi.systems/api/interpreter", {
            method: "POST",
            body: query
        });

        if (!response.ok) {
            throw new Error(`Overpass API error: ${response.status}`);
        }

        const data = await response.json();

        // Update cache with new data
        data.elements.forEach(element => {
            const id = element.id;
            // Store relevant data
            externalGymsCache.set(id, {
                id: id,
                lat: element.lat || element.center.lat,
                lon: element.lon || element.center.lon,
                name: element.tags.name || "Fitness Studio"
            });
        });

        renderAllGyms();

    } catch (error) {
        console.error("Error fetching from Overpass API:", error);
        // Even if Overpass fails, we should still show what we have
        renderAllGyms();
    } finally {
        document.getElementById("map").style.cursor = "";
    }
}

// Helper function to render both sources from cache and internal list
function renderAllGyms() {
    gymsLayer.clearLayers();

    const renderedInternalIds = new Set();
    const bounds = map.getBounds();

    // 1. Process External Gyms from Cache
    externalGymsCache.forEach(externalGym => {
        let shouldRender = false;

        if (activeSearch) {
            // Strictly check if within search radius
            const dist = map.distance([externalGym.lat, externalGym.lon], [activeSearch.lat, activeSearch.lon]);
            if (dist <= activeSearch.radiusMeters) {
                shouldRender = true;
            }
        } else {
            // Default: check if within visible bounds
            if (bounds.contains([externalGym.lat, externalGym.lon])) {
                shouldRender = true;
            }
        }

        if (shouldRender) {
            // Check if this gym matches one in our database
            const match = internalGyms.find(internal => {
                const dist = Math.sqrt(Math.pow(internal.lat - externalGym.lat, 2) + Math.pow(internal.lon - externalGym.lon, 2));
                return dist < 0.001; // 100m threshold
            });

            if (match) {
                // It's an internal gym found in OSM
                addMarker(externalGym.lat, externalGym.lon, externalGym.name, true, match.id);
                renderedInternalIds.add(match.id);
            } else {
                // External gym
                if (!onlyInternal) {
                    addMarker(externalGym.lat, externalGym.lon, externalGym.name, false, null);
                }
            }
        }
    });

    // 2. Process remaining Internal Gyms (NOT found in OSM or OSM failed)
    internalGyms.forEach(gym => {
        if (!renderedInternalIds.has(gym.id)) {
            let shouldRender = false;

            if (activeSearch) {
                 const dist = map.distance([gym.lat, gym.lon], [activeSearch.lat, activeSearch.lon]);
                 if (dist <= activeSearch.radiusMeters) {
                     shouldRender = true;
                 }
            } else {
                 if (bounds.contains([gym.lat, gym.lon])) {
                     shouldRender = true;
                 }
            }

            if (shouldRender) {
                 addMarker(gym.lat, gym.lon, gym.name, true, gym.id);
            }
        }
    });
}

function addMarker(lat, lon, name, isInternal, id) {
    let marker;
    let popupContent = `<b>${name}</b>`;

    if (isInternal) {
        marker = L.marker([lat, lon], { icon: redIcon() });
        popupContent += `<br><a href="/gym/${id}" class="btn btn-sm btn-primary mt-2 text-white">View Details</a>`;
    } else {
        marker = L.marker([lat, lon], { icon: blueIcon() });
        popupContent += `<br><span>External Gym</span>`;
    }

    marker.bindPopup(popupContent).addTo(gymsLayer);
}

// Helper to start a search (draw circle, set activeSearch, fetch data)
function startSearch(lat, lon, radiusKm) {
    activeSearch = { lat, lon, radiusMeters: radiusKm * 1000 };

    // DO NOT clear cache here. We want to keep old results in case we revisit.
    // externalGymsCache.clear();

    // Remove old circle if it exists
    if (searchAreaCircle) {
        map.removeLayer(searchAreaCircle);
    }
    // Add a new circle to show the search area
    searchAreaCircle = L.circle([lat, lon], {
        radius: activeSearch.radiusMeters,
        color: '#0d6efd',
        fillColor: '#0d6efd',
        fillOpacity: 0.1
    }).addTo(map);

    map.fitBounds(searchAreaCircle.getBounds());

    // Render immediately from cache (if we have data for this area)
    renderAllGyms();

    // Then fetch new data to update/fill gaps
    fetchGymsFromOverpass();
}

loadInternalGyms().then(() => {
    const radiusKm = parseFloat(document.getElementById("search-radius").value) || 5;
    
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const userLat = position.coords.latitude;
                const userLon = position.coords.longitude;
                
                // Update map view to user location
                map.setView([userLat, userLon], 13);
                
                // Start search at user location
                startSearch(userLat, userLon, radiusKm);
            },
            (error) => {
                console.warn("Geolocation denied or failed, using default location:", error);
                // Fallback to default location
                startSearch(defaultLat, defaultLon, radiusKm);
            }
        );
    } else {
        // Fallback if geolocation is not supported
        startSearch(defaultLat, defaultLon, radiusKm);
    }
});

// Reload when map moves, but only if not in an active search
// Debounced to prevent spamming the API
map.on("moveend", () => {
    if (!activeSearch) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(fetchGymsFromOverpass, 500);
    } else {
        // Even in search mode, we might want to re-render if we pan around (though search is fixed radius)
        // But renderAllGyms checks bounds, so re-rendering on move is good to show/hide markers
        renderAllGyms();
    }
});


// =======================
// Filter & Search
// =======================
document.getElementById("filter-gyms").addEventListener("change", e => {
    if (e.target.checked) {
        map.addLayer(gymsLayer);
        renderAllGyms(); // Re-render from cache
    } else {
        map.removeLayer(gymsLayer);
    }
});

document.getElementById("filter-internal-only").addEventListener("change", e => {
    onlyInternal = e.target.checked;
    renderAllGyms(); // Re-render from cache
});

// Search Logic
document.getElementById("btn-search").addEventListener("click", async () => {
    const city = document.getElementById("search-city").value;
    const radiusKm = parseFloat(document.getElementById("search-radius").value) || 5;

    if (!city) return;

    try {
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(city)}&limit=1`);

        if (!response.ok) {
             throw new Error(`Nominatim API error: ${response.status}`);
        }

        const results = await response.json();

        if (results.length > 0) {
            const lat = parseFloat(results[0].lat);
            const lon = parseFloat(results[0].lon);

            startSearch(lat, lon, radiusKm);

        } else {
            alert("Stadt nicht gefunden!");
        }
    } catch (error) {
        console.error("Geocoding failed:", error);
        alert("Fehler bei der Suche: " + error.message);
    }
});

// Clear Search Logic
document.getElementById("btn-clear-search").addEventListener("click", () => {
    activeSearch = null;
    document.getElementById("search-city").value = '';
    if (searchAreaCircle) {
        map.removeLayer(searchAreaCircle);
        searchAreaCircle = null;
    }
    // externalGymsCache.clear(); // Keep cache even on reset
    fetchGymsFromOverpass(); // Fetch gyms for the current view
});
