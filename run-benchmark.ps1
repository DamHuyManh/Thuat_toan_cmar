$ErrorActionPreference = "Stop"

# Không đổi dataset. Script chỉ tối ưu "cách chạy" JVM để nhanh hơn và ổn định hơn.

Push-Location $PSScriptRoot
try {
  if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
  if (!(Test-Path "results")) { New-Item -ItemType Directory -Path "results" | Out-Null }

  Write-Host "== Compile ==" -ForegroundColor Cyan
  javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

  # JVM flags: giảm dao động do GC + tránh resize heap + dùng server compiler (JIT tốt hơn)
  $jvm = @(
    "-server",
    "-Xms2g", "-Xmx2g",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=50",
    "-XX:+AlwaysPreTouch"
  )

  Write-Host "`n== Warmup (JIT) ==" -ForegroundColor Cyan
  # Warmup nhanh để JIT compile các hot path trước khi đo thật
  java @jvm -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --warmupOnly

  Write-Host "`n== Baseline ==" -ForegroundColor Cyan
  java @jvm -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

  Write-Host "`n== Improved ==" -ForegroundColor Cyan
  java @jvm -cp bin cmar.benchmark.BenchmarkRunner --mode=improved

  Write-Host "`nDone. Xem results\summary-report.md và results\profiling-metrics.csv" -ForegroundColor Green
} finally {
  Pop-Location
}

