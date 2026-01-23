function getIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id");
}

function tokenizeText(text) {
    return text
        .replace(/\s+/g, " ")
        .trim()
        .split(" ")
        .filter(Boolean);
}

function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function renderWordWithFocus(word) {
    if (!word) return "-";
    const safe = escapeHtml(word);
    const mid = Math.floor(safe.length / 2);
    const before = safe.slice(0, mid);
    const focus = safe.charAt(mid);
    const after = safe.slice(mid + 1);
    return `${before}<span class="focus-char">${focus}</span>${after}`;
}

function storageKey(id) {
    return `dms-reader:${id}`;
}

function focusKey(id) {
    return `dms-reader-focus:${id}`;
}

function loadStoredState(id) {
    try {
        const raw = localStorage.getItem(storageKey(id));
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        console.warn("Failed to read reader state:", error);
        return null;
    }
}

function saveStoredState(id, state) {
    try {
        localStorage.setItem(storageKey(id), JSON.stringify(state));
    } catch (error) {
        console.warn("Failed to save reader state:", error);
    }
}

function loadFocusState(id) {
    const raw = localStorage.getItem(focusKey(id));
    if (raw === "off") return false;
    return true;
}

function saveFocusState(id, enabled) {
    localStorage.setItem(focusKey(id), enabled ? "on" : "off");
}

document.addEventListener("DOMContentLoaded", async () => {
    const id = getIdFromUrl();
    if (!id) {
        alert("Missing document ID.");
        return;
    }

    const themeToggleBtn = document.getElementById("themeToggleBtn");
    const fullscreenToggleBtn = document.getElementById("fullscreenToggleBtn");
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

    function updateFullscreenButton() {
        if (!fullscreenToggleBtn) return;
        const isFullscreen = document.fullscreenElement != null;
        fullscreenToggleBtn.textContent = isFullscreen ? "Exit Fullscreen" : "Fullscreen";
        document.body.classList.toggle("reader-fullscreen", isFullscreen);
    }

    if (fullscreenToggleBtn) {
        fullscreenToggleBtn.addEventListener("click", async () => {
            if (document.fullscreenElement) {
                await document.exitFullscreen();
            } else {
                await document.documentElement.requestFullscreen();
            }
            updateFullscreenButton();
        });
    }

    document.addEventListener("fullscreenchange", updateFullscreenButton);

    const readerTitle = document.getElementById("readerTitle");
    const readerStatus = document.getElementById("readerStatus");
    const wordDisplay = document.getElementById("wordDisplay");
    const wordProgress = document.getElementById("wordProgress");
    const startPauseBtn = document.getElementById("startPauseBtn");
    const resetBtn = document.getElementById("resetBtn");
    const stepBackBtn = document.getElementById("stepBackBtn");
    const stepForwardBtn = document.getElementById("stepForwardBtn");
    const focusToggleBtn = document.getElementById("focusToggleBtn");
    const wpmInput = document.getElementById("wpmInput");
    const wpmValue = document.getElementById("wpmValue");
    const pdfContainer = document.getElementById("pdfContainer");
    const openPdfBtn = document.getElementById("openPdfBtn");

    const state = {
        words: [],
        wordIndex: 0,
        wpm: 300,
        timerId: null,
        isPlaying: false,
        textReady: false,
        pollTimeout: null,
        focusEnabled: loadFocusState(id),
        pdfWordSpans: [],
        pdfWordLineKeys: [],
        pdfLineMap: new Map(),
        activeLineSpans: []
    };

    const stored = loadStoredState(id);
    if (stored && Number.isFinite(stored.wordIndex)) {
        state.wordIndex = Math.max(0, stored.wordIndex);
    }
    if (stored && Number.isFinite(stored.wpm)) {
        state.wpm = Math.min(800, Math.max(150, stored.wpm));
    }

    function updateWordDisplay() {
        if (!state.words.length) {
            wordDisplay.textContent = "-";
            wordProgress.textContent = "0 / 0";
            return;
        }

        if (state.wordIndex >= state.words.length) {
            state.wordIndex = state.words.length - 1;
        }

        if (state.focusEnabled) {
            wordDisplay.innerHTML = renderWordWithFocus(state.words[state.wordIndex]);
        } else {
            wordDisplay.textContent = state.words[state.wordIndex];
        }
        wordProgress.textContent = `${state.wordIndex + 1} / ${state.words.length}`;
        highlightPdfLine();
    }

    function highlightPdfLine() {
        if (!state.pdfWordLineKeys.length || !state.pdfLineMap.size) {
            return;
        }

        const lineKey = state.pdfWordLineKeys[state.wordIndex];
        if (!lineKey) {
            return;
        }

        if (state.activeLineSpans.length) {
            state.activeLineSpans.forEach(active => active.classList.remove("pdf-highlight"));
        }

        const lineSpans = state.pdfLineMap.get(lineKey) || [];
        lineSpans.forEach(span => span.classList.add("pdf-highlight"));
        state.activeLineSpans = lineSpans;

        if (lineSpans.length) {
            ensureSpanVisible(lineSpans[0]);
        }
    }

    function ensureSpanVisible(span) {
        if (!pdfContainer) return;
        const containerRect = pdfContainer.getBoundingClientRect();
        const spanRect = span.getBoundingClientRect();

        if (spanRect.top < containerRect.top || spanRect.bottom > containerRect.bottom) {
            span.scrollIntoView({ block: "center", behavior: "smooth" });
        }
    }

    function updateControls() {
        startPauseBtn.disabled = !state.textReady;
        resetBtn.disabled = !state.textReady;
        stepBackBtn.disabled = !state.textReady;
        stepForwardBtn.disabled = !state.textReady;
        startPauseBtn.textContent = state.isPlaying ? "Pause" : "Start";
    }

    function saveState() {
        saveStoredState(id, {
            wordIndex: state.wordIndex,
            wpm: state.wpm
        });
    }

    function stopPlayback() {
        if (state.timerId) {
            clearInterval(state.timerId);
            state.timerId = null;
        }
        state.isPlaying = false;
        updateControls();
    }

    function startPlayback() {
        if (!state.textReady || !state.words.length) {
            return;
        }

        stopPlayback();
        state.isPlaying = true;

        const intervalMs = Math.max(60, Math.floor(60000 / state.wpm));
        state.timerId = setInterval(() => {
            if (state.wordIndex < state.words.length - 1) {
                state.wordIndex += 1;
                updateWordDisplay();
                saveState();
            } else {
                stopPlayback();
            }
        }, intervalMs);

        updateControls();
    }

    startPauseBtn.addEventListener("click", () => {
        if (state.isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    });

    resetBtn.addEventListener("click", () => {
        stopPlayback();
        state.wordIndex = 0;
        updateWordDisplay();
        saveState();
    });

    stepBackBtn.addEventListener("click", () => {
        stopPlayback();
        state.wordIndex = Math.max(0, state.wordIndex - 1);
        updateWordDisplay();
        saveState();
    });

    stepForwardBtn.addEventListener("click", () => {
        stopPlayback();
        state.wordIndex = Math.min(state.words.length - 1, state.wordIndex + 1);
        updateWordDisplay();
        saveState();
    });

    wpmInput.addEventListener("input", () => {
        state.wpm = Number(wpmInput.value);
        wpmValue.textContent = state.wpm;
        saveState();
        if (state.isPlaying) {
            startPlayback();
        }
    });

    if (focusToggleBtn) {
        focusToggleBtn.textContent = state.focusEnabled ? "Focus Letter: On" : "Focus Letter: Off";
        focusToggleBtn.addEventListener("click", () => {
            state.focusEnabled = !state.focusEnabled;
            saveFocusState(id, state.focusEnabled);
            focusToggleBtn.textContent = state.focusEnabled ? "Focus Letter: On" : "Focus Letter: Off";
            updateWordDisplay();
        });
    }

    window.addEventListener("beforeunload", () => {
        saveState();
        stopPolling();
    });

    function stopPolling() {
        if (state.pollTimeout) {
            clearTimeout(state.pollTimeout);
            state.pollTimeout = null;
        }
    }

    async function fetchTextWithPolling() {
        try {
            const textRes = await fetch(`/api/documents/${id}/text`);
            if (textRes.status === 202) {
                readerStatus.textContent = "Text is still processing. Please wait...";
                state.textReady = false;
                updateControls();
                stopPolling();
                state.pollTimeout = setTimeout(fetchTextWithPolling, 2500);
                return;
            }

            if (textRes.status === 404) {
                readerStatus.textContent = "Document text not found.";
                state.textReady = false;
                updateControls();
                stopPolling();
                return;
            }

            if (textRes.status === 409) {
                const data = await textRes.json().catch(() => ({}));
                readerStatus.textContent = data.message
                    ? `Text processing failed: ${data.message}`
                    : "Text processing failed.";
                state.textReady = false;
                updateControls();
                stopPolling();
                return;
            }

            if (!textRes.ok) {
                readerStatus.textContent = "Failed to load text. Please retry.";
                state.textReady = false;
                updateControls();
                stopPolling();
                return;
            }

            const data = await textRes.json();
            const status = data.status || "READY";
            if (status === "FAILED") {
                readerStatus.textContent = data.message
                    ? `Text processing failed: ${data.message}`
                    : "Text processing failed.";
                state.textReady = false;
                updateControls();
                stopPolling();
                return;
            }

            if (status !== "READY") {
                readerStatus.textContent = "Text is still processing. Please wait...";
                state.textReady = false;
                updateControls();
                stopPolling();
                state.pollTimeout = setTimeout(fetchTextWithPolling, 2500);
                return;
            }

            state.words = tokenizeText(data.text || "");
            state.textReady = state.words.length > 0;

            if (!state.textReady) {
                readerStatus.textContent = "No readable text found yet.";
            } else {
                readerStatus.textContent = `Ready - ${state.words.length} words`;
            }

            if (stored && Number.isFinite(stored.wordIndex)) {
                state.wordIndex = Math.min(state.words.length - 1, stored.wordIndex);
            }

            updateWordDisplay();
            updateControls();
            stopPolling();
        } catch (error) {
            console.error("Failed to load document text:", error);
            readerStatus.textContent = "Failed to load text. Please retry.";
            state.textReady = false;
            updateControls();
            stopPolling();
        }
    }

    function renderPdfFallback(pdfUrl) {
        if (!pdfContainer) return;
        pdfContainer.innerHTML = "";
        const iframe = document.createElement("iframe");
        iframe.className = "pdf-frame";
        iframe.src = pdfUrl;
        iframe.title = "PDF Preview";
        pdfContainer.appendChild(iframe);
    }

    async function renderPdfAndExtractText(pdfUrl) {
        if (!pdfContainer || !window.pdfjsLib) {
            renderPdfFallback(pdfUrl);
            return { words: [], spans: [], usedPdfJs: false };
        }

        try {
            pdfContainer.innerHTML = "";
            window.pdfjsLib.GlobalWorkerOptions.workerSrc = "/js/vendor/pdf.worker.min.js";

            const loadingTask = window.pdfjsLib.getDocument(pdfUrl);
            const pdf = await loadingTask.promise;

            const words = [];
            const spans = [];
            const lineKeys = [];
            const lineMap = new Map();

            for (let pageNum = 1; pageNum <= pdf.numPages; pageNum += 1) {
                const page = await pdf.getPage(pageNum);
                const unscaledViewport = page.getViewport({ scale: 1 });
                const containerWidth = pdfContainer.clientWidth || 800;
                const scale = containerWidth / unscaledViewport.width;
                const viewport = page.getViewport({ scale });

                const pageWrapper = document.createElement("div");
                pageWrapper.className = "pdf-page";
                pageWrapper.style.width = `${viewport.width}px`;
                pageWrapper.style.height = `${viewport.height}px`;

                const canvas = document.createElement("canvas");
                const context = canvas.getContext("2d");
                canvas.width = viewport.width;
                canvas.height = viewport.height;
                pageWrapper.appendChild(canvas);

                const textLayer = document.createElement("div");
                textLayer.className = "textLayer";
                pageWrapper.appendChild(textLayer);

                pdfContainer.appendChild(pageWrapper);

                await page.render({ canvasContext: context, viewport }).promise;

                const textContent = await page.getTextContent();
                await window.pdfjsLib.renderTextLayer({
                    textContent,
                    container: textLayer,
                    viewport,
                    textDivs: []
                }).promise;

                const pageSpans = Array.from(textLayer.querySelectorAll("span"));
                const pageRect = pageWrapper.getBoundingClientRect();
                pageSpans.forEach(span => {
                    const raw = span.textContent || "";
                    const tokens = raw.trim().split(/\s+/).filter(Boolean);
                    if (!tokens.length) {
                        return;
                    }
                    tokens.forEach(token => {
                        const spanRect = span.getBoundingClientRect();
                        const relativeTop = spanRect.top - pageRect.top;
                        const lineBucket = Math.round(relativeTop / 3);
                        const lineKey = `${pageNum}:${lineBucket}`;

                        words.push(token);
                        spans.push(span);
                        lineKeys.push(lineKey);

                        if (!lineMap.has(lineKey)) {
                            lineMap.set(lineKey, []);
                        }
                        lineMap.get(lineKey).push(span);
                    });
                });
            }

            return { words, spans, lineKeys, lineMap, usedPdfJs: true };
        } catch (error) {
            console.error("Failed to render PDF:", error);
            renderPdfFallback(pdfUrl);
            return { words: [], spans: [], usedPdfJs: false };
        }
    }

    try {
        const docRes = await fetch(`/api/documents/${id}`);
        if (docRes.ok) {
            const doc = await docRes.json();
            readerTitle.textContent = doc.fileName || `Document ${id}`;
        } else {
            readerTitle.textContent = `Document ${id}`;
        }
    } catch (error) {
        readerTitle.textContent = `Document ${id}`;
    }

    const pdfUrl = `/api/documents/${id}/file`;
    openPdfBtn.href = pdfUrl;

    wpmInput.value = state.wpm;
    wpmValue.textContent = state.wpm;

    readerStatus.textContent = "Loading PDF text layer...";
    const pdfResult = await renderPdfAndExtractText(pdfUrl);

    if (pdfResult.words.length) {
        state.words = pdfResult.words;
        state.pdfWordSpans = pdfResult.spans;
        state.pdfWordLineKeys = pdfResult.lineKeys || [];
        state.pdfLineMap = pdfResult.lineMap || new Map();
        state.textReady = true;
        readerStatus.textContent = `Ready - ${state.words.length} words`;
        updateWordDisplay();
        updateControls();
    } else {
        if (!pdfResult.usedPdfJs) {
            readerStatus.textContent = "PDF text layer unavailable - using OCR text.";
        } else {
            readerStatus.textContent = "Loading extracted text...";
        }
        await fetchTextWithPolling();
    }
});
