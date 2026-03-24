// Wait until the DOM (HTML elements) is fully loaded before running the script
window.addEventListener('DOMContentLoaded', async () => {
    const currentUser = await window.dmsAuth.requireAuth();
    if (!currentUser) {
        return;
    }
    window.dmsAuth.initNavbar();

    const themeToggleBtn = document.getElementById("themeToggleBtn");
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

    // Reference to the <ul> element where documents will be listed
    const ul = document.getElementById('docs');
    let retryTimer = null;

    const pdfPreview = document.getElementById("pdfPreview");
    const pdfPlaceholder = document.getElementById("pdfPlaceholder");
    const docCount = document.getElementById("docCount");
    let selectedDocumentId = null;
    const FILE_NAME_LINE_LENGTH = 27;

    function buildFileNameNode(fileName) {
        const wrapper = document.createElement("div");
        wrapper.className = "doc-file-name fw-bold";

        const normalizedName = fileName || "";
        const firstLine = normalizedName.slice(0, FILE_NAME_LINE_LENGTH);
        const secondLine = normalizedName.slice(FILE_NAME_LINE_LENGTH);

        wrapper.textContent = secondLine ? `${firstLine}\n${secondLine}` : firstLine;
        return wrapper;
    }

    function showPdf(id) {
        if (!pdfPreview) return;
        selectedDocumentId = id;
        pdfPreview.src = `/api/documents/${id}/file`;
        if (pdfPlaceholder) {
            pdfPlaceholder.style.display = "none";
        }
    }

    async function deleteDocument(id) {
        if (!confirm("Möchtest du dieses Dokument wirklich löschen?")) {
            return;
        }
        try {
            const res = await window.dmsAuth.authenticatedFetch(`/api/documents/${id}`, { method: "DELETE" });
            if (!res.ok) {
                alert("Löschen fehlgeschlagen.");
                return;
            }
            if (selectedDocumentId === id) {
                selectedDocumentId = null;
                if (pdfPreview) {
                    pdfPreview.src = "";
                }
                if (pdfPlaceholder) {
                    pdfPlaceholder.style.display = "flex";
                }
            }
            await fetchDocs();
        } catch (error) {
            if (error.message === "UNAUTHORIZED") {
                return;
            }
            console.error("Delete failed:", error);
            alert("Löschen fehlgeschlagen.");
        }
    }

    // Function to fetch documents from the backend and display them
    // Accepts an optional "query" parameter to filter the list of documents by name
    async function fetchDocs(query = "") {
        // Call the backend REST API to get the list of documents
        const url = query
            ? `/api/documents/search?query=${encodeURIComponent(query)}`
            : '/api/documents';

        try {
            const res = await window.dmsAuth.authenticatedFetch(url);
            if (!res.ok) {
                throw new Error(`Request failed: ${res.status}`);
            }

            const docs = await res.json(); // Convert the response to JSON (JavaScript objects)

            ul.innerHTML = ""; // Clear out any existing document list items
            if (docCount) {
                docCount.textContent = docs.length.toString();
            }

            // Filter the document list based on the search query (case-insensitive)
            docs
                // For each document, create a <li> element and append it to the <ul>
                .forEach(doc => {
                    // Destructure properties from the document object
                    const {fileName, fileSize, id} = doc;

                    // Create a new list item
                    const li = document.createElement('li');
                    li.className = "list-group-item doc-item";

                    const info = document.createElement("div");
                    info.className = "doc-info";

                    info.appendChild(buildFileNameNode(fileName));

                    const fileSizeNode = document.createElement("div");
                    fileSizeNode.className = "text-muted small";
                    fileSizeNode.textContent = `${fileSize} bytes`;
                    info.appendChild(fileSizeNode);

                    const actions = document.createElement("div");
                    actions.className = "doc-actions";

                    const viewBtn = document.createElement("button");
                    viewBtn.type = "button";
                    viewBtn.className = "btn btn-sm btn-outline-primary";
                    viewBtn.textContent = "View";
                    viewBtn.addEventListener("click", () => showPdf(id));

                    const detailsBtn = document.createElement("button");
                    detailsBtn.type = "button";
                    detailsBtn.className = "btn btn-sm btn-outline-secondary";
                    detailsBtn.textContent = "Details";
                    detailsBtn.addEventListener("click", () => viewDetails(id));

                    const readBtn = document.createElement("a");
                    readBtn.className = "btn btn-sm btn-primary";
                    readBtn.href = `document-reader.html?id=${id}`;
                    readBtn.textContent = "Read";

                    const deleteBtn = document.createElement("button");
                    deleteBtn.type = "button";
                    deleteBtn.className = "btn btn-sm btn-danger";
                    deleteBtn.textContent = "Delete";
                    deleteBtn.addEventListener("click", () => deleteDocument(id));

                    actions.append(viewBtn, detailsBtn, readBtn, deleteBtn);
                    li.append(info, actions);

                    // Add the list item to the <ul>
                    ul.appendChild(li);
                });

            if (retryTimer) {
                clearTimeout(retryTimer);
                retryTimer = null;
            }
        } catch (error) {
            if (error.message === "UNAUTHORIZED") {
                return;
            }
            console.warn("Document list fetch failed, retrying...", error);
            ul.innerHTML = "";
            const li = document.createElement("li");
            li.className = "list-group-item text-muted";
            li.textContent = "Server startet... liste wird geladen.";
            ul.appendChild(li);
            if (docCount) {
                docCount.textContent = "0";
            }
            if (!retryTimer) {
                retryTimer = setTimeout(() => fetchDocs(query), 2000);
            }
        }
    }

    // Function to navigate to the details page of a document
    // It redirects the browser to document-details.html with the document ID in the query string
    function viewDetails(id) {
        window.location.href = `document-details.html?id=${id}`;
    }

    // Add an event listener for the upload form submission
    document.getElementById('uploadForm').addEventListener('submit',
        async function (e) {
            e.preventDefault(); // Prevent the page from reloading when submitting the form

            // Get the uploaded file from the file input
            const fileInput = document.getElementById('fileInput');
            const file = fileInput.files[0]; // First (and only) selected file
            if (!file) return alert("Choose a file!"); // Stop if no file was selected

            // Use FormData to send the file in a POST request
            const form = new FormData();
            form.append("file", file);

            try {
                // Send the file to the backend API for upload
                const res = await window.dmsAuth.authenticatedFetch('/api/documents/upload', {method: 'POST', body: form});
                if (res.ok) {
                    alert("Upload success!"); // Notify success
                    await fetchDocs(); // Refresh the list to include the new document
                } else {
                    alert("Upload failed!"); // Notify failure
                }
            } catch (error) {
                if (error.message === "UNAUTHORIZED") {
                    return;
                }
                // Handle network or server errors
                console.error("Upload error:", error);
                alert("Upload failed due to network error.");
            }
        });

    // Search form submit handler
    document.getElementById('searchForm').addEventListener('submit', async function (e) {
        e.preventDefault(); // Prevent page reload
        const query = document.getElementById('searchInput').value;
        await fetchDocs(query); // Call fetchDocs with the query
    });

    // Perform an initial fetch of documents when the page loads
    await fetchDocs();
});
