#!/bin/bash

set -e

if command -v curl &>/dev/null; then
  DOWNLOAD_CMD="curl -L -o"
elif command -v wget &>/dev/null; then
  DOWNLOAD_CMD="wget -O"
else
  echo "Neither curl nor wget is installed. Please install one of them using your package manager."
  exit 1
fi

GITHUB_REPO="darthorimar/rekot"

echo "Fetching the latest release information for $GITHUB_REPO..."
if command -v curl &>/dev/null; then
  LATEST_RELEASE_INFO=$(curl -s "https://api.github.com/repos/darthorimar/rekot/releases/latest")
else
  LATEST_RELEASE_INFO=$(wget -qO- "https://api.github.com/repos/darthorimar/rekot/releases/latest")
fi

JAR_URL=$(echo "$LATEST_RELEASE_INFO" | grep '"browser_download_url":' | grep -Eo 'https://[^"]+\.jar' | head -n 1)

if [[ -z "$JAR_URL" ]]; then
  echo "No JAR file found in the latest release. Please check the repository."
  exit 1
fi

JAR_NAME=$(basename "$JAR_URL")

echo "Downloading $JAR_NAME from $JAR_URL..."
$DOWNLOAD_CMD "$JAR_NAME" "$JAR_URL"

INSTALL_DIR="$HOME/.local/bin"
mkdir -p "$INSTALL_DIR"

mv "$JAR_NAME" "$INSTALL_DIR/"

RUN_SCRIPT_NAME="rekot"
RUN_SCRIPT_PATH="$INSTALL_DIR/$RUN_SCRIPT_NAME"

cat <<EOF >"$RUN_SCRIPT_PATH"
#!/bin/bash
java -jar "$INSTALL_DIR/$JAR_NAME" "\$@"
EOF

chmod +x "$RUN_SCRIPT_PATH"

if ! echo "$PATH" | grep -q "$INSTALL_DIR"; then
  SHELL_PROFILE=""
  if [[ "$SHELL" == */zsh ]]; then
    SHELL_PROFILE="$HOME/.zshrc"
  elif [[ "$SHELL" == */bash ]]; then
    SHELL_PROFILE="$HOME/.bashrc"
  else
    SHELL_PROFILE="$HOME/.profile"
  fi

  echo "Adding $INSTALL_DIR to PATH in $SHELL_PROFILE..."
  echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >>"$SHELL_PROFILE"
  echo "Run 'source $SHELL_PROFILE' or restart your terminal for the changes to take effect."
fi

echo "Installation complete! You can now run the application with '$RUN_SCRIPT_NAME'."
