document.addEventListener("DOMContentLoaded", () => {
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const savedTheme = localStorage.getItem("dms-theme");

    if (savedTheme !== "light") {
        document.body.classList.add("dark-mode");
        if (themeToggleBtn) {
            themeToggleBtn.textContent = "Light Mode";
        }
    }

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener("click", () => {
            document.body.classList.toggle("dark-mode");
            const isDark = document.body.classList.contains("dark-mode");
            localStorage.setItem("dms-theme", isDark ? "dark" : "light");
            themeToggleBtn.textContent = isDark ? "Light Mode" : "Dark Mode";
        });
    }
});
