# clog

Accesslog analyzer in clojure

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

Load the sql schema from clog.sql into your mysql database.
Configure mysql settings in src/clog/config.clj.

To start a web server for the application, run:

    lein run

It will be located at http://localhost:3000

## License

Copyright © 2013 FIXME
