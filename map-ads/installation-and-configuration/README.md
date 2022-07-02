# Installation and configuration

## How to install the Map-Ads plugin

1. Download the 'map-ads' plugin from SpigotMC.org
2. Download the 'maps' plugin from GitHub
   1. For 1.1.4 and below download [maps v1.0.6](https://github.com/cerus/maps/releases/download/1.0.6/maps.jar)
   2. For 1.2.0 and up download [maps v2](https://github.com/cerus/maps/releases/download/2.0.0/maps-plugin.jar)
3. Drop both plugins into your plugins folder
4. Restart the server

{% hint style="warning" %}
Note: You will need at least Java 16 and Minecraft 1.16.5 to run this plugin.
{% endhint %}

## Configuring Map-Ads

This is the default config:

```yaml
image-storage:
  type: "SQLite" # SQLite, MySQL
  mysql:
    host: "127.0.0.1"
    port: 3306
    db: "mapads"
    user: "mapads_user"
    pass: "secure_password_123"
  sqlite:
    db-name: "images.db"
advert-storage:
  type: "SQLite" # SQLite, MySQL
  mysql:
    host: "127.0.0.1"
    port: 3306
    db: "mapads"
    user: "mapads_user"
    pass: "secure_password_123"
  sqlite:
    db-name: "adverts.db"

images:
  max-size: 5242880
  url-whitelist:
    - "imgur.com"
    - "*.imgur.com"
ads:
  min-ad-minutes: 10
  max-ad-minutes: 120
  step: 5
  price-per-minute: 1000.0

enable-advanced-optimization-algorithm: false
send-update-message: true
disable-default-command: false
only-show-groups: false
deduct-each-screen-in-group: false

default-images: { }
```

### image-storage and advert-storage

Here you can choose where you want to store the images/adverts. The supported storage types are MySQL and SQLite. Simply adjust the section of the storage type you want to use and change the 'type' field.

### images

This section contains general settings for images. You can specify the maximum size and add/remove websites from the whitelist. See footnote 1 for examples on how the whitelist wildcards work.\
The maximum size is measured in bytes (the default equates to exactly 5 MB). 1 KB = 1024 bytes and 1 MB = 1024 KB.

~~You can also enable / disable the color cache feature. The color cache will reduce the time that image converting takes by _a lot_, but it will also take up \~16 MB of memory in return. Use this if you have enough memory.~~

{% hint style="danger" %}
Important security note: Only add **trusted** sites to the whitelist because players will try to exploit this functionality.
{% endhint %}

{% hint style="info" %}
The color cache has been removed. A more efficient memory cache is now used instead.
{% endhint %}

> ### ads

This controls general settings for advertisements.\
`min-ad-minutes` specifies the minimum amount of minutes for an advertisement.\
`max-ad-minutes` specifies the maximum amount of minutes for an advertisement.\
`step` specifies the minute increments when purchasing an ad.\
`price-per-minute` specifies the price for each minute. The total price will be calculated like this: `selected_minutes * price_per_minute`

![This is what the default settings (min = 10, max = 120, step = 5) look like ingame](../../.gitbook/assets/mapads\_clock\_opt.gif)

### ~~enable-advanced-optimization-algorithm~~

~~Enables (or disables) the~~ [~~Advanced Content Change Algorithm~~](https://github.com/cerus/maps/wiki/Concepts:-The-Advanced-Content-Change-Algorithm)~~. This algorithm is an optimization method that (in theory) reduces bandwidth and CPU usage by essentially doubling the memory usage.~~

{% hint style="info" %}
This algorithm has been removed in maps v2. A better algorithm is now used.
{% endhint %}

### disable-default-command

Disables the `/mapads` command which shows the current version, author and website of the plugin.

### only-show-groups

If enabled this will only show screen groups in the screen selector when creating an ad.

### deduct-each-screen-in-group

Will deduct one minute from the adverts lifetime for each screen in the rented screen group.&#x20;

Example: Group 'abc' has three screens. I rent the 'abc' group for a 15 minute advertisement. If this option is enabled each time my ad is shown on each of the screens one minute is deducted from the remaining minutes. If this option is disabled only the first screen in the group will deduct one minute from the remaining minutes.

### record-transitions

If set to `true` the plugin will record transitions. This has the potential to save lots of processing power because the plugin only needs to compute the transition once for each image pair and can then play back the recordings.

{% hint style="warning" %}
The transition recording feature is experimental and disabled by default. Please report bugs on the Discord server.
{% endhint %}

### override-economy

This allows you to force Map-Ads to use a specific economy plugin. Please see the "[Economy plugins](economy-plugins.md)" page for a list of supported economy plugins.

### default-images

Will be interpreted and changed by the plugin. Do not change anything here if you don't know what you're doing.







#### Footnote 1

Lets say your whitelist contains "imgur.com" and "\*.imgur.com". The following urls would be accepted:

* https://imgur.com/something
* https://imgur.com
* https://i.imgur.com/abcdef.png
* https://beans.imgur.com/

The following urls would **not** be accepted:

* https://abc.def.imgur.com/
* https://imgur.xyz
* https://google.com
