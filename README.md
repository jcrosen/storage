# storage

A simple bucket/key storage service with a super-simple HTTP API and configurable storage
adapters (currently file-system and S3).

## Installation

### Dependencies
**Docker**
* [Docker](https://www.docker.com/) (1.3+ required for OSX shared folders)
* [fig/docker-compose](https://github.com/docker/fig)

**Native**
* [Leiningen](http://leiningen.org/) (Can be installed via homebrew on OSX)
* [Java Virtual Machine](https://java.com) (Any newer JVM should be fine; tested against 1.7.x)

### Setup
* Clone the repo

**Docker**
* From project root `cp fig.example.yml fig.yml`
  * For details on configuration options see below
* `fig build` _(optional; will build automatically when starting via `fig up`)_
* If you're using boot2docker on OSX you need to ensure that you've mapped ports into your
  virtual machine running docker.

**Native**
* From project root `lein deps`


## Execution
**Docker**
* To start a development environment with an nREPL execute `fig up`
  * To connect to the nREPL you can use `fig run --rm app lein repl :connect <NREPL_PORT>`
    _(default port is 7888)_

**Native**
* To start a development environment with an nREPL execute `lein trapoline run dev`
  * To connect to the nREPL you can use `lein repl :connect <NREPL_PORT>` _(default port is 7888)_

From the project root you can execute `lein trampoline run main` to start the service.  If using
fig/docker `fig up` should get you going (note this will default to a development environment).

## Usage

Interaction with the service is done over HTTP via standard HTTP verbs:

* GET /:bucket/:key
* PUT /:bucket/:key {:data multi-part-file}
* DELETE /:bucket/:key

### Example using [cURL](https://github.com/bagder/curl)

```shell
echo "some data" > data.txt
curl -XPUT -F data=@data.txt http://localhost:3000/some-bucket/data.txt
> Successfully put some-bucket/data.txt

curl -XGET http://localhost:3000/some-bucket/data.txt
> some data
```

## Options

Options are configured via environment variables. Options below are also referenced as
clojure-standard map keys *(eg. :environ-var == ENVIRON_VAR)*.

* **:serve-port** (SERVE_PORT) - HTTP serve port
* **:nrepl-port** (NREPL_PORT) - nREPL port (only used for development)
* **:storage-aliases** (STORAGE_ALIASES) - Map with alias keys and adapter configuration values
  * **Example**: STORAGE_ALIASES='{:local [storage.store.FileStorage {:base-path "/storage"}]}'
  * **Adapters**:
    * `storage.store.FileStorage` - local file system
      * `{:base-path "/path/to/storage"}`
    * `storage.store.S3Storage` - Amazon S3 storage
      * `{:access-key "<access_key>" :secret-key "<secret_key>"}`
    * `storage.store.HTTPStorage` - HTTP storage (if you want to chain multiple service instances)
      * `{:base-url "http://url/to/storage"}`
* **:bucket-alias-map** (BUCKET_ALIAS_MAP) - Map with bucket keys and storage alias values
  * **Example**: BUCKET_ALIAS_MAP="{:some-bucket :local}"


## Reloader - Development Workflow

THis app supports hot code reloading for most purposes.  So instead of tearing down the whole JVM
every time you make a change you can instead connect to the nREPL and execute the (reload-app!)
function in the `user` namespace.  Here's a quick example:

* First start the server in dev mode (in a separate tab or background)
* Now fire up a REPL client and connect to the nREPL server:

```
Connecting to nREPL at 127.0.0.1:7888
REPL-y 0.3.5, nREPL 0.2.6
Clojure 1.6.0
OpenJDK 64-Bit Server VM 1.7.0_65-b32
    Docs: (doc function-name-here)
          (find-doc "part-of-name-here")
  Source: (source function-name-here)
 Javadoc: (javadoc java-object-or-class-here)
    Exit: Control+D or (exit) or (quit)
 Results: Stored in vars *1, *2, *3, an exception in *e

user=> !app
#<Ref@4086ba06: {:context {:storage #<store$gen_storage$reify__9373 storage.store$gen_storage$reify__9373@64243105>, :memcached nil}, :handler #<not_modified$wrap_not_modified$fn__2076 ring.middleware.not_modified$wrap_not_modified$fn__2076@50876874>, :server #<clojure.lang.AFunction$1@6d165176>, :serve-port 3000, :stop! #<core$reify__9466$fn__9467 storage.core$reify__9466$fn__9467@4774c9d0>, :app #<core$reify__9466 storage.core$reify__9466@77f14e32>}>
user=> reload-app!
#<core$bind_user_tools_BANG_$fn__9482 storage.core$bind_user_tools_BANG_$fn__9482@f4b52ce>
user=> (reload-app!)
Shutting down http storage app at Sat Jan 24 02:31:44 UTC 2015!
:reloading (storage.util storage.store storage.serve storage.core storage.core-test)
Starting http storage app at Sat Jan 24 02:31:45 UTC 2015 on port 3000!
{:context {:storage #<store$gen_storage$reify__9971 storage.store$gen_storage$reify__9971@2e0f9e70>, :memcached nil}, :handler #<not_modified$wrap_not_modified$fn__2076 ring.middleware.not_modified$wrap_not_modified$fn__2076@36ae2b0>, :server #<clojure.lang.AFunction$1@5e489290>, :serve-port 3000, :stop! #<core$reify__10064$fn__10065 storage.core$reify__10064$fn__10065@444d78a1>, :app #<core$reify__10064 storage.core$reify__10064@49d5099e>}
```

## Known Issues

* Sometimes you'll get strange errors when trying to run in dev mode, running `lein clean`
  genreally fixes it (or `fig run --rm app lein clean` if you don't have leingingen installed locally)

## Thanks

Thanks to all of the amazing work of clojure and java developers upon which this is built. It
took very little means to put this together due to the power and simplicity of the toolsets.

## License

Copyright © 2015 Jeremy Crosen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
