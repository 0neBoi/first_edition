# 无 Maven 时自动下载并启动后端（在 backend 目录下执行 .\run.ps1）
$ErrorActionPreference = "Stop"
$MAVEN_VERSION = "3.9.6"
$MAVEN_ZIP = "apache-maven-$MAVEN_VERSION-bin.zip"
$MAVEN_URL = "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/$MAVEN_ZIP"
$LOCAL_MAVEN = "$PSScriptRoot\.mvn-local\apache-maven-$MAVEN_VERSION"

function Find-Mvn {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) { return $mvn.Source }
    if (Test-Path "$LOCAL_MAVEN\bin\mvn.cmd") { return "$LOCAL_MAVEN\bin\mvn.cmd" }
    return $null
}

$mvnExe = Find-Mvn
if (-not $mvnExe) {
    Write-Host "未检测到 Maven，正在下载 $MAVEN_VERSION ..."
    $zipPath = "$PSScriptRoot\.mvn-local\$MAVEN_ZIP"
    $mvnDir = "$PSScriptRoot\.mvn-local"
    New-Item -ItemType Directory -Force -Path $mvnDir | Out-Null
    try {
        Invoke-WebRequest -Uri $MAVEN_URL -OutFile $zipPath -UseBasicParsing
    } catch {
        $MAVEN_URL = "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/$MAVEN_ZIP"
        Invoke-WebRequest -Uri $MAVEN_URL -OutFile $zipPath -UseBasicParsing
    }
    Expand-Archive -Path $zipPath -DestinationPath $mvnDir -Force
    $mvnExe = "$LOCAL_MAVEN\bin\mvn.cmd"
    if (-not (Test-Path $mvnExe)) { Write-Error "Maven 解压后未找到 mvn.cmd" }
    Write-Host "Maven 已下载到 .mvn-local"
}

Set-Location $PSScriptRoot
& $mvnExe spring-boot:run
