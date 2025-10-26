# MC Remote Control
Control and view your Minecraft client through Discord.

<div align="center">
    <a href="https://discord.gg/2b2tism"><img src="https://img.shields.io/discord/1340108466370641960?logo=discord" alt="Discord"/></a>
    <img src="https://img.shields.io/github/languages/code-size/Cypphi/mc-remote-control" alt="GitHub code size in bytes"/>
</div>

## Discord
- Join our Discord [here](https://discord.gg/2b2tism) for:
  - Suggestions
  - Bug reports
  - Further mod support
  - Updates
  - Yapping :D

## Supported versions
Feel free to request specific versions to be added :) (Join my support Discord.)

| MCRC Version                                                            | Minecraft version |
|-------------------------------------------------------------------------|-------------------|
| [1.0.1](https://github.com/Cypphi/mc-remote-control/releases/tag/1.0.1) | 1.21.4 & 1.21.10  |


## Usage
Here's a simple guide for how to use this mod.

### 1. Installing FabricMC & required mods
- This mod is for FabricMC exclusively. You can install FabricMC [here](https://fabricmc.net/use/installer/)
- Download the following dependencies:
  - [YetAnotherConfigLib](https://modrinth.com/mod/yacl)
  - [Fabric API](https://modrinth.com/mod/fabric-api)
  - [Mod Menu](https://modrinth.com/mod/modmenu)
- Download the latest release or most relevant version of MCRC [here](https://github.com/Cypphi/mc-remote-control/releases)

### 2. Initial launch & configuration
- Launch the client and open the **Mods** menu in the title screen. 
- Look for the MC Remote Control entry. And click on its icon.
- In the configs, turn on **Start on launch**. (No startup command or logic is added yet.)
- Configure the settings to your needs by following the next step

### 3. Setting up your Discord bot
- Create an application at [Discord Developer Portal](https://discord.com/developers/applications)
- Click on this newly created application and head to the **Bot** tab.
- Click the "reset token" button. Keep this token to yourself only. Place this token inside the 3rd settings tab in the "Discord Bot Token" box.
- On the Discord Developers page go to the OAuth2 tab. In the URL generator, tick the bot box. Copy the generated URL and open it in your browser. Invite the bot to your server.
- Turn on Developer mode in Discord. [How to enable developer mode](https://help.mee6.xyz/support/solutions/articles/101000482629-how-to-enable-developer-mode).
- Populate the remaining 3 ID settings with the correct IDs. To copy an ID, right-click a user, role, channel, server icon etc. Then select "Copy X ID".
  - Discord Channel ID: The ID of the channel you want to use the bot in. The bot will only work in this channel!
  - Allowed Discord User ID: This setting only works if you have "Allow Public Commands" off. You should probably keep this off.
  - Discord Guild ID: The ID of the server you use the bot in.

### 4. Relaunch
- Relaunch. Have fun :)
- Run /help through the Discord bot to view a list of commands you can use.

## Contributing
- Contributions are welcome!
- Please follow my code style, clearly state what you changed/added.
- Only include 1 feature per pull request. Please don't submit pull requests that change 10 different things all at once.

## Licensing Notice
- This project uses [JDA (Java Discord API)](https://github.com/discord-jda/JDA)
  licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- All bundled third-party components are documented in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
