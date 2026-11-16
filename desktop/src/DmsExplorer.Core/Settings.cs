namespace DmsExplorer.Core;

public sealed class Settings
{
    /// <summary>Web UI of the document system (docker compose default).</summary>
    public string DmsUrl { get; set; } = "http://localhost:8080";

    /// <summary>Default speed of the fast reader in words per minute.</summary>
    public int ReaderWpm { get; set; } = 300;

    public static Settings Load(AppData appData) =>
        appData.ReadJson(appData.PathOf("settings.json"), () => new Settings());

    public void Save(AppData appData) => AppData.WriteJson(appData.PathOf("settings.json"), this);
}
