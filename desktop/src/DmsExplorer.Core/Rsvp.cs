namespace DmsExplorer.Core;

/// <summary>
/// Timing rules for RSVP speed reading (one word at a time at a fixed point).
/// </summary>
public static class Rsvp
{
    public const int MinWpm = 100;
    public const int MaxWpm = 1000;

    /// <summary>How long a word stays on screen; punctuation and long words get extra time.</summary>
    public static TimeSpan DelayFor(string word, int wpm)
    {
        wpm = Math.Clamp(wpm, MinWpm, MaxWpm);
        double ms = 60_000.0 / wpm;

        if (word.Length > 0)
        {
            char last = word[^1];
            if (last is '.' or '!' or '?')
            {
                ms *= 2.0;
            }
            else if (last is ',' or ';' or ':')
            {
                ms *= 1.5;
            }
        }
        if (word.Length > 8)
        {
            ms *= 1.2;
        }
        return TimeSpan.FromMilliseconds(ms);
    }

    /// <summary>
    /// Optimal recognition point: the letter the eye should fix on, highlighted in the reader.
    /// </summary>
    public static int FocusIndex(string word) => word.Length switch
    {
        <= 1 => 0,
        <= 5 => 1,
        <= 9 => 2,
        <= 13 => 3,
        _ => 4,
    };
}
