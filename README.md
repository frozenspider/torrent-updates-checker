torrent-updates-checker
=======================
Application for checking torrent updates automatically.


Tracker support
---------------
Currently only supports the following torrent trackers:

* [RUTOR](http://rutor.info/)
* [ALLTOR.ME](https://alltor.me)
* [TAS-IX.ME](http://tas-ix.me)

Last two are only accessible from inside TAS-IX - Uzbekistan internal Internet exchange point.


Building
--------
Java binary can be assembled via SBT by running `sbt assembly` command (will create 
`torrent-updates-checker-<VER>.jar` in `_build` subfolder).


Configuration
-------------
Copy `application.conf.example` as `application.conf` to the location of application jar
and modify it accordingly.


Usage
-----
Make sure that Java (at least version 8) is installed and `java`/`javaw` are available in `PATH`.

To start an application in the background, run it as

    javaw -jar torrent-updates-checker-<VER>.jar start

or use the startup script in the project root.

There are two ways of adding/removing aliases - via CLI or web interface.

Recommended way to control the application is through web interface at http://localhost:8100/
(given that default port is left unchanged). From there it's pretty straightforward.

Alternatively, you may use this application through CLI.
To see a list of available CLI commands, run

    java -jar torrent-updates-checker-<VER>.jar

However, note that aliases can't be handled through CLI if they contain some special characters (e.g. spaces).


Changelog
---------

See [CHANGELOG.md](CHANGELOG.md)
