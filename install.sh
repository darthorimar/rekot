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

INSTALL_DIR="$HOME/.local/bin"
mkdir -p "$INSTALL_DIR"

JAR_NAME="rekot.jar"
echo "Downloading $JAR_URL to $INSTALL_DIR/$JAR_NAME ..."
$DOWNLOAD_CMD "$INSTALL_DIR/$JAR_NAME" "$JAR_URL"

RUN_SCRIPT_NAME="rekot"
RUN_SCRIPT_PATH="$INSTALL_DIR/$RUN_SCRIPT_NAME"

cat <<EOF >"$RUN_SCRIPT_PATH"
#!/bin/bash

set -e

APP_DIR=\$(java -jar "$INSTALL_DIR/$JAR_NAME" --app-dir)

function download() {
    if command -v curl &>/dev/null; then
        DOWNLOAD_CMD="curl -L -o"
    elif command -v wget &>/dev/null; then
        DOWNLOAD_CMD="wget -O"
    else
        echo "Neither curl nor wget is installed. Please install one of them using your package manager to check for updates."
        return 1
    fi

    echo "Checking for updates..."
    if command -v curl &>/dev/null; then
        LATEST_RELEASE_INFO=\$(curl -s "https://api.github.com/repos/darthorimar/rekot/releases/latest")
    else
        LATEST_RELEASE_INFO=\$(wget -qO- "https://api.github.com/repos/darthorimar/rekot/releases/latest")
    fi

    CURRENT_VERSION=\$(java -jar "$INSTALL_DIR/$JAR_NAME" --version)

    LATEST_VERSION=\$(echo "\$LATEST_RELEASE_INFO" | grep 'tag_name' | head -n 1 | cut -d '"' -f4)

    if [[ "\$CURRENT_VERSION" != "\$LATEST_VERSION" ]]; then
        JAR_URL=\$(echo "\$LATEST_RELEASE_INFO" | grep '"browser_download_url":' | grep -Eo 'https://[^"]+\.jar' | head -n 1)

        if [[ -z "\$JAR_URL" ]]; then
            echo "No JAR file found in the latest release. Please check the repository."
            return 1
        fi

        echo "A new version (\$LATEST_VERSION) of an ReKot is available. The current version is \$CURRENT_VERSION."
        echo "Do you want to update? (Y)es/(n)o/(d)o not ask again/(s)kip this version"
        read -r response

        case "\$response" in
            n)
                return 0
                ;;
            d)
                touch "\$APP_DIR/DO_NOT_UPDATE"
                echo "You will not be asked again."
                return 0
                ;;
            s)
                echo "\$LATEST_VERSION" >> \$APP_DIR/IGNORED_VERSIONS
                return 0
                ;;
            *)
                ;;
        esac

        INSTALL_DIR="\$HOME/.local/bin"
        mkdir -p "\$INSTALL_DIR"

        JAR_NAME="rekot.jar"
        echo "Downloading \$JAR_URL to \$INSTALL_DIR/\$JAR_NAME ..."
        \$DOWNLOAD_CMD "\$INSTALL_DIR/\$JAR_NAME" "\$JAR_URL"
    else
        echo "Already up to date."
    fi
}

UPDATE_FILE="\$APP_DIR/UPDATE"

if [[ -f "\$UPDATE_FILE" ]]; then
    if download; then
        rm -f "\$UPDATE_FILE"
    fi
fi

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
  echo "Run the following command or restart your terminal for the changes to take effect:"
  echo "source $SHELL_PROFILE"
  echo ""
fi

echo "Installation complete! You can now run the application with '$RUN_SCRIPT_NAME'."
