# Setup

## 1. Creating a map screen

{% tabs %}
{% tab title="Step 1" %}
Create an array of item frames in the desired width and height.

![](../.gitbook/assets/Screenshot\_20211008\_013730.png)
{% endtab %}

{% tab title="Step 2" %}
Look at the bottom left corner and type the command `/maps createscreen`.

![](../.gitbook/assets/Screenshot\_20211008\_013903.png)
{% endtab %}

{% tab title="Step 3" %}
Done!

![](../.gitbook/assets/Screenshot\_20211008\_013950.png)
{% endtab %}
{% endtabs %}

## 2. Creating an ad-screen

Use the command `/mapads screen create NAME SCREENID` to create an ad-screen from a map screen. Replace NAME with a name of your choice (no spaces) and replace SCREENID with the id of the map screen you just created. Example: `/mapads screen create my_cool_screen 8`

## 3. Customizing the ad-screen

You can set a transition animation by using the command `/mapads screen set transition NAME TRANSITION`. Replace NAME with the name of your ad-screen and TRANSITION with the name of the transition. You can find all available transitions below. Example command usage: `/mapads screen set transition my_cool_screen shift`

{% tabs %}
{% tab title="instant" %}
![](../.gitbook/assets/mapads\_transition\_instant.gif)
{% endtab %}

{% tab title="gradual_bar" %}
![](../.gitbook/assets/mapads\_transition\_bar.gif)
{% endtab %}

{% tab title="shift" %}
![](../.gitbook/assets/mapads\_transition\_shift.gif)
{% endtab %}

{% tab title="pixelate_big" %}
![](../.gitbook/assets/mapads\_transition\_pixelbig.gif)
{% endtab %}

{% tab title="pixelate_small" %}
![](../.gitbook/assets/mapads\_transition\_pixelsmall.gif)
{% endtab %}

{% tab title="overlay" %}
![](https://i.imgur.com/Dd5Xwz0.gif)
{% endtab %}

{% tab title="alpha" %}
![Added in v1.2.0](https://i.imgur.com/wMSyEOr.gif)
{% endtab %}

{% tab title="growing" %}
![Added in v1.2.0](https://i.imgur.com/XAP9s27.gif)
{% endtab %}

{% tab title="shrinking" %}
![Added in v1.2.0](https://i.imgur.com/Z2CugjM.gif)
{% endtab %}

{% tab title="snake_big" %}
![Added in v1.2.0](https://i.imgur.com/FmtFeRt.gif)
{% endtab %}

{% tab title="snake_small" %}
![Added in v1.2.0](https://i.imgur.com/TDHy1aV.gif)
{% endtab %}

{% tab title="stripes" %}
![Added in v1.2.0](https://i.imgur.com/7D34rwD.gif)
{% endtab %}
{% endtabs %}

In addition to transitions you can also set a fixed price or fixed time for adverts. When a fixed time is set, all adverts on this screen will run for the time you specified. Players can not change the time. When a fixed price is specified, all adverts on this screen will cost the price you specified, regardless of the amount of minutes the player has selected.

To set a fixed time use `/mapads screen set fixedtime SCREEN TIME`. Replace SCREEN with an ad-screen id and TIME with the amount of time in minutes.

To set a fixed price use `/mapads screen set fixedprice SCREEN PRICE`. Replace SCREEN with an ad-screen id and PRICE with a price.

You can remove the fixed time and price with `/mapads screen remove fixedtime` and `/mapads screen remove fixedprice`.

## 4. Setting the default image

Don't forget to set a default image! The default image is shown if no ads are currently running and it will be shared across all ad-screens that have the same size. For example, if you change the default image for 3x2 ad-screens, all 3x2 ad-screens will use that default image. Since version 1.2.4 you can also set different default images for each screen.

You can set the default image with the `/mapads defaultimage set SIZE/SCREEN DITHER URL`command. Replace SIZE/SCREE with either a valid size (like 3x2, 10x10, 2x9) or a screen id, DITHER with either 'none' or 'floyd\_steinberg' and URL with a url pointing to a valid image of the same size. You can calculate the required image width and height using this formula: `width = ad_screen_width * 128, height = ad_screen_height * 128`

Examples:\
`/mapads defaultimage set 3x2 none https://example.com/image.png` will set a default image for all 3x2 screens.\
`/mapads defaultimage set screen1 none https://example.com/image.png` will set a default image for screen `screen1`. This will override any other default images.&#x20;

## 5. Creating screen groups

Since version 1.2.4 you can group your screens. Screen groups allow players to rent multiple screens at once time for the same price.

You can create a screen group with the command `/mapads group create group_id Group Name`. Replace group\_id with a unique id (e.g. group1) and replace `Group Name` with a name of your choice (e.g. My Cool Group).

You can add screens to your group using the command `/mapads group screen add group_id screen_id`. Replace `group_id` with the id of a group you created and `screen_id` with the id of an ad screen.

You can remove screens from groups using `/mapads group screen remove group_id screen_id` and you can delete groups using `/mapads group remove group_id`.
