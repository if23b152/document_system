function getIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id");
}

function formatTimestamp(ts) {
    const date = new Date(ts);
    return date.toLocaleString();
}

let originalDoc = null;

// Load and render document details
async function loadDetail() {
    const id = getIdFromUrl();
    const detailsEl = document.getElementById("details");
    detailsEl.innerHTML = "";

    try {
        const res = await fetch(`/api/documents/${id}`);
        if (!res.ok) {
            detailsEl.textContent = "Document was not found!";
            return;
        }

        const doc = await res.json();
        originalDoc = doc;

        // Destructure
        const { fileName, fileSize, uploadTimestamp, storagePath, summary, tags } = doc;

        // Render static details
        const fields = [
            { label: "File Name", value: fileName },
            { label: "File Size", value: `${fileSize} bytes` },
            { label: "Upload Timestamp", value: formatTimestamp(uploadTimestamp) },
            { label: "Storage Path", value: storagePath },
            { label: "Summary", value: summary || "(not yet created)" },
            { label: "Tags", value: tags || "(none)" }
        ];

        fields.forEach(item => {
            const p = document.createElement("p");
            const strong = document.createElement("b");
            strong.textContent = item.label + ": ";
            p.appendChild(strong);
            p.appendChild(document.createTextNode(item.value));
            detailsEl.appendChild(p);
        });

        // Fill form with existing data
        document.getElementById("fileNameInput").value = fileName;
        document.getElementById("summaryInput").value = summary || "";
        document.getElementById("tagsInput").value = tags || "";

    } catch (err) {
        console.error("Error loading document:", err);
        detailsEl.textContent = "Document was not found!";
    }
}

// Handle form submission (PUT request)
async function handleUpdate(event) {
    event.preventDefault();
    const id = getIdFromUrl();

    let fileName = document.getElementById("fileNameInput").value.trim();

    // Ensure the filename ends with .pdf
    if (!fileName.toLowerCase().endsWith(".pdf")) {
        fileName += ".pdf";
    }

    const updatedDoc = {
        fileName,
        summary: document.getElementById("summaryInput").value,
        tags: document.getElementById("tagsInput").value
    };

    try {
        const res = await fetch(`/api/documents/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(updatedDoc)
        });

        if (res.ok) {
            alert("Document updated successfully!");
            await loadDetail(); // now properly awaited
        } else {
            alert("Failed to update document.");
        }
    } catch (err) {
        console.error("Update failed:", err);
        alert("An error occurred while updating.");
    }
}

// Reset form to original values
function handleCancel() {
    if (!originalDoc) return;
    document.getElementById("fileNameInput").value = originalDoc.fileName;
    document.getElementById("summaryInput").value = originalDoc.summary || "";
    document.getElementById("tagsInput").value = originalDoc.tags || "";
}

// Delete a document by ID
async function handleDelete() {
    const id = getIdFromUrl();

    if (!confirm("Are you sure you want to delete this document? This cannot be undone.")) {
        return;
    }

    try {
        const res = await fetch(`/api/documents/${id}`, { method: "DELETE" });
        if (res.ok) {
            alert("Document deleted successfully.");
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

document.addEventListener("DOMContentLoaded", async () => {
    await loadDetail();
    document.getElementById("editForm").addEventListener("submit", handleUpdate);
    document.getElementById("cancelBtn").addEventListener("click", handleCancel);
    document.getElementById("deleteBtn").addEventListener("click", handleDelete);
});
