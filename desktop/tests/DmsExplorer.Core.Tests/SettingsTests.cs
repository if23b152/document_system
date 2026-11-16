namespace DmsExplorer.Core.Tests;

public class SettingsTests : IDisposable
{
    private readonly TestFiles _files = new();

    [Fact]
    public void Defaults_are_used_when_nothing_is_saved()
    {
        var settings = Settings.Load(new AppData(_files.Root));

        Assert.Equal("http://localhost:8080", settings.DmsUrl);
        Assert.Equal(300, settings.ReaderWpm);
    }

    [Fact]
    public void Corrupt_settings_fall_back_to_defaults()
    {
        var appData = new AppData(_files.Root);
        File.WriteAllText(appData.PathOf("settings.json"), "{ not json");

        Assert.Equal(300, Settings.Load(appData).ReaderWpm);
    }

    [Fact]
    public void Reader_progress_is_stored_per_document()
    {
        var progress = new ReaderProgress(new AppData(_files.Root));

        progress.Set("doc-a", new ReaderProgress.Entry(42, 450));

        Assert.Equal(new ReaderProgress.Entry(42, 450), progress.Get("doc-a"));
        Assert.Null(progress.Get("doc-b"));
    }

    public void Dispose() => _files.Dispose();
}
