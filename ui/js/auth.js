(function () {

    async function fetchCurrentUser() {
        const response = await fetch("/api/auth/me", { credentials: "same-origin" });
        if (!response.ok) {
            return null;
        }
        return response.json();
    }

    function redirectToLogin() {
        window.location.href = "index.html";
    }

    function redirectToHome() {
        window.location.href = "index.html";
    }

    function redirectToDashboard() {
        window.location.href = "dashboard.html";
    }

    async function requireAuth() {
        const user = await fetchCurrentUser();
        if (!user) {
            redirectToLogin();
            return null;
        }
        return user;
    }

    async function redirectIfAuthenticated() {
        const user = await fetchCurrentUser();
        if (user) {
            redirectToDashboard();
            return user;
        }
        return null;
    }

    async function authenticatedFetch(url, options = {}) {
        const response = await fetch(url, { ...options, credentials: "same-origin" });
        if (response.status === 401) {
            redirectToLogin();
            throw new Error("UNAUTHORIZED");
        }
        return response;
    }

    function initNavbar() {
        const logoutButton = document.getElementById("logoutBtn");
        const userNameNode = document.getElementById("navUser");

        fetchCurrentUser().then(user => {
            if (user && userNameNode) {
                userNameNode.textContent = user.temporary ? `${user.username} (Demo)` : user.username;
            }
        });

        if (logoutButton) {
            logoutButton.addEventListener("click", async () => {
                try {
                    await fetch("/api/auth/logout", {
                        method: "POST",
                        credentials: "same-origin"
                    });
                } finally {
                    redirectToHome();
                }
            });
        }
    }

    window.dmsAuth = {
        authenticatedFetch,
        fetchCurrentUser,
        initNavbar,
        redirectIfAuthenticated,
        requireAuth
    };
})();
