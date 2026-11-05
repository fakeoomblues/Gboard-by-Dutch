# de Volkskrant

**App version:** 7.53.1 · **Build:** 23136515 · **Original file:** APKS

[Download original app](https://buzzheavier.com/gsqypsmnoa36){ .md-button .md-button--primary }

Select the exact original file in Morphe Manager. Use the complete archive for a split app.

## Patches

| Patch | Default | What it does |
| --- | --- | --- |
| **Android Auto nieuws** | On | Makes Nieuws the primary Android Auto tab and lists the latest headlines from the home feed with their spoken audio. |
| **Disable tracking** | On | Stops the supported optional usage statistics, advertising identifiers and diagnostics. |
| **Hide Premium banner** | On | Hides automatic subscription offers and Premium promotions. |
| **Hide ads** | On | Removes supported ads in feeds, articles, videos and games. |
| **Read full articles** | On | Displays complete article text when the publisher supplies it. Does not create a subscription. |
| **Skip consent screens** | On | Skips automatic consent, login and notification setup. Starts with guest reading and notifications off; manual settings remain. |

## Android Auto nieuws

The car interface's first browse tab is now **Nieuws**: a running list of the latest headlines that the publisher supplies spoken audio for, exactly the items the phone app offers its listen button on. Rows use the app's own car styling (title, small cover, duration and date) and play through the app's existing article audio player. The former Start tab's server content is replaced entirely by this feed.

The tab includes every article with spoken audio in the app's home feed, without a fixed article limit. Lightweight browse rows keep the complete list available; choosing a row restores the app's full playback queue, including next and previous articles. The feed refreshes as the car browses, with a short cache for repeated requests, without extra accounts or settings. Articles behind the paywall keep their entitlement: they play for subscribers and stay gated for everyone else. If the feed is unavailable, the tab uses its cached news or stays empty; it never shows the former Start content.

## Advertising and tracking services

The ad and tracking controls cover the supported optional activity from: Snowplow, Contentsquare / Heap, Gemius, Comscore, Piano/Cxense, Firebase, Usabilla, BrandMetrics; publisher and player measurement.

Blocking measurement does not block every function from the same provider. Essential app services can still connect.

## Before using

Article availability depends on what the publisher supplies. No account, purchase or subscription is created. Accounts, push and media services remain online.

[Installation](../guides/install.md) · [Choose patches](../guides/choose-patches.md) · [Compatibility and defaults](../reference/catalogue.md)
