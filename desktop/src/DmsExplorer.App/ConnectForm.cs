using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// Connects the app to the DMS: web address plus a personal access token. The connection is
/// tested (who am I?) before anything is saved; the token is stored encrypted.
/// </summary>
internal sealed class ConnectForm : Form
{
    private readonly AppData _appData;
    private readonly TokenStore _tokens;
    private readonly TextBox _url;
    private readonly TextBox _token;
    private readonly Label _status;
    private readonly Button _connect;
    private readonly Button _disconnect;

    public ConnectForm(AppData appData)
    {
        _appData = appData;
        _tokens = new TokenStore(appData);
        var settings = Settings.Load(appData);
        bool connected = _tokens.Load() != null;

        Text = "Connect to the Document System";
        Font = Theme.Ui;
        BackColor = Color.White;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        ClientSize = new Size(500, 300);

        _url = new TextBox { Text = settings.DmsUrl, Dock = DockStyle.Fill };
        _token = new TextBox
        {
            Dock = DockStyle.Fill,
            UseSystemPasswordChar = true,
            PlaceholderText = connected ? "(saved – paste a new token to replace it)" : "dms_…",
        };
        var hint = new Label
        {
            AutoSize = true,
            MaximumSize = new Size(460, 0),
            ForeColor = Theme.Muted,
            Text = "Create a personal access token in the DMS (POST /api/tokens while logged in). " +
                   "It is stored encrypted for your Windows account and can be revoked at any time.",
        };
        _status = new Label { AutoSize = true, MaximumSize = new Size(460, 0), ForeColor = Theme.Muted };
        if (connected)
        {
            _status.Text = "A token is saved.";
        }

        _connect = Theme.ToolbarButton("Test and save", async (_, _) => await ConnectAsync());
        _disconnect = Theme.ToolbarButton("Remove token", (_, _) => Disconnect());
        _disconnect.Enabled = connected;
        var cancel = Theme.ToolbarButton("Close", (_, _) => Close());
        AcceptButton = _connect;
        CancelButton = cancel;

        var buttons = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.RightToLeft, AutoSize = true };
        buttons.Controls.AddRange([cancel, _connect, _disconnect]);

        var layout = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            Padding = new Padding(16),
            ColumnCount = 2,
            RowCount = 5,
        };
        layout.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        layout.Controls.Add(new Label { Text = "DMS address", AutoSize = true, Padding = new Padding(0, 6, 8, 0) }, 0, 0);
        layout.Controls.Add(_url, 1, 0);
        layout.Controls.Add(new Label { Text = "Access token", AutoSize = true, Padding = new Padding(0, 6, 8, 0) }, 0, 1);
        layout.Controls.Add(_token, 1, 1);
        layout.Controls.Add(hint, 0, 2);
        layout.SetColumnSpan(hint, 2);
        layout.Controls.Add(_status, 0, 3);
        layout.SetColumnSpan(_status, 2);
        layout.Controls.Add(buttons, 0, 4);
        layout.SetColumnSpan(buttons, 2);
        Controls.Add(layout);
    }

    /// <summary>Returns a client for the saved connection, asking the user to connect first if needed.</summary>
    public static DmsClient? EnsureConnected(IWin32Window? owner, AppData appData)
    {
        string? token = new TokenStore(appData).Load();
        if (token == null)
        {
            using var form = new ConnectForm(appData);
            if (form.ShowDialog(owner) != DialogResult.OK)
            {
                return null;
            }
            token = new TokenStore(appData).Load();
        }
        return token == null ? null : new DmsClient(Settings.Load(appData).DmsUrl, token);
    }

    private async Task ConnectAsync()
    {
        string url = _url.Text.Trim();
        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri) || (uri.Scheme != "http" && uri.Scheme != "https"))
        {
            ShowStatus("Enter the address of the DMS, e.g. http://localhost:8080", error: true);
            return;
        }
        string token = _token.Text.Trim();
        if (token.Length == 0)
        {
            token = _tokens.Load() ?? "";
        }
        if (!token.StartsWith("dms_", StringComparison.Ordinal))
        {
            ShowStatus("Paste a token that starts with dms_", error: true);
            return;
        }

        _connect.Enabled = false;
        ShowStatus("Connecting…", error: false);
        try
        {
            using var client = new DmsClient(url, token);
            var user = await client.GetCurrentUserAsync();

            var settings = Settings.Load(_appData);
            settings.DmsUrl = url;
            settings.Save(_appData);
            _tokens.Save(token);

            ShowStatus($"Connected as {user.Username}.", error: false);
            DialogResult = DialogResult.OK;
            Close();
        }
        catch (DmsException e)
        {
            ShowStatus(e.Message, error: true);
        }
        finally
        {
            _connect.Enabled = true;
        }
    }

    private void Disconnect()
    {
        _tokens.Clear();
        _token.Clear();
        _token.PlaceholderText = "dms_…";
        _disconnect.Enabled = false;
        ShowStatus("Token removed. Revoke it in the DMS as well if you no longer need it.", error: false);
    }

    private void ShowStatus(string text, bool error)
    {
        _status.Text = text;
        _status.ForeColor = error ? Theme.Focus : Theme.Muted;
    }
}
