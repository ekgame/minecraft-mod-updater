# Minecraft Mod Updater
A simple CLI tool to update mods in a Minecraft mods folder. Only supports updating [Fabric](https://fabricmc.net/) mods from [Modrinth](https://modrinth.com/).

## Usage
1. Download the latest release from releases page.
2. Place the .jar file in your mods folder.
3. Run the command: 
```shell
java -jar <filename>.jar --mc=<target minecraft version>
```
or
```shell
java -jar <filename>.jar # You will be prompted for the target minecraft version
```
Notes:
- The target minecraft version may be different from the mod's supported Minecraft version. In that case the mods will be updated to the latest version that supports the target Minecraft version.
- The target minecraft version must be a valid Minecraft version. You can find a list of valid versions [here](https://meta.fabricmc.net/v2/versions/game).
- If the tool is in a different folder than the mods folder, you can specify the mods folder in the command, like:
```shell
java -jar <filename>.jar --mc=<target minecraft version> /path/to/mods/folder
```