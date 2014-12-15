(ns openslack.views.sidebar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.views.user :refer [user]]
            [openslack.views.channels :refer [channels]]
            [openslack.views.subscriptions :refer [subscriptions]]))

(defn sidebar
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sidebar")

    om/IRender
    (render [_]
      (s/html [:div.client-sidebar
               [:div.logo "SlothLogo"]
               (om/build user state)
               (om/build channels state)
               [:hr]
               (om/build subscriptions state)]))))
