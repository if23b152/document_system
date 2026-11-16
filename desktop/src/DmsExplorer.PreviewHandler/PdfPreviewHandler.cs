using System.Diagnostics;
using System.Runtime.InteropServices;
using Microsoft.Win32;

namespace DmsExplorer.PreviewHandler;

/// <summary>
/// Explorer preview handler for PDFs. Explorer hands over the file and the preview pane window;
/// the handler starts "DmsExplorer.exe preview &lt;file&gt; --parent &lt;hwnd&gt;", which embeds its
/// own window into the pane and follows its size. Unload ends that process.
/// </summary>
[ComVisible(true)]
[Guid(ClassId)]
[ClassInterface(ClassInterfaceType.None)]
public sealed class PdfPreviewHandler : IPreviewHandler, IInitializeWithFile, IObjectWithSite, IOleWindow
{
    public const string ClassId = "45879d77-e474-43ac-890e-0f89b3020d3e";
    private const int S_FALSE = 1;

    private string? _file;
    private IntPtr _parent;
    private object? _site;
    private Process? _preview;

    public void Initialize(string pszFilePath, uint grfMode) => _file = pszFilePath;

    public void SetWindow(IntPtr hwnd, ref RECT rect)
    {
        bool parentChanged = hwnd != _parent;
        _parent = hwnd;
        if (parentChanged && _preview is { HasExited: false })
        {
            // Explorer moved the preview to another window: start again inside the new one
            StopPreview();
            DoPreview();
        }
    }

    // The companion app follows the size of the parent window itself
    public void SetRect(ref RECT rect) { }

    public void DoPreview()
    {
        if (_file == null || _parent == IntPtr.Zero || _preview is { HasExited: false })
        {
            return;
        }
        try
        {
            var start = new ProcessStartInfo(CompanionPath()) { UseShellExecute = false };
            start.ArgumentList.Add("preview");
            start.ArgumentList.Add(_file);
            start.ArgumentList.Add("--parent");
            start.ArgumentList.Add(_parent.ToInt64().ToString());
            _preview = Process.Start(start);
        }
        catch (Exception e)
        {
            Log("DoPreview failed: " + e);
            throw;
        }
    }

    public void Unload()
    {
        StopPreview();
        _file = null;
    }

    public void SetFocus() { }

    public void QueryFocus(out IntPtr phwnd) => phwnd = _parent;

    public int TranslateAccelerator(ref MSG pmsg) => S_FALSE;

    public void SetSite(object? pUnkSite) => _site = pUnkSite;

    public void GetSite(ref Guid riid, out object? ppvSite) => ppvSite = _site;

    public void GetWindow(out IntPtr phwnd) => phwnd = _parent;

    public void ContextSensitiveHelp(bool fEnterMode) { }

    private void StopPreview()
    {
        try
        {
            if (_preview is { HasExited: false })
            {
                _preview.Kill();
            }
        }
        catch (InvalidOperationException)
        {
            // already gone
        }
        _preview?.Dispose();
        _preview = null;
    }

    /// <summary>Path written by install.ps1; falls back to the folder above the handler.</summary>
    private static string CompanionPath()
    {
        if (Registry.GetValue(@"HKEY_LOCAL_MACHINE\SOFTWARE\DmsExplorer", "AppPath", null) is string path && File.Exists(path))
        {
            return path;
        }
        string handlerDir = Path.GetDirectoryName(typeof(PdfPreviewHandler).Assembly.Location)!;
        return Path.GetFullPath(Path.Combine(handlerDir, "..", "DmsExplorer.exe"));
    }

    private static void Log(string message)
    {
        try
        {
            File.AppendAllText(Path.Combine(Path.GetTempPath(), "DmsExplorer-handler.log"),
                $"{DateTime.Now:s} {message}{Environment.NewLine}");
        }
        catch (IOException)
        {
            // logging must never break the preview
        }
    }
}
