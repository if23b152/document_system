namespace DmsExplorer.Core;

public sealed record Note(string Text, string SourcePath, DateTime UpdatedUtc);

/// <summary>
/// Stores one note per document (keyed by <see cref="FileKey"/>) under %APPDATA%\DmsExplorer\notes.
/// </summary>
public sealed class NotesStore(AppData appData)
{
    private string FileFor(string key) => appData.PathOf("notes", key + ".json");

    public Note? Load(string filePath) => LoadByKey(FileKey.Of(filePath));

    /// <summary>Loads by an already computed <see cref="FileKey"/> (avoids hashing large files again).</summary>
    public Note? LoadByKey(string fileKey)
    {
        string path = FileFor(fileKey);
        return File.Exists(path) ? appData.ReadJson<Note?>(path, () => null) : null;
    }

    public bool HasNote(string filePath) => !string.IsNullOrWhiteSpace(Load(filePath)?.Text);

    /// <summary>Saves the note; an empty text deletes it.</summary>
    public void Save(string filePath, string text)
    {
        string path = FileFor(FileKey.Of(filePath));
        if (string.IsNullOrWhiteSpace(text))
        {
            File.Delete(path);
            return;
        }
        AppData.WriteJson(path, new Note(text, Path.GetFullPath(filePath), DateTime.UtcNow));
    }
}
