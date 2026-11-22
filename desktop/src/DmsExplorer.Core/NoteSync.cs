namespace DmsExplorer.Core;

public enum SyncDecision
{
    /// <summary>Local note and DMS comment have the same text.</summary>
    InSync,
    /// <summary>Only the local note changed: send it to the DMS.</summary>
    PushLocal,
    /// <summary>Only the DMS comment changed (e.g. in the web UI): take it over locally.</summary>
    PullRemote,
    /// <summary>Both changed differently: the user decides.</summary>
    Conflict,
    /// <summary>The comment was deleted in the DMS: the local note stays, the link is removed.</summary>
    RemoteDeleted,
}

public enum ShareResult
{
    Shared,
    AlreadyShared,
    /// <summary>The document is not in the DMS yet – it has to be uploaded first.</summary>
    DocumentNotInDms,
}

public sealed record SyncOutcome(SyncDecision Decision, string? LocalText, string? RemoteText);

/// <summary>
/// Keeps a local note and its DMS comment in sync. The rule compares both sides with the text
/// they last agreed on (<see cref="DmsLink.SyncedText"/>), so it never needs synchronised clocks.
/// </summary>
public sealed class NoteSync(NotesStore notes, DmsClient client)
{
    public static SyncDecision Decide(string localText, DmsLink link, DmsComment? remote)
    {
        if (remote == null)
        {
            return SyncDecision.RemoteDeleted;
        }
        bool localChanged = !SameText(localText, link.SyncedText);
        bool remoteChanged = !SameText(remote.Content, link.SyncedText);
        return (localChanged, remoteChanged) switch
        {
            (false, false) => SyncDecision.InSync,
            (true, false) => SyncDecision.PushLocal,
            (false, true) => SyncDecision.PullRemote,
            _ => SameText(localText, remote.Content) ? SyncDecision.InSync : SyncDecision.Conflict,
        };
    }

    /// <summary>Compares texts ignoring line ending style (Windows editor vs. web UI).</summary>
    public static bool SameText(string a, string b) => a.ReplaceLineEndings("\n") == b.ReplaceLineEndings("\n");

    /// <summary>Publishes the note as a new comment on the matching DMS document and links them.</summary>
    public async Task<ShareResult> ShareAsync(string filePath, CancellationToken ct = default)
    {
        var note = notes.Load(filePath);
        if (note == null)
        {
            throw new InvalidOperationException("Write a note before sharing it.");
        }
        if (note.Dms != null)
        {
            return ShareResult.AlreadyShared;
        }
        var document = await client.FindByHashAsync(FileKey.Of(filePath), ct);
        if (document == null)
        {
            return ShareResult.DocumentNotInDms;
        }
        var comment = await client.AddCommentAsync(document.Id, note.Text, ct);
        notes.SetLink(filePath, new DmsLink(document.Id, comment.Id, note.Text));
        return ShareResult.Shared;
    }

    /// <summary>
    /// Brings a shared note and its comment together. Conflicts are reported, not resolved;
    /// call <see cref="ResolveAsync"/> with the user's choice.
    /// </summary>
    public async Task<SyncOutcome?> SyncAsync(string filePath, CancellationToken ct = default)
    {
        var note = notes.Load(filePath);
        if (note?.Dms is not { } link)
        {
            return null;
        }
        var remote = await FindCommentAsync(link, ct);
        var decision = Decide(note.Text, link, remote);
        switch (decision)
        {
            case SyncDecision.InSync when remote != null && !SameText(link.SyncedText, remote.Content):
                notes.SaveSynced(filePath, note.Text, link);
                break;
            case SyncDecision.PushLocal:
                if (await client.UpdateCommentAsync(link.CommentId, note.Text, ct) == null)
                {
                    notes.SetLink(filePath, null);
                    return new SyncOutcome(SyncDecision.RemoteDeleted, note.Text, null);
                }
                notes.SaveSynced(filePath, note.Text, link);
                break;
            case SyncDecision.PullRemote:
                notes.SaveSynced(filePath, remote!.Content, link);
                break;
            case SyncDecision.RemoteDeleted:
                notes.SetLink(filePath, null);
                break;
        }
        return new SyncOutcome(decision, note.Text, remote?.Content);
    }

    /// <summary>Resolves a conflict: keep the local text (overwrite the comment) or take the DMS text.</summary>
    public async Task ResolveAsync(string filePath, bool keepLocal, string remoteText, CancellationToken ct = default)
    {
        var note = notes.Load(filePath);
        if (note?.Dms is not { } link)
        {
            return;
        }
        if (!keepLocal)
        {
            notes.SaveSynced(filePath, remoteText, link);
            return;
        }
        if (await client.UpdateCommentAsync(link.CommentId, note.Text, ct) == null)
        {
            notes.SetLink(filePath, null);
            return;
        }
        notes.SaveSynced(filePath, note.Text, link);
    }

    /// <summary>The other comments of the document (everything except the note's own comment).</summary>
    public async Task<IReadOnlyList<DmsComment>> OtherCommentsAsync(DmsLink link, CancellationToken ct = default) =>
        (await client.GetCommentsAsync(link.DocumentId, ct)).Where(c => c.Id != link.CommentId).ToList();

    private async Task<DmsComment?> FindCommentAsync(DmsLink link, CancellationToken ct)
    {
        try
        {
            return (await client.GetCommentsAsync(link.DocumentId, ct)).FirstOrDefault(c => c.Id == link.CommentId);
        }
        catch (DmsNotFoundException)
        {
            // The whole document was deleted in the DMS
            return null;
        }
    }
}
