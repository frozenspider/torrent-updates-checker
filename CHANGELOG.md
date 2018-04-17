### SNAPSHOT
* Fixed issue with non-ASCII chars in alias and username/password

### 1.2.1
* Adding URLs no longer causes immediate auth query from every provider

### 1.2
* Links in popup are now clickable
  * Works on Windows, untested best-shot implementation for Linux (uses `BROWSER` env var) and Mac OS

### 1.1.1
* Fixed issue with incorrectly determined provider page encoding
  * Now uses a fallback encoding detector
* Fixed issue with failure to dump misunderstood page content

### 1.1
(Baseline version)
