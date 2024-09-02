# Path to the folder containing the files to sign
$folder = "./compiled/installers"

# Search for signtool.exe in the system's PATH
$signtoolPath = Get-Command -Name 'signtool.exe' | Select-Object -ExpandProperty Source

# Check if the folder exists
if (Test-Path $folder -PathType Container) {
    # Get all files in the folder
    $files = Get-ChildItem -Recurse -Path $folder -File 

    # Loop to sign each file
    foreach ($file in $files) {
        # Build the sign command for the current file
        $command = "& '$signtoolPath' sign /a /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 '$($file.FullName)'"

        # Execute the sign command
        Invoke-Expression -Command $command
    }

    Write-Host "Signing completed for all files in the folder."
} else {
    Write-Host "The specified folder does not exist."
}