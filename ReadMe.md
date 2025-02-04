<p align="left">
<img src="logo.svg" alt="ReKot" width="100">
</p>
<h1 align="left">ReKot</h1>

##### <i>Kotlin REPL with an IDE-like experience in your terminal</i>

[![GitHub Release](https://img.shields.io/github/v/release/darthorimar/rekot)](https://github.com/darthorimar/rekot/releases/latest)

## ⚙️ Installation

_Make sure you have the JDK installed_

Paste this into the terminal:

```bash
bash <(curl -s https://raw.githubusercontent.com/darthorimar/rekot/master/install.sh)
```

Or if you prefer wget:

```bash
bash <(wget -qO- https://raw.githubusercontent.com/darthorimar/rekot/master/install.sh)
```

## ✨ Features

### Multiline Code Editing

A full-fledged multiline code editor with code highlighting
<img src="images/multi_line.png" alt="Multiline code editing" >

### Multiple Cells

With results that can be reused between the cells
<img src="images/multi_cell.png" alt="Multiple cells" >

### Code Completion

<img src="images/completion.png" alt="Code completion" >

### In-editor Code Highlighting
<img src="images/errors.png" alt="In-editor code highlighting" >


## 🧪 Compatibility
* Tested on _macOS Sequoia 15.2_ on _iTerm2_ and _Terminal.app_
* Not tested on _Linux_ or _Windows_

## ⚠️ Known Problems

On macOS Sonoma, some text may be printed on the terminal like:
```
2025-01-28 23:39:24.855 java[2091:30776] +[IMKClient subclass]: chose IMKClient_Modern
2025-01-28 23:39:24.855 java[2091:30776] +[IMKInputSession subclass]: chose IMKInputSession_Modern
```

See https://discussions.apple.com/thread/255761734

As a workaround:
- On Mac systems, ReKot occasionally fully refreshes the screen at some interval.
- You can press `Ctrl+R` to manually refresh the screen.

## 🛠️ Building/Developing ReKot

Currently, ReKot depends on the [Kotlin Analysis API](https://kotlin.github.io/analysis-api) with a few patches on top. These patches are in my fork of the Kotlin repository, in the branch `rekot`: https://github.com/darthorimar/kotlin/tree/rekot.

To start developing/building ReKot:

1. Clone the Kotlin on the branch `rekot` repository from https://github.com/darthorimar/kotlin/tree/rekot.
2. Run `./gradlew installIdeArtifacts -Ppublish.ide.plugin.dependencies=true` in the cloned repository. This will install the Analysis API to your Maven Local.
3. Now you can start working in the ReKot repository, and it can be imported into IntelliJ IDEA.
4. Gradle tasks:
    - `:app:buildProd` - This will create a release (a shadow jar) in `app/build/libs/rekot-VERSION.jar`.
    - `:app:run` - Run the app in the Swing-based terminal emulator, which sometimes can look quite blurry, but it's useful for debugging.
