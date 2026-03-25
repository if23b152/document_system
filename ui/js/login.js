document.addEventListener("DOMContentLoaded", async () => {
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const loginForm = document.getElementById("loginForm");
    const errorBox = document.getElementById("authError");
    const savedTheme = localStorage.getItem("dms-theme");

    if (savedTheme === "dark") {
        document.body.classList.add("dark-mode");
        if (themeToggleBtn) themeToggleBtn.textContent = "Light Mode";
    }

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener("click", () => {
            document.body.classList.toggle("dark-mode");
            const isDark = document.body.classList.contains("dark-mode");
            localStorage.setItem("dms-theme", isDark ? "dark" : "light");
            themeToggleBtn.textContent = isDark ? "Light Mode" : "Dark Mode";
        });
    }

    await window.dmsAuth.redirectIfAuthenticated();

    loginForm.addEventListener("submit", async event => {
        event.preventDefault();
        errorBox.classList.remove("visible");

        const payload = {
            username: document.getElementById("usernameInput").value.trim(),
            password: document.getElementById("passwordInput").value
        };

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error("Login failed");
            }

            window.location.href = "dashboard.html";
        } catch (error) {
            errorBox.textContent = "Login failed. Check your username and password.";
            errorBox.classList.add("visible");
        }
    });
});
