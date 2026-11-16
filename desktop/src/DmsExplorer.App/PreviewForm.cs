using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// The preview shown in the Explorer preview pane: action bar on top, rendered PDF pages below.
/// When started with a parent window (from the preview handler) it embeds itself into it.
/// </summary>
internal sealed class PreviewForm : Form
{
    private const int MaxPreviewPages = 30;
    private const int RenderWidth = 1100;

    private readonly string _file;
    private readonly AppData _appData;
    private readonly IntPtr _parent;
    private readonly NotesStore _notes;
    private readonly FlowLayoutPanel _pages;
    private readonly Label _status;
    private readonly Label _noteStrip;
    private readonly System.Windows.Forms.Timer _hostTimer = new() { Interval = 200 };
    private readonly System.Windows.Forms.Timer _noteTimer = new() { Interval = 2000 };
    private readonly List<PictureBox> _pageBoxes = [];
    private string? _fileKey;
    private Native.RECT _lastParentRect;

    public PreviewForm(string file, AppData appData, IntPtr parent)
    {
        _file = file;
        _appData = appData;
        _parent = parent;
        _notes = new NotesStore(appData);

        Text = Path.GetFileName(file) + " – DmsExplorer";
        BackColor = Theme.Background;
        Font = Theme.Ui;
        ClientSize = new Size(720, 900);
        if (parent != IntPtr.Zero)
        {
            FormBorderStyle = FormBorderStyle.None;
            ShowInTaskbar = false;
            StartPosition = FormStartPosition.Manual;
        }

        var actions = new FlowLayoutPanel
        {
            Dock = DockStyle.Fill,
            WrapContents = false,
            AutoSize = false,
            Padding = new Padding(0),
        };
        actions.Controls.Add(Theme.ToolbarButton("⚡ Fast reader", (_, _) => Program.Launch("reader", _file)));
        actions.Controls.Add(Theme.ToolbarButton("📝 Notes", (_, _) => Program.Launch("notes", _file)));
        actions.Controls.Add(Theme.ToolbarButton("↗ Open in DMS", (_, _) => OpenInDms()));

        _status = new Label
        {
            Dock = DockStyle.Right,
            AutoSize = true,
            ForeColor = Theme.Muted,
            TextAlign = ContentAlignment.MiddleRight,
            Padding = new Padding(0, 8, 4, 0),
        };

        var toolbar = new Panel { Dock = DockStyle.Top, Height = 44, BackColor = Theme.Toolbar, Padding = new Padding(8, 7, 8, 7) };
        toolbar.Controls.Add(actions);
        toolbar.Controls.Add(_status);

        _noteStrip = new Label
        {
            Dock = DockStyle.Top,
            Height = 30,
            Visible = false,
            BackColor = Theme.NoteBackground,
            ForeColor = Theme.Text,
            Padding = new Padding(10, 0, 10, 0),
            TextAlign = ContentAlignment.MiddleLeft,
            AutoEllipsis = true,
            Cursor = Cursors.Hand,
        };
        _noteStrip.Click += (_, _) => Program.Launch("notes", _file);

        _pages = new FlowLayoutPanel
        {
            Dock = DockStyle.Fill,
            AutoScroll = true,
            FlowDirection = FlowDirection.TopDown,
            WrapContents = false,
            Padding = new Padding(12),
            BackColor = Theme.Background,
        };
        _pages.Resize += (_, _) => LayoutPages();

        Controls.Add(_pages);
        Controls.Add(_noteStrip);
        Controls.Add(toolbar);

        _hostTimer.Tick += (_, _) => FollowParent();
        _noteTimer.Tick += (_, _) => RefreshNote();
    }

    protected override async void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        if (_parent != IntPtr.Zero)
        {
            Native.EmbedInto(Handle, _parent);
            FollowParent();
            _hostTimer.Start();
        }

        _fileKey = await Task.Run(() => FileKey.Of(_file));
        RefreshNote();
        _noteTimer.Start();
        await RenderPagesAsync();
    }

    private async Task RenderPagesAsync()
    {
        _status.Text = "Loading…";
        try
        {
            var document = await PdfRenderer.OpenAsync(_file);
            uint count = Math.Min(document.PageCount, MaxPreviewPages);
            _status.Text = document.PageCount == 1 ? "1 page" : $"{document.PageCount} pages";

            for (uint i = 0; i < count; i++)
            {
                var image = await PdfRenderer.RenderPageAsync(document, i, RenderWidth);
                var box = new PictureBox
                {
                    Image = image,
                    SizeMode = PictureBoxSizeMode.Zoom,
                    BackColor = Color.White,
                    Margin = new Padding(0, 0, 0, 12),
                    BorderStyle = BorderStyle.FixedSingle,
                };
                _pageBoxes.Add(box);
                _pages.Controls.Add(box);
                LayoutPage(box);
                if (i == 0)
                {
                    // Adding controls can scroll the panel; always start at the top of page 1
                    _pages.AutoScrollPosition = Point.Empty;
                }
            }
            if (document.PageCount > count)
            {
                _pages.Controls.Add(new Label
                {
                    AutoSize = true,
                    ForeColor = Theme.Muted,
                    Text = $"Preview shows the first {count} pages – open the fast reader or the DMS for the full document.",
                });
            }
        }
        catch (Exception ex)
        {
            _status.Text = "";
            _pages.Controls.Add(new Label
            {
                AutoSize = true,
                ForeColor = Theme.Focus,
                Text = "This PDF cannot be previewed: " + ex.Message,
            });
        }
    }

    private void LayoutPages()
    {
        _pages.SuspendLayout();
        foreach (var box in _pageBoxes)
        {
            LayoutPage(box);
        }
        _pages.ResumeLayout();
    }

    private void LayoutPage(PictureBox box)
    {
        if (box.Image == null)
        {
            return;
        }
        int width = Math.Max(_pages.ClientSize.Width - _pages.Padding.Horizontal - SystemInformation.VerticalScrollBarWidth, 50);
        box.Size = new Size(width, width * box.Image.Height / box.Image.Width);
    }

    private void RefreshNote()
    {
        if (_fileKey == null)
        {
            return;
        }
        var note = _notes.LoadByKey(_fileKey);
        string? firstLine = note?.Text.Split('\n', StringSplitOptions.RemoveEmptyEntries).FirstOrDefault()?.Trim();
        _noteStrip.Visible = !string.IsNullOrEmpty(firstLine);
        _noteStrip.Text = "📝  " + firstLine;
    }

    private void OpenInDms()
    {
        // Step 2 of the integration will look the document up by its content hash;
        // until then the dashboard is opened.
        Program.OpenInBrowser(Settings.Load(_appData).DmsUrl.TrimEnd('/') + "/dashboard.html");
    }

    /// <summary>Keeps the embedded window the size of the preview pane; exits when the pane is gone.</summary>
    private void FollowParent()
    {
        if (!Native.IsWindow(_parent))
        {
            Close();
            return;
        }
        if (Native.GetClientRect(_parent, out var rect) &&
            (rect.Width != _lastParentRect.Width || rect.Height != _lastParentRect.Height))
        {
            _lastParentRect = rect;
            Native.MoveWindow(Handle, 0, 0, rect.Width, rect.Height, true);
        }
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _hostTimer.Dispose();
            _noteTimer.Dispose();
            foreach (var box in _pageBoxes)
            {
                box.Image?.Dispose();
            }
        }
        base.Dispose(disposing);
    }
}
