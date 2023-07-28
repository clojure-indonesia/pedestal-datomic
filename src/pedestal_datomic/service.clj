(ns pedestal-datomic.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.response :as ring-resp]
            [datomic.client.api :as d]
            [pedestal-datomic.datomic :as p]))

(def datomic-client-interceptor
  (interceptor/interceptor {:name ::datomic-client-interceptor
                            :enter (fn [ctx]
                                     (let [client (p/get-client)]
                                       (assoc ctx ::client client)))}))

(def datomic-conn-db-interceptor
  (interceptor/interceptor {:name ::datomic-conn-db-interceptor
                            :enter (fn [ctx]
                                     (let [conn (p/get-connection (::client ctx) "pedestal-datomic")
                                           m {::conn conn
                                              ::db (d/db conn)}]
                                       (-> ctx
                                           (merge m)
                                           (update-in [:request] merge m))))}))
(def movie-interceptor
  (interceptor/interceptor {:name ::movie-interceptor
                            :enter (fn [ctx]
                                     (let [db (::db ctx)
                                           id (long (Integer/valueOf (or (get-in ctx [:request :path-params :id])
                                                                         (get-in ctx [:request :json-params :id]))))
                                           e (d/pull db '[*] [:movie/id id])]
                                       (assoc-in ctx [:request ::movie] (dissoc e :db/id))))}))
(defn about
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about))))

(defn home
  [request]
  (ring-resp/response "Hello, world!"))

(defn movies
  [request]
  (let [db (::db request)]
    (ring-resp/response
     (map (comp #(dissoc % :db/id) first)
          (d/q '[:find (pull ?e [*])
                 :where [?e :movie/id]]
               db)))))

(defn get-movie
  [request]
  (let [pet (::movie request)]
    (when (seq pet)
      (ring-resp/response pet))))

(defn add-movie
  [request]
  (let [conn (::conn request)
        movie (::movie request)
        {:keys [id title genre release-year]} (:json-params request)]
    (if (seq movie)
      (ring-resp/status (ring-resp/response (format "Movie with id %d exists." id))
                        500)
      (do
        (d/transact conn {:tx-data [{:db/id "new-movie"
                                     :movie/id (long id)
                                     :movie/title title
                                     :movie/genre genre
                                     :movie/release-year release-year}]})
        (ring-resp/status (ring-resp/response "Created")
                          201)))))

(defn update-movie
  [request]
  (let [conn (::conn request)
        movie (::movie request)
        id (Long/valueOf (get-in request [:path-params :id]))
        {:keys [title genre release-year]} (:json-params request)]
    (when (seq movie)
      (let [{:keys [db-after]} (d/transact conn {:tx-data [{:db/id [:movie/id id]
                                                            :movie/id id
                                                            :movie/title title
                                                            :movie/genre genre
                                                            :movie/release-year release-year}]})]
        (ring-resp/response (dissoc (d/pull db-after '[*] [:movie/id id])
                                    :db/id))))))

(defn remove-movie
  [request]
  (let [conn (::conn request)
        movie (::movie request)]
    (when (seq movie)
      (d/transact conn {:tx-data [[:db/retractEntity [:movie/id (:movie/id movie)]]]})
      (ring-resp/status (ring-resp/response "No Content.")
                        204))))

(def common-interceptors [(body-params/body-params)
                          http/json-body])

(def app-interceptors
  (into [datomic-client-interceptor
         datomic-conn-db-interceptor]
        common-interceptors))

(def routes #{["/" :get (conj common-interceptors `home)]
              ["/about" :get (conj common-interceptors `about)]
              ["/movies" :post (into app-interceptors [movie-interceptor `add-movie])]
              ["/movies" :get (conj app-interceptors `movies)]
              ["/movie/:id" :get (into app-interceptors [movie-interceptor `get-movie])]
              ["/movie/:id" :put (into app-interceptors [movie-interceptor `update-movie])]
              ["/movie/:id" :delete (into app-interceptors [movie-interceptor `remove-movie])]})
