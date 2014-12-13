(ns openslack.async
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cats.protocols :as proto]
            [cats.monad.either :as either]
            [cljs.core.async :refer [<! chan put!]]))

(def sequential-chan-monad
  (reify
    proto/Monad
    (mreturn [_ av]
      (go av))
    (mbind [_ mv f]
      (go (<! (f (<! mv)))))))

(def either-pipeline-monad (either/either-transformer sequential-chan-monad))
