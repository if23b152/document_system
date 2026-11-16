using System.Runtime.InteropServices;

namespace DmsExplorer.App;

/// <summary>Win32 calls needed to live inside the Explorer preview pane.</summary>
internal static partial class Native
{
    public const int GWL_STYLE = -16;
    public const long WS_CHILD = 0x40000000;
    public const long WS_VISIBLE = 0x10000000;
    public const long WS_POPUP = 0x80000000;
    public const long WS_CAPTION = 0x00C00000;
    public const long WS_THICKFRAME = 0x00040000;

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT
    {
        public int Left, Top, Right, Bottom;
        public readonly int Width => Right - Left;
        public readonly int Height => Bottom - Top;
    }

    [LibraryImport("user32.dll", SetLastError = true)]
    public static partial IntPtr SetParent(IntPtr child, IntPtr newParent);

    [LibraryImport("user32.dll", EntryPoint = "GetWindowLongPtrW")]
    public static partial IntPtr GetWindowLongPtr(IntPtr hwnd, int index);

    [LibraryImport("user32.dll", EntryPoint = "SetWindowLongPtrW")]
    public static partial IntPtr SetWindowLongPtr(IntPtr hwnd, int index, IntPtr value);

    [LibraryImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static partial bool IsWindow(IntPtr hwnd);

    [LibraryImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static partial bool GetClientRect(IntPtr hwnd, out RECT rect);

    [LibraryImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static partial bool MoveWindow(IntPtr hwnd, int x, int y, int width, int height,
        [MarshalAs(UnmanagedType.Bool)] bool repaint);

    /// <summary>Turns a top-level window into a borderless child of <paramref name="parent"/>.</summary>
    public static void EmbedInto(IntPtr window, IntPtr parent)
    {
        long style = GetWindowLongPtr(window, GWL_STYLE).ToInt64();
        style = (style & ~(WS_POPUP | WS_CAPTION | WS_THICKFRAME)) | WS_CHILD | WS_VISIBLE;
        SetWindowLongPtr(window, GWL_STYLE, new IntPtr(style));
        SetParent(window, parent);
    }
}
