# Notify-send requires an absolute path
ICON="$(realpath "$(dirname "$0")/../../../main/resources/emoji_u2705.png")"
notify-send -h "string:image-path:$ICON" "Maven" "Build Successful"
