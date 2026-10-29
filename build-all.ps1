# Builds the Ender Brews jar for every 1.21.x target in versions/ and collects them in build/release.
# Each target needs its own Gradle run: Loom, ModDevGradle and ForgeGradle set up one Minecraft each.
Set-Location $PSScriptRoot
$release = New-Item -ItemType Directory -Force 'build/release'
foreach ($file in Get-ChildItem 'versions/*.properties') {
    $target = $file.BaseName
    & ./gradlew.bat "-Ptarget=$target" clean build --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build for $target failed" }
    $label = (Select-String -Path $file -Pattern '^minecraft_versions_label=(.*)$').Matches[0].Groups[1].Value
    Copy-Item "build/libs/EnderBrews-*+mc$label.jar" $release
}
Get-ChildItem $release
