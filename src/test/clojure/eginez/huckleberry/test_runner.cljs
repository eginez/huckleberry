(ns eginez.huckleberry.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [eginez.huckleberry.core-test]))

(doo-tests 'eginez.huckleberry.core-test)
