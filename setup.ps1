$ErrorActionPreference = "Stop"

$externalImagesDir = "d:\JAVA6\external_images"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "   STARTING DATABASE & IMAGES AUTO-SETUP       " -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan

if (-not (Test-Path -Path $externalImagesDir)) {
    Write-Host "[1/2] Creating external images directory: $externalImagesDir" -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $externalImagesDir | Out-Null
} else {
    Write-Host "[1/2] Images directory already exists: $externalImagesDir" -ForegroundColor Green
}

Write-Host "[2/2] Downloading 5 sample fashion images..." -ForegroundColor Yellow
$imageNames = @("ao-so-mi-trang.jpg", "chan-vay-xep-ly.jpg", "dam-di-tiec-den.jpg", "ao-blazer.jpg", "quan-tay-nu.jpg")
$urls = @(
    "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600&h=800&fit=crop", 
    "https://images.unsplash.com/photo-1583496661160-c5e8a5a9480f?w=600&h=800&fit=crop", 
    "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=600&h=800&fit=crop", 
    "https://images.unsplash.com/photo-1594223274512-ad4803739b7c?w=600&h=800&fit=crop", 
    "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=600&h=800&fit=crop"  
)

for ($i = 0; $i -lt 5; $i++) {
    $targetPath = Join-Path -Path $externalImagesDir -ChildPath $imageNames[$i]
    if (-not (Test-Path -Path $targetPath)) {
        try {
            Write-Host "   -> Downloading $(($i + 1))/5..."
            Invoke-WebRequest -Uri $urls[$i] -OutFile $targetPath -UseBasicParsing
        } catch {
            Write-Host "   -> Error downloading $($imageNames[$i]). Skipping." -ForegroundColor Red
        }
    } else {
        Write-Host "   -> $($imageNames[$i]) already exists." -ForegroundColor Green
    }
}

Write-Host "Download complete. Starting Spring Boot to apply Data and Image copies..." -ForegroundColor Yellow

Set-Location "d:\JAVA6"
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host " RUNNING MAVEN: mvn spring-boot:run " -ForegroundColor Cyan
Write-Host " DB and Images will be initialized via Spring Boot SetupDataService." -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
mvn spring-boot:run
