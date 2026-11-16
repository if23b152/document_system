using Windows.Data.Pdf;
using Windows.Storage;
using Windows.Storage.Streams;

namespace DmsExplorer.App;

/// <summary>Renders PDF pages with the PDF engine built into Windows (no extra dependency).</summary>
internal static class PdfRenderer
{
    public static async Task<PdfDocument> OpenAsync(string path)
    {
        var file = await StorageFile.GetFileFromPathAsync(path);
        return await PdfDocument.LoadFromFileAsync(file);
    }

    public static async Task<Image> RenderPageAsync(PdfDocument document, uint index, int width)
    {
        using var page = document.GetPage(index);
        using var stream = new InMemoryRandomAccessStream();
        var options = new PdfPageRenderOptions { DestinationWidth = (uint)Math.Max(width, 100) };
        await page.RenderToStreamAsync(stream, options);

        // Copy into a managed stream: GDI+ needs it to stay alive as long as the image
        var buffer = new MemoryStream();
        await stream.AsStreamForRead().CopyToAsync(buffer);
        buffer.Position = 0;
        return Image.FromStream(buffer);
    }
}
