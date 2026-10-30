$ErrorActionPreference = "Stop"
if (!(Test-Path ".env")) { Write-Error "Missing .env. Copy .env.example to .env and configure it."; exit 1 }
Get-Content ".env" | ForEach-Object {
  $line = $_.Trim()
  if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
    $parts = $line.Split("=", 2)
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
  }
}
mvn spring-boot:run
