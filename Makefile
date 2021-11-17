test:
	clojure -A:dev:test -m kaocha.runner

autotest: start-dbs
	clojure -A:dev:test -m kaocha.runner --watch

.PHONY: test autotest
