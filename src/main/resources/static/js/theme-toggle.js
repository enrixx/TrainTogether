document.addEventListener("DOMContentLoaded", () => {
    const htmlEl = document.documentElement; // <html>
    const themeToggle = document.getElementById("theme-toggle");
    const themeLabel = document.getElementById("theme-label");

    let theme = localStorage.getItem("theme") || "light";
    htmlEl.setAttribute("data-bs-theme", theme);

    // Label im Button anpassen
    themeLabel.textContent = theme === "dark" ? "Light" : "Dark";

    // 2. Klick-Event zum Umschalten
    themeToggle.addEventListener("click", () => {
        theme = theme === "dark" ? "light" : "dark";
        localStorage.setItem("theme", theme);
        htmlEl.setAttribute("data-bs-theme", theme);
        themeLabel.textContent = theme === "dark" ? "Light" : "Dark";
    });
});
