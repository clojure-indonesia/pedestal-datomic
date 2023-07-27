(ns pedestal-datomic.server
  (:require [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [datomic.client.api :as d]
            [pedestal-datomic.service :as service]
            [pedestal-datomic.datomic :as p]))

(def service {::http/routes #(route/expand-routes @#'service/routes)
              ::http/type :jetty})

(defn -main
  [& [port]]
  (p/load-dataset (d/connect (p/get-client) {:db-name "pedestal-datomic"}))
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)]
    (-> service
        (merge {::http/port 8080
                ::http/join? false})
        http/create-server
        http/start)))

(defonce server (atom nil))
(defn start-dev
  []
  (reset! server (-> service
                     (merge {::http/port 8080
                             ::http/join? false})
                     http/create-server
                     http/start)))
(defn stop-dev
  []
  (http/stop @server))
(defn restart
  []
  (stop-dev)
  (start-dev))

(comment
  (require '[pedestal-datomic.service] :reload)
  (p/load-dataset (d/connect (p/get-client) {:db-name "pedestal-datomic"}))
  (start-dev)
  (restart)
  (stop-dev)
  (io.pedestal.test/response-for (::http/service-fn @server) :get "/movies")
  (-> (io.pedestal.test/response-for (::http/service-fn @server) :get "/movie/1")
      (get :body)
      (json/read-json))
  (io.pedestal.test/response-for (::http/service-fn @server) :post "/movies" :headers {"Content-Type" "application/json"} :body (json/write-str {:id 4
                                                                                                                                                 :title "Oppenheimer"
                                                                                                                                                 :genre "history"
                                                                                                                                                 :release-year 2023}))
  (io.pedestal.test/response-for (::http/service-fn @server) :put "/movie/4" :headers {"Content-Type" "application/json"} :body (json/write-str {:title "Oppenheimer"
                                                                                                                                                 :genre "drama"
                                                                                                                                                 :release-year 2023}))
  (io.pedestal.test/response-for (::http/service-fn @server) :delete "/movie/4"))
