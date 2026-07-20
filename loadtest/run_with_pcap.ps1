Param(
    [string]$Script = 'loadtest/k6/wallet_10k_1M.js',
    [string]$BaseUrl = 'http://localhost:8083',
    [string]$AuthToken = 'dummy-token'
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$pcap = "k6_run_$timestamp.pcap"
$artifacts = "loadtest\artifacts"
New-Item -ItemType Directory -Path $artifacts -Force | Out-Null

# Requires WinPcap/Npcap and tcpdump available in PATH (or use Wireshark's dumpcap)
Write-Host "Starting capture to $pcap - you may need Administrator privileges"
$targetHost = ([uri]$BaseUrl).Host
# Start dumpcap (preferred on Windows) if available, otherwise try tcpdump
if (Get-Command dumpcap -ErrorAction SilentlyContinue) {
    $dumpProc = Start-Process -FilePath dumpcap -ArgumentList "-i 1 -w $pcap host $targetHost" -PassThru
} elseif (Get-Command tcpdump -ErrorAction SilentlyContinue) {
    $dumpProc = Start-Process -FilePath tcpdump -ArgumentList "-i any host $targetHost -w $pcap" -PassThru
} else {
    Write-Error "Neither dumpcap nor tcpdump found in PATH. Install Wireshark (dumpcap) or tcpdump."
    exit 1
}

Start-Sleep -Seconds 2
Write-Host "Running k6 script $Script"
$env:BASE_URL = $BaseUrl
$env:TEST_TOKEN = $AuthToken
k6 run $Script

Start-Sleep -Seconds 2
if ($dumpProc -and !$dumpProc.HasExited) {
    Write-Host "Stopping capture (pid $($dumpProc.Id))"
    $dumpProc | Stop-Process -Force
}

Move-Item -Path $pcap -Destination $artifacts -Force
Write-Host "PCAP saved to $artifacts\$pcap"

