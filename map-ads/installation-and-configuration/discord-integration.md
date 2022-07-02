# Discord integration

If you want to use the Map-Ads Discord integration you need to download the extension and drop the file into your plugins folder.

Staff members need the 'Manage messages' permission to accept/deny ads using the buttons.

[Download extension here](https://github.com/cerus-mc/map-ads/releases/download/1.1.0/map-ads-discord-bot-1.1.0.jar)



![How the default settings look like](https://i.imgur.com/OxnL0tL.png)



### Configuration

Default config (discord.yml):

```yaml
enable: false
token: "?"

storage:
  sqlite:
    path: "discord.db"

activity:
  online-status: "ONLINE" # ONLINE, IDLE, DO_NOT_DISTURB, INVISIBLE
  type: "WATCHING" # LISTENING, WATCHING, COMPETING
  text: "the server" # type + text = "watching the server"

time-format: "dd.MM.yyyy HH:mm:ss"
message:
  title: "Advertisement by {{PLAYER_NAME}}"
  description: |-
    Player {{PLAYER_NAME}} (`{{PLAYER_ID}}`) submitted a new advertisement
    ID: {{ADVERT_ID}}
    Date: {{TIME}} (<t:{{TIME_RAW}}:f>, <t:{{TIME_RAW}}:R>)
    Screen: {{SCREEN}}
    Purchased minutes: {{MINUTES}} for ${{PRICE}}
  color: "#00AFFE"
  image: "{{IMAGE_URL}}"
  #thumbnail: "{{IMAGE_URL}}"
  button:
    accept: "Accept ad"
    deny: "Deny ad"

channel-ids: [ ]
```

#### enable & token

`enable` is a simple toggle for enabling or disabling the extension. The `token` field holds your Discord bot token.

#### Creating a Discord bot token

1. Visit [https://discord.com/developers/applications](https://discord.com/developers/applications) and log in with your Discord account
2. Click 'new application'
3. Enter a name of your choice and click 'create'
4. On the left sidebar click 'Bot'
5. Click 'add bot' and confirm
6. (Optional) Change the picture and username
7. (Optional) Disable 'public bot'
8. Click 'reveal token', copy the token and paste it into the 'token' field in the discord.yml file
9. On the left sidebar click 'OAuth2'
10. Scroll down and select the 'bot' scope
11. Scroll down even further and select the permissions (At least 'View channels', 'Send messages', 'Manage messages', 'Read message history')
12. Copy the link above the permissions selector and enter it into your browser
13. Select the server and invite the bot

#### activity

This controls the bot appearance.

`online-status`: Either `ONLINE`, `IDLE`, `DO_NOT_DISTURB` or `INVISIBLE`

`type`: Either `WATCHING`, `LISTENING` or `COMPETING`

`text`: Text of your choice, will be displayed next to the type ("Watching the server")

#### time-format

Format for dates and time (used for the \{{TIME\}} placeholder)

See [this page](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html) for an explanation on how the format works

#### message

Controls the appearance of the message. The `image` and `thumbnail` fields are optional and can be removed. `color` has to be specified using hex.

Placeholders:

`{{PLAYER_NAME}}` - Player name

`{{PLAYER_ID}}` - Player uuid

`{{ADVERT_ID}}` - Advertisement ID

`{{TIME}}` - Formatted ad creation according to the specified format

`{{TIME_RAW}}` - Ad creation time in seconds (can be used for [Discord's time formatter](https://www.reddit.com/r/discordapp/comments/ob2h2l/comment/h3l4fxs/?utm\_source=share\&utm\_medium=web2x\&context=3))

`{{SCREEN}}` - Name of the ad screen

`{{MINUTES}}` - Purchased minutes

`{{PRICE}}` - Price paid

`{{IMAGE_URL}}` - Url of the submitted image

#### channel-ids

Enter the ID of every channel that should receive a message here. Example: `channel-ids: [ "123456789", "123567864" ]`

Check out [this article](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) if you don't know how to get your channel ids
