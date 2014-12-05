# storage

A simple bucket/key storage service with a super-simple HTTP API and configurable storage
adapters (currently file-system and S3).

## Installation

**Dependencies**
* [Leiningen](http://leiningen.org/) (Can be installed via homebrew on OSX)
* [JVM](https://java.com) (Any newer JVM should be fine)

**Optional**
* [Docker](https://www.docker.com/) (1.3 required for OSX shared folders)
* [fig](http://www.fig.sh/index.html)

Download the repo.  Optionally you may use docker and fig to build and start a development
environment via `fig up` assuming you have the right options configured (see below).

## Usage

From the project root you can execute `lein trampoline run main` to start the service.  If using
fig/docker `fig up` should get you going (note this will default to a development environment).

Interaction with the service is done over HTTP via standard HTTP verbs:

* GET /:bucket/:key
* PUT /:bucket/:key {:data multi-part-file}
* DELETE /:bucket/:key

## Options

Options are configured via environment variables. Options below are also referenced as clojure-standard map keys.  For example :environ-var corresponds to ENVIRON_VAR.

* **:http-port** (HTTP_PORT) - It's a port ya dingus!  Binds at runtime.
* **:file-storage-path** (FILE_STORAGE_PATH) - A base file path
* **:aws-access-key** (AWS_ACCESS_KEY) - An aws access key for S3 storage
* **:aws-secret-key** (AWS_SECRET_KEY) - An aws secret key for S3 storage
* **:bucket-storage-map** (BUCKET_STORAGE_MAP) - An edn-formatted map with bucket keywords as keys and storage adapter keywords as values
  * **Example**: BUCKET_STORAGE_MAP="{:some-bucket :file}"
  * **Adapters**:
    * **:s3** - An S3 storage adapter; the bucket must exist or operations will fail
    * **:file** - A file-system storage adapter that uses standard file i/o

## Examples

* First start the service

```shell
FILE_STORAGE_PATH=/tmp BUCKET_STORAGE_MAP="{:some-bucket :file}" SERVER_PORT=3000 lein trampoline run main
```

* Next you can use any HTTP client to interact, like `curl`

```shell
echo "some data" > data.txt
curl -XPUT -F data=@data.txt http://localhost:3000/some-bucket/data.txt
> Successfully put some-bucket/data.txt

curl -XGET http://localhost:3000/some-bucket/data.txt
> some data
```

**With Docker**
The only notes with docker are port and volume mappings.

* **Ports**: If you're on OSX you need to forward the ports in virtualbox in addition to the docker port mapping.
* **Volumes**: If you want to serve a local directory via the docker instance you need to map the local volume to a folder in the docker container and then configure the `FILE_STORAGE_PATH` as the same folder within the docker container.
  * For example, if instantiating a volume mapping as `-v /path/to/local/storage:/storage` then be sure that `FILE_STORAGE_PATH=/storage`.

## Development

Developing against this is pretty easy.  When starting the server instead of executing the `main` task
you specify `dev` instead which will start up an nREPL on port 7888 and add some helpful functions to
the `user` namespace so you can reload the application after making source changes without reloading
the whole JVM process!  Here's a quick example:

#### First start the server in dev mode (in a separate tab or background)
```shell
lein trampoline run dev &
```
_Fig/Docker_
```shell
fig up -d
```

#### Now fire up a REPL client
_Using Leiningen's repl_
```
lein repl :connect 7888
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
```

_From here you can inspect the current instance via `!app` and reload the whole application (including code changes) via `reload-app!`_
```
user=> !app
#<Ref@677c2c46: {:context {:storage #<store$gen_storage_proxy$reify__6557 storage.store$gen_storage_proxy$reify__6557@24ad3488>, :memcached nil}, :handler #<not_modified$wrap_not_modified$fn__2070 ring.middleware.not_modified$wrap_not_modified$fn__2070@33784d10>, :server #<clojure.lang.AFunction$1@4140e6f8>, :stop! #<core$reify__6639$fn__6640 storage.core$reify__6639$fn__6640@3e52f2e5>, :app #<core$reify__6639 storage.core$reify__6639@5abf3952>}>
user=> reload-app!
#<core$bind_user_tools_BANG_$fn__6662 storage.core$bind_user_tools_BANG_$fn__6662@9c7edce>
user=> (reload-app!)
Shutting down http storage server at Thu Dec 04 21:36:50 UTC 2014!
:reloading (storage.core storage.core-test)
Starting http storage server at Thu Dec 04 21:36:50 UTC 2014!
{:context {:storage #<store$gen_storage_proxy$reify__6557 storage.store$gen_storage_proxy$reify__6557@6cbc461a>, :memcached nil}, :handler #<not_modified$wrap_not_modified$fn__2070 ring.middleware.not_modified$wrap_not_modified$fn__2070@3c9e8cbb>, :server #<clojure.lang.AFunction$1@4d296814>, :stop! #<core$reify__7089$fn__7090 storage.core$reify__7089$fn__7090@506dd498>, :app #<core$reify__7089 storage.core$reify__7089@5b33ea53>}
```

## Bugs

...

## Thanks

Thanks to all of the amazing work of clojure and java developers upon which this is built. It
took very little means to put this together due to the power and simplicity of the toolsets.

## License

Copyright © 2014 Jeremy Crosen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
