using System.Net;
using System.Text;
using System.Text.Json;

namespace DmsExplorer.Core.Tests;

public class NoteSyncTests : IDisposable
{
    private readonly TestFiles _files = new();
    private readonly NotesStore _notes;
    private readonly FakeDms _dms = new();
    private readonly NoteSync _sync;
    private readonly string _pdf;

    public NoteSyncTests()
    {
        _notes = new NotesStore(new AppData(Path.Combine(_files.Root, "appdata")));
        _sync = new NoteSync(_notes, new DmsClient("http://dms", "dms_test", _dms));
        _pdf = _files.Pdf("doc.pdf", "content");
    }

    // ---------------------------------------------------------------- decision rule

    [Theory]
    [InlineData("base", "base", SyncDecision.InSync)]
    [InlineData("local edit", "base", SyncDecision.PushLocal)]
    [InlineData("base", "web edit", SyncDecision.PullRemote)]
    [InlineData("local edit", "web edit", SyncDecision.Conflict)]
    [InlineData("same edit", "same edit", SyncDecision.InSync)]
    [InlineData("base\r\n", "base\n", SyncDecision.InSync)]
    public void Decide_compares_both_sides_with_the_last_synced_text(string local, string remote, SyncDecision expected)
    {
        var link = new DmsLink(7, 3, "base");
        var comment = new DmsComment(3, remote, DateTime.Now, null);

        Assert.Equal(expected, NoteSync.Decide(local, link, comment));
    }

    [Fact]
    public void Decide_reports_a_deleted_comment()
    {
        Assert.Equal(SyncDecision.RemoteDeleted, NoteSync.Decide("text", new DmsLink(7, 3, "text"), null));
    }

    // ---------------------------------------------------------------- flows against a fake DMS

    [Fact]
    public async Task Share_adds_a_comment_and_links_the_note()
    {
        _notes.Save(_pdf, "My note");

        Assert.Equal(ShareResult.Shared, await _sync.ShareAsync(_pdf));

        var comment = Assert.Single(_dms.Comments);
        Assert.Equal("My note", comment.Content);
        Assert.Equal(new DmsLink(FakeDms.DocumentId, comment.Id, "My note"), _notes.Load(_pdf)!.Dms);
        Assert.Equal(ShareResult.AlreadyShared, await _sync.ShareAsync(_pdf));
    }

    [Fact]
    public async Task Share_reports_a_document_that_is_not_in_the_dms()
    {
        _dms.DocumentExists = false;
        _notes.Save(_pdf, "My note");

        Assert.Equal(ShareResult.DocumentNotInDms, await _sync.ShareAsync(_pdf));
        Assert.Empty(_dms.Comments);
        Assert.Null(_notes.Load(_pdf)!.Dms);
    }

    [Fact]
    public async Task Local_edits_are_pushed_and_the_link_is_kept()
    {
        await ShareAsync("first");
        _notes.Save(_pdf, "second");

        var outcome = await _sync.SyncAsync(_pdf);

        Assert.Equal(SyncDecision.PushLocal, outcome!.Decision);
        Assert.Equal("second", _dms.Comments[0].Content);
        Assert.Equal("second", _notes.Load(_pdf)!.Dms!.SyncedText);
    }

    [Fact]
    public async Task Edits_in_the_web_ui_are_taken_over()
    {
        await ShareAsync("first");
        _dms.Comments[0] = _dms.Comments[0] with { Content = "edited in the browser" };

        var outcome = await _sync.SyncAsync(_pdf);

        Assert.Equal(SyncDecision.PullRemote, outcome!.Decision);
        Assert.Equal("edited in the browser", _notes.Load(_pdf)!.Text);
    }

    [Fact]
    public async Task Conflicts_are_reported_and_resolved_by_the_users_choice()
    {
        await ShareAsync("first");
        _notes.Save(_pdf, "mine");
        _dms.Comments[0] = _dms.Comments[0] with { Content = "theirs" };

        var outcome = await _sync.SyncAsync(_pdf);
        Assert.Equal(SyncDecision.Conflict, outcome!.Decision);
        Assert.Equal("mine", _notes.Load(_pdf)!.Text);
        Assert.Equal("theirs", _dms.Comments[0].Content);

        await _sync.ResolveAsync(_pdf, keepLocal: true, outcome.RemoteText!);
        Assert.Equal("mine", _dms.Comments[0].Content);
        Assert.Equal(SyncDecision.InSync, (await _sync.SyncAsync(_pdf))!.Decision);
    }

    [Fact]
    public async Task Taking_the_dms_version_replaces_the_local_note()
    {
        await ShareAsync("first");
        _notes.Save(_pdf, "mine");
        _dms.Comments[0] = _dms.Comments[0] with { Content = "theirs" };

        await _sync.ResolveAsync(_pdf, keepLocal: false, "theirs");

        Assert.Equal("theirs", _notes.Load(_pdf)!.Text);
        Assert.Equal("theirs", _notes.Load(_pdf)!.Dms!.SyncedText);
    }

    [Fact]
    public async Task A_deleted_comment_unlinks_but_keeps_the_local_note()
    {
        await ShareAsync("first");
        _dms.Comments.Clear();

        var outcome = await _sync.SyncAsync(_pdf);

        Assert.Equal(SyncDecision.RemoteDeleted, outcome!.Decision);
        Assert.Equal("first", _notes.Load(_pdf)!.Text);
        Assert.Null(_notes.Load(_pdf)!.Dms);
    }

    [Fact]
    public async Task Other_comments_exclude_the_note_itself()
    {
        _dms.Comments.Add(new DmsComment(50, "from a colleague", DateTime.Now, null));
        await ShareAsync("my note");

        var others = await _sync.OtherCommentsAsync(_notes.Load(_pdf)!.Dms!);

        Assert.Equal("from a colleague", Assert.Single(others).Content);
    }

    [Fact]
    public async Task Unshared_notes_are_not_synced()
    {
        _notes.Save(_pdf, "local only");

        Assert.Null(await _sync.SyncAsync(_pdf));
        Assert.Empty(_dms.Requests);
    }

    private async Task ShareAsync(string text)
    {
        _notes.Save(_pdf, text);
        Assert.Equal(ShareResult.Shared, await _sync.ShareAsync(_pdf));
    }

    public void Dispose() => _files.Dispose();

    /// <summary>Minimal in-memory DMS: one document with comments.</summary>
    private sealed class FakeDms : HttpMessageHandler
    {
        public const long DocumentId = 7;
        private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);
        private long _nextId = 100;

        public bool DocumentExists { get; set; } = true;
        public List<DmsComment> Comments { get; } = [];
        public List<string> Requests { get; } = [];

        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
        {
            string path = request.RequestUri!.AbsolutePath;
            string body = request.Content == null ? "" : await request.Content.ReadAsStringAsync(ct);
            Requests.Add($"{request.Method} {path}");

            if (path.StartsWith("/api/documents/by-hash/"))
            {
                return DocumentExists
                    ? Ok(new DmsDocument(DocumentId, "doc.pdf", 10, null))
                    : new HttpResponseMessage(HttpStatusCode.NotFound);
            }
            if (path == $"/api/comments/document/{DocumentId}" && request.Method == HttpMethod.Get)
            {
                return Ok(Comments);
            }
            if (path == $"/api/comments/document/{DocumentId}" && request.Method == HttpMethod.Post)
            {
                var comment = new DmsComment(_nextId++, body, DateTime.Now, null);
                Comments.Add(comment);
                return Ok(comment);
            }
            if (path.StartsWith("/api/comments/") && request.Method == HttpMethod.Put)
            {
                long id = long.Parse(path["/api/comments/".Length..]);
                int index = Comments.FindIndex(c => c.Id == id);
                if (index < 0)
                {
                    return new HttpResponseMessage(HttpStatusCode.NotFound);
                }
                Comments[index] = Comments[index] with { Content = body, UpdatedAt = DateTime.Now };
                return Ok(Comments[index]);
            }
            return new HttpResponseMessage(HttpStatusCode.NotFound);
        }

        private static HttpResponseMessage Ok<T>(T value) => new(HttpStatusCode.OK)
        {
            Content = new StringContent(JsonSerializer.Serialize(value, Json), Encoding.UTF8, "application/json"),
        };
    }
}
