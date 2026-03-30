document.addEventListener("DOMContentLoaded", async () => {
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");
    const tryNowBtn = document.getElementById("tryNowBtn");
    const authError = document.getElementById("authError");
    const savedTheme = localStorage.getItem("dms-theme");

    if (savedTheme === "dark") {
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

    const user = await window.dmsAuth.redirectIfAuthenticated();
    if (user) {
        return;
    }

    function showError(message) {
        if (!authError) {
            return;
        }
        authError.textContent = message;
        authError.classList.add("visible");
    }

    function clearError() {
        if (!authError) {
            return;
        }
        authError.classList.remove("visible");
        authError.textContent = "";
    }

    async function resolveAuthError(response, unauthorizedMessage) {
        if (response.status === 401) {
            return unauthorizedMessage;
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

        return `Request failed (HTTP ${response.status}).`;
    }

    if (loginForm) {
        loginForm.addEventListener("submit", async event => {
            event.preventDefault();
            clearError();

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
                    throw new Error(await resolveAuthError(response, "Login failed. Check your username and password."));
                }

                window.location.href = "dashboard.html";
            } catch (error) {
                showError(error.message || "Login failed.");
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener("submit", async event => {
            event.preventDefault();
            clearError();

            const password = document.getElementById("registerPasswordInput").value;
            const confirmPassword = document.getElementById("confirmPasswordInput").value;
            if (password !== confirmPassword) {
                showError("Passwords do not match.");
                return;
            }

            const payload = {
                username: document.getElementById("registerUsernameInput").value.trim(),
                email: document.getElementById("emailInput").value.trim(),
                password
            };

            try {
                const response = await fetch("/api/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "same-origin",
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    throw new Error(await resolveAuthError(response, "Registration failed."));
                }

                window.location.href = "dashboard.html";
            } catch (error) {
                showError(error.message || "Registration failed.");
            }
        });
    }

    if (tryNowBtn) {
        tryNowBtn.addEventListener("click", async () => {
            clearError();
            try {
                const response = await fetch("/api/auth/try", {
                    method: "POST",
                    credentials: "same-origin"
                });

                if (!response.ok) {
                    throw new Error(await resolveAuthError(response, "Demo session could not be started."));
                }

                window.location.href = "dashboard.html";
            } catch (error) {
                showError(error.message || "Demo session could not be started.");
            }
        });
    }
});
