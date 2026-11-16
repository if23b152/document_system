namespace DmsExplorer.Core;

/// <summary>
/// Remembers the last word position and speed of the fast reader per document,
/// like the localStorage progress in the web reader.
/// </summary>
public sealed class ReaderProgress(AppData appData)
{
    public sealed record Entry(int WordIndex, int Wpm);

    private string FilePath => appData.PathOf("reader-progress.json");

    public Entry? Get(string fileKey) =>
        appData.ReadJson(FilePath, () => new Dictionary<string, Entry>()).GetValueOrDefault(fileKey);

    public void Set(string fileKey, Entry entry)
    {
        var all = appData.ReadJson(FilePath, () => new Dictionary<string, Entry>());
        all[fileKey] = entry;
        AppData.WriteJson(FilePath, all);
    }
}
