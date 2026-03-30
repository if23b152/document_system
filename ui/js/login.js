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

    async function resolveLoginError(response) {
        if (response.status === 401) {
            return "Login failed. Check your username and password.";
        }
        if (response.status >= 500) {
            return "The server is currently unavailable. Please try again in a moment.";
        }

        try {
            const errorData = await response.json();
            if (errorData.detail) {
                return errorData.detail;
            }
            if (errorData.message) {
                return errorData.message;
            }
        } catch (ignored) {
        }

        return `Login failed (HTTP ${response.status}).`;
    }

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
                throw new Error(await resolveLoginError(response));
            }

            window.location.href = "dashboard.html";
        } catch (error) {
            errorBox.textContent = error.message || "Login failed.";
            errorBox.classList.add("visible");
        }
    });
});
