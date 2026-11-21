namespace DmsExplorer.Core;

/// <summary>
/// Link of a local note to the DMS comment it is shared as. <see cref="SyncedText"/> is the text
/// both sides last agreed on; comparing against it tells which side changed since.
/// </summary>
public sealed record DmsLink(long DocumentId, long CommentId, string SyncedText);

public sealed record Note(string Text, string SourcePath, DateTime UpdatedUtc, DmsLink? Dms = null);

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

    /// <summary>
    /// Saves the note text and keeps an existing DMS link. An empty text deletes the note
    /// (and its link – the DMS comment itself stays).
    /// </summary>
    public void Save(string filePath, string text) =>
        Write(filePath, text, link => link);

    /// <summary>Links the note to a DMS comment (or removes the link with null).</summary>
    public void SetLink(string filePath, DmsLink? link)
    {
        var note = Load(filePath);
        if (note != null)
        {
            Write(filePath, note.Text, _ => link);
        }
    }

    /// <summary>Saves a text that both sides agree on, e.g. after pushing or pulling.</summary>
    public void SaveSynced(string filePath, string text, DmsLink link) =>
        Write(filePath, text, _ => link with { SyncedText = text });

    private void Write(string filePath, string text, Func<DmsLink?, DmsLink?> link)
    {
        string path = FileFor(FileKey.Of(filePath));
        if (string.IsNullOrWhiteSpace(text))
        {
            File.Delete(path);
            return;
        }
        DmsLink? existing = File.Exists(path) ? appData.ReadJson<Note?>(path, () => null)?.Dms : null;
        AppData.WriteJson(path, new Note(text, Path.GetFullPath(filePath), DateTime.UtcNow, link(existing)));
    }
}
