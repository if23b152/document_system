using DmsExplorer.Core;

namespace DmsExplorer.App;

internal static class Program
{
    private const string Usage =
        "DmsExplorer – Document System integration for Windows Explorer\n\n" +
        "  DmsExplorer preview <file.pdf> [--parent <hwnd>]\n" +
        "  DmsExplorer reader <file.pdf>\n" +
        "  DmsExplorer notes <file.pdf>";

    [STAThread]
    private static int Main(string[] args)
    {
        ApplicationConfiguration.Initialize();
        var appData = new AppData();

        string command = args.Length > 0 ? args[0].ToLowerInvariant() : "";
        string? file = args.Length > 1 ? Path.GetFullPath(args[1]) : null;

        switch (command)
        {
            case "preview" when file != null:
                Application.Run(new PreviewForm(file, appData, ParseParent(args)));
                return 0;
            case "reader" when file != null:
                Application.Run(new ReaderForm(file, appData));
                return 0;
            case "notes" when file != null:
                Application.Run(new NotesForm(file, appData));
                return 0;
            default:
                MessageBox.Show(Usage, "DmsExplorer", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return command.Length == 0 ? 0 : 2;
        }
    }

    private static IntPtr ParseParent(string[] args)
    {
        int i = Array.IndexOf(args, "--parent");
        return i >= 0 && i + 1 < args.Length && long.TryParse(args[i + 1], out long hwnd)
            ? new IntPtr(hwnd)
            : IntPtr.Zero;
    }

    /// <summary>Starts another instance of this app, e.g. the reader from the preview pane.</summary>
    internal static void Launch(string command, string file) =>
        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(Environment.ProcessPath!)
        {
            ArgumentList = { command, file },
            UseShellExecute = false,
        });

    internal static void OpenInBrowser(string url) =>
        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true });
}
