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

From the project root you can execute `lein run` to start the service.

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
FILE_STORAGE_PATH=/tmp BUCKET_STORAGE_MAP="{:some-bucket :file}" HTTP_PORT=3000 lein run
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

### Bugs

...

### Thanks

Thanks to all of the amazing work of clojure and java developers upon which this is built. It
took very little means to put this together due to the power and simplicity of the toolsets.

## License

Copyright © 2014 Jeremy Crosen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
