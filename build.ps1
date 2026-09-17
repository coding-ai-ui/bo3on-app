param([string[]]$Tasks = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!$env:JAVA_HOME) {
    $localJdk = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
    if (Test-Path -LiteralPath $localJdk) { $env:JAVA_HOME = $localJdk }
}
$localTools = Join-Path $env:USERPROFILE 'Documents\Codex\android-tools'
if (Test-Path -LiteralPath "$localTools\gradle-8.11.1\bin\gradle.bat") {
    $env:GRADLE_USER_HOME = "$localTools\gradle-cache"
    & "$localTools\gradle-8.11.1\bin\gradle.bat" @Tasks --console=plain
} else {
    & "$PSScriptRoot\gradlew.bat" @Tasks --console=plain
}
exit $LASTEXITCODE
