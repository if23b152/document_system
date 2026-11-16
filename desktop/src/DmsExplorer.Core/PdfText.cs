using UglyToad.PdfPig;

namespace DmsExplorer.Core;

/// <summary>Extracts the words of a PDF locally – no backend or OCR needed for text PDFs.</summary>
public static class PdfText
{
    public static IReadOnlyList<string> Words(string pdfPath)
    {
        var words = new List<string>();
        using var document = PdfDocument.Open(pdfPath);
        foreach (var page in document.GetPages())
        {
            foreach (var word in page.GetWords())
            {
                string text = word.Text.Trim();
                if (text.Length > 0)
                {
                    words.Add(text);
                }
            }
        }
        return words;
    }

    public static int PageCount(string pdfPath)
    {
        using var document = PdfDocument.Open(pdfPath);
        return document.NumberOfPages;
    }
}
