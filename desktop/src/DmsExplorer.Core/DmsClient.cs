using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace DmsExplorer.Core;

public sealed record DmsDocument(long Id, string FileName, long FileSize, string? ContentSha256);

public sealed record DmsUser(string Username);

/// <summary>The DMS could not be reached or answered with an unexpected error.</summary>
public class DmsException(string message, Exception? inner = null) : Exception(message, inner);

/// <summary>The access token is missing, invalid, expired or revoked.</summary>
public sealed class DmsAuthException() : DmsException("The access token was rejected – create a new one in the DMS.");

/// <summary>
/// Talks to the DMS REST API through the web UI host (nginx forwards /api to the REST server),
/// authenticated with a personal access token.
/// </summary>
public sealed class DmsClient : IDisposable
{
    /// <summary>Upload limit of the REST server (spring.servlet.multipart.max-file-size).</summary>
    public const long MaxUploadBytes = 20L * 1024 * 1024;

    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);
    private readonly HttpClient _http;

    public DmsClient(string baseUrl, string token, HttpMessageHandler? handler = null)
    {
        BaseUrl = new Uri(baseUrl.TrimEnd('/') + "/");
        _http = handler == null ? new HttpClient() : new HttpClient(handler);
        _http.BaseAddress = BaseUrl;
        _http.Timeout = TimeSpan.FromSeconds(30);
        _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    public Uri BaseUrl { get; }

    /// <summary>Web UI page of a document.</summary>
    public Uri DocumentPage(long id) => new(BaseUrl, $"document-details.html?id={id}");

    public Task<DmsUser> GetCurrentUserAsync(CancellationToken ct = default) =>
        SendAsync<DmsUser>(() => new HttpRequestMessage(HttpMethod.Get, "api/auth/me"), ct)!;

    /// <summary>The newest own document with this content hash, or null if it is not in the DMS.</summary>
    public async Task<DmsDocument?> FindByHashAsync(string contentSha256, CancellationToken ct = default) =>
        await SendAsync<DmsDocument>(
            () => new HttpRequestMessage(HttpMethod.Get, $"api/documents/by-hash/{Uri.EscapeDataString(contentSha256)}"),
            ct, notFoundIsNull: true);

    public async Task<DmsDocument> UploadAsync(string pdfPath, CancellationToken ct = default)
    {
        var info = new FileInfo(pdfPath);
        if (info.Length > MaxUploadBytes)
        {
            throw new DmsException($"The file is larger than the upload limit of {MaxUploadBytes / 1024 / 1024} MB.");
        }

        var document = await SendAsync<DmsDocument>(() =>
        {
            var file = new StreamContent(File.OpenRead(pdfPath));
            file.Headers.ContentType = new MediaTypeHeaderValue("application/pdf");
            var form = new MultipartFormDataContent { { file, "file", info.Name } };
            return new HttpRequestMessage(HttpMethod.Post, "api/documents/upload") { Content = form };
        }, ct);
        return document ?? throw new DmsException("The DMS did not return the uploaded document.");
    }

    private async Task<T?> SendAsync<T>(Func<HttpRequestMessage> request, CancellationToken ct, bool notFoundIsNull = false)
        where T : class
    {
        HttpResponseMessage response;
        try
        {
            using var message = request();
            response = await _http.SendAsync(message, ct);
        }
        catch (HttpRequestException e)
        {
            throw new DmsException($"The DMS at {BaseUrl} is not reachable. Is it running?", e);
        }
        catch (TaskCanceledException e) when (!ct.IsCancellationRequested)
        {
            throw new DmsException($"The DMS at {BaseUrl} did not answer in time.", e);
        }

        using (response)
        {
            switch (response.StatusCode)
            {
                case HttpStatusCode.Unauthorized:
                case HttpStatusCode.Forbidden:
                    throw new DmsAuthException();
                case HttpStatusCode.NotFound when notFoundIsNull:
                    return null;
                case HttpStatusCode.UnsupportedMediaType:
                    throw new DmsException("The DMS only accepts PDF files.");
                case HttpStatusCode.RequestEntityTooLarge:
                    throw new DmsException("The file is too large for the DMS.");
            }
            if (!response.IsSuccessStatusCode)
            {
                throw new DmsException($"The DMS answered with {(int)response.StatusCode} {response.ReasonPhrase}.");
            }
            return await response.Content.ReadFromJsonAsync<T>(Json, ct);
        }
    }

    public void Dispose() => _http.Dispose();
}
