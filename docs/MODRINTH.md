<p align="center">
  <iframe width="640" height="360" src="https://www.youtube-nocookie.com/embed/_1pOw55mNeg" title="Ender Brews showcase" frameborder="0" allowfullscreen></iframe>
</p>

<p align="center"><b>Put 8 potions of the same kind in your Ender Chest and that effect becomes endless.<br>Take them out and it's gone.</b></p>

## 🧪 How it works

1. **Brew** potions as usual.
2. **Store** 8 of the same potion in your Ender Chest. A shulker box inside the Ender Chest works too, so one box holds 27.
3. **Enjoy** the effect. It shows `∞` in the HUD and never runs out. Take the potions out and it's gone.

## 💡 Good to know

- **Only the Ender Chest counts.** Not your inventory, not other chests. Every player has their own endless effects.
- **Stronger potions stack on top.** Drink Strength II over endless Strength I and you get II until it ends, then endless I comes back.
- **Milk and totems don't break it.** The effects return on the next tick.
- **Pause what annoys you.** Run `/enderbrews` and click an effect to pause it, for example invisibility or slow falling while you build.
- **No instant or harmful effects.** Healing and harming are never endless. Poison, weakness and the like aren't either, unless the server enables `allow_harmful_effects`.
- **Your server, your rules.** You can change the potion count, set it per effect, cap the level and ban effects in the config.

## 📥 Install

One jar works as a mod and as a plugin. Pick the file for your Minecraft version and loader.

It only has to be on the **server**. Vanilla clients can join, and in singleplayer you just install it.

Installing it on the client as well adds potion tooltips (`Strength: 5/8 in Ender Chest`), a chat list that updates in place, click-to-pause on the effect icons and a pause-all key.

## ⌨️ Commands

| Command | Who | What |
|---|---|---|
| `/enderbrews` | everyone | Your endless effects and pause buttons |
| `/enderbrews pause\|resume\|toggle <effect>` | everyone | Pause or resume one effect |
| `/enderbrews reload` | operators | Reload the config |

## ⚙️ Config

`config/enderbrews.toml` (plugin: `plugins/EnderBrews/enderbrews.toml`). How many potions you need, per-effect amounts, max level, limits, blacklist and whitelist and more. Every option is commented in the file.
