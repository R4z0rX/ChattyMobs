# ChattyMobs
ChattyMobs is a mod that lets you chat with Minecraft mobs and other entities by creating prompts and using the OpenAI API.

### Requirements
- Minecraft 1.20.4
- Fabric
- Fabric API

### Usage
After installing the mod, grab your OpenAI API key from [here](https://beta.openai.com/account/api-keys), and set it with the `/chattymobs setkey <key>` command.

You should now be able to **talk to mobs by shift+clicking** on them!

### Commands
- `/chattymobs` - View configuration status
- `/chattymobs help` - View commands help
- `/chattymobs enable/disable` - Enable/disable the mod
- `/chattymobs setkey <key>` - Set OpenAI API key
- `/chattymobs setmodel <model>` - Set AI model
- `/chattymobs settemp <temperature>` - Set model temperature

### Notes
This mod is based on a fork of AIMobs. The original author is rebane2001, and the fork is from Eianex.

This project was initially made in 1.12 as a client Forge mod, then ported to 1.19 PaperMC as a server plugin, then ported to Fabric 1.19. Because of this, the code can be a little messy and weird. A couple hardcoded limits are 512 as the max token length and 4096 as the max prompt length (longer prompts will get the beginning cut off), these could be made configurable in the future.

Some plans for the future:  
- Native Support for the Quilt modloader.
- Support for other AI APIs.
- More languages, already in english, spanish and ukranian.

The icon used is the **🧠** emoji from [Twemoji](https://twemoji.twitter.com/) (CC BY 4.0)
