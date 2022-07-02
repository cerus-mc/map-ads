# Developer API

## Installation

{% tabs %}
{% tab title="Maven" %}
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.cerus-mc</groupId>
    <artifactId>map-ads</artifactId>
    <version>Tag</version>
</dependency>
```
{% endtab %}

{% tab title="Gradle" %}
```groovy
allprojects {
    repositories {
        ...
	maven { url 'https://jitpack.io' }
    }
}

dependencies {
        implementation 'com.github.cerus-mc:map-ads:Tag'
}
```
{% endtab %}
{% endtabs %}

## Events

Map-Ads currently has two events: `AdvertCreateEvent` and `AdvertReviewEvent`. Both events can be cancelled.

The `AdvertCreateEvent` is called when a player tries to create an ad.

The `AdvertReviewEvent` is called when a staff member tries to review an ad.

## Transitions

Map-Ads allows you to create your own transitions. Just make a new class and implement the `Transition` interface:

```java
public class MyTransition implements Transition {

    @Override
    public void makeTransition(final @NotNull MapScreen screen, @Nullable final MapImage oldImg, final @NotNull MapImage newImg) {
        // Make your transition here
    }
    
}
```

After that you can register the transition by calling `TransitionRegistry.register("my_transition", new MyTransition());`.

I encourage you to take a look at the [existing transitions](https://github.com/cerus-mc/map-ads/tree/main/plugin/src/main/java/dev/cerus/mapads/image/transition) to see how they work.

## Services

You can get an instance of the following services using the Bukkit service manager (`Bukkit.getServicesManager().getRegistration(ServiceClass.class).getProvider()` - You'll have to replace ServiceClass.class with the class of the service):

* AdvertStorage
* AdScreenStorage
* ImageStorage
* DefaultImageController
* ImageConverter
* ImageRetriever
