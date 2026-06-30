# ========================================
# Blog Docker Deploy Script (PowerShell)
# Deploy from Windows to remote Linux host
# ========================================

param(
    [string]$RemoteHost = "192.168.152.128",
    [string]$RemoteUser = "root",
    [string]$RemotePath = "/opt/blog",
    [string]$SourcePath = "C:\Users\Karry\Desktop\Blog\Blog"
)

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host $Message -ForegroundColor Yellow
}

$sshHost = "$RemoteUser@$RemoteHost"

Write-Host ("=" * 50) -ForegroundColor Cyan
Write-Host "  Blog Docker Deployment" -ForegroundColor Cyan
Write-Host ("  Target: " + $RemoteUser + "@" + $RemoteHost + ":" + $RemotePath) -ForegroundColor Cyan
Write-Host ("=" * 50) -ForegroundColor Cyan

# ---- Step 1
Write-Step "[1/5] Creating remote directories..."
ssh $sshHost "mkdir -p $RemotePath/backend $RemotePath/frontend $RemotePath/ai_service"
if ($LASTEXITCODE -ne 0) {
    Write-Error "SSH connection or mkdir failed. Check SSH access."
    exit 1
}

# ---- Step 2
Write-Step "[2/5] Uploading config files..."
$remoteDest = "$RemoteUser@${RemoteHost}:$RemotePath/"
scp (Join-Path $SourcePath "docker-compose.yml") $remoteDest
scp (Join-Path $SourcePath ".env") $remoteDest
scp (Join-Path $SourcePath "init.sql") $remoteDest

# ---- Step 3
Write-Step "[3/5] Uploading backend..."
$rBackend = "$RemoteUser@${RemoteHost}:$RemotePath/backend/"
scp -r (Join-Path $SourcePath "backend\pom.xml") $rBackend
scp -r (Join-Path $SourcePath "backend\Dockerfile") $rBackend
scp -r (Join-Path $SourcePath "backend\.dockerignore") $rBackend
scp -r (Join-Path $SourcePath "backend\src") $rBackend

# ---- Step 4
Write-Step "[4/5] Uploading frontend..."
$rFrontend = "$RemoteUser@${RemoteHost}:$RemotePath/frontend/"
$frontendItems = @(
    "package.json", "package-lock.json", "Dockerfile", "nginx.conf",
    ".dockerignore", "vite.config.js", "index.html", "src"
)
foreach ($item in $frontendItems) {
    scp -r (Join-Path (Join-Path $SourcePath "frontend") $item) $rFrontend
}

# ---- Step 5
Write-Step "[5/5] Uploading AI service..."
$rAi = "$RemoteUser@${RemoteHost}:$RemotePath/ai_service/"
$aiItems = @("main.py", "requirements.txt", "Dockerfile", ".dockerignore", "services")
foreach ($item in $aiItems) {
    scp -r (Join-Path (Join-Path $SourcePath "ai_service") $item) $rAi
}

Write-Host ""
Write-Host "All files uploaded successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "Building and starting Docker services on remote host..." -ForegroundColor Yellow

$remoteCmd = "cd $RemotePath"
$remoteCmd = "$remoteCmd && docker compose up -d --build"
$remoteCmd = "$remoteCmd && echo '== Services Started =='"
$remoteCmd = "$remoteCmd && docker compose ps"

ssh -t $sshHost $remoteCmd

Write-Host ""
Write-Host ("=" * 50) -ForegroundColor Cyan
Write-Host "  Deployment Complete!" -ForegroundColor Green
Write-Host ("  Frontend:      http://" + $RemoteHost) -ForegroundColor Green
Write-Host ("  Backend API:   http://" + $RemoteHost + ":8080") -ForegroundColor Green
Write-Host ("  AI Service:    http://" + $RemoteHost + ":8000") -ForegroundColor Green
Write-Host ("  RabbitMQ:      http://" + $RemoteHost + ":15672") -ForegroundColor Green
Write-Host ("  Elasticsearch: http://" + $RemoteHost + ":9201") -ForegroundColor Green
Write-Host ("=" * 50) -ForegroundColor Cyan
