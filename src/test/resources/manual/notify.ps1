
# Install-Module BurntToast -Scope CurrentUser

Import-Module BurntToast

New-BurntToastNotification `
    -Text "Maven", "Build Successful" `
    -AppLogo "../../../main/resources/emoji_u2705.png"
