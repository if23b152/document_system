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
    const detailsEl = document.getElementById("details");
    detailsEl.innerHTML = ""; // clear existing content

    try {
        // Fetch document details from backend REST API
        const res = await fetch(`/api/documents/${id}`);
        if (!res.ok) {
            detailsEl.textContent = "Document was not found!";
            return;
        }

        const doc = await res.json(); // parse JSON response
        originalDoc = doc; // save for later use (resetting form)

        // Extract fields from the document
        const { fileName, fileSize, uploadTimestamp, storagePath, summary, tags } = doc;

        // Define which fields to render and how to label them
        const fields = [
            { label: "File Name", value: fileName },
            { label: "File Size", value: `${fileSize} bytes` },
            { label: "Upload Timestamp", value: formatTimestamp(uploadTimestamp) },
            { label: "Storage Path", value: storagePath },
            { label: "Summary", value: summary || "(not yet created)" },
            { label: "Tags", value: tags || "(none)" }
        ];

        // Create a <p> element for each field and append to details container
        fields.forEach(item => {
            const p = document.createElement("p");
            const strong = document.createElement("b");
            strong.textContent = item.label + ": ";
            p.appendChild(strong);
            p.appendChild(document.createTextNode(item.value));
            detailsEl.appendChild(p);
        });

        // Pre-fill the edit form with existing values
        document.getElementById("fileNameInput").value = fileName;
        document.getElementById("summaryInput").value = summary || "";
        document.getElementById("tagsInput").value = tags || "";

    } catch (err) {
        console.error("Error loading document:", err);
        detailsEl.textContent = "Document was not found!";
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

    // Gather updated values from form
    const updatedDoc = {
        fileName,
        summary: document.getElementById("summaryInput").value,
        tags: document.getElementById("tagsInput").value
    };

    try {
        // Send PUT request with updated document data
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
            // Redirect back to dashboard after delete
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

// Wait for DOM to load, then initialize page
document.addEventListener("DOMContentLoaded", async () => {
    await loadDetail(); // load document details on page load
    // Attach event listeners to form buttons
    document.getElementById("editForm").addEventListener("submit", handleUpdate);
    document.getElementById("cancelBtn").addEventListener("click", handleCancel);
    document.getElementById("deleteBtn").addEventListener("click", handleDelete);
});
