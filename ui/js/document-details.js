// Extract the "id" parameter from the URL query string
function getIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id"); // returns the value of ?id=...
}

// Convert a timestamp into a human-readable date/time string
function formatTimestamp(ts) {
    const date = new Date(ts);
    return date.toLocaleString(); // e.g. "9/23/2025, 12:34:56 PM"
}

// Store the original document so we can reset fields if needed
let originalDoc = null;

// Load and display document details from the backend
// Load and display document details from the backend
async function loadDetail() {
    const id = getIdFromUrl();

    try {
        const res = await fetch(`/api/documents/${id}`);
        if (!res.ok) {
            alert("Document was not found!");
            return;
        }

        const doc = await res.json();
        originalDoc = doc;

        const { fileName, fileSize, uploadTimestamp, summary, tags } = doc;

        // FIX: 'tags' is now an array from the backend.
        // We join it with a comma and space for the UI.
        const tagsString = (tags && tags.length > 0) ? tags.join(", ") : "";

        document.getElementById("detailFileName").value = fileName || "";
        document.getElementById("detailFileSize").value = fileSize ? `${fileSize} bytes` : "";
        document.getElementById("detailUploadedOn").value = uploadTimestamp ? formatTimestamp(uploadTimestamp) : "";
        document.getElementById("detailTags").value = tagsString || "(none)";
        document.getElementById("detailSummary").value = summary || "(not yet created)";

        document.getElementById("fileNameInput").value = fileName || "";
        document.getElementById("summaryInput").value = summary || "";
        document.getElementById("tagsInput").value = tagsString; // Pre-fill with joined string

        const openPdfBtn = document.getElementById("openPdfBtn");
        const openReaderBtn = document.getElementById("openReaderBtn");
        if (openPdfBtn) {
            openPdfBtn.href = `/api/documents/${id}/file`;
        }
        if (openReaderBtn) {
            openReaderBtn.href = `document-reader.html?id=${id}`;
        }

    } catch (err) {
        console.error("Error loading document:", err);
        alert("Document was not found!");
    }
}

// Handle form submission to update a document (PUT request)
async function handleUpdate(event) {
    event.preventDefault();
    const id = getIdFromUrl();

    let fileName = document.getElementById("fileNameInput").value.trim();
    if (!fileName.toLowerCase().endsWith(".pdf")) {
        fileName += ".pdf";
    }

    // FIX: Convert the comma-separated string from the input into an Array
    const tagsInputValue = document.getElementById("tagsInput").value;
    const tagsArray = tagsInputValue
        .split(",")
        .map(t => t.trim())
        .filter(t => t !== ""); // Remove empty strings

    const updatedDoc = {
        fileName,
        summary: document.getElementById("summaryInput").value,
        tags: tagsArray // Now sending a proper JSON Array: ["tag1", "tag2"]
    };

    console.log("Sending payload:", JSON.stringify(updatedDoc));

    try {
        const res = await fetch(`/api/documents/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(updatedDoc)
        });

        if (res.ok) {
            alert("Document updated successfully!");
            await loadDetail();
        } else {
            const errorData = await res.json().catch(() => ({}));
            console.error("Server Error:", errorData);
            alert("Failed to update document: " + (errorData.message || "Check console"));
        }
    } catch (err) {
        console.error("Update failed:", err);
        alert("An error occurred while updating.");
    }
}

// Reset the edit form
function handleCancel() {
    if (!originalDoc) return;
    document.getElementById("fileNameInput").value = originalDoc.fileName;
    document.getElementById("summaryInput").value = originalDoc.summary || "";
    // Re-join the array for the cancel reset
    document.getElementById("tagsInput").value = (originalDoc.tags) ? originalDoc.tags.join(", ") : "";
}

// Delete the current document by ID
async function handleDelete() {
    const id = getIdFromUrl();

    // Confirm deletion with the user
    if (!confirm("Are you sure you want to delete this document? This cannot be undone.")) {
        return;
    }

    try {
        // Send DELETE request to backend
        const res = await fetch(`/api/documents/${id}`, { method: "DELETE" });
        if (res.ok) {
            alert("Document deleted successfully.");
            // Redirect back to the dashboard after delete
            window.location.href = "dashboard.html";
        } else if (res.status === 404) {
            alert("Document not found.");
        } else {
            alert("Failed to delete document.");
        }
    } catch (err) {
        console.error("Delete failed:", err);
        alert("An error occurred while deleting.");
    }
}

// =====================
// COMMENTS LOGIC
// =====================

// Load comments for the current document
async function loadComments() {
    const id = getIdFromUrl();
    const list = document.getElementById("commentsList");
    list.innerHTML = "";

    try {
        const res = await fetch(`/api/comments/document/${id}`);
        if (!res.ok) return;

        const comments = await res.json();

        if (comments.length === 0) {
            const li = document.createElement("li");
            li.className = "list-group-item text-muted";
            li.textContent = "No comments yet.";
            list.appendChild(li);
            return;
        }

        comments.forEach(comment => {
            const li = document.createElement("li");
            li.className = "list-group-item";

            const date = new Date(comment.createdAt).toLocaleString();

            li.innerHTML = `
                <div class="fw-bold small text-muted mb-1">${date}</div>
                <div>${comment.content}</div>
            `;

            list.appendChild(li);
        });

    } catch (err) {
        console.error("Failed to load comments:", err);
    }
}

// Handle adding a new comment
async function handleAddComment(event) {
    event.preventDefault();
    const id = getIdFromUrl();
    const textarea = document.getElementById("commentInput");
    const content = textarea.value.trim();

    if (!content) return;

    try {
        const res = await fetch(`/api/comments/document/${id}`, {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: content
        });

        if (res.ok) {
            textarea.value = "";
            await loadComments();
        } else {
            alert("Failed to add comment.");
        }
    } catch (err) {
        console.error("Failed to add comment:", err);
        alert("Error while adding comment.");
    }
}

// Wait for DOM to load, then initialize the page
document.addEventListener("DOMContentLoaded", async () => {
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

    await loadDetail(); // load document details on the page load
    await loadComments();
    // Attach event listeners to form buttons
    document.getElementById("editForm").addEventListener("submit", handleUpdate);
    document.getElementById("cancelBtn").addEventListener("click", handleCancel);
    document.getElementById("deleteBtn").addEventListener("click", handleDelete);
    document.getElementById("commentForm").addEventListener("submit", handleAddComment);
});
