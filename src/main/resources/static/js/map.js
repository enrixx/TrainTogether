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
// Gyms automatisch laden
// =======================
async function loadNearbyGyms() {
    gymsLayer.clearLayers();

    const response = await fetch("/api/gyms");
    const gyms = await response.json();

    gyms.forEach(gym => {
        const marker = L.marker([gym.lat, gym.lon], { icon: blueIcon() })
            .bindPopup(gym.name)
            .addTo(gymsLayer);
        marker.on('click', () => {
            window.location.href = `/gym/${gym.id}`;
        });
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
