// =======================
// Karte initialisieren
// =======================
const map = L.map("map").setView([49.0139, 12.1016], 13);

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
    .addTo(map);

// =======================
// Layer
// =======================
const gymsLayer   = L.layerGroup().addTo(map);
const parksLayer  = L.layerGroup().addTo(map);
const customLayer = L.layerGroup().addTo(map);

// =======================
// Modal (für Löschen)
// =======================
const deleteModal = new bootstrap.Modal(document.getElementById("deleteModal"));
let markerToDelete = null;

// =======================
// Icons
// =======================
function blueIcon() {
    return new L.Icon({
        iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
        shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41]
    });
}

function greenIcon() {
    return new L.Icon({
        iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png",
        shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41]
    });
}

// =======================
// Eigene Marker setzen
// =======================
map.on("click", async (e) => {
    const m = L.marker(e.latlng, { icon: greenIcon() })
        .bindPopup("Eigener Punkt<br><small>(Rechtsklick zum Löschen)</small>")
        .addTo(customLayer);

    // Rechtsklick → löschen
    m.on("contextmenu", () => {
        markerToDelete = m;
        deleteModal.show();
    });

    // Spring Backend speichern
    await fetch("/api/points", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ lat: e.latlng.lat, lng: e.latlng.lng })
    });
});

// =======================
// Punkt wirklich löschen
// =======================
document.getElementById("delete-confirm").onclick = async () => {
    if (!markerToDelete) return;

    const { lat, lng } = markerToDelete.getLatLng();

    // Backend DELETE
    await fetch(`/api/points?lat=${lat}&lng=${lng}`, {
        method: "DELETE"
    });

    customLayer.removeLayer(markerToDelete);
    markerToDelete = null;
    deleteModal.hide();
};

// =======================
// Gyms automatisch laden (Overpass API)
// =======================
async function loadNearbyGyms() {
    gymsLayer.clearLayers();

    const bounds = map.getBounds();
    const query = `
        [out:json];
        node["amenity"="gym"](${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()});
        out;
    `;

    const response = await fetch("https://overpass-api.de/api/interpreter", {
        method: "POST",
        body: query
    });

    const json = await response.json();

    json.elements.forEach(g => {
        L.marker([g.lat, g.lon], { icon: blueIcon() })
            .bindPopup(g.tags.name || "Gym")
            .addTo(gymsLayer);
    });
}

// Beim ersten Laden + beim Bewegen/Zoomen
map.on("moveend", loadNearbyGyms);
loadNearbyGyms();

// =======================
// Filter
// =======================
document.getElementById("filter-gyms").addEventListener("change", e =>
    e.target.checked ? map.addLayer(gymsLayer) : map.removeLayer(gymsLayer)
);
document.getElementById("filter-parks").addEventListener("change", e =>
    e.target.checked ? map.addLayer(parksLayer) : map.removeLayer(parksLayer)
);
document.getElementById("filter-custom").addEventListener("change", e =>
    e.target.checked ? map.addLayer(customLayer) : map.removeLayer(customLayer)
);
