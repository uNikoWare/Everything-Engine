param(
    [Parameter(Mandatory = $true)][int]$AppPid,
    [Parameter(Mandatory = $true)][string]$InstallerPath,
    [Parameter(Mandatory = $true)][string]$ApplicationPath,
    [Parameter(Mandatory = $true)][string]$HelperPath
)

while (Get-Process -Id $AppPid -ErrorAction SilentlyContinue) {
    Start-Sleep -Seconds 1
}

$installer = Start-Process -FilePath "$env:SystemRoot\System32\msiexec.exe" `
    -ArgumentList @('/i', ('"{0}"' -f $InstallerPath), '/passive', '/norestart') `
    -Wait -PassThru

if ($installer.ExitCode -eq 0 -or $installer.ExitCode -eq 3010) {
    Start-Process -FilePath $ApplicationPath
} elseif (Test-Path -LiteralPath $ApplicationPath) {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show(
        "The update could not be installed (Windows Installer exit code $($installer.ExitCode)). The previous version will be restarted.",
        "Everything Engine Update",
        [System.Windows.MessageBoxButton]::OK,
        [System.Windows.MessageBoxImage]::Error
    ) | Out-Null
    Start-Process -FilePath $ApplicationPath
}

Remove-Item -LiteralPath $InstallerPath -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $HelperPath -Force -ErrorAction SilentlyContinue
