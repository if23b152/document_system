// Wait until the DOM (HTML elements) is fully loaded before running the script
window.addEventListener('DOMContentLoaded', async () => {
    // Reference to the <ul> element where documents will be listed
    const ul = document.getElementById('docs');

    // Function to fetch documents from the backend and display them
    // Accepts an optional "query" parameter to filter the list of documents by name
    async function fetchDocs(query = "") {
        // Call the backend REST API to get the list of documents
        const res = await fetch('/api/documents');
        const docs = await res.json(); // Convert the response to JSON (JavaScript objects)

        ul.innerHTML = ""; // Clear out any existing document list items

        // Filter the documents list based on the search query (case-insensitive)
        docs
            .filter(doc => {
                // Extract fileName from the current document object
                const { fileName } = doc;
                // Return only docs whose fileName contains the query string
                return fileName.toLowerCase().includes(query.toLowerCase());
            })
            // For each document, create a <li> element and append it to the <ul>
            .forEach(doc => {
                // Destructure properties from the document object
                const { fileName, fileSize, id } = doc;

                // Create a new list item
                const li = document.createElement('li');
                // Show filename and file size in the list item
                li.textContent = `${fileName} (${fileSize} bytes)`;

                // Add a CSS class to make the list item look clickable
                li.classList.add('clickable-doc');
                // Add an event listener so clicking a list item navigates to the details page
                li.addEventListener('click', () => viewDetails(id));

                // Add the list item to the <ul>
                ul.appendChild(li);
            });
    }

    // Function to navigate to the details page of a document
    // It redirects the browser to document-details.html with the document ID in the query string
    function viewDetails(id) {
        window.location.href = `document-details.html?id=${id}`;
    }

    // Add event listener for the upload form submission
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
            // Send file to the backend API for upload
            const res = await fetch('/api/documents/upload', { method: 'POST', body: form });
            if (res.ok) {
                alert("Upload success!"); // Notify success
                await fetchDocs(); // Refresh the list to include the new document
            } else {
                alert("Upload failed!"); // Notify failure
            }
        } catch (error) {
            // Handle network or server errors
            console.error("Upload error:", error);
            alert("Upload failed due to network error.");
        }
    });

    // Add an event listener to the search button
    // When clicked, it filters the documents by the value typed in the search input
    document.getElementById('searchBtn').addEventListener('click', async () => {
        await fetchDocs(document.getElementById('searchInput').value);
    });

    // Perform an initial fetch of documents when the page loads
    await fetchDocs();
});
