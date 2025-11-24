document.addEventListener("DOMContentLoaded", () => {
    const htmlEl = document.documentElement;
    const slider = document.getElementById("themeSlider");
    const icon = document.getElementById("themeIcon");

    let theme = localStorage.getItem("theme") || "light";
    htmlEl.setAttribute("data-bs-theme", theme);

    if (theme === "dark") {
        slider.style.left = "calc(100% - 36px)";
        icon.textContent = "🌙";
    } else {
        slider.style.left = "4px";
        icon.textContent = "☀️";
    }
});

function animateThemeSwitch(event) {
    event.preventDefault();
    event.stopPropagation();

    const htmlEl = document.documentElement;
    const slider = document.getElementById("themeSlider");
    const icon = document.getElementById("themeIcon");

    let theme = htmlEl.getAttribute("data-bs-theme");

    if (theme === "light") {
        slider.style.left = "calc(100% - 36px)";
        icon.textContent = "🌙";
        htmlEl.setAttribute("data-bs-theme", "dark");
        localStorage.setItem("theme", "dark");
    } else {
        slider.style.left = "4px";
        icon.textContent = "☀️";
        htmlEl.setAttribute("data-bs-theme", "light");
        localStorage.setItem("theme", "light");
    }
}
