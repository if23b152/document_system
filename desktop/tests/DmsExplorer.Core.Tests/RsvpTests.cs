namespace DmsExplorer.Core.Tests;

public class RsvpTests
{
    [Fact]
    public void Plain_word_uses_the_base_delay()
    {
        Assert.Equal(TimeSpan.FromMilliseconds(200), Rsvp.DelayFor("word", 300));
    }

    [Theory]
    [InlineData("end.", 400)]
    [InlineData("pause,", 300)]
    public void Punctuation_gets_extra_time(string word, double expectedMs)
    {
        Assert.Equal(TimeSpan.FromMilliseconds(expectedMs), Rsvp.DelayFor(word, 300));
    }

    [Fact]
    public void Speed_is_clamped_to_the_supported_range()
    {
        Assert.Equal(Rsvp.DelayFor("word", Rsvp.MaxWpm), Rsvp.DelayFor("word", 5000));
        Assert.Equal(Rsvp.DelayFor("word", Rsvp.MinWpm), Rsvp.DelayFor("word", 1));
    }

    [Theory]
    [InlineData("a", 0)]
    [InlineData("read", 1)]
    [InlineData("document", 2)]
    [InlineData("international", 3)]
    [InlineData("internationalisation", 4)]
    public void Focus_letter_moves_right_for_longer_words(string word, int expected)
    {
        Assert.Equal(expected, Rsvp.FocusIndex(word));
    }
}
