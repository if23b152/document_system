using UglyToad.PdfPig.Content;
using UglyToad.PdfPig.Core;
using UglyToad.PdfPig.Fonts.Standard14Fonts;
using UglyToad.PdfPig.Writer;

namespace DmsExplorer.Core.Tests;

/// <summary>Temporary folder with generated test PDFs, deleted after each test class.</summary>
public sealed class TestFiles : IDisposable
{
    public TestFiles()
    {
        Root = Path.Combine(Path.GetTempPath(), "dmsexplorer-tests-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(Root);
    }

    public string Root { get; }

    public string Pdf(string name, params string[] pages)
    {
        var builder = new PdfDocumentBuilder();
        var font = builder.AddStandard14Font(Standard14Font.Helvetica);
        foreach (string text in pages)
        {
            builder.AddPage(PageSize.A4).AddText(text, 12, new PdfPoint(40, 750), font);
        }
        string path = Path.Combine(Root, name);
        File.WriteAllBytes(path, builder.Build());
        return path;
    }

    public void Dispose() => Directory.Delete(Root, recursive: true);
}
