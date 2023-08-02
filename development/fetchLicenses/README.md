# Fetch Licenses

A service that makes license files readable using headless Chrome.

### Setup

* This project uses TypeScript and Node.
* Download `fnm` and run `fnm install` to install a suitable version of Node.
* Run `npm install` to setup dependencies.
* Use Visual Studio code to debug and test.

### Testing

#### Local debugging
Run the web service locally using the provided package scripts.
Run `npm run-script debug` and this spins up a local web server. You can use Visual Studio code to attach to this process.
`nodemon` and `tsc --watch` compiles and restarts your service as you are making changes to the source code automatically in `debug` mode.

#### Example HTTP Request

```
curl -d '{"url": "https://opensource.org/licenses/bsd-license.php"}' -H 'Content-Type: application/json' -X POST 'http://localhost:8080/convert/licenses'
```

### Deploy

* Install the `gcloud` CLI.
* Run `npm run-script setupGcpProject` to setup credentials.
* Run `npm run-script deploy` to deploy the project to App Engine.
