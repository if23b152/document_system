using System.Security.Cryptography;
using System.Text;
using DmsExplorer.Core;

namespace DmsExplorer.App;

/// <summary>
/// Keeps the DMS access token encrypted with Windows DPAPI (bound to the current Windows user),
/// so it never lies in plain text in %APPDATA%.
/// </summary>
internal sealed class TokenStore(AppData appData)
{
    // Extra entropy so other programs using DPAPI cannot decrypt the file by accident
    private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("DmsExplorer.AccessToken.v1");

    private string FilePath => appData.PathOf("token.bin");

    public string? Load()
    {
        try
        {
            if (!File.Exists(FilePath))
            {
                return null;
            }
            byte[] plain = ProtectedData.Unprotect(File.ReadAllBytes(FilePath), Entropy, DataProtectionScope.CurrentUser);
            return Encoding.UTF8.GetString(plain);
        }
        catch (CryptographicException)
        {
            // Created by another Windows user or corrupted: treat as not connected
            return null;
        }
    }

    public void Save(string token)
    {
        byte[] secret = ProtectedData.Protect(Encoding.UTF8.GetBytes(token), Entropy, DataProtectionScope.CurrentUser);
        File.WriteAllBytes(FilePath, secret);
    }

    public void Clear() => File.Delete(FilePath);
}
