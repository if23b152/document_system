namespace DmsExplorer.Core.Tests;

public class PdfTextTests : IDisposable
{
    private readonly TestFiles _files = new();

    [Fact]
    public void Words_returns_words_of_all_pages_in_order()
    {
        string pdf = _files.Pdf("two-pages.pdf", "Hello fast reader.", "Second page here");

        var words = PdfText.Words(pdf);

        Assert.Equal(["Hello", "fast", "reader.", "Second", "page", "here"], words);
    }

    [Fact]
    public void PageCount_counts_pages()
    {
        string pdf = _files.Pdf("three.pdf", "a", "b", "c");

        Assert.Equal(3, PdfText.PageCount(pdf));
    }

    public void Dispose() => _files.Dispose();
}
