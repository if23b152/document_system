using System.Net.Http.Json;
using System.Text.Json;

namespace DmsExplorer.Core.Tests;

/// <summary>
/// The desktop logic against a running REST server. Skipped unless DMS_E2E_URL is set, e.g. to the
/// server started with rest-server's E2eServer (no Docker needed).
/// </summary>
public class RealServerTests : IDisposable
{
    private readonly TestFiles _files = new();

    [RealServerFact]
    public async Task Token_identifies_the_user_and_a_wrong_token_is_rejected()
    {
        var (name, client) = await ConnectAsync();

        Assert.Equal(name, (await client.GetCurrentUserAsync()).Username);
        using var wrong = new DmsClient(RealServerFactAttribute.Url!, "dms_wrong");
        await Assert.ThrowsAsync<DmsAuthException>(() => wrong.GetCurrentUserAsync());
    }

    [RealServerFact]
    public async Task Uploaded_file_is_found_by_the_hash_the_desktop_app_computes()
    {
        var (_, client) = await ConnectAsync();
        string pdf = _files.Pdf("report.pdf", Guid.NewGuid().ToString());

        Assert.Null(await client.FindByHashAsync(FileKey.Of(pdf)));
        var uploaded = await client.UploadAsync(pdf);

        Assert.Equal(FileKey.Of(pdf), uploaded.ContentSha256);
        Assert.Equal(uploaded.Id, (await client.FindByHashAsync(FileKey.Of(pdf)))!.Id);
    }

    [RealServerFact]
    public async Task File_names_with_umlauts_survive_the_upload()
    {
        var (_, client) = await ConnectAsync();
        string pdf = _files.Pdf("Prüfung Übersicht.pdf", Guid.NewGuid().ToString());

        Assert.Equal("Prüfung Übersicht.pdf", (await client.UploadAsync(pdf)).FileName);
    }

    [RealServerFact]
    public async Task Note_is_shared_pushed_pulled_and_conflicts_are_detected()
    {
        var (_, client) = await ConnectAsync();
        var notes = new NotesStore(new AppData(Path.Combine(_files.Root, "appdata")));
        var sync = new NoteSync(notes, client);
        string pdf = _files.Pdf("doc.pdf", Guid.NewGuid().ToString());

        notes.Save(pdf, "Größe prüfen\r\nzweite Zeile");
        Assert.Equal(ShareResult.DocumentNotInDms, await sync.ShareAsync(pdf));
        await client.UploadAsync(pdf);
        Assert.Equal(ShareResult.Shared, await sync.ShareAsync(pdf));
        Assert.Equal(SyncDecision.InSync, (await sync.SyncAsync(pdf))!.Decision);

        notes.Save(pdf, "lokal geändert");
        Assert.Equal(SyncDecision.PushLocal, (await sync.SyncAsync(pdf))!.Decision);
        var link = notes.Load(pdf)!.Dms!;
        var comment = Assert.Single(await client.GetCommentsAsync(link.DocumentId));
        Assert.Equal("lokal geändert", comment.Content);
        Assert.NotNull(comment.UpdatedAt);

        await client.UpdateCommentAsync(link.CommentId, "im Web geändert");
        Assert.Equal(SyncDecision.PullRemote, (await sync.SyncAsync(pdf))!.Decision);
        Assert.Equal("im Web geändert", notes.Load(pdf)!.Text);

        notes.Save(pdf, "meine Version");
        await client.UpdateCommentAsync(link.CommentId, "deren Version");
        Assert.Equal(SyncDecision.Conflict, (await sync.SyncAsync(pdf))!.Decision);
        await sync.ResolveAsync(pdf, keepLocal: true, "deren Version");
        Assert.Equal(SyncDecision.InSync, (await sync.SyncAsync(pdf))!.Decision);
    }

    [RealServerFact]
    public async Task Sharing_a_note_above_the_comment_limit_is_a_readable_error_and_keeps_the_note()
    {
        var (_, client) = await ConnectAsync();
        var notes = new NotesStore(new AppData(Path.Combine(_files.Root, "appdata")));
        string pdf = _files.Pdf("doc.pdf", Guid.NewGuid().ToString());
        await client.UploadAsync(pdf);
        notes.Save(pdf, new string('x', 2001));

        var error = await Assert.ThrowsAsync<DmsException>(() => new NoteSync(notes, client).ShareAsync(pdf));

        Assert.Contains("2000", error.Message);
        Assert.Equal(2001, notes.Load(pdf)!.Text.Length);
    }

    /// <summary>Registers a new user in a browser-like session and creates a token for the client.</summary>
    private static async Task<(string Name, DmsClient Client)> ConnectAsync()
    {
        string name = "d" + Guid.NewGuid().ToString("N")[..12];
        using var browser = new HttpClient { BaseAddress = new Uri(RealServerFactAttribute.Url!) };
        var registered = await browser.PostAsJsonAsync("api/auth/register",
            new { username = name, email = $"{name}@example.org", password = $"Test-{name}-pw1" });
        registered.EnsureSuccessStatusCode();
        var created = await browser.PostAsJsonAsync("api/tokens", new { name = "e2e" });
        created.EnsureSuccessStatusCode();
        string secret = (await created.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("secret").GetString()!;
        return (name, new DmsClient(RealServerFactAttribute.Url!, secret));
    }

    public void Dispose() => _files.Dispose();
}

public sealed class RealServerFactAttribute : FactAttribute
{
    public static string? Url => Environment.GetEnvironmentVariable("DMS_E2E_URL");

    public RealServerFactAttribute()
    {
        if (string.IsNullOrEmpty(Url))
        {
            Skip = "Set DMS_E2E_URL to run against a server";
        }
    }
}
