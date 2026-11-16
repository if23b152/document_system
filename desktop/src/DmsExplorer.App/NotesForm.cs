using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>Note editor for one document; saves automatically shortly after typing stops.</summary>
internal sealed class NotesForm : Form
{
    private readonly string _file;
    private readonly NotesStore _notes;
    private readonly TextBox _text;
    private readonly Label _status;
    private readonly System.Windows.Forms.Timer _saveTimer = new() { Interval = 800 };
    private bool _loading = true;

    public NotesForm(string file, AppData appData)
    {
        _file = file;
        _notes = new NotesStore(appData);

        Text = Path.GetFileName(file) + " – Notes";
        Font = Theme.Ui;
        BackColor = Color.White;
        ClientSize = new Size(560, 520);
        MinimumSize = new Size(360, 260);
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

        _status = new Label { Dock = DockStyle.Fill, ForeColor = Theme.Muted, TextAlign = ContentAlignment.MiddleLeft };
        var delete = Theme.ToolbarButton("Delete note", (_, _) => DeleteNote());
        delete.Dock = DockStyle.Right;
        var footer = new Panel { Dock = DockStyle.Bottom, Height = 48, BackColor = Theme.Toolbar, Padding = new Padding(12, 8, 12, 8) };
        footer.Controls.Add(_status);
        footer.Controls.Add(delete);

        Controls.Add(textHost);
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
        _saveTimer.Tick += (_, _) => Save();
    }

    protected override async void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        var note = await Task.Run(() => _notes.Load(_file));
        _text.Text = note?.Text ?? "";
        _text.SelectionStart = _text.TextLength;
        _status.Text = note == null ? "No note yet" : $"Saved {note.UpdatedUtc.ToLocalTime():g}";
        _loading = false;
    }

    protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
    {
        if (keyData == (Keys.Control | Keys.S))
        {
            Save();
            return true;
        }
        if (keyData == Keys.Escape)
        {
            Close();
            return true;
        }
        return base.ProcessCmdKey(ref msg, keyData);
    }

    private void Save()
    {
        _saveTimer.Stop();
        _notes.Save(_file, _text.Text);
        _status.Text = string.IsNullOrWhiteSpace(_text.Text) ? "Note removed" : $"Saved {DateTime.Now:t}";
    }

    private void DeleteNote()
    {
        if (MessageBox.Show(this, "Delete the note for this document?", "Notes",
                MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes)
        {
            _text.Clear();
            Save();
        }
    }

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        if (_saveTimer.Enabled)
        {
            Save();
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
