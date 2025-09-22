// Extract 'id' from the URL query parameters
function getIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id");
}

// Convert timestamp to readable string
function formatTimestamp(ts) {
    const date = new Date(ts);
    return date.toLocaleString();
}

// Load document details from API and populate the page
async function loadDetail() {
    const id = getIdFromUrl();
    const detailsEl = document.getElementById('details');
    detailsEl.innerHTML = ''; // Clear previous content

    try {
        const res = await fetch(`/api/documents/${id}`);
        if (!res.ok) {
            detailsEl.textContent = "Document was not found!";
            return;
        }

        const doc = await res.json();

        // Destructure backend properties
        const { fileName, fileSize, uploadTimestamp, storagePath, summary, tags } = doc;

        // Create an array of fields to render
        const fields = [
            { label: "File Name", value: fileName },
            { label: "File Size", value: `${fileSize} bytes` },
            { label: "Upload Timestamp", value: formatTimestamp(uploadTimestamp) },
            { label: "Storage Path", value: storagePath },
            { label: "Summary", value: summary || "(not yet created)" },
            { label: "Tags", value: tags || "(none)" }
        ];

        // Render fields safely using DOM methods
        fields.forEach(item => {
            const p = document.createElement('p');
            const strong = document.createElement('b');
            strong.textContent = item.label + ': ';
            p.appendChild(strong);
            p.appendChild(document.createTextNode(item.value));
            detailsEl.appendChild(p);
        });

    } catch (error) {
        console.error("Error loading document:", error);
        detailsEl.textContent = "Document was not found!";
    }
}

// Load the details when the page loads
document.addEventListener('DOMContentLoaded', loadDetail);
