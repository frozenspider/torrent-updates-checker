### 1.4.1
* Fixed issue with Rutor provider being excluded on startup
* (Internal) Configuration and checks timing cache are now stored in two different config files
* (Internal) Support for data migration
* (Internal) Bunch of smaller infrastructural changes

### 1.4
* Rutor (rutor.info, rutor.is) provider added

### 1.3
* Popup now plays a notification sound when displayed
* Links in web interface are now also clickable
* Fixed startup script to work properly with path containing spaces
* Providers are now skipped if their configured login is empty
(previously, was skipped if their config lines were deleted)

### 1.2.2
* Fixed issue with non-ASCII chars in alias and username/password

### 1.2.1
* Adding URLs no longer causes immediate auth query from every provider

### 1.2
* Links in popup are now clickable
  * Works on Windows and Linux (uses `BROWSER` env var), untested best-shot implementation for MacOS

### 1.1.1
* Fixed issue with incorrectly determined provider page encoding
  * Now uses a fallback encoding detector
* Fixed issue with failure to dump misunderstood page content

### 1.1
(Baseline version)
