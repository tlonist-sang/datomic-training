(ns core
  (:require [datomic.client.api :as d]))

;;; https://docs.datomic.com/on-prem/learning/videos.html
;;; Datomic, Part 1

;; Extensible Data Notation
;; Primray way of talking to Datomic
;; Symbol & Keyword -> what was made possible? and what are not possible by the absence of these?

;; # -> extension point -> edn doesn't have to understand it (pass that on to the next person!)
;; all data can be literal (#foobar , just pass it around)
;; allows application domain to extend how data is represented
;; no need to spend time in marshalling/unmarshalling
;; #inst, #uuid ...

;;;; Architecture (bottom up)

;;; Datoms
;; Databases that forget
;; entity / attribute / value
;; granular, atomic facts. Immutable, 5-tuple: entity/attribute/value/transaction/op
;; time travel (.asOf, .since, .with, history, tx report)

;; Inflexible Modeling
;; RDBMS : write data in one shape (rectangles)
;; NoSQL arguable better: get a choice of N write shapes (by running N different NoSQL systems with ETL jobs)
;; NoSQL : copying of information, and making them consistent (cumbersome!)
;; Datomic : shape your data at read time

;; One database, Many Shapes
;; k/v AVET row EVAT column AEVT document EAVT,partitions,components graph VAET
;; more of architecture problem than that of development

;; Schema
;; dataomic schema is plain data
;; make history-compatible changes at any time
;; db/ident, valueType, cardinality, index, unique, isComponent

;; Database
;; set of datoms
;; efficient storage
;; many sort of orders
;; accumulate only (not append-only)

;;; Operations
;; peers/transactors/stroage/caching/indexing

;; peers : embedded JVM lib (directly in application server, introduce a service tier)
;; query load does not bother the transactor
;; answer queries in memory via automatic cache

;; Transactors (also does indexing)
;; ACID (single writer thread)
;; stream changes to peers
;; Background indexing
;; Dead-simple HA (conditional puts on storage)
;; don't scale arbitraily (because there is only one writer thread)
;; needs to know - 1) where IO cluster is (URL) 2)whether the primary died

;; Storage options
;; dynamoDB, SQL, cassandra, riak ...
;; choosing storage  important : reliability, manageability, familiarity/ less-important: read-load latency (because peers cache so much data)
;; immutable data structure -> never reuses name
;; eventually-consistent VS immediately consistent -> clojure atom?
;; gets rid of 1+N problem (automatic, return one in every thousands!)
;; how to sync between cache and storage? how -> memcached, jvm memory .. etc
;; cache speeds are VERY fast (peer object cache)
;; Indexing
;; problem with append-only system -> always re-build indexes
;; datomic maintains live index and memory of recent jobs only when it exceeds chunk sides does it send wors to background automatically
;; set aside 32mb of memory for doing this
;; wide trees (branching factor of thousand...)

(comment
  (def config {:server-type        :peer-server
               :access-key         "myaccesskey"
               :secret             "mysecret"
               :endpoint           "localhost:8998"
               :validate-hostnames false})

  (def client (d/client config))
  (def conn (d/connect client {:db-name "hello"}))

  (def movie-schema [{:db/ident       :movie/title
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc         "The title of the movie"}

                     {:db/ident       :movie/genre
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc         "The genre of the movie"}

                     {:db/ident       :movie/release-year
                      :db/valueType   :db.type/long
                      :db/cardinality :db.cardinality/one
                      :db/doc         "The year the movie was released in theaters"}])

  (d/transact conn {:tx-data movie-schema})

  (def first-movies [{:movie/title        "The Goonies"
                      :movie/genre        "action/adventure"
                      :movie/release-year 1985}
                     {:movie/title        "Commando"
                      :movie/genre        "action/adventure"
                      :movie/release-year 1985}
                     {:movie/title        "Repo Man"
                      :movie/genre        "punk dystopia"
                      :movie/release-year 1984}])

  (d/transact conn {:tx-data first-movies})

  (def db (d/db conn))
  (def all-movies-q '[:find ?e
                      :where [?e :movie/title]])

  (d/q all-movies-q db)
  (def all-titles-q '[:find ?movie-title
                      :where [_ :movie/title ?movie-title]])

  (d/q all-titles-q db)

  (def titles-from-1985 '[:find ?title
                          :where [?e :movie/title ?title]
                          [?e :movie/release-year 1985]])

  (d/q titles-from-1985 db)

  (def all-data-from-1985 '[:find ?title ?year ?genre
                            :where [?e :movie/title ?title]
                            [?e :movie/release-year ?year]
                            [?e :movie/genre ?genre]
                            [?e :movie/release-year 1985]])
  (d/q all-data-from-1985 db)

  (d/q '[:find ?e
         :where [?e :movie/title "Commando"]]
       db)

  (def commando-id
    (ffirst (d/q '[:find ?e
                   :where [?e :movie/title "Commando"]]
                 db)))

  (d/transact conn {:tx-data [{:db/id commando-id :movie/genre "future governor"}]})
  (def all-data '[:find ?title ?year ?genre
                  :where
                  [?e :movie/title ?title]
                  [?e :movie/genre ?genre]
                  [?e :movie/release-year ?year]])

  (d/q all-data db)
  (d/q all-data-from-1985 db)

  (def old-db (d/as-of db 1004))
  (d/q all-data old-db)

  (def history-db (d/history db))
  (d/q '[:find ?genre
         :where
         [?e :movie/title "Commando"]
         [?e :movie/genre ?genre]] history-db)






  ,)
