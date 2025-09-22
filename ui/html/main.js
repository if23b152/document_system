// при загрузке страницы получаем все документы через Rest API
fetch('/api/documents')
    .then(resp => resp.json())
    .then(docs => {
        const ul = document.getElementById('docs');
        docs.forEach(doc => {
            const li = document.createElement('li');
            li.textContent = `${doc.fileName} (${doc.fileSize} bytes)`;
            ul.appendChild(li);
        });
    });
