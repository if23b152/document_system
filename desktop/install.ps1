<#
.SYNOPSIS
  Builds and installs the Explorer integration (preview pane + context menu for PDF files).

.DESCRIPTION
  1. As the current user: publishes the app and the preview handler to a staging folder.
  2. Elevated (one UAC prompt): copies them to %ProgramFiles%\DmsExplorer and registers the
     preview handler and the "Document System" context menu machine-wide (HKLM).
  3. As the current user: removes a per-user PDF preview registration (e.g. Edge's) that would
     otherwise take precedence, and keeps a backup to restore it on uninstall.

  Every registry step is logged; on failure the log names the exact key.
  Notes and settings stay per user in %APPDATA%\DmsExplorer.

.EXAMPLE
  .\install.ps1              # build, install and register
  .\install.ps1 -Uninstall   # remove everything and restore the previous PDF preview
#>
param(
    [switch]$Uninstall,
    # internal: the elevated part
    [switch]$Elevated,
    [string]$Staging,
    [string]$LogFile
)

$ErrorActionPreference = 'Stop'

$installDir = Join-Path $env:ProgramFiles 'DmsExplorer'
$exe = Join-Path $installDir 'DmsExplorer.exe'
$comHost = Join-Path $installDir 'handler\DmsExplorer.PreviewHandler.comhost.dll'

$handlerClsid = '{45879d77-e474-43ac-890e-0f89b3020d3e}'   # must match PdfPreviewHandler.ClassId
$surrogateAppId = '{4746ec66-067b-4e4b-9fed-b139cc994773}'
$previewShellEx = '{8895b1c6-b41f-4c1c-a562-0d564250836f}'  # "preview handler" shell extension slot
$handlerName = 'DmsExplorer PDF preview'

$classes = 'SOFTWARE\Classes'
$appKey = 'SOFTWARE\DmsExplorer'
$previewList = 'SOFTWARE\Microsoft\Windows\CurrentVersion\PreviewHandlers'
$contextMenu = "$classes\SystemFileAssociations\.pdf\shell\DmsExplorer"
$pdfShellEx = "$classes\.pdf\shellex\$previewShellEx"
$sfaShellEx = "$classes\SystemFileAssociations\.pdf\shellex\$previewShellEx"

function Write-Log([string]$message) {
    $line = "$(Get-Date -Format HH:mm:ss) $message"
    Write-Host $line
    if ($LogFile) { Add-Content -Path $LogFile -Value $line }
}

function Set-Key([Microsoft.Win32.RegistryKey]$root, [string]$path, [hashtable]$values) {
    try {
        $key = $root.CreateSubKey($path)
        try {
            foreach ($name in $values.Keys) {
                $value = $values[$name]
                if ($value -is [int]) { $key.SetValue($name, $value, 'DWord') }
                elseif ($name -eq 'DllSurrogate') { $key.SetValue($name, $value, 'ExpandString') }
                else { $key.SetValue($name, $value) }
            }
        } finally { $key.Close() }
        Write-Log "  ok      $($root.Name)\$path"
    } catch {
        Write-Log "  FAILED  $($root.Name)\$path : $($_.Exception.Message)"
        throw
    }
}

function Remove-Key([Microsoft.Win32.RegistryKey]$root, [string]$path) {
    try {
        $root.DeleteSubKeyTree($path, $false)
        Write-Log "  removed $($root.Name)\$path"
    } catch {
        Write-Log "  FAILED  removing $($root.Name)\$path : $($_.Exception.Message)"
        throw
    }
}

function Get-Default([Microsoft.Win32.RegistryKey]$root, [string]$path) {
    $key = $root.OpenSubKey($path)
    if ($key) { try { return $key.GetValue('') } finally { $key.Close() } }
}

function Send-AssocChanged {
    Add-Type -Namespace DmsExplorer -Name Shell -MemberDefinition `
        '[DllImport("shell32.dll")] public static extern void SHChangeNotify(int e, uint f, IntPtr a, IntPtr b);'
    [DmsExplorer.Shell]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero)
}

function Stop-Running {
    # Running previews lock the files; Explorer restarts prevhost on the next preview
    Get-Process DmsExplorer -ErrorAction SilentlyContinue | Stop-Process -Force
    foreach ($proc in Get-Process prevhost -ErrorAction SilentlyContinue) {
        try {
            if ($proc.Modules.FileName -like "*DmsExplorer*") { Stop-Process -Id $proc.Id -Force }
        } catch { }   # processes without access
    }
}

# ---------------------------------------------------------------- elevated part (HKLM, Program Files)

function Install-Machine {
    $hklm = [Microsoft.Win32.Registry]::LocalMachine
    Stop-Running
    Write-Log "Copying files to $installDir"
    if (Test-Path $installDir) { Remove-Item -Recurse -Force $installDir }
    Copy-Item -Recurse $Staging $installDir

    Write-Log 'Registering preview handler (HKLM)'
    $previous = Get-Default $hklm $pdfShellEx
    $appValues = @{ AppPath = $exe }
    if ($previous -and $previous -ne $handlerClsid) { $appValues.PreviousPdfPreviewHandler = $previous }
    Set-Key $hklm $appKey $appValues

    Set-Key $hklm "$classes\CLSID\$handlerClsid" @{
        '' = $handlerName
        DisplayName = $handlerName
        AppID = $surrogateAppId
        # Normal rights instead of low integrity, so the app can save notes and open windows
        DisableLowILProcessIsolation = 1
    }
    Set-Key $hklm "$classes\CLSID\$handlerClsid\InprocServer32" @{ '' = $comHost; ThreadingModel = 'Apartment' }
    # Own surrogate process: keeps our .NET runtime apart from other preview handlers
    Set-Key $hklm "$classes\AppID\$surrogateAppId" @{ '' = 'DmsExplorer preview host'; DllSurrogate = '%SystemRoot%\system32\prevhost.exe' }
    Set-Key $hklm $pdfShellEx @{ '' = $handlerClsid }
    Set-Key $hklm $sfaShellEx @{ '' = $handlerClsid }
    Set-Key $hklm $previewList @{ $handlerClsid = $handlerName }

    Write-Log 'Registering context menu (HKLM)'
    Remove-Key $hklm $contextMenu
    Set-Key $hklm $contextMenu @{ MUIVerb = 'Document System'; Icon = "`"$exe`",0"; SubCommands = '' }
    $entries = @(
        @('01reader', 'Fast reader', 'reader'),
        @('02notes', 'Notes', 'notes'),
        @('03preview', 'Open preview window', 'preview')
    )
    foreach ($e in $entries) {
        Set-Key $hklm "$contextMenu\shell\$($e[0])" @{ MUIVerb = $e[1] }
        Set-Key $hklm "$contextMenu\shell\$($e[0])\command" @{ '' = "`"$exe`" $($e[2]) `"%1`"" }
    }
}

function Uninstall-Machine {
    $hklm = [Microsoft.Win32.Registry]::LocalMachine
    $app = $hklm.OpenSubKey($appKey)
    $previous = if ($app) { $app.GetValue('PreviousPdfPreviewHandler') }

    Write-Log 'Removing machine-wide registration'
    foreach ($k in $pdfShellEx, $sfaShellEx) {
        if ((Get-Default $hklm $k) -eq $handlerClsid) { Remove-Key $hklm $k }
    }
    if ($previous) { Set-Key $hklm $pdfShellEx @{ '' = $previous } }
    $list = $hklm.OpenSubKey($previewList, $true)
    if ($list) { $list.DeleteValue($handlerClsid, $false); $list.Close() }
    foreach ($k in "$classes\CLSID\$handlerClsid", "$classes\AppID\$surrogateAppId", $contextMenu, $appKey) {
        Remove-Key $hklm $k
    }
    Stop-Running
    if (Test-Path $installDir) { Remove-Item -Recurse -Force $installDir; Write-Log "Deleted $installDir" }
}

if ($Elevated) {
    try {
        if ($Uninstall) { Uninstall-Machine } else { Install-Machine }
        Write-Log 'ELEVATED-OK'
    } catch {
        Write-Log "ELEVATED-FAILED: $($_.Exception.Message)"
        exit 1
    }
    return
}

# ---------------------------------------------------------------- user part

$hkcu = [Microsoft.Win32.Registry]::CurrentUser
$LogFile = Join-Path $env:TEMP 'DmsExplorer-install.log'
Set-Content -Path $LogFile -Value "DmsExplorer $(if ($Uninstall) { 'uninstall' } else { 'install' }) $(Get-Date -Format s)"

function Invoke-Elevated([string]$stagingDir) {
    $arguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"", '-Elevated', '-LogFile', "`"$LogFile`"")
    if ($Uninstall) { $arguments += '-Uninstall' } else { $arguments += @('-Staging', "`"$stagingDir`"") }
    Write-Host 'Waiting for the administrator prompt (UAC)...'
    try {
        $p = Start-Process powershell -Verb RunAs -ArgumentList $arguments -Wait -PassThru
    } catch {
        throw 'Administrator rights were not granted - nothing was registered.'
    }
    if ($p.ExitCode -ne 0 -or -not (Select-String -Path $LogFile -Pattern 'ELEVATED-OK' -Quiet)) {
        Get-Content $LogFile | Select-Object -Skip 1 | ForEach-Object { Write-Host $_ }
        throw "The elevated step failed - see $LogFile"
    }
}

function Remove-UserLeftovers {
    # Earlier per-user prototype installs (HKCU registration and files in LocalAppData)
    Write-Log 'Removing per-user leftovers'
    foreach ($k in "Software\Classes\CLSID\$handlerClsid", "Software\Classes\AppID\$surrogateAppId",
                   'Software\Classes\SystemFileAssociations\.pdf\shell\DmsExplorer') {
        Remove-Key $hkcu $k
    }
    $old = $hkcu.OpenSubKey('Software\DmsExplorer', $true)
    if ($old) {
        # keep PreviousUserPdfPreviewHandler (backup of a re-install), drop the old per-user values
        foreach ($v in 'AppPath', 'PreviousPdfPreviewHandler') { $old.DeleteValue($v, $false) }
        $old.Close()
    }
    $userApp = Join-Path $env:LOCALAPPDATA 'DmsExplorer\app'
    if (Test-Path $userApp) { Remove-Item -Recurse -Force $userApp; Write-Log "  deleted $userApp" }
}

if ($Uninstall) {
    Invoke-Elevated $null
    # Restore the per-user PDF preview (e.g. Edge) that the install moved aside
    $backup = $hkcu.OpenSubKey('Software\DmsExplorer')
    $previousUser = if ($backup) { $backup.GetValue('PreviousUserPdfPreviewHandler') }
    if ($previousUser) { Set-Key $hkcu "Software\Classes\.pdf\shellex\$previewShellEx" @{ '' = $previousUser } }
    Remove-Key $hkcu 'Software\DmsExplorer'
    Send-AssocChanged
    Write-Host 'DmsExplorer removed. Notes and settings are kept in' (Join-Path $env:APPDATA 'DmsExplorer')
    return
}

$dotnet = Join-Path $env:LOCALAPPDATA 'Microsoft\dotnet\dotnet.exe'
$onPath = Get-Command dotnet -ErrorAction SilentlyContinue
if ($onPath -and (& $onPath.Source --list-sdks)) { $dotnet = $onPath.Source }
if (-not (Test-Path $dotnet)) { throw '.NET 8 SDK not found - see desktop/README.md' }

# The handler runs inside Explorer's prevhost.exe and needs the machine-wide .NET runtime
$machineDotnet = Join-Path $env:ProgramFiles 'dotnet\dotnet.exe'
if (-not ((Test-Path $machineDotnet) -and (& $machineDotnet --list-runtimes | Select-String 'Microsoft\.NETCore\.App (8|9|1\d)\.'))) {
    Write-Warning 'No machine-wide .NET 8+ runtime found in Program Files - the preview pane will stay empty.'
}

$staging = Join-Path $PSScriptRoot 'artifacts\install'
if (Test-Path $staging) { Remove-Item -Recurse -Force $staging }
$src = Join-Path $PSScriptRoot 'src'
Write-Log 'Publishing app and preview handler'
& $dotnet publish (Join-Path $src 'DmsExplorer.App') -c Release -o $staging --nologo -v q
if ($LASTEXITCODE -ne 0) { throw 'Publishing the app failed' }
& $dotnet publish (Join-Path $src 'DmsExplorer.PreviewHandler') -c Release -o (Join-Path $staging 'handler') --nologo -v q
if ($LASTEXITCODE -ne 0) { throw 'Publishing the preview handler failed' }

Remove-UserLeftovers
Invoke-Elevated $staging

# A per-user PDF preview handler (e.g. Edge's) overrides the machine-wide one: move it aside
$userShellEx = "Software\Classes\.pdf\shellex\$previewShellEx"
$current = Get-Default $hkcu $userShellEx
if ($current -and $current -ne $handlerClsid) {
    Write-Log "Moving per-user PDF preview $current aside"
    Set-Key $hkcu 'Software\DmsExplorer' @{ PreviousUserPdfPreviewHandler = $current }
    Remove-Key $hkcu $userShellEx
}
Send-AssocChanged

Write-Host ''
Write-Host "Installed to $installDir"
Write-Host 'Select a PDF in Explorer with the preview pane open (Alt+P), or right-click it -> Show more options -> Document System.'
