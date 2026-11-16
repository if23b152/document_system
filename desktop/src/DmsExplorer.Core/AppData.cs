using System.Text.Json;

namespace DmsExplorer.Core;

/// <summary>
/// Location of the per-user data folder (%APPDATA%\DmsExplorer) and small JSON helpers.
/// Tests pass their own root folder.
/// </summary>
public sealed class AppData
{
    internal static readonly JsonSerializerOptions Json = new() { WriteIndented = true };

    public AppData(string? root = null)
    {
        Root = root ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "DmsExplorer");
        Directory.CreateDirectory(Root);
    }

    public string Root { get; }

    public string PathOf(params string[] parts) => Path.Combine([Root, .. parts]);

    internal T ReadJson<T>(string path, Func<T> fallback)
    {
        try
        {
            return File.Exists(path)
                ? JsonSerializer.Deserialize<T>(File.ReadAllText(path), Json) ?? fallback()
                : fallback();
        }
        catch (JsonException)
        {
            // A corrupt file must not break the preview – start fresh
            return fallback();
        }
    }

    internal static void WriteJson<T>(string path, T value)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        // Write to a temp file first so a crash never leaves half-written JSON behind
        string tmp = path + ".tmp";
        File.WriteAllText(tmp, JsonSerializer.Serialize(value, Json));
        File.Move(tmp, path, overwrite: true);
    }
}
