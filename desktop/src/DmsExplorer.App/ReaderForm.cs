using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// RSVP fast reader: shows one word at a time with the focus letter highlighted,
/// like the reader mode of the web UI – but working directly on the local PDF.
/// Keys: Space = start/pause, ←/→ = step, ↑/↓ = speed, Home = restart.
/// </summary>
internal sealed class ReaderForm : Form
{
    private readonly string _file;
    private readonly ReaderProgress _progress;
    private readonly WordView _word = new() { Dock = DockStyle.Fill };
    private readonly TrackBar _speed;
    private readonly Label _speedLabel;
    private readonly Label _position;
    private readonly ProgressBar _bar;
    private readonly Button _play;
    private readonly System.Windows.Forms.Timer _timer = new();
    private IReadOnlyList<string> _words = [];
    private string? _fileKey;
    private int _index;

    public ReaderForm(string file, AppData appData)
    {
        _file = file;
        _progress = new ReaderProgress(appData);
        int wpm = Settings.Load(appData).ReaderWpm;

        Text = Path.GetFileName(file) + " – Fast reader";
        Font = Theme.Ui;
        BackColor = Color.White;
        ClientSize = new Size(820, 380);
        MinimumSize = new Size(560, 300);
        StartPosition = FormStartPosition.CenterScreen;
        KeyPreview = true;

        _play = Theme.ToolbarButton("▶ Start", (_, _) => TogglePlay());
        var back = Theme.ToolbarButton("◀", (_, _) => Step(-1));
        var forward = Theme.ToolbarButton("▶", (_, _) => Step(1));
        var restart = Theme.ToolbarButton("⟲ Restart", (_, _) => { Pause(); Show(0); });

        _speed = new TrackBar
        {
            Minimum = Rsvp.MinWpm,
            Maximum = Rsvp.MaxWpm,
            SmallChange = 25,
            LargeChange = 100,
            TickFrequency = 100,
            Value = Math.Clamp(wpm, Rsvp.MinWpm, Rsvp.MaxWpm),
            Width = 220,
            AutoSize = false,
            Height = 30,
            TabStop = false,
        };
        _speedLabel = new Label { AutoSize = true, ForeColor = Theme.Muted, Padding = new Padding(0, 6, 0, 0) };
        _speed.ValueChanged += (_, _) => UpdateSpeedLabel();

        var controls = new FlowLayoutPanel
        {
            Dock = DockStyle.Bottom,
            Height = 46,
            Padding = new Padding(10, 8, 10, 6),
            BackColor = Theme.Toolbar,
            WrapContents = false,
        };
        controls.Controls.AddRange([_play, back, forward, restart, _speed, _speedLabel]);

        _position = new Label { Dock = DockStyle.Right, Width = 130, ForeColor = Theme.Muted, TextAlign = ContentAlignment.MiddleRight, Padding = new Padding(0, 0, 10, 0) };
        _bar = new ProgressBar { Dock = DockStyle.Fill, Height = 6, Style = ProgressBarStyle.Continuous };
        var progressRow = new Panel { Dock = DockStyle.Bottom, Height = 30, Padding = new Padding(10, 12, 0, 12) };
        progressRow.Controls.Add(_bar);
        progressRow.Controls.Add(_position);

        Controls.Add(_word);
        Controls.Add(progressRow);
        Controls.Add(controls);

        _timer.Tick += (_, _) => Advance();
        UpdateSpeedLabel();
    }

    protected override async void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        _word.Word = "Loading…";
        try
        {
            (_fileKey, _words) = await Task.Run(() => (FileKey.Of(_file), PdfText.Words(_file)));
        }
        catch (Exception ex)
        {
            _word.Word = "Cannot read this PDF";
            _position.Text = ex.Message;
            return;
        }

        if (_words.Count == 0)
        {
            // Scanned PDFs have no text layer – the DMS worker runs OCR for those
            _word.Word = "No text found";
            _position.Text = "Scanned PDF? Upload it to the DMS for OCR.";
            _play.Enabled = false;
            return;
        }

        var saved = _progress.Get(_fileKey);
        if (saved != null)
        {
            _speed.Value = Math.Clamp(saved.Wpm, Rsvp.MinWpm, Rsvp.MaxWpm);
        }
        _bar.Maximum = _words.Count - 1;
        Show(saved != null && saved.WordIndex < _words.Count ? saved.WordIndex : 0);
    }

    protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
    {
        switch (keyData)
        {
            case Keys.Space: TogglePlay(); return true;
            case Keys.Left: Step(-1); return true;
            case Keys.Right: Step(1); return true;
            case Keys.Up: _speed.Value = Math.Min(_speed.Maximum, _speed.Value + 25); return true;
            case Keys.Down: _speed.Value = Math.Max(_speed.Minimum, _speed.Value - 25); return true;
            case Keys.Home: Pause(); Show(0); return true;
            case Keys.Escape: Close(); return true;
        }
        return base.ProcessCmdKey(ref msg, keyData);
    }

    private void TogglePlay()
    {
        if (_words.Count == 0)
        {
            return;
        }
        if (_timer.Enabled)
        {
            Pause();
            return;
        }
        if (_index >= _words.Count - 1)
        {
            Show(0);
        }
        _play.Text = "⏸ Pause";
        ScheduleNext();
    }

    private void Pause()
    {
        _timer.Stop();
        _play.Text = "▶ Start";
    }

    private void Step(int delta)
    {
        Pause();
        Show(Math.Clamp(_index + delta, 0, Math.Max(_words.Count - 1, 0)));
    }

    private void Advance()
    {
        _timer.Stop();
        if (_index >= _words.Count - 1)
        {
            Pause();
            return;
        }
        Show(_index + 1);
        ScheduleNext();
    }

    private void ScheduleNext()
    {
        _timer.Interval = Math.Max(1, (int)Rsvp.DelayFor(_words[_index], _speed.Value).TotalMilliseconds);
        _timer.Start();
    }

    private void Show(int index)
    {
        if (_words.Count == 0)
        {
            return;
        }
        _index = index;
        _word.Word = _words[index];
        _bar.Value = index;
        _position.Text = $"{index + 1:N0} / {_words.Count:N0}";
    }

    private void UpdateSpeedLabel() => _speedLabel.Text = $"{_speed.Value} wpm";

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        if (_fileKey != null && _words.Count > 0)
        {
            _progress.Set(_fileKey, new ReaderProgress.Entry(_index, _speed.Value));
        }
        base.OnFormClosing(e);
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _timer.Dispose();
        }
        base.Dispose(disposing);
    }

    /// <summary>Draws the current word so that its focus letter always sits at the same spot.</summary>
    private sealed class WordView : Control
    {
        private readonly Font _font = new("Segoe UI", 40f);
        private string _wordText = "";

        public WordView()
        {
            DoubleBuffered = true;
            BackColor = Color.White;
            SetStyle(ControlStyles.ResizeRedraw, true);
        }

        public string Word
        {
            get => _wordText;
            set { _wordText = value; Invalidate(); }
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.AntiAliasGridFit;
            int centerX = Width * 2 / 5;
            int centerY = Height / 2;

            // Guide lines above and below the focus point
            using (var guide = new Pen(Theme.Border, 2))
            {
                g.DrawLine(guide, 40, centerY - 45, Width - 40, centerY - 45);
                g.DrawLine(guide, 40, centerY + 45, Width - 40, centerY + 45);
                g.DrawLine(guide, centerX, centerY - 45, centerX, centerY - 32);
                g.DrawLine(guide, centerX, centerY + 32, centerX, centerY + 45);
            }

            if (_wordText.Length == 0)
            {
                return;
            }
            int focus = Math.Min(Rsvp.FocusIndex(_wordText), _wordText.Length - 1);
            string before = _wordText[..focus];
            string letter = _wordText[focus].ToString();
            string after = _wordText[(focus + 1)..];

            var flags = TextFormatFlags.NoPadding | TextFormatFlags.SingleLine;
            Size beforeSize = TextRenderer.MeasureText(g, before, _font, Size.Empty, flags);
            Size letterSize = TextRenderer.MeasureText(g, letter, _font, Size.Empty, flags);
            int letterX = centerX - letterSize.Width / 2;
            int y = centerY - letterSize.Height / 2;

            TextRenderer.DrawText(g, before, _font, new Point(letterX - beforeSize.Width, y), Theme.Text, flags);
            TextRenderer.DrawText(g, letter, _font, new Point(letterX, y), Theme.Focus, flags);
            TextRenderer.DrawText(g, after, _font, new Point(letterX + letterSize.Width, y), Theme.Text, flags);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _font.Dispose();
            }
            base.Dispose(disposing);
        }
    }
}
