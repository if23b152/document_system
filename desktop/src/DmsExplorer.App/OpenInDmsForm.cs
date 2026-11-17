using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// Small progress window for "Open in DMS": finds the document by its content hash and opens its
/// page in the browser; if it is not in the DMS yet, offers to upload it first.
/// Runs as its own process so no dialog ever appears inside the Explorer preview pane.
/// </summary>
internal sealed class OpenInDmsForm : Form
{
    private readonly string _file;
    private readonly AppData _appData;
    private readonly Label _status;

    public OpenInDmsForm(string file, AppData appData)
    {
        _file = file;
        _appData = appData;

        Text = "Open in DMS";
        Font = Theme.Ui;
        BackColor = Color.White;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        ClientSize = new Size(420, 110);

        var title = new Label
        {
            Dock = DockStyle.Top,
            Height = 34,
            Text = Path.GetFileName(file),
            Font = Theme.UiBold,
            AutoEllipsis = true,
            Padding = new Padding(16, 12, 16, 0),
        };
        _status = new Label { Dock = DockStyle.Top, Height = 30, ForeColor = Theme.Muted, Padding = new Padding(16, 6, 16, 0) };
        var progress = new ProgressBar { Dock = DockStyle.Top, Height = 6, Style = ProgressBarStyle.Marquee };
        var progressHost = new Panel { Dock = DockStyle.Top, Height = 30, Padding = new Padding(16, 8, 16, 16) };
        progressHost.Controls.Add(progress);

        Controls.Add(progressHost);
        Controls.Add(_status);
        Controls.Add(title);
    }

    protected override async void OnShown(EventArgs e)
    {
        base.OnShown(e);
        await RunAsync();
        Close();
    }

    private async Task RunAsync()
    {
        using var client = ConnectForm.EnsureConnected(this, _appData);
        if (client == null)
        {
            return;
        }
        try
        {
            _status.Text = "Looking for this document in the DMS…";
            string key = await Task.Run(() => FileKey.Of(_file));
            var document = await client.FindByHashAsync(key);

            if (document == null)
            {
                var answer = MessageBox.Show(this,
                    $"\"{Path.GetFileName(_file)}\" is not in the DMS yet.\n\nUpload it now?",
                    "Open in DMS", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (answer != DialogResult.Yes)
                {
                    return;
                }
                _status.Text = "Uploading…";
                document = await client.UploadAsync(_file);
            }

            Program.OpenInBrowser(client.DocumentPage(document.Id).ToString());
        }
        catch (DmsAuthException ex)
        {
            new TokenStore(_appData).Clear();
            MessageBox.Show(this, ex.Message, "Open in DMS", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
        catch (DmsException ex)
        {
            MessageBox.Show(this, ex.Message, "Open in DMS", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        catch (IOException ex)
        {
            MessageBox.Show(this, "The file cannot be read: " + ex.Message, "Open in DMS",
                MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
