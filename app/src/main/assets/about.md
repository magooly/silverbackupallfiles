METALS & CRYPTO HOLDINGS - USER GUIDE
App version 1.1.0 | Guide updated 14 October 2026

PAGE ONE
Gold Spot oz and Silver Spot oz are prices for one ounce, independent of your
holding quantities. Holding values use your saved quantities. Gold/Silver prices
are requested even when neither metal is held.

Back on page one shows "Press Back again to exit." Press Back again within two
seconds to leave. The confirmation resets when you leave page one. Other screens
and dialogs keep normal Back behavior.

THREE-DOT MENU
- Update Holding opens page two for adding, editing, and deleting.
- Get Latest Prices retries market quotes while online.
- Display currency supports AUD, NZD, USD, GBP, EUR, CNY, and JPY.
- Whole dollars only is a checkbox in the currency dialog. It rounds displays,
	not saved amounts, calculations, or history. Settings apply immediately and
	are remembered; press OK to close the dialog.
- Notes, Backup, Restore, Version History, and Dark mode are also available.
	Page-one Notes currently opens page two; select Notes there to view/edit it.

PAGE TWO
- Metals: Gold/Silver use number of items, weight per item, and description.
	Silver's weight label switches oz/kg. Other metals use total units; confirm
	provider quote units before relying on their values.
- Crypto: enter the coin amount, including fractions.
- Stocks: enter an ASX ticker and shares. Use uppercase for a new ticker, such
	as TLS or BHP. Subsequent additions recognize existing stock names regardless
	of capitalization, combine shares, and preserve stock classification.
- Other: enter a unique name and a total AUD value, not just a unit count.
	Different names stay separate. Adding an existing non-detailed name increases
	its amount; use Edit to replace it instead.
Select an existing item to edit/delete. Successful deletion closes its dialog.
Page-two entries are quantities or base AUD values, not currency conversions.

PRICES AND N/A
New share tickers request prices on return to page one online. Existing requested
tickers can be refreshed with Get Latest Prices. Provider failures, missing
configuration, unsupported symbols, and account limits can prevent updates.
Held assets without a quote show N/A; the total also shows N/A when incomplete.
Missing FX rates can cause N/A too. Clear dismisses the multiline warning, not
the underlying issue. A zero holding is different from an unavailable price.

Metals use MetalCharts quotes assumed USD. CoinGecko crypto, EODHD ASX shares,
and entered Other values are treated as AUD. MetalCharts FX rates are cached,
with refresh after 24 hours when needed online. Offline prices/rates may be old;
successful live Gold/Silver quotes override cache. Offline coverage varies by
asset. Some base-metal mappings still need provider symbol/unit verification.

BACKUP AND RESTORE
Automatic snapshots are internal. Manual Backup accepts a filename and attempts
public JSON export to Documents/SilverHolding on the Android device. Default:
holdings_backup.json. Check the result: Android storage restrictions may prevent
public export although the internal snapshot is saved. Keep an accessible copy
separately before uninstalling or moving phones.
Snapshots contain holdings, Notes text, and stock/crypto lookup preferences.
- Merge adds incoming non-detailed quantities/values. Identical holdings payloads
	previously applied are skipped; changed backups can still add totals again.
- Replace deletes current holdings first. Save a separate backup beforehand;
	failed or partial restore may require manual recovery.
Merge is not two-way synchronization. PC source backups do not hold phone data.

NOTES AND VERSION HISTORY
Notes are editable and included in backup JSON. Internal about.txt overrides
external SilverHolding/about.txt, which overrides bundled help. App updates
do not replace old editable copies. Preserve personal Notes before replacing
saved help. Version history records version-name changes, not each APK rebuild.

Build instructions: project README.md/README.txt.
Transfer instructions: project readmefirst.txt/readmefirst.md.
This bundled guide is supplied as about.txt and about.md.
