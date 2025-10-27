# MC Remote Control

<div align="center">
    <a href="https://discord.gg/2b2tism"><img src="https://img.shields.io/discord/1340108466370641960?logo=discord" alt="Discord"/></a>
    <img src="https://img.shields.io/github/languages/code-size/Cypphi/mc-remote-control" alt="Code size in bytes"/>
    [![Lines of Code](https://tokei.rs/b1/github/OWNER/REPO?category=code)](https://github.com/Cypphi/mc-remote-control)
</div>

Control and view your Minecraft client directly through Discord.

## Discord
- Join our Discord [here](https://discord.gg/2b2tism) for:
  - Suggestions, support & discussion regarding the mod.
  - Other Minecraft (or 2b2t) related projects.

## Supported versions
Feel free to request additional versions to be supported :) (Join my support Discord.)

| MCRC Version                                                            | Minecraft version |
|-------------------------------------------------------------------------|-------------------|
| [1.1.0](https://github.com/Cypphi/mc-remote-control/releases/tag/1.1.0) | 1.21.4 & 1.21.10  |


## Usage
Here's a quick guide to get started.

### 1. Installing FabricMC & required mods
- This mod is Fabric-only. Install FabricMC [here](https://fabricmc.net/use/installer/).
- Download the following dependencies:
  - [YetAnotherConfigLib](https://modrinth.com/mod/yacl)
  - [Fabric API](https://modrinth.com/mod/fabric-api)
  - [Mod Menu](https://modrinth.com/mod/modmenu)
- Grab the latest release (or the version you need) of MCRC [here](https://github.com/Cypphi/mc-remote-control/releases).

### 2. Initial launch & configuration
- Launch the client and open the **Mods** menu on the title screen. 
- Find the MC Remote Control entry and click its icon.
- In the config screen, enable **Start on launch** (startup logic is not implemented yet).
- Configure the remaining settings using the next step.

### 3. Setting up your Discord bot
- Create an application at the [Discord Developer Portal](https://discord.com/developers/applications).
- Open the new application and switch to the **Bot** tab.
- Click **Reset Token**, store the token securely, and paste it into the mod’s third settings tab under “Discord Bot Token.”
- In the **OAuth2** tab, open the URL generator, enable the bot scope, copy the generated link, and invite the bot to your server.
- Enable Developer Mode in Discord ([guide](https://help.mee6.xyz/support/solutions/articles/101000482629-how-to-enable-developer-mode)).
- Fill out the remaining three ID fields. To copy an ID, right-click the user/role/channel/server and choose “Copy X ID.”
  - Discord Channel ID: The channel the bot should monitor. It only responds here.
  - Allowed Discord User ID: Used only when “Allow Public Commands” is disabled; recommended to keep public commands off.
  - Discord Guild ID: The server hosting the bot.

### 4. Relaunch
- Relaunch the client and have fun :)
- Run `/help` via the Discord bot to view the available commands.

## Contributing
- Contributions are welcome!
- Follow the existing code style and clearly describe what you changed or added.
- Limit each pull request to a single feature; avoid bundling multiple unrelated changes together.

## Licensing Notice
- This project uses [JDA (Java Discord API)](https://github.com/discord-jda/JDA)
  licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- All bundled third-party components are documented in [THIRD_PARTY_NOTICES.md](https://github.com/Cypphi/mc-remote-control/blob/1.21.4/THIRD_PARTY_NOTICES.md).
