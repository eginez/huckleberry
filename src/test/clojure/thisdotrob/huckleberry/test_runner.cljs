(ns thisdotrob.huckleberry.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [thisdotrob.huckleberry.core-test]))

(doo-tests 'thisdotrob.huckleberry.core-test)
