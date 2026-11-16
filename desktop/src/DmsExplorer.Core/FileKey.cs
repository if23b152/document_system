using System.Security.Cryptography;

namespace DmsExplorer.Core;

/// <summary>
/// Identifies a document by the SHA-256 of its content, so notes and reading progress
/// survive renaming or moving the file. The same key will be used to look the
/// document up in the DMS backend later.
/// </summary>
public static class FileKey
{
    public static string Of(string filePath)
    {
        using var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite | FileShare.Delete);
        return Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();
    }
}
