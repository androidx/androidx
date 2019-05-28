# Fetch Licenses

A service that makes license files readable using headless Chrome.

### Setup

* This project uses TypeScript and Node.
* Download `nvm` and run `nvm install` to install a suitable version of Node.
* Install `yarn` using `npm install -g yarn`.
* Run `yarn install` to setup dependencies.
* Use Visual Studio code to debug and test.

### Testing

#### Local debugging
Run the web service locally using the provided package scripts.
Run `yarn debug` and this spins up a local web server. You can use Visual Studio code to attach to this process.
`nodemon` and `tsc --watch` compiles and restarts your service as you are making changes to the source code automatically in `debug` mode.

#### Example HTTP Request

```
curl -d '{"url": "https://opensource.org/licenses/bsd-license.php"}' -H 'Content-Type: application/json' -X POST 'http://localhost:8080/convert/licenses'
```

### Deploy

* Install the `gcloud` CLI.
* Run `yarn setupGcpProject` to setup credentials.
* Run `yarn deploy` to deploy the project to App Engine.
