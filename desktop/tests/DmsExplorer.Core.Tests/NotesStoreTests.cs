namespace DmsExplorer.Core.Tests;

public class NotesStoreTests : IDisposable
{
    private readonly TestFiles _files = new();
    private readonly NotesStore _notes;

    public NotesStoreTests()
    {
        _notes = new NotesStore(new AppData(Path.Combine(_files.Root, "appdata")));
    }

    [Fact]
    public void Save_and_load_round_trip()
    {
        string pdf = _files.Pdf("doc.pdf", "content");

        _notes.Save(pdf, "Check chapter 2");

        var note = _notes.Load(pdf);
        Assert.NotNull(note);
        Assert.Equal("Check chapter 2", note.Text);
        Assert.Equal(Path.GetFullPath(pdf), note.SourcePath);
        Assert.True(_notes.HasNote(pdf));
    }

    [Fact]
    public void Note_follows_the_content_when_the_file_is_moved()
    {
        string pdf = _files.Pdf("original.pdf", "same content");
        _notes.Save(pdf, "still here");

        string moved = Path.Combine(_files.Root, "renamed.pdf");
        File.Move(pdf, moved);

        Assert.Equal("still here", _notes.Load(moved)?.Text);
    }

    [Fact]
    public void Saving_empty_text_deletes_the_note()
    {
        string pdf = _files.Pdf("doc.pdf", "content");
        _notes.Save(pdf, "temporary");

        _notes.Save(pdf, "   ");

        Assert.Null(_notes.Load(pdf));
        Assert.False(_notes.HasNote(pdf));
    }

    [Fact]
    public void Different_documents_have_separate_notes()
    {
        string a = _files.Pdf("a.pdf", "first");
        string b = _files.Pdf("b.pdf", "second");

        _notes.Save(a, "note a");

        Assert.Null(_notes.Load(b));
    }

    [Fact]
    public void Saving_text_keeps_the_dms_link()
    {
        string pdf = _files.Pdf("doc.pdf", "content");
        _notes.Save(pdf, "first");
        _notes.SetLink(pdf, new DmsLink(7, 3, "first"));

        _notes.Save(pdf, "second");

        Assert.Equal(new DmsLink(7, 3, "first"), _notes.Load(pdf)!.Dms);
    }

    [Fact]
    public void Notes_saved_before_dms_sync_still_load()
    {
        string pdf = _files.Pdf("doc.pdf", "content");
        var appData = new AppData(Path.Combine(_files.Root, "appdata"));
        string file = appData.PathOf("notes", FileKey.Of(pdf) + ".json");
        Directory.CreateDirectory(Path.GetDirectoryName(file)!);
        File.WriteAllText(file, """{"Text":"old note","SourcePath":"C:\\x.pdf","UpdatedUtc":"2026-11-16T10:00:00Z"}""");

        var note = _notes.Load(pdf);

        Assert.Equal("old note", note!.Text);
        Assert.Null(note.Dms);
    }

    public void Dispose() => _files.Dispose();
}
