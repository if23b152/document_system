namespace DmsExplorer.App;

internal static class Theme
{
    public static readonly Color Background = Color.FromArgb(250, 250, 251);
    public static readonly Color Toolbar = Color.FromArgb(243, 244, 246);
    public static readonly Color Border = Color.FromArgb(229, 231, 235);
    public static readonly Color Text = Color.FromArgb(17, 24, 39);
    public static readonly Color Muted = Color.FromArgb(107, 114, 128);
    public static readonly Color Accent = Color.FromArgb(37, 99, 235);
    public static readonly Color Focus = Color.FromArgb(220, 38, 38);
    public static readonly Color NoteBackground = Color.FromArgb(254, 249, 195);

    public static readonly Font Ui = new("Segoe UI", 9.5f);
    public static readonly Font UiBold = new("Segoe UI Semibold", 9.5f);

    public static Button ToolbarButton(string text, EventHandler onClick)
    {
        var button = new Button
        {
            Text = text,
            AutoSize = true,
            FlatStyle = FlatStyle.Flat,
            BackColor = Color.White,
            ForeColor = Text,
            Font = Ui,
            Margin = new Padding(0, 0, 6, 0),
            Padding = new Padding(6, 2, 6, 2),
            Cursor = Cursors.Hand,
        };
        button.FlatAppearance.BorderColor = Border;
        button.FlatAppearance.MouseOverBackColor = Color.FromArgb(239, 246, 255);
        button.Click += onClick;
        return button;
    }
}
