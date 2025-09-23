// Function to fetch documents from the backend and display them
// Optionally accepts a search query to filter the documents by file name
// Wait until DOM is ready
window.addEventListener('DOMContentLoaded', async () => {
    const ul = document.getElementById('docs');

    // Function to fetch documents and display them
    async function fetchDocs(query = "") {
        const res = await fetch('/api/documents');
        const docs = await res.json();

        ul.innerHTML = ""; // Clear the existing list

        docs
            .filter(doc => {
                // Destructure doc here inside filter to remove linter warning
                const { fileName } = doc;
                return fileName.toLowerCase().includes(query.toLowerCase());
            })
            .forEach(doc => {
                const { fileName, fileSize, id } = doc;

                const li = document.createElement('li');
                li.textContent = `${fileName} (${fileSize} bytes)`;

                // Make the whole list item clickable
                li.classList.add('clickable-doc');
                li.addEventListener('click', () => viewDetails(id));

                ul.appendChild(li);
            });
    }

    // Navigate to details page
    function viewDetails(id) {
        window.location.href = `document-details.html?id=${id}`;
    }

    // File upload form
    document.getElementById('uploadForm').addEventListener('submit', async function (e) {
        e.preventDefault();

        const fileInput = document.getElementById('fileInput');
        const file = fileInput.files[0];
        if (!file) return alert("Choose a file!");

        const form = new FormData();
        form.append("file", file);

        try {
            const res = await fetch('/api/documents/upload', { method: 'POST', body: form });
            if (res.ok) {
                alert("Upload success!");
                await fetchDocs();
            } else {
                alert("Upload failed!");
            }
        } catch (error) {
            console.error("Upload error:", error);
            alert("Upload failed due to network error.");
        }
    });

    // Search button
    // Add an event listener to the search button for manual search
    document.getElementById('searchBtn').addEventListener('click', async () => {
        await fetchDocs(document.getElementById('searchInput').value);
    });

    // Initial load
    await fetchDocs();
});
