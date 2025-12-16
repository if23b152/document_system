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
async function loadDetail() {
    const id = getIdFromUrl(); // get document ID from URL

    try {
        // Fetch document details from backend REST API
        const res = await fetch(`/api/documents/${id}`);
        if (!res.ok) {
            alert("Document was not found!");
            return;
        }

        const doc = await res.json(); // parse JSON response
        originalDoc = doc; // save for later use (resetting form)

        // Extract fields from the document
        const { fileName, fileSize, uploadTimestamp, summary, tags } = doc;

        // Populate read-only inputs in the details column
        document.getElementById("detailFileName").value = fileName || "";
        document.getElementById("detailFileSize").value = fileSize ? `${fileSize} bytes` : "";
        document.getElementById("detailUploadedOn").value = uploadTimestamp ? formatTimestamp(uploadTimestamp) : "";
        document.getElementById("detailTags").value = tags || "(none)";
        document.getElementById("detailSummary").value = summary || "(not yet created)";

        // Pre-fill the edit form with existing values
        document.getElementById("fileNameInput").value = fileName || "";
        document.getElementById("summaryInput").value = summary || "";
        document.getElementById("tagsInput").value = tags || "";

    } catch (err) {
        console.error("Error loading document:", err);
        alert("Document was not found!");
    }
}

// Handle form submission to update a document (PUT request)
async function handleUpdate(event) {
    event.preventDefault(); // stop form from refreshing the page
    const id = getIdFromUrl();

    let fileName = document.getElementById("fileNameInput").value.trim();

    // Make sure the filename always ends with ".pdf"
    if (!fileName.toLowerCase().endsWith(".pdf")) {
        fileName += ".pdf";
    }

    // Gather updated values from the form
    const updatedDoc = {
        fileName,
        summary: document.getElementById("summaryInput").value,
        tags: document.getElementById("tagsInput").value
    };

    try {
        // Send the PUT request with updated document data
        const res = await fetch(`/api/documents/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(updatedDoc)
        });

        if (res.ok) {
            alert("Document updated successfully!");
            await loadDetail(); // reload details to show changes
        } else {
            alert("Failed to update document.");
        }
    } catch (err) {
        console.error("Update failed:", err);
        alert("An error occurred while updating.");
    }
}

// Reset the edit form back to the original document values
function handleCancel() {
    if (!originalDoc) return;
    document.getElementById("fileNameInput").value = originalDoc.fileName;
    document.getElementById("summaryInput").value = originalDoc.summary || "";
    document.getElementById("tagsInput").value = originalDoc.tags || "";
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
    await loadDetail(); // load document details on the page load
    await loadComments();
    // Attach event listeners to form buttons
    document.getElementById("editForm").addEventListener("submit", handleUpdate);
    document.getElementById("cancelBtn").addEventListener("click", handleCancel);
    document.getElementById("deleteBtn").addEventListener("click", handleDelete);
    document.getElementById("commentForm").addEventListener("submit", handleAddComment);
});
