using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// Note editor for one document; saves automatically shortly after typing stops.
/// A note can be shared as a comment on the matching DMS document; it is then kept in sync
/// (on opening and after every save) and the document's other comments are shown below.
/// </summary>
internal sealed class NotesForm : Form
{
    private readonly string _file;
    private readonly AppData _appData;
    private readonly NotesStore _notes;
    private readonly TextBox _text;
    private readonly Label _status;
    private readonly Button _share;
    private readonly ListBox _others;
    private readonly Label _othersTitle;
    private readonly Panel _othersPanel;
    private readonly System.Windows.Forms.Timer _saveTimer = new() { Interval = 800 };
    private bool _loading = true;
    private bool _syncing;
    private bool _syncAgain;

    public NotesForm(string file, AppData appData)
    {
        _file = file;
        _appData = appData;
        _notes = new NotesStore(appData);

        Text = Path.GetFileName(file) + " – Notes";
        Font = Theme.Ui;
        BackColor = Color.White;
        ClientSize = new Size(560, 560);
        MinimumSize = new Size(380, 300);
        StartPosition = FormStartPosition.CenterScreen;

        var title = new Label
        {
            Dock = DockStyle.Top,
            Height = 40,
            Text = Path.GetFileName(file),
            Font = new Font("Segoe UI Semibold", 12f),
            ForeColor = Theme.Text,
            Padding = new Padding(12, 10, 12, 0),
            AutoEllipsis = true,
        };

        _text = new TextBox
        {
            Dock = DockStyle.Fill,
            Multiline = true,
            ScrollBars = ScrollBars.Vertical,
            AcceptsReturn = true,
            AcceptsTab = true,
            BorderStyle = BorderStyle.None,
            Font = new Font("Segoe UI", 11f),
            PlaceholderText = "Write your notes about this document…",
        };
        var textHost = new Panel { Dock = DockStyle.Fill, Padding = new Padding(12, 6, 12, 6) };
        textHost.Controls.Add(_text);

        // Comments of other people / the web UI on the same DMS document (read-only)
        _othersTitle = new Label { Dock = DockStyle.Top, Height = 24, ForeColor = Theme.Muted, Padding = new Padding(0, 4, 0, 0) };
        _others = new ListBox
        {
            Dock = DockStyle.Fill,
            BorderStyle = BorderStyle.None,
            BackColor = Theme.Background,
            HorizontalScrollbar = true,
            SelectionMode = SelectionMode.None,
        };
        _othersPanel = new Panel { Dock = DockStyle.Bottom, Height = 140, Padding = new Padding(12, 0, 12, 8), Visible = false };
        _othersPanel.Controls.Add(_others);
        _othersPanel.Controls.Add(_othersTitle);

        _status = new Label { Dock = DockStyle.Fill, ForeColor = Theme.Muted, TextAlign = ContentAlignment.MiddleLeft, AutoEllipsis = true };
        _share = Theme.ToolbarButton("Share to DMS", async (_, _) => await ShareAsync());
        _share.Dock = DockStyle.Right;
        var delete = Theme.ToolbarButton("Delete note", (_, _) => DeleteNote());
        delete.Dock = DockStyle.Right;
        var footer = new Panel { Dock = DockStyle.Bottom, Height = 48, BackColor = Theme.Toolbar, Padding = new Padding(12, 8, 12, 8) };
        footer.Controls.Add(_status);
        footer.Controls.Add(_share);
        footer.Controls.Add(delete);

        Controls.Add(textHost);
        Controls.Add(_othersPanel);
        Controls.Add(footer);
        Controls.Add(title);

        _text.TextChanged += (_, _) =>
        {
            if (_loading)
            {
                return;
            }
            _status.Text = "Editing…";
            _saveTimer.Stop();
            _saveTimer.Start();
        };
        _saveTimer.Tick += async (_, _) => await SaveAsync();
    }

    protected override async void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        var note = await Task.Run(() => _notes.Load(_file));
        ShowText(note?.Text ?? "");
        _status.Text = note == null ? "No note yet" : $"Saved {note.UpdatedUtc.ToLocalTime():g}";
        UpdateShareButton(note);
        _loading = false;

        if (note?.Dms != null)
        {
            await SyncAsync();
        }
    }

    protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
    {
        if (keyData == (Keys.Control | Keys.S))
        {
            _ = SaveAsync();
            return true;
        }
        if (keyData == Keys.Escape)
        {
            Close();
            return true;
        }
        return base.ProcessCmdKey(ref msg, keyData);
    }

    private async Task SaveAsync()
    {
        _status.ForeColor = Theme.Muted;
        _saveTimer.Stop();
        _notes.Save(_file, _text.Text);
        bool empty = string.IsNullOrWhiteSpace(_text.Text);
        _status.Text = empty ? "Note removed (a shared DMS comment stays)" : $"Saved {DateTime.Now:t}";
        var note = _notes.Load(_file);
        UpdateShareButton(note);
        if (note?.Dms != null)
        {
            await SyncAsync();
        }
    }

    /// <summary>Publishes the note as a comment; uploads the document first if the user agrees.</summary>
    private async Task ShareAsync()
    {
        if (string.IsNullOrWhiteSpace(_text.Text))
        {
            _status.Text = "Write a note first.";
            return;
        }
        _status.ForeColor = Theme.Muted;
        _saveTimer.Stop();
        _notes.Save(_file, _text.Text);

        using var client = ConnectForm.EnsureConnected(this, _appData);
        if (client == null)
        {
            return;
        }
        _share.Enabled = false;
        try
        {
            var sync = new NoteSync(_notes, client);
            _status.Text = "Sharing…";
            var result = await sync.ShareAsync(_file);
            if (result == ShareResult.DocumentNotInDms)
            {
                if (MessageBox.Show(this, "This document is not in the DMS yet.\n\nUpload it and share the note?",
                        "Share to DMS", MessageBoxButtons.YesNo, MessageBoxIcon.Question) != DialogResult.Yes)
                {
                    _status.Text = "Not shared.";
                    return;
                }
                _status.Text = "Uploading…";
                await client.UploadAsync(_file);
                result = await sync.ShareAsync(_file);
            }
            _status.Text = result == ShareResult.DocumentNotInDms ? "Upload did not work – not shared." : "Shared as a DMS comment.";
            await ShowOtherCommentsAsync(sync);
        }
        catch (Exception ex) when (ex is DmsException or IOException)
        {
            ShowDmsError(ex);
        }
        finally
        {
            UpdateShareButton(_notes.Load(_file));
        }
    }

    /// <summary>Brings the note and its comment together; only one sync runs at a time.</summary>
    private async Task SyncAsync()
    {
        if (_syncing)
        {
            _syncAgain = true;
            return;
        }
        string? token = new TokenStore(_appData).Load();
        if (token == null)
        {
            _status.Text += " · not synced (DMS not connected)";
            return;
        }

        _syncing = true;
        try
        {
            using var client = new DmsClient(Settings.Load(_appData).DmsUrl, token);
            _status.ForeColor = Theme.Muted;
            var sync = new NoteSync(_notes, client);
            do
            {
                _syncAgain = false;
                var outcome = await sync.SyncAsync(_file);
                if (outcome != null)
                {
                    await HandleOutcomeAsync(sync, outcome);
                }
            }
            while (_syncAgain);
            if (_notes.Load(_file)?.Dms != null)
            {
                await ShowOtherCommentsAsync(sync);
            }
        }
        catch (DmsException ex)
        {
            ShowDmsError(ex);
        }
        finally
        {
            _syncing = false;
            UpdateShareButton(_notes.Load(_file));
        }
    }

    private async Task HandleOutcomeAsync(NoteSync sync, SyncOutcome outcome)
    {
        switch (outcome.Decision)
        {
            case SyncDecision.InSync:
            case SyncDecision.PushLocal:
                _status.Text = $"Saved and synced {DateTime.Now:t}";
                break;
            case SyncDecision.PullRemote:
                ShowText(outcome.RemoteText ?? "");
                _status.Text = "Updated from the DMS";
                break;
            case SyncDecision.RemoteDeleted:
                _status.Text = "The DMS comment was deleted – the note is local only now";
                _othersPanel.Visible = false;
                break;
            case SyncDecision.Conflict:
                var answer = MessageBox.Show(this,
                    "The note was changed here and in the DMS.\n\n" +
                    $"DMS version:\n{Shorten(outcome.RemoteText)}\n\n" +
                    "Keep your version? (No = take the DMS version)",
                    "Notes – conflict", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                bool keepLocal = answer == DialogResult.Yes;
                await sync.ResolveAsync(_file, keepLocal, outcome.RemoteText ?? "");
                if (!keepLocal)
                {
                    ShowText(outcome.RemoteText ?? "");
                }
                _status.Text = keepLocal ? "Your version was sent to the DMS" : "Took the DMS version";
                break;
        }
    }

    private async Task ShowOtherCommentsAsync(NoteSync sync)
    {
        if (_notes.Load(_file)?.Dms is not { } link)
        {
            return;
        }
        var others = await sync.OtherCommentsAsync(link);
        _others.BeginUpdate();
        _others.Items.Clear();
        foreach (var comment in others.OrderBy(c => c.CreatedAt))
        {
            _others.Items.Add($"{comment.CreatedAt:g}  {comment.Content.ReplaceLineEndings(" ")}");
        }
        _others.EndUpdate();
        _othersTitle.Text = others.Count == 1 ? "1 other comment in the DMS" : $"{others.Count} other comments in the DMS";
        _othersPanel.Visible = others.Count > 0;
    }

    private void ShowText(string text)
    {
        bool wasLoading = _loading;
        _loading = true;
        _text.Text = text;
        _text.SelectionStart = _text.TextLength;
        _loading = wasLoading;
    }

    private void UpdateShareButton(Note? note)
    {
        bool shared = note?.Dms != null;
        _share.Text = shared ? "✓ Shared" : "Share to DMS";
        _share.Enabled = !shared && !string.IsNullOrWhiteSpace(_text.Text);
    }

    private void ShowDmsError(Exception ex)
    {
        if (ex is DmsAuthException)
        {
            new TokenStore(_appData).Clear();
        }
        _status.Text = "Not synced: " + ex.Message;
        _status.ForeColor = Theme.Focus;
    }

    private static string Shorten(string? text) =>
        text == null ? "" : text.Length <= 300 ? text : text[..300] + "…";

    private void DeleteNote()
    {
        if (MessageBox.Show(this, "Delete the note for this document?\n\nA shared DMS comment is kept.", "Notes",
                MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes)
        {
            _text.Clear();
            _ = SaveAsync();
            _othersPanel.Visible = false;
        }
    }

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        if (_saveTimer.Enabled)
        {
            // Save locally right away; the DMS copy follows the next time the note is opened
            _saveTimer.Stop();
            _notes.Save(_file, _text.Text);
        }
        base.OnFormClosing(e);
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _saveTimer.Dispose();
        }
        base.Dispose(disposing);
    }
}
