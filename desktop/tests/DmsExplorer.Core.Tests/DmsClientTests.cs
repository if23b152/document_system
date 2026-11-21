using System.Net;
using System.Text;

namespace DmsExplorer.Core.Tests;

public class DmsClientTests : IDisposable
{
    private const string Hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private readonly TestFiles _files = new();
    private readonly StubHandler _server = new();

    private DmsClient Client() => new("http://localhost:8080", "dms_test", _server);

    [Fact]
    public async Task FindByHash_returns_the_document_and_sends_the_token()
    {
        _server.Respond(HttpStatusCode.OK, $$"""{"id":7,"fileName":"a.pdf","fileSize":10,"contentSha256":"{{Hash}}"}""");

        var document = await Client().FindByHashAsync(Hash);

        Assert.Equal(new DmsDocument(7, "a.pdf", 10, Hash), document);
        var request = Assert.Single(_server.Requests);
        Assert.Equal($"http://localhost:8080/api/documents/by-hash/{Hash}", request.Uri);
        Assert.Equal("Bearer dms_test", request.Authorization);
    }

    [Fact]
    public async Task FindByHash_returns_null_when_the_document_is_not_in_the_dms()
    {
        _server.Respond(HttpStatusCode.NotFound);

        Assert.Null(await Client().FindByHashAsync(Hash));
    }

    [Theory]
    [InlineData(HttpStatusCode.Unauthorized)]
    [InlineData(HttpStatusCode.Forbidden)]
    public async Task Rejected_token_raises_an_auth_error(HttpStatusCode status)
    {
        _server.Respond(status);

        await Assert.ThrowsAsync<DmsAuthException>(() => Client().FindByHashAsync(Hash));
    }

    [Fact]
    public async Task Unreachable_server_raises_a_readable_error()
    {
        _server.Fail(new HttpRequestException("connection refused"));

        var error = await Assert.ThrowsAsync<DmsException>(() => Client().GetCurrentUserAsync());
        Assert.Contains("not reachable", error.Message);
    }

    [Fact]
    public async Task Upload_posts_the_pdf_as_multipart()
    {
        string pdf = _files.Pdf("report.pdf", "content");
        _server.Respond(HttpStatusCode.Created, """{"id":9,"fileName":"report.pdf","fileSize":100,"contentSha256":null}""");

        var document = await Client().UploadAsync(pdf);

        Assert.Equal(9, document.Id);
        var request = Assert.Single(_server.Requests);
        Assert.Equal("POST", request.Method);
        Assert.EndsWith("/api/documents/upload", request.Uri);
        Assert.Contains("name=file", request.Body);
        Assert.Contains("filename=report.pdf", request.Body);
        Assert.Contains("Content-Type: application/pdf", request.Body);
        Assert.Contains("%PDF", request.Body);
    }

    [Fact]
    public async Task Upload_refuses_files_above_the_server_limit()
    {
        string big = Path.Combine(_files.Root, "big.pdf");
        using (var stream = File.Create(big))
        {
            stream.SetLength(DmsClient.MaxUploadBytes + 1);
        }

        await Assert.ThrowsAsync<DmsException>(() => Client().UploadAsync(big));
        Assert.Empty(_server.Requests);
    }

    [Fact]
    public async Task GetComments_reads_the_comments_of_a_document()
    {
        _server.Respond(HttpStatusCode.OK, """
            [{"id":1,"content":"First","createdAt":"2026-11-20T10:00:00","updatedAt":null},
             {"id":2,"content":"Second","createdAt":"2026-11-20T11:00:00","updatedAt":"2026-11-21T09:30:00"}]
            """);

        var comments = await Client().GetCommentsAsync(7);

        Assert.Equal(2, comments.Count);
        Assert.Null(comments[0].UpdatedAt);
        Assert.Equal(new DateTime(2026, 11, 21, 9, 30, 0), comments[1].UpdatedAt);
        Assert.EndsWith("/api/comments/document/7", _server.Requests[0].Uri);
    }

    [Fact]
    public async Task AddComment_posts_plain_text()
    {
        _server.Respond(HttpStatusCode.OK, """{"id":3,"content":"Read chapter 2","createdAt":"2026-11-21T10:00:00"}""");

        var comment = await Client().AddCommentAsync(7, "Read chapter 2");

        Assert.Equal(3, comment.Id);
        var request = Assert.Single(_server.Requests);
        Assert.Equal("POST", request.Method);
        Assert.Equal("Read chapter 2", request.Body);
    }

    [Fact]
    public async Task UpdateComment_returns_null_when_the_comment_was_deleted()
    {
        _server.Respond(HttpStatusCode.NotFound);

        Assert.Null(await Client().UpdateCommentAsync(3, "new text"));
        Assert.Equal("PUT", _server.Requests[0].Method);
        Assert.EndsWith("/api/comments/3", _server.Requests[0].Uri);
    }

    [Fact]
    public void DocumentPage_points_to_the_web_ui()
    {
        Assert.Equal("http://localhost:8080/document-details.html?id=7", Client().DocumentPage(7).ToString());
    }

    public void Dispose() => _files.Dispose();

    private sealed class StubHandler : HttpMessageHandler
    {
        private HttpStatusCode _status = HttpStatusCode.OK;
        private string _body = "";
        private Exception? _failure;

        public List<(string Method, string Uri, string? Authorization, string Body)> Requests { get; } = [];

        public void Respond(HttpStatusCode status, string body = "")
        {
            _status = status;
            _body = body;
        }

        public void Fail(Exception failure) => _failure = failure;

        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
        {
            if (_failure != null)
            {
                throw _failure;
            }
            string body = request.Content == null ? "" : await request.Content.ReadAsStringAsync(ct);
            Requests.Add((request.Method.Method, request.RequestUri!.ToString(),
                request.Headers.Authorization?.ToString(), body));
            return new HttpResponseMessage(_status) { Content = new StringContent(_body, Encoding.UTF8, "application/json") };
        }
    }
}
