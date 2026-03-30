document.addEventListener("DOMContentLoaded", async () => {
    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const registerForm = document.getElementById("registerForm");
    const errorBox = document.getElementById("authError");
    const savedTheme = localStorage.getItem("dms-theme");

    if (savedTheme !== "light") {
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

    registerForm.addEventListener("submit", async event => {
        event.preventDefault();
        errorBox.classList.remove("visible");

        const password = document.getElementById("passwordInput").value;
        const confirmPassword = document.getElementById("confirmPasswordInput").value;
        if (password !== confirmPassword) {
            errorBox.textContent = "Passwords do not match.";
            errorBox.classList.add("visible");
            return;
        }

        const payload = {
            username: document.getElementById("usernameInput").value.trim(),
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
                let message = "Registration failed.";
                try {
                    const errorData = await response.json();
                    if (errorData.detail) {
                        message = errorData.detail;
                    } else if (errorData.message) {
                        message = errorData.message;
                    }
                } catch (ignored) {
                }
                throw new Error(message);
            }

            window.location.href = "dashboard.html";
        } catch (error) {
            errorBox.textContent = error.message || "Registration failed.";
            errorBox.classList.add("visible");
        }
    });
});
