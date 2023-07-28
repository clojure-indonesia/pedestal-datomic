### DATOMIC PRO
```bash
curl -O https://datomic-pro-downloads.s3.amazonaws.com/1.0.6735/datomic-pro-1.0.6735.zip
unzip datomic-pro-1.0.6735.zip
cd datomic-pro-1.0.6735
```

### STORAGE
```bash
psql -f bin/sql/postgres-db.sql -U postgres
psql -f bin/sql/postgres-table.sql -U postgres -d datomic
psql -f bin/sql/postgres-user.sql -U postgres -d datomic
```
### TRANSACTOR
```bash
bin/transactor -Ddatomic.printConnectionInfo=true config/samples/sql-transactor-template.properties
```
### CREATE DATABASE
```bash
bin/shell
```
```
uri = "datomic:sql://pedestal-datomic?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic";
Peer.createDatabase(uri);
```
### PEER SERVER
```bash
bin/run -m datomic.peer-server \
        -h localhost \
        -p 8998 \
        -a datomic,d4t0m1c \
        -d pedestal-datomic,"datomic:sql://pedestal-datomic?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"
```
### CONSOLE
```bash
bin/console -p 8080 sql "datomic:sql://?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"
```

---


### RUN
```bash
clojure -Mrun
```
### ENDPOINT
```
http://localhost:8080
```
| METHOD | URL        |
|--------|------------|
| GET    | /          |
| GET    | /about     |
| POST   | /movies    |
| GET    | /movies    |
| GET    | /movie/:id |
| UPDATE | /movie/:id |
| DELETE | /movie/:id |

### REFERENCES
- [http://pedestal.io/](http://pedestal.io/)
- [https://docs.datomic.com/pro/](https://docs.datomic.com/pro/)
